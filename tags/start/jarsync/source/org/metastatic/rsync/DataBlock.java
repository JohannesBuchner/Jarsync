// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// DataBlock: A new block of data to insert.
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
 * This is the {@link Update} in the rsync algorithm that introduces new
 * data. It is an array of bytes and an offset, such that the updated
 * file should contain this block at the given offset.
 *
 * @version $Revision$
 */
public class DataBlock implements Delta, java.io.Serializable {

   // Constants and variables.
   // -----------------------------------------------------------------

   public static final String RCSID = "$Id$";

   private static final long serialVersionUID = -3132452687703522201L;

   /**
    * The block of data to insert.
    *
    * @since 1.1
    */
   protected byte[] data;

   /**
    * The offset in the file to start this block.
    *
    * @since 1.1
    */
   protected long offset;
  
   // Constructors.
   // -----------------------------------------------------------------

   /**
    * Create a new instance of a DataBlock with a given offset and
    * block of bytes.
    *
    * @param offset The offset where this data should go.
    * @param data The data itself.
    * @since 1.1
    */
   public DataBlock(long offset, byte[] data) {
     this.offset = offset;
     this.data = (byte[]) data.clone();
   }

 // Instance methods.
   // -----------------------------------------------------------------

   /**
    * Get the offset at which this block should begin.
    *
    * @return The offset at which this block should begin.
    * @since 1.1
    */
   public long getOffset() {
      return offset;
   }

   /**
    * Return the array of bytes that is the data block.
    *
    * @return The block itself.
    * @since 1.1
    */
   public byte[] getData() {
      return data;
   }

   /**
    * Return the length of this block, in bytes.
    *
    * @return The size of this data block, in bytes.
    * @since 1.1
    */
   public int getBlockLength() {
      return data.length;
   }

 // Instance methods overriding java.lang.Object. -------------------

   /**
    * Return a printable string that represents this object.
    *
    * @return A string representation of this block.
    * @since 1.1
    */
   public String toString() {
      String str = "[ off=" + offset + " len=" + data.length + " data=";
      int i;
      for (i = 0; i < data.length && i < 500; i++) {
         String s = Integer.toHexString((int) data[i] & 0xff);
         if (s.length() != 2)
            str += "0" + s;
         else
            str += s;
      }
      if (i != data.length) str += "...";
      return str + " ]";
   }

   /**
    * Return the hash code for this data block.
    *
    * @return The hash code.
    * @since 1.1
    */
   public int hashCode() {
      int b = 0;
      // For fun.
      for (int i = 0; i < data.length; i++)
         b ^= data[i] << ((i * 8) % 32);
      return b + (int) offset;
   }

   /**
    * Test if another object equals this one.
    *
    * @return <tt>true</tt> If <tt>o</tt> is an instance of DataBlock and
    * 	if both the offsets and the byte arrays of both are equal.
    * @throws java.lang.ClassCastException If <tt>o</tt> is not an
    * 	instance of this class.
    * @throws java.lang.NullPointerException If <tt>o</tt> is null.
    */
   public boolean equals(Object o) {
      return offset == ( (DataBlock) o ).offset &&
            java.util.Arrays.equals(data, ( (DataBlock) o ).data);
   }
}
