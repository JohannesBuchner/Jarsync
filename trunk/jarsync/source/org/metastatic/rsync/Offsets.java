/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   Offsets: A block of data that has simply moved.
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
 * This class represents an update to a file or array of bytes wherein
 * the bytes themselves have not changed, but have moved to another
 * location. This is represented by three fields: the offset in the
 * original data, the offset in the new data, and the length, in bytes,
 * of this block.
 *
 * @version $Revision$
 */
public class Offsets implements Delta, java.io.Serializable {

 // Constants and variables
   // ------------------------------------------------------------------------

   private static final long serialVersionUID = 2787420454508237262L;

   /**
    * The original offset.
    *
    * @since 1.1
    */
   protected long oldOffset;

   /**
    * The new offset.
    *
    * @since 1.1
    */
   protected long newOffset;

   /**
    * The size of the moved block, in bytes.
    *
    * @since 1.1
    */
   protected int blockLength;

 // Constructors
   // -----------------------------------------------------------------

   /**
    * Create a new pair of offsets. The idea behind this object is
    * that this sort of {@link Delta} represents original data
    * that has simply moved in the new data.
    *
    * @since 1.1
    * @param oldOffset The offset in the original data.
    * @param newOffset The offset in the new data.
    * @param blockLength The size, in bytes, of the block that has moved.
    */
   public Offsets(long oldOffset, long newOffset, int blockLength) {
      this.oldOffset = oldOffset;
      this.newOffset = newOffset;
      this.blockLength = blockLength;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   // Delta interface implementation.

   public long getWriteOffset() {
      return newOffset;
   }

   public int getBlockLength() {
      return blockLength;
   }

 // Property accessor methods

   /**
    * Get the original offset.
    *
    * @return The original offset.
    */
   public long getOldOffset() {
      return oldOffset;
   }

   /**
    * Set the original offset.
    *
    * @param off The new value for the original offset.
    */
   public void setOldOffset(long off) {
      oldOffset = off;
   }

   /**
    * Get the updated offset.
    *
    * @return The updated offset.
    */
   public long getNewOffset() {
      return newOffset;
   }

   /**
    * Set the updated offset.
    *
    * @param off The new value for the updated offset.
    */
   public void setNewOffset(long off) {
      newOffset = off;
   }

   /**
    * Set the block size.
    *
    * @param len The new value for the block size.
    */
   public void setBlockLength(int len) {
      blockLength = len;
   }

 // Public instance methods overriding java.lang.Object -------------

   /**
    * Return a {@link java.lang.String} representation of this object.
    *
    * @return A string representing this object.
    */
   public String toString() {
      return "[ old=" + oldOffset + " new=" + newOffset 
         + " len=" + blockLength + " ]";
   }

   /**
    * Test if one object is equal to this one.
    *
    * @return <tt>true</tt> If <tt>o</tt> is an Offsets instance and the
    * 	{@link #oldOffset}, {@link #newOffset}, and {@link
    * 	#blockLength} fields are all equal.
    * @throws java.lang.ClassCastException If <tt>o</tt> is not an
    * 	instance of this class.
    * @throws java.lang.NullPointerException If <tt>o</tt> is null.
    */
   public boolean equals(Object o) {
      return oldOffset == ( (Offsets) o ).oldOffset
         && newOffset == ( (Offsets) o ).newOffset 
         && blockLength == ( (Offsets) o ).blockLength;
   }

   /**
    * Returns the hash code of this object, defined as:
    * <blockquote>
    * <tt>{@link #oldOffset} + {@link #newOffset} + {@link
    * #blockLength}
    * % 2^32</tt>
    * </blockquote>
    *
    * @return The hash code of this object.
    */
   public int hashCode() {
      return (int) (oldOffset + newOffset + blockLength);
   }
}
