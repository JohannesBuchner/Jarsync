/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   Checksum32: A simple, "rolling" checksum based on Adler32.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync.

   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2 of the License, or (at
   your option) any later version.

   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jarsync; if not, write to the

      Free Software Foundation, Inc.,
      59 Temple Place, Suite 330,
      Boston, MA  02111-1307
      USA

   Linking Jarsync statically or dynamically with other modules is
   making a combined work based on Jarsync.  Thus, the terms and
   conditions of the GNU General Public License cover the whole
   combination.

   As a special exception, the copyright holders of Jarsync give you
   permission to link Jarsync with independent modules to produce an
   executable, regardless of the license terms of these independent
   modules, and to copy and distribute the resulting executable under
   terms of your choice, provided that you also meet, for each linked
   independent module, the terms and conditions of the license of that
   module.  An independent module is a module which is not derived from
   or based on Jarsync.  If you modify Jarsync, you may extend this
   exception to your version of it, but you are not obligated to do so.
   If you do not wish to do so, delete this exception statement from
   your version.  */

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
public class Checksum32 implements RollingChecksum, Cloneable, java.io.Serializable {

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

   private Checksum32(Checksum32 that) {
      this.char_offset = that.char_offset;
      this.a = that.a;
      this.b = that.b;
      this.l = that.l;
      this.k = that.k;
      this.block = (that.block != null) ? (byte[]) that.block.clone() : null;
      this.new_index = that.new_index;
      this.new_block = (that.new_block != null)
         ? (byte[]) that.new_block.clone() : null;
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
      a -= block[k] + char_offset;
      b -= l * (block[k] + char_offset);
      a += bt + char_offset;
      b += a;
      block[k] = bt;
      k++;
      if (k == l) k = 0;
   }

   /**
    * Update the checksum by trimming off a byte only, not adding
    * anything.
    */
   public void trim() {
      a -= block[k % block.length] + char_offset;
      b -= l * (block[k % block.length] + char_offset);
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
         b += 4 * (a + block[i]) + 3 * block[i+1] +
              2 * block[i+2] + block[i+3] + 10 * char_offset;
         a += block[i] + block[i+1] + block[i+2]
              + block[i+3] + 4 * char_offset;
      }
      for (; i < block.length; i++) {
         a += block[i] + char_offset;
         b += a;
      }
   }

   public Object clone() {
      return new Checksum32(this);
   }

   public boolean equals(Object o) {
      return ((Checksum32)o).a == a && ((Checksum32)o).b == b;
   }
}
