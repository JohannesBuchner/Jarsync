// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// Receiver -- File receiving methods.
// Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the
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

public class Receiver {

   // Constants and fields.
   // -----------------------------------------------------------------------

   private static final Logger logger =
      Logger.getLogger(Receiver.class.getName());

   private final MultiplexedInputStream in;
   private final MultiplexedOutputStream out;

   private Statistics stats;

   private final Configuration config;

   private final Generator generator;

   private final int remoteVersion;

   private int[] retry;

   private int retryIndex;

   // Constructors.
   // -----------------------------------------------------------------------

   public Receiver(MultiplexedInputStream in, MultiplexedOutputStream out,
                   Configuration config, int remoteVersion, boolean amServer)
   {
      this.in = in;
      this.out = out;
      if (amServer)
         logger.addAppender(new RsyncAppener(out));
      this.config = config;
      generator = new Generator(config);
      this.remoteVersion = remoteVersion;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public Statistics getStatistics()
   {
      return stats;
   }

   public void setStatistics(Statistics stats)
   {
      if (stats != null) this.stats = stats;
   }

   public void generateFiles(List files) throws IOException 
   {
      int phase = 0;

      retryIndex = 0;
      retry = new int[files.size()];
      for (int i = 0; i < retry.length; i++)
         retry[i] = -1;

      for (int i = 0; i <= files.count; i++) {
         sendSums((File) files.get(i), i);
      }

      phase++;
      out.writeInt(-1);
      logger.info("generateFiles phase=" + phase);
      config.setStrongSumLength(SUM_LENGTH);

      if (remoteVersion >= 13) {
         // in newer versions of the protocol the files can cycle
         // through the system more than once to catch initial checksum
         // errors.
         //
         // Rsync uses a socket with two processes talking to one
         // another. Here, since we are running two threads with the
         // same object, we just use an array of integers.
         for (int i = 0; i < retryIndex && retry[i] != -1; i++) {
            sendSums(files.get(retry[i]), retry[i]);
         }
         phase++;
         logger.info("generateFiles phase=" + phase);
         out.writeInt(-1);
      }
   }

   public void receiveFiles(List files) throws IOException
   {
      logger.info("receiveFiles starting thread=" + Thread.currentThread());
      
      int phase = 0;

      while (true) {
         int i = in.readInt();
         if (i == -1) {
            if (phase == 0 && remoteVersion >= 13) {
               phase++;
               logger.info("receiveFiles phase=" + phase);
               continue;
            }
            break;
         }

         if (i < 0 || i >= files.size()) {
            String msg = "Invalid file index " + i + " in receiveFiles count="
               + files.size();
            logger.fatal(msg);
            throw new IOException(msg);
         }

         File f = (File) files.get(i);

         stats.num_transferred_files++;
         stats.total_transferred_size += f.length();

         if (!receiveData(f)) {
            if (config.getStrongSumLength() == SUM_LENGTH) {
               logger.error("File corruption in " + f.getName()
                  + ". File changed during transfer?");
            } else {
               // We need to retry this file. See the generateFiles
               // method above for how these integers are used.
               logger.warn("redoing " + f.getName() + "(" + i + ")");
               retry[retryIndex++] = i;
            }
         }
      }

      logger.info("receiveFiles finished");
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private void sendSums(File f, int i) throws IOException
   {
      // XXX fiddle with permissions.
      if (config.getBlockLength() == BLOCK_LENGTH) {
         int l = (int) (f.length() / 10000) & ~15;
         if (l < config.getBlockLength())
            l = config.getBlockLength();
         if (l > CHUNK_SIZE / 2)
            l = CHUNK_SIZE / 2;
         config.setBlockSize(l);
      }
      int rem = (int) (f.length() % config.getBlockSize());
      Collection sums = generator.generateSums(f);

      out.writeInt(i);
      out.writeInt(sums.size());
      out.writeInt(config.getBlockLength());
      out.writeInt(rem);

      for (Iterator it = sums.iterator(); it.hasNext(); ) {
         CheksumPair p = (ChecksumPair) it.next();
         out.writeInt(p.getWeakSum());
         out.write(p.getStrongSum());
      }
   }

   private boolean receiveData(File f) throws IOException
   {
      int count = in.readInt();
      int n = in.readInt();
      int remainder = in.readInt();
      long offset = 0;
      byte[] file_sum1, file_sum2;
      byte[] data = new byte[CHUNK_SIZE];
      List deltas = new LinkedList();

      for (int i = receiveToken(data); i != 0; i = receiveToken(data)) {
         if (i > 0) {
            logger.debug("data received " + i + " at " + offset);
            deltas.add(new DataBlock(offset, data));
            offset += i;
            stats.literal_data += i;
         } else {
            i = -(i+1);
            long offset2 = (long) i * (long) n;
            int len = n;
            if (i == count - 1 && remainder != 0)
               len = remainder;

            logger.debug("chunk[" + i + "] of size " + len + " at "
               + offset2 + " offset=" + offset);
            deltas.add(new Offsets(offset, offset2, len));
            offset += len;
            stats.matched_data += len;
         }
      }

      File newf = Rebuilder.rebuildFile(f, deltas);
      newf.renameTo(f);

      if (remoteVersion >= 14) {
         MessageDigest md = null;
         try {
            md = MessageDigest.getInstance("BrokenMD4");
         } catch (NoSuchAlgorithmException nsae) {
            throw new Error(nsae);
         }
         FileInputStream fin = new FileInputStream(f);
         int i = 0;
         while ((i = fin.read(data)) != -1) {
            md.update(data, 0, i);
         }
         file_sum1 = md.digest();
         file_sum2 = new byte[file_sum2.length];
         in.read(file_sum2);
         return Arrays.areEqual(file_sum1, file_sum2);
      }

      return true;
   }

   private int residue = 0;

   private int receiveToken(byte[] buf) throws IOException
   {
      // XXX compression.
      if (residue == 0) {
         int i = in.readInt();
         if (i <= 0) return i;
         residue = i;
      }

      int n = Math.min(CHUNK_SIZE, residue);
      residue -= n;
      in.read(buf, 0, n);
      return n;
   }
}
