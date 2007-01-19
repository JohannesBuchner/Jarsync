/* NonblockingSender -- NIO version of sender process.
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with Jarsync; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.rsync.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import java.security.MessageDigest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.metastatic.rsync.ChecksumPair;
import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.DataBlock;
import org.metastatic.rsync.Delta;
import org.metastatic.rsync.ListenerException;
import org.metastatic.rsync.MatcherEvent;
import org.metastatic.rsync.MatcherListener;
import org.metastatic.rsync.MatcherStream;
import org.metastatic.rsync.Offsets;
import org.metastatic.rsync.Util;

final class NonblockingSender implements NonblockingTool, Constants,
   MatcherListener
{

  // Constants and fields.
  // -------------------------------------------------------------------------

  private final Options options;
  private final Configuration config;
  private final List files;
  private final String path;
  private final Logger logger;
  private final int remoteVersion;
  private int state;

  private int phase;
  private int index;

  private int remainder, i, count;
  private long offset;
  private List sums;
  private MatcherStream matcher;
  private final MessageDigest file_sum;
  private FileInfo file;
  private FileInputStream fin;
  private final byte[] buf = new byte[4096];
  private Statistics stats;

  private DuplexByteBuffer outBuffer;
  private ByteBuffer inBuffer;

  // Constructors.
  // -------------------------------------------------------------------------

  NonblockingSender(Options options, Configuration config, List files,
                    String path, Logger logger, int remoteVersion)
  {
    this.options = options;
    this.config = config;
    this.files = files;
    this.path = path;
    this.logger = logger;
    this.remoteVersion = remoteVersion;
    this.state = SENDER_RECEIVE_INDEX;
    phase = 0;
    matcher = new MatcherStream(config);
    matcher.addListener(this);
    stats = new Statistics();
    try
      {
        file_sum = MessageDigest.getInstance("BrokenMD4");
      }
    catch (Exception x)
      {
        throw new Error(x);
      }
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public void setStatistics(Statistics stats)
  {
    this.stats = stats;
  }

  // BufferTool implementation.
  // -------------------------------------------------------------------------

  public void setBuffers(DuplexByteBuffer out, ByteBuffer in)
  {
    this.outBuffer = out;
    this.inBuffer = in;
  }

  public boolean updateInput() throws Exception
  {
    switch (state)
      {
      case SENDER_RECEIVE_INDEX:
        index = inBuffer.getInt();
        logger.debug("read index=" + index);
        if (index == -1)
          {
            if (phase == 0)
              {
                phase++;
                config.strongSumLength = SUM_LENGTH;
                outBuffer.putInt(-1);
                outBuffer.flush();
                if (options.verbose > 2)
                  logger.info("send_files phase=" + phase);
              }
            else
              {
                outBuffer.putInt(-1);
                outBuffer.flush();
                state = SENDER_DONE;
                return false;
              }
            return true;
          }
        if (index < 0 || index > files.size())
          throw new ProtocolException("Invalid file index " +
                                      index + " (count=" + files.size() + ")");
        state = SENDER_RECEIVE_SUMS;
        return true;

      case SENDER_RECEIVE_SUMS:
        receiveSums();
        return true;

      case SENDER_DONE:
        return false;

      default:
        return true;
      }
  }

  public boolean updateOutput() throws Exception
  {
    switch (state)
      {
      case SENDER_SEND_DELTAS:
        sendDeltas();
        return true;

      case SENDER_DONE:
        return false;

      default:
        return true;
      }
  }

  // MatcherListener implementation.
  // -------------------------------------------------------------------------

  public void update(MatcherEvent e)
  {
    Delta d = e.getDelta();
    if (d instanceof DataBlock)
      {
        if (options.verbose > 3 )
          logger.info("literal data at " + d.getWriteOffset() +
                      " len=" + d.getBlockLength());
        outBuffer.putInt(d.getBlockLength());
        outBuffer.put(((DataBlock) d).getData());
      }
    else
      {
        if (options.verbose > 3)
          logger.info("matched data=" + d);
        int token = (int) (((Offsets) d).getOldOffset() / config.blockLength);
        outBuffer.putInt(-(token+1));
      }
    outBuffer.flush();
  }

  // Own methods.
  // -------------------------------------------------------------------------

  private void receiveSums()
  {
    if (sums == null)
      {
        if (inBuffer.remaining() < 12)
          throw new BufferUnderflowException();
        i = 0;
        offset = 0;
        count = inBuffer.getInt();
        sums = new ArrayList(count);
        config.blockLength = inBuffer.getInt();
        remainder = inBuffer.getInt();
        logger.debug("recv_sums count=" + count + " n=" + config.blockLength
                     + " remainder=" + remainder);
      }
    else
      {
        if (i < count)
          {
            if (inBuffer.remaining() < 4 + config.strongSumLength)
              throw new BufferUnderflowException();
            int sum1 = inBuffer.getInt();
            byte[] sum2 = new byte[config.strongSumLength];
            inBuffer.get(sum2);
            ChecksumPair pair = new ChecksumPair(sum1, sum2, offset,
              (i == count-1 && remainder != 0) ? remainder
               : config.blockLength, i);
            sums.add(pair);
            offset += pair.getLength();
            if (options.verbose > 3)
              logger.info("chunk[" + i + "] " + pair);
            i++;
          }
        if (i == count)
          {
            file_sum.reset();
            if (config.checksumSeed != null)
              {
                file_sum.update(config.checksumSeed);
              }
            file = (FileInfo) files.get(index);
            String fname = path + File.separator;
            if (file.dirname.length() > 0)
              fname += file.dirname + File.separator;
            fname += file.basename;
            logger.debug("about to match " + fname);
            try
              {
                fin = new FileInputStream(fname);
                if (count > 0)
                  matcher.setChecksums(sums);
                sums = null;
                outBuffer.putInt(index);
                outBuffer.putInt(count);
                outBuffer.putInt(config.blockLength);
                outBuffer.putInt(remainder);
                outBuffer.flush();
                state = SENDER_SEND_DELTAS;
              }
            catch (IOException ioe)
              {
                logger.warn("error reading " + fname + ": " + ioe.getMessage());
                sums = null;
                state = SENDER_RECEIVE_INDEX;
              }
          }
      }
  }

  private void sendDeltas() throws IOException
  {
    int len = fin.read(buf);
    if (len == -1)
      {
        if (count > 0)
          {
            try
              {
                matcher.doFinal();
              }
            catch (ListenerException le)
              {
              }
          }
        if (options.verbose > 2)
          logger.info("sending file_sum");
        file.sum = file_sum.digest();
        outBuffer.putInt(0);
        outBuffer.put(file.sum);
        outBuffer.flush();
        fin.close();
        state = SENDER_RECEIVE_INDEX;
        return;
      }
    stats.total_size += len;
    file_sum.update(buf, 0, len);
    if (count > 0)
      {
        try
          {
            matcher.update(buf, 0, len);
          }
        catch (ListenerException le)
          {
          }
      }
    else
      {
        outBuffer.putInt(len);
        outBuffer.put(buf, 0, len);
      }
  }
}
