// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// Generator: Checksum and file list generation methods.
// Copyright (C) 2001,2002  Casey Marshall <rsdio@metastatic.org>
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
// --------------------------------------------------------------------

package org.metastatic.rsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Checksum generation methods.
 * 
 * @version $Revision$
 */
public class Generator implements RsyncConstants {

   // Constants and variables.
   // ------------------------------------------------------------------------

   protected Configuration config;
   protected RollingChecksum weakSum;

   // Constructors.
   // ------------------------------------------------------------------------

   public Generator() {
      this(new Configuration());
   }

   public Generator(Configuration config) {
      this.config = config;
      weakSum = new RollingChecksum();      
   }

   // Instance methods.
   // ------------------------------------------------------------------------

   /**
    * Generate checksums over an entire byte array, with a base offset
    * of 0.
    *
    * @param buf The byte buffer to checksum.
    * @return A {@link java.util.Collection} of {@link ChecksumPair}s
    *    generated from the array.
    * @see #generateSums(byte[],int,int,long)
    */
   public Collection generateSums(byte[] buf) {
      return generateSums(buf, 0, buf.length, 0);
   }

   /**
    * Generate checksums over a portion of a byte array, with a base
    * offset of 0.
    *
    * @param buf The byte array to checksum.
    * @param off The offset in <code>buf</code> to begin.
    * @param len The number of bytes to checksum.
    * @return A {@link java.util.Collection} of {@link ChecksumPair}s
    *    generated from the array.
    * @see #generateSums(byte[],int,int,long)
    */
   public Collection generateSums(byte[] buf, int off, int len) {
      return generateSums(buf, off, len, 0);
   }

   /**
    * Generate checksums over an entire byte array, with a specified
    * base offset. This <code>baseOffset</code> is added to the offset
    * stored in each {@link ChecksumPair}.
    *
    * @param buf        The byte array to checksum.
    * @param baseOffset The offset from whence this byte array came.
    * @return A {@link java.util.Collection} of {@link ChecksumPair}s
    *    generated from the array.
    * @see #generateSums(byte[],int,int,long)
    */
   public Collection generateSums(byte[] buf, long baseOffset) {
      return generateSums(buf, 0, buf.length, baseOffset);
   }

   /**
    * Generate checksums over a portion of abyte array, with a specified
    * base offset. This <code>baseOffset</code> is added to the offset
    * stored in each {@link ChecksumPair}.
    *
    * @param buf        The byte array to checksum.
    * @param off        From whence in <code>buf</code> to start.
    * @param len        The number of bytes to check in
    *                   <code>buf</code>.
    * @param baseOffset The offset from whence this byte array came.
    * @return A {@link java.util.Collection} of {@link ChecksumPair}s
    *    generated from the array.
    */
   public Collection
   generateSums(byte[] buf, int off, int len, long baseOffset) {
      int count = (len+(config.blockLength-1)) / config.blockLength;
      int remainder = len % config.blockLength;
      int offset = off;
      Collection sums = new ArrayList(count);

      for (int i = 0; i < count; i++) {
         int n = Math.min(len, config.blockLength);
         ChecksumPair pair = generateSum(buf, offset, n, offset+baseOffset);
         pair.seq = i;

         sums.add(pair);
         len -= n;
         offset += n;
      }

      return sums;
   }

   /**
    * Generate checksums for an entire file.
    *
    * @param f The {@link java.io.File} to checksum.
    * @return A {@link java.util.Collection} of {@link ChecksumPair}s
    *    generated from the file.
    * @throws java.io.IOException if <code>f</code> cannot be read from.
    */
   public Collection generateSums(File f) throws IOException {
      long len = f.length();
      int count = (int) ((len+(config.blockLength+1)) / config.blockLength);
      long offset = 0;
      FileInputStream fin = new FileInputStream(f);
      Collection sums = new ArrayList(count);
      int n = (int) Math.min(len, config.blockLength);
      byte[] buf = new byte[n];

      for (int i = 0; i < count; i++) {
         int l = fin.read(buf, 0, n);
         if (l == -1) break;
         ChecksumPair pair = generateSum(buf, 0, Math.min(l, n), offset);
         pair.seq = i;

         sums.add(pair);
         len -= n;
         offset += n;
         n = (int) Math.min(len, config.blockLength);
      }

      fin.close();
      return sums;
   }

   /**
    * Generate checksums for an InputStream.
    *
    * @param in The {@link java.io.InputStream} to checksum.
    * @return A {@link java.util.Collection} of {@link ChecksumPair}s
    *    generated from the bytes read.
    * @throws java.io.IOException if reading fails.
    */
   public Collection generateSums(InputStream in) throws IOException {
      Collection sums = null;
      byte[] buf = new byte[config.blockLength*config.blockLength];
      long offset = 0;
      int len = 0;

      while ((len = in.read(buf)) != -1) {
         if (sums == null) {
            sums = generateSums(buf, 0, len, offset);
         } else {
            sums.addAll(generateSums(buf, 0, len, offset));
         }
         offset += len;
      }

      return sums;
   }

   /**
    * Generate a sum pair for an entire byte array.
    *
    * @param buf The byte array to checksum.
    * @param fileOffset The offset in the original file from whence
    *    this block came.
    * @return A {@link ChecksumPair} for this byte array.
    */
   public ChecksumPair generateSum(byte[] buf, long fileOffset) {
      return generateSum(buf, 0, buf.length, fileOffset);
   }

   /**
    * Generate a sum pair for a portion of a byte array.
    * 
    * @param buf The byte array to checksum.
    * @param off Where in <code>buf</code> to start.
    * @param len How many bytes to checksum.
    * @param fileOffset The original offset of this byte array.
    * @return A {@link ChecksumPair} for this byte array.
    */
   public ChecksumPair
   generateSum(byte[] buf, int off, int len, long fileOffset) {
      ChecksumPair p = new ChecksumPair();
      weakSum.check(buf, off, len);
      config.strongSum.update(buf, off, len);
      if (config.checksumSeed != null) {
         config.strongSum.update(config.checksumSeed, 0,
            config.checksumSeed.length);
      }
      p.weak = new Integer(weakSum.getValue());
      p.strong = new byte[config.strongSumLength];
      System.arraycopy(config.strongSum.digest(), 0, p.strong, 0,
         p.strong.length);
      p.offset = new Long(fileOffset);
      p.length = len;
      return p;
   }
}
