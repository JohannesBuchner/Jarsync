// vim:set tabstop=3 expandtab tw=72:
// $Id$
// 
// RollingChecksum: A simple, "rolling" checksum based on Adler32.
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

/**
 * A simple 32-bit "rolling" checksum. This checksum algorithm is based
 * upon the algorithm outlined in the paper "The rsync algorithm" by
 * Andrew Tridgell and Paul Mackerras. The algorithm works in such a way
 * that if one knows the sum of a block
 * <em>X<sub>k</sub>...X<sub>l</sub></em>, then it is a simple matter to
 * compute the sum for <em>X<sub>k+1</sub>...X<sub>l+1</sub></em>.
 * 
 * @author Casey Marshall
 * @version $Revision$
 */
public class RollingChecksum {

   // Constants and variables.
   // -----------------------------------------------------------------

   public static final String RCSID = "$Id$";

   private static final short CHAR_OFFSET = 31;

   /**
    * The first half of the checksum.
    *
    * @since 1.1
    */
   protected short a;

   /**
    * The second half of the checksum.
    *
    * @since 1.1
    */
   protected short b;

   /**
    * The place from whence the current checksum has been computed.
    *
    * @since 1.1
    */
   protected int k;

   /**
    * The place to where the current checksum has been computed.
    *
    * @since 1.1
    */
   protected int l;

   /**
    * The block from which the checksum is computed.
    *
    * @since 1.1
    */
   protected byte[] block;

   /**
    * The index in {@link #new_block} where the newest byte has
    * been stored.
    *
    * @since 1.1
    */
   protected int new_index;

   /**
    * The block that is recieving new input.
    *
    * @since 1.1
    */
   protected byte[] new_block;

 // Constructors.
   // -----------------------------------------------------------------

   /**
    * Creates a new rolling checksum. Use this by supplying an
    * appropriately-sized array of bytes as the parameter, as the
    * size of this array will determine the size of the blocks used
    * to compute the checksum in subsequent operations.
    *
    * @param buf The initial bytes to sum.
    * @since 1.1
    */
   public RollingChecksum(byte[] buf) {
      block = (byte[]) buf.clone();
      k = 0;
      l = buf.length - 1;
      a = b = 0;
      int i;
      for (i = 0; i < buf.length - 4; i += 4) {
         b += 4*(a+buf[i]) + 3*buf[i+1] + 2*buf[i+2] + buf[i+3]
              + 10*CHAR_OFFSET;
         a += buf[i] + buf[i+1] + buf[i+2] + buf[i+3] + 4*CHAR_OFFSET;
      }
      for (; i < buf.length; i++) {
         a += buf[i] + CHAR_OFFSET;
         b += a;
      }
      new_block = new byte[block.length];
      new_index = 0;
   }

   public RollingChecksum() {
      a = b = 0;
      k = 0;
   }

 // Public instance methods.
   // -----------------------------------------------------------------

   /**
    * Return the value of the currently computed checksum.
    *
    * @returns The currently computed checksum.
    * @since 1.1
    */
   public int getValue() {
      return (a&0xffff) | (b << 16);
   }

   /**
    * Reset the checksum.
    *
    * @since 1.1
    */
   public void reset() {
      k = 0;
      a = b = 0;
      l = 0;
   }

   /**
    * "Roll" the checksum. This method takes a single byte as byte
    * <em>X<sub>l+1</sub></em>, and recomputes the checksum for
    * <em>X<sub>k+1</sub>...X<sub>l+1</sub></em>. This is the
    * preferred method for updating the checksum.
    *
    * @param bt The next byte.
    * @since 1.1
    */
   public void roll(byte bt) {
      int i = k % block.length;
      if (k != 0 && i == 0) {
         block = new_block;
         new_block = new byte[block.length];
      }
      new_block[i] = bt;
      a -= (block[i]&0xff) + CHAR_OFFSET;
      a += (bt & 0xff) + CHAR_OFFSET;
      b -= l * ((block[i]&0xff) + CHAR_OFFSET);
      b += a;
      k++;
   }

   /**
    * Update the checksum by trimming off a byte only, not adding
    * anything.
    */
   public void trim() {
      a -= (block[k%block.length]&0xff) + CHAR_OFFSET;
      b -= l * ((block[k%block.length]&0xff) + CHAR_OFFSET);
      k++;
      l--;
   }

   /**
    * Update the checksum with an entirely different block, and
    * potentially a different block length.
    *
    * @param buf The byte array that holds the new block.
    * @param off From whence to begin reading.
    * @param len The length of the block to read.
    * @since 1.1
    */
   public void check(byte[] buf, int off, int len) {
      block = new byte[len];
      System.arraycopy(buf, off, block, 0, len);
      reset();
      l = block.length;
      int i;

      for (i = 0; i < block.length - 4; i += 4) {
         b += 4 * (a+(block[i]&0xff)) + 3 * (block[i+1]&0xff) +
              2 * (block[i+2]&0xff) + (block[i+3]&0xff) + 10 * CHAR_OFFSET;
         a += (block[i]&0xff) + (block[i+1]&0xff) + (block[i+2]&0xff)
              + (block[i+3]&0xff) + 4 * CHAR_OFFSET;
      }
      for (; i < block.length; i++) {
         a += (block[i]&0xff) + CHAR_OFFSET;
         b += a;
      }
      new_block = new byte[block.length];
      new_index = 0;
   }

   public boolean equals(Object o) {
      return ((RollingChecksum)o).a == a && ((RollingChecksum)o).b == b;
   }
}
