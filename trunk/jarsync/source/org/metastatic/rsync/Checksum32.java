// vim:set tw=72 expandtab softtabstop=3 shiftwidth=3 tabstop=3:
// $Id$
// 
// Checksum32: A simple, "rolling" checksum based on Adler32.
// Copyright (C) 2001,2002,2003  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of Jarsync.
//
// Jarsync is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2, or (at your option) any
// later version.
//
// Jarsync is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Jarsync; see the file COPYING.  If not, write to the
//
//    Free Software Foundation Inc.,
//    59 Temple Place - Suite 330,
//    Boston, MA 02111-1307
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
public class Checksum32 implements RollingChecksum, Cloneable {

   // Constants and variables.
   // -----------------------------------------------------------------

   private final short char_offset;

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
    * Creates a new rolling checksum. The <i>char_offset</i> argument
    * affects the output of this checksum; rsync uses a char offset of
    * 0, librsync 31.
    */
   public Checksum32(short char_offset) {
      this.char_offset = char_offset;
      a = b = 0;
      k = 0;
   }

   public Checksum32() {
      this((short) 0);
   }

 // Public instance methods.
   // -----------------------------------------------------------------

   /**
    * Return the value of the currently computed checksum.
    *
    * @return The currently computed checksum.
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
      a -= (block[i]&0xff) + char_offset;
      a += (bt & 0xff) + char_offset;
      b -= l * ((block[i]&0xff) + char_offset);
      b += a;
      k++;
   }

   /**
    * Update the checksum by trimming off a byte only, not adding
    * anything.
    */
   public void trim() {
      a -= (block[k%block.length]&0xff) + char_offset;
      b -= l * ((block[k%block.length]&0xff) + char_offset);
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
              2 * (block[i+2]&0xff) + (block[i+3]&0xff) + 10 * char_offset;
         a += (block[i]&0xff) + (block[i+1]&0xff) + (block[i+2]&0xff)
              + (block[i+3]&0xff) + 4 * char_offset;
      }
      for (; i < block.length; i++) {
         a += (block[i]&0xff) + char_offset;
         b += a;
      }
      new_block = new byte[block.length];
      new_index = 0;
   }

   public Object clone() {
      Checksum32 copy = new Checksum32();
      copy.a = a;
      copy.b = b;
      copy.l = l;
      copy.k = k;
      copy.block = (block != null) ? (byte[]) block.clone() : null;
      copy.new_index = new_index;
      copy.new_block = (new_block != null) ? (byte[]) new_block.clone() : null;
      return copy;
   }

   public boolean equals(Object o) {
      return ((Checksum32)o).a == a && ((Checksum32)o).b == b;
   }
}
