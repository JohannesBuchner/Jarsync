/* vim:set softtabstop=3 tabstop=3 shiftwidth=3 expandtab tw=72:
   $Id$

   NonblockingReceiver: NIO receiver process.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync.

   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2, or (at your option) any
   later version.

   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jarsync; see the file COPYING.  If not, write to the

      Free Software Foundation Inc.,
      59 Temple Place - Suite 330,
      Boston, MA 02111-1307
      USA  */

package org.metastatic.rsync.v2;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.GeneratorEvent;
import org.metastatic.rsync.GeneratorListener;
import org.metastatic.rsync.GeneratorStream;
import org.metastatic.rsync.RebuilderEvent;
import org.metastatic.rsync.RebuilderListener;
import org.metastatic.rsync.RebuilderStream;

final class NonblockingReceiver implements NonblockingTool, Constants,
   GeneratorListener, RebuilderListener
{

   // Fields.
   // -----------------------------------------------------------------------

   private final Configuration config;
   private final Options options;
   private final Logger logger;
   private final List files;
   private final Map uids, gids;
   private final int remoteVersion;
   private final Logger logger;

   private int state;
   private int phase;
   private int genIndex;
   private int recvIndex;
   private int[] retries;
   private int retryIndex;
   private int nRetries;

   private FileInfo file;
   private UnixFile basisFile;
   private RandomAccessFile basisIn;
   private MappedByteBuffer basisMap;
   private UnixFile targetFile;
   private RandomAccessFile targetOut;
   private MappedByteFile targetMap;

   private GeneratorStream generator;
   private MappedRebuilderStream rebuilder;

   private DuplexByteBuffer outBuffer;
   private ByteBuffer inBuffer;

   // Constructor.
   // -----------------------------------------------------------------------

   NonblockingReceiver(Options options, Configuration config, List files,
      Map uids, Map gids, int remoteVersion, Logger logger)
   {
      this.options = options;
      this.config = config;
      this.files = files;
      this.uids = uids;
      this.gids = gids;
      this.remoteVersion = remoteVersion;
      this.logger = logger;
      genIndex = 0;
      recvIndex = 0;
      retries = new int[files.size()];
      retryIndex = 0;
      nRetries = 0;
      phase = 0;
      state = RECV_SEND_INDEX;
   }

   // NonblockingTool implementation.
   // -----------------------------------------------------------------------

   public void setBuffers(DuplexByteBuffer outBuffer, ByteBuffer inBuffer) {
      this.outBuffer = outBuffer;
      this.inBuffer = inBuffer;
   }

   public void updateInput() throws Exception {
      switch (state & INPUT_MASK) {
         case RECV_
      }
   }

   public void updateOutput() throws Exception {
      switch (state & OUTPUT_MASK) {
         case RECV_SEND_INDEX:
            if (phase == 0) {
               if (genIndex < files.size()) {
                  outBuffer.putInt(genIndex);
                  state = RECV_SEND_SUMS | RECV_RECEIVE_DELTAS;
               } else {
                  config.strongSumLength = SUM_LENGTH;
                  outBuffer.putInt(-1);
                  phase++;
                  if (options.verbose > 2)
                     logger.info("receive files phase=" + phase);
               }
            } else {
               if (retryIndex < nRetries) {
                  genIndex = retries[retryIndex++];
                  outBuffer.put(index);
                  state = RECV_SEND_SUMS | RECV_RECEIVE_DELTAS;
               } else {
                  if (options.verbose > 2)
                     logger.info("receive files finished");
                  outBuffer.putInt(-1);
                  state = RECV_DONE;
               }
            }
            return true;

         case RECV_SEND_SUMS:
      }
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private void sendSums() {
      if (file == null) {
         file = files.get(genIndex);
         try {
            config.blockLength = origBlockLength;
            config.blockLength = RsyncUtil.adaptBlockSize(config.blockLength);
            basisFile = new UnixFile(path, file.dirname + File.separator
               + file.basename);
            basisIn = new FileInputStream(basisFile);
            rebuilder.setBasisFile(basisFile);
            tempfile = File.createTempFile("." + file.basename, "",
               new File(path));
            targetFile = new RandomAccessFile(tempfile);
            if (file.length > MAP_LIMIT)
               targetMap = targetFile.getChannel().map(
                  FileChannel.MapMode.READ_WRITE, 0,
                  Math.min(Integer.MAX_VALUE, file.length));
         } catch (FileNotFoundException ioe) {
            outBuffer.putInt(0);
         }
      } else {
         basisIn.read(genBuffer);
         generator.update(
      }
   }
}
