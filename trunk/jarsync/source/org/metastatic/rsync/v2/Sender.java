// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// Sender -- File-sending methods.
// Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of Jarsync.
//
// Jarsync is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// Jarsync is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Jarsync; if not, write to the
//
//    Free Software Foundation, Inc.,
//    59 Temple Place, Suite 330,
//    Boston, MA  02111-1307
//    USA
//
// Linking this library statically or dynamically with other modules is
// making a combined work based on this library.  Thus, the terms and
// conditions of the GNU General Public License cover the whole
// combination.
//
// As a special exception, the copyright holders of this library give
// you permission to link this library with independent modules to
// produce an executable, regardless of the license terms of these
// independent modules, and to copy and distribute the resulting
// executable under terms of your choice, provided that you also meet,
// for each linked independent module, the terms and conditions of the
// license of that module.  An independent module is a module which is
// not derived from or based on this library.  If you modify this
// library, you may extend this exception to your version of the
// library, but you are not obligated to do so.  If you do not wish to
// do so, delete this exception statement from your version.
//
// --------------------------------------------------------------------------

/*
 * Based on rsync-2.5.5.
 * 
 * Rsync Copyright (C) 1992-2001 Andrew Tridgell
 *                     1996 Paul Mackerras
 *                     2001, 2002 Martin Pool
 *                     and others.
 */

package org.metastatic.rsync.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.metastatic.org.*;

import org.apache.log4j.*;

public class Sender {

   // Constants and variables.
   // -----------------------------------------------------------------------

   private static Logger logger = Logger.getLogger(Sender.class.getName());

   private final Configuration config;

   private final MultiplexedInputStream in;
   private final MultiplexedOutputStream out;

   private Statistics stats;

   private final int remoteVersion;

   // Constructors.
   // -----------------------------------------------------------------------

   /**
    * Create a new Sender object.
    *
    * @param in The underlying input stream.
    * @param out The underlying output stream.
    * @param config The configuration to use.
    * @param remoteVersion The remote protocol version.
    * @param amServer Should be true if we are the server.
    */
   public Rsync(MultiplexedInputStream in, MultiplexedOutputStream out,
                Configuration config, int remoteVersion, boolean amServer)
   {
      this.in = in;
      this.out = out;
      if (amServer)
         logger.addAppender(new RsyncAppender(this.out));
      this.config = config;
      this.remoteVersion = remoteVersion;
      stats = new Statistics();
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void setStatistics(Statistics newStats) {
      if (newStats != null) stats = newStats;
   }

   public Statistics getStatistics() {
      return stats;
   }

   /**
    * Send the set of files.
    */
   public void sendFiles(List files) throws IOException {
      logger.info("sendFiles starting");

      int i;
      int phase = 0;

      while (true) {
         int offset = 0;

         i = in.readInt();
         if (i == -1) {
            if (phase == 0 && remote_version >= 13) {
               phase++;
               config.setStrongSumLength(RsyncConstants.SUM_LENGTH);
               out.writeInt(-1);
               logger.info("sendFiles phase=" + phase);
               continue;
            }
            break;
         }

         if (i < 0 || i >= files.size()) {
            String msg = "invalid file index " + i + " (count=" +
               files.size() + ")");
            logger.fatal(msg);
            throw new IOException(msg);
         }

         File file = (File) files.get(i);

         stats.num_transferred_files++;
         stats.total_transferred_size += file.length();

         List sums = receiveSums();
         Collection deltas = new Matcher(config).hashSearch(sums, file);

         sendDeltas(deltas);
      }

      out.writeInt(-1);
      logger.info("sendFiles finished");
   }

 // Own methods.
   // -----------------------------------------------------------------------

   private List receiveSums() throws IOException {
      int count = in.readInt();
      int n = in.readInt();
      int remainder = in.readInt();
      long offset = 0;

      logger.debug("count=" + count + " n=" + n + " rem=" + remainder);

      if (count < 0) {
         throw new IOException("bad sum count " + count);
      }

      if (count == 0) return null;

      config.setBlockSize(n);
      List sums = new ArrayList(count);

      for (int i = 0; i < count; i++) {
         int weak = in.readInt();
         byte[] strong = new byte[config.getStrongSumLength()];
         in.read(strong);
         ChecksumPair pair = null;

         if (i == count - 1 && remainder > 0) {
            pair = new ChecksumPair(weak, strong, offset, remainder, i);
            offset += remainder;
         } else {
            pair = new ChecksumPair(weak, strong, offset, n, i);
            offset += n;
         }

         logger.debug("chunk[" + i + "] " + pair);

         sums.add(pair);
      }

      return sums;
   }

   private void sendDeltas(Collection deltas) throws IOException {
      // XXX compression
      int copy = 0;
      for (Iterator i = deltas.iterator(); i.hasNext(); ) {
         Delta d = (Delta) i.next();
         if (d instanceof DataBlock) {
            byte[] block = ((DataBlock) d).getData();
            int n = block.length;
            int l = 0;
            while (l < n) {
               int n1 = Math.min(CHUNK_SIZE, n-l);
               out.writeInt(n1);
               out.write(block, l, n1);
            }
         } else {    // Offsets
            int token = (int) (((Offsets) d).getOldOffset() % d.getBlockLength());
            out.writeInt(-(token+1));
         }
      }
   }
}
