/* NonblockingReceiver -- NIO receiver process.
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2, or (at your option) any
later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with Jarsync; see the file COPYING.  If not, write to the

   Free Software Foundation Inc.,
   59 Temple Place - Suite 330,
   Boston, MA 02111-1307
   USA  */

package org.metastatic.rsync.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

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
import org.metastatic.rsync.MappedRebuilderStream;
import org.metastatic.rsync.RebuilderEvent;
import org.metastatic.rsync.RebuilderListener;
import org.metastatic.rsync.RebuilderStream;

final class NonblockingReceiver implements NonblockingTool, Constants,
   GeneratorListener, RebuilderListener
{

   // Constants and fields.
   // -----------------------------------------------------------------------

   private static final int MAP_LIMIT = 32768;
   private static final int MAP_SIZE  = 1048576;

   private final Configuration config;
   private final Options options;
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
   private final int origBlockLength;

   // Generator process's data.
   private FileInfo genFile;
   private File sigFile;
   private FileInputStream sigIn;
   private byte[] genBuffer;

   // Receiver process's data.
   private FileInfo recvFile;
   private File basisFile;
   private RandomAccessFile basisIn;
   private File targetFile;
   private File tempfile;
   private RandomAccessFile targetOut;
   private MappedByteBuffer targetMap;
   private long recvOffset = 0L;
   private final byte[] recvBuffer = new byte[8192];

   private final GeneratorStream generator;
   private final MappedRebuilderStream rebuilder;

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
      generator = new GeneratorStream(config);
      generator.addListener(this);
      rebuilder = new MappedRebuilderStream(config);
      rebuilder.addListener(this);
      origBlockLength = config.blockLength;
   }

   // NonblockingTool implementation.
   // -----------------------------------------------------------------------

   public void setBuffers(DuplexByteBuffer outBuffer, ByteBuffer inBuffer) {
      this.outBuffer = outBuffer;
      this.inBuffer = inBuffer;
   }

   public void updateInput() throws Exception {
      switch (state & INPUT_MASK) {
         case RECV_RECEIVE_INDEX:
            recvIndex = inBuffer.getInt();
            if (recvIndex == -1) {
               if (phase > 0) {
                  state = (state & OUTPUT_MASK) | RECV_RECEIVE_DONE;
                  return false;
               } // else in-state is still RECV_RECEIVE_INDEX.
            } else {
               state = (state & OUTPUT_MASK) | RECV_RECEIVE_DELTAS;
            }
            return true;

         case RECV_RECEIVE_DELTAS:
            receiveDeltas();
            return true;

         case RECV_RECEIVE_DONE:
            return false;

         default:
            return true;
      }
   }

   public void updateOutput() throws Exception {
      switch (state & OUTPUT_MASK) {
         case RECV_SEND_INDEX:
            if (phase == 0) {
               if (genIndex < files.size()) {
                  outBuffer.putInt(genIndex);
                  state = RECV_SEND_SUMS | RECV_RECEIVE_INDEX;
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
                  state = (state * INPUT_MASK) | RECV_SEND_DONE;
               }
            }
            return true;

         case RECV_SEND_SUMS:
            sendSums();
            return true;

         case RECV_SEND_DONE:
            return false;

         default:
            return true;
      }
   }

   // GeneratorListener implementation.
   // -----------------------------------------------------------------------

   public void update(GeneratorEvent event) {
      ChecksumPair pair = event.getChecksumPair();
      if (options.verbose > 3)
         logger.info("updating sum=" + pair);
      outBuffer.putInt(pair.getWeak());
      outBuffer.put(pair.getStrong());
   }

   // ReceiverListener interface.
   // -----------------------------------------------------------------------

   public void update(RebuilderEvent event) {
      long off = event.getOffset();
      byte[] buf = event.getData();
      if (targetMap != null) {
         if (mapOffset < off ||
             mapOffset + buf.length > mapOffset + targetMap.capacity()) {
            targetMap = targetOut.map(FileChannel.MapMode.READ_WRITE,
               off, MAP_SIZE);
            mapOffset = off;
         }
         targetMap.position((int) (off - mapOffset)).put(buf);
      } else {
         targetOut.seek(off);
         targetOut.write(buf);
      }
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private void sendSums() {
      if (genFile == null) {
         genFile = files.get(genIndex);
         try {
            config.blockLength = origBlockLength;
            sigFile = new File(path, file.dirname + File.separator
               + file.basename);
            sigIn = new FileInputStream(basisFile);
            config.blockLength = RsyncUtil.adaptBlockSize(sigFile,
               config.blockLength);
            if (genBuffer == null || genBuffer.length != config.blockLength)
               genBuffer = new byte[Math.min(config.blockLength, 1024)];
            int count = (int) (sigFile.length() + config.blockLength - 1)
               / config.blockLength;
            int remainder = (int) (basisFile.length() % config.blockLength);
            if (options.verbose > 3)
               logger.info("count=" + count + " rem=" + remainder + " n=" +
                  config.blockLength + " flength=" + basisFile.length());
            outBuffer.putInt(count);
            outBuffer.putInt(config.blockLength);
            outBuffer.putInt(remainder);
         } catch (FileNotFoundException ioe) {
            genFile = null;
            sigFile = null;
            outBuffer.putInt(0);
            outBuffer.putInt(config.blockLength);
            outBuffer.putInt(0);
            state = (state & INPUT_MASK) | RECV_SEND_INDEX;
         }
      } else {
         int len = basisIn.read(genBuffer);
         if (len == -1) {
            basisIn.close();
            genFile = null;
            state = (state & INPUT_MASK) | RECV_SEND_INDEX;
            return;
         }
         generator.update(genBuffer, 0, len);
      }
   }

   private void receiveDeltas() {
      if (recvFile == null) {
         recvFile = files.get(recvIndex);
         basisFile = new File(path, recvFile.dirname + File.separator
            + recvFile.baseName);
         if (basisFile.exists())
            rebulider.setBasisFile(basisFile);
         else
            rebuilder.setBasisFile(null);
         tempfile = File.createTempFile("." + recvFile.basename, "",
            new File(path));
         targetOut = new RandomAccessFile(tempfile, "rw");
         targetOut.setLength(recvFile.length);
         if (recvFile.length > MAP_LIMIT) {
            targetMap = targetOut.getChannel().map(
               FileChannel.MapMode.READ_WRITE, 0, MAP_SIZE);
            mapOffset = 0L;
         }
      } else {
         if (residue > 0) {
            int len = Math.min(residue, Math.min(inBuffer.remaining(),
               recvBuffer.length));

         } else {
            int tag = inBuffer.getInt();
            if (tag < 0) {
               long offset = (-token + 1) * (long) config.blockLength;
               int len = (int) (basisFile.length()-offset) < config.blockLength
                  ? (int) (basisFile.length() - offset) : config.blockLength;
               rebuilder.update(new Offsets(offset, recvOffset, len));
               recvOffset += len;
            } else if (tag > 0) {
               residue = tag;
               int len = Math.min(residue, Math.min(inBuffer().remaining,
                  recvBuffer.length));
               inBuffer.get(recvBuffer, 0, len);
               if (len <= residue)
                  residue -= len;
               else
                  residue = 0;
               rebuilder.update(new DataBlock(recvOffset, recvBuffer, 0, len));
               recvOffset += len;
            } else {
               targetOut.close();
               tempfile.renameTo(basisFile);
            }
         }
      }
   }
}
