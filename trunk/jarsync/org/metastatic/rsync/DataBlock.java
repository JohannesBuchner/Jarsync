/* DataBlock: A new block of data to insert.
   Copyright (C) 2003, 2007  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with Jarsync; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

Linking Jarsync statically or dynamically with other modules is making
a combined work based on Jarsync.  Thus, the terms and conditions of
the GNU General Public License cover the whole combination.

As a special exception, the copyright holders of Jarsync give you
permission to link Jarsync with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on Jarsync.  If you modify Jarsync, you may extend this
exception to your version of it, but you are not obligated to do so.
If you do not wish to do so, delete this exception statement from your
version.

ALTERNATIVELY, Jarsync may be licensed under the Apache License,
Version 2.0 (the "License"); you may not use this file except in
compliance with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.  See the License for the specific language governing
permissions and limitations under the License.

If you modify Jarsync, you may extend this same choice of license for
your library, but you are not obligated to do so. If you do not offer
the same license terms, delete the license terms that your library is
NOT licensed under.  */


package org.metastatic.rsync;

/**
 * This is the {@link Delta} in the rsync algorithm that introduces new
 * data. It is an array of bytes and an offset, such that the updated
 * file should contain this block at the given offset.
 *
 * @version $Revision$
 */
public class DataBlock implements Delta, java.io.Serializable {

   // Constants and variables.
   // -----------------------------------------------------------------

   private static final long serialVersionUID = -3132452687703522201L;

   /**
    * The block of data to insert.
    *
    * @since 1.1
    */
   protected final byte[] data;

   /**
    * The offset in the file to start this block.
    *
    * @since 1.1
    */
   protected final long offset;

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
   public DataBlock(long offset, byte[] data)
   {
      this.offset = offset;
      this.data = (byte[]) data.clone();
   }

   /**
    * Create a new instance of a DataBlock with a given offset and a
    * portion of a byte array.
    *
    * @param offset The write offset of this data block.
    * @param data   The data itself.
    * @param off    The offset in the array to begin copying.
    * @param len    The number of bytes to copy.
    */
   public DataBlock(long offset, byte[] data, int off, int len)
   {
      this.offset = offset;
      if (data.length == len && off == 0) {
         this.data = (byte[]) data.clone();
      } else {
         this.data = new byte[len];
         System.arraycopy(data, 0, this.data, off, len);
      }
   }

 // Instance methods.
   // -----------------------------------------------------------------

   // Delta interface implementation.

   public long getWriteOffset() {
      return offset;
   }

   public int getBlockLength() {
      return data.length;
   }

   // Property accessor methods. --------------------------------------

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
      return (byte[]) data.clone();
   }

 // Instance methods overriding java.lang.Object. -------------------

   /**
    * Return a printable string that represents this object.
    *
    * @return A string representation of this block.
    * @since 1.1
    */
   public String toString() {
     StringBuffer buf = new StringBuffer();
     buf.append(super.toString());
     buf.append(" [ off=").append(offset);
     buf.append(" len=").append(data.length);
     buf.append(" data=");
     final int n = Math.min(data.length, 256);
     for (int i = 0; i < n; i++)
       {
         if ((data[i] & 0xFF) < 0x10)
           buf.append('0');
         buf.append(Integer.toHexString(data[i] & 0xFF));
       }
     if (n != data.length)
       buf.append("...");
     buf.append(" ]");
     return buf.toString();
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
    *   if both the offsets and the byte arrays of both are equal.
    * @throws java.lang.ClassCastException If <tt>o</tt> is not an
    *   instance of this class.
    * @throws java.lang.NullPointerException If <tt>o</tt> is null.
    */
   public boolean equals(Object o) {
      return offset == ( (DataBlock) o ).offset &&
            java.util.Arrays.equals(data, ( (DataBlock) o ).data);
   }
}
