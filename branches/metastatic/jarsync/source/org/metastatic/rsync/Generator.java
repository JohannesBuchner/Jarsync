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
import java.io.IOException;
import java.io.OutputStream;

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

   protected MD4 strongSum;
   protected RollingChecksum weakSum;
   protected int blockLength;

   // Constructors.
   // ------------------------------------------------------------------------

   public Generator() {
      this(BLOCK_LENGTH);
   }

   public Generator(int blockLength) {
      this.blockLength = blockLength;
      strongSum = new MD4();
      weakSum = new RollingChecksum();
   }

   // Instance methods.
   // ------------------------------------------------------------------------

   public Collection generateSums(byte[] buf) {
      return generateSums(buf, 0, buf.length, 0);
   }

   public Collection generateSums(byte[] buf, int off, int len) {
      return generateSums(buf, off, len, 0);
   }

   public Collection generateSums(byte[] buf, long baseOffset) {
      return generateSums(buf, 0, buf.length, baseOffset);
   }

   /**
    * Generate a list of checksums for the given buffer, generating
    * approximately one checksum per <code>blockLength</code> bytes.
    *
    * @param buf The data to generate sums for.
    * @param baseOffset The original offset of this byte array.
    * @return A list of {@link ChecksumPair}s computed from <code>buf</code>.
    */
   public Collection
   generateSums(byte[] buf, int off, int len, long baseOffset) {
      int count = (len+(blockLength-1)) / blockLength;
      int remainder = len % blockLength;
      int offset = off;
      Collection sums = new ArrayList(count);

      for (int i = 0; i < count; i++) {
         int n = Math.min(len, blockLength);
         ChecksumPair pair = generateSum(buf, offset, n, offset+baseOffset);
         pair.seq = i;

         sums.add(pair);
         len -= n;
         offset += n;
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
   public ChecksumPair generateSum(byte[] buf, int off, int len, long fileOffset) {
      ChecksumPair p = new ChecksumPair();
      weakSum.check(buf, off, len);
      strongSum.update(buf, off, len);
      p.weak = new Integer(weakSum.getValue());
      p.strong = strongSum.digest();
      p.offset = new Long(fileOffset);
      p.length = len;
      return p;
   }
}
