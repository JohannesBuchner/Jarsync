/* ChecksumPair: A pair of weak, strong checksums.
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

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
along with Jarsync; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA

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
version.  */


package org.metastatic.rsync;

import java.util.Arrays;

/**
 * A pair of weak and strong checksums for use with the Rsync algorithm.
 * The weak "rolling" checksum is typically a 32-bit sum derived from
 * the Adler32 algorithm; the strong checksum is usually a 128-bit MD4
 * checksum.
 *
 * @author Casey Marshall
 * @version $Revision$
 */
public class ChecksumPair implements java.io.Serializable {

   // Constants and variables.
   // -------------------------------------------------------------------------

   /**
    * The weak, rolling checksum.
    *
    * @since 1.1
    */
   int weak;

   /**
    * The strong checksum.
    *
    * @since 1.1
    */
   byte[] strong;

   /**
    * The offset in the original data where this pair was
    * generated.
    */
   long offset;

   /** The number of bytes these sums are over. */
   int length;

   /** The sequence number of these sums. */
   int seq;

 // Constructors.
   // -------------------------------------------------------------------------

   /**
    * Create a new checksum pair.
    *
    * @param weak The weak, rolling checksum.
    * @param strong The strong checksum.
    * @param offset The offset at which this checksum was computed.
    * @param length The length of the data over which this sum was
    *        computed.
    * @param seq    The sequence number of this checksum pair.
    */
   public ChecksumPair(int weak, byte[] strong, long offset,
                       int length, int seq) {
      this.weak = weak;
      this.strong = strong;
      this.offset = offset;
      this.length = length;
      this.seq = seq;
   }

   /**
    * Create a new checksum pair with no length or sequence fields.
    *
    * @param weak The weak checksum.
    * @param strong The strong checksum.
    * @param offset The offset at which this checksum was computed.
    */
   public ChecksumPair(int weak, byte[] strong, long offset) {
      this(weak, strong, offset, 0, 0);
   }

   /**
    * Create a new checksum pair with no associated offset.
    *
    * @param weak The weak checksum.
    * @param strong The strong checksum.
    */
   public ChecksumPair(int weak, byte[] strong) {
      this(weak, strong, -1L, 0, 0);
   }

   /**
    * Default 0-arguments constructor for package access.
    */
   ChecksumPair() { }

 // Instance methods.
   // -------------------------------------------------------------------------

   /**
    * Get the weak checksum.
    *
    * @return The weak checksum.
    * @since 1.1
    */
   public int getWeak() {
      return weak;
   }

   /**
    * Get the strong checksum.
    *
    * @return The strong checksum.
    * @since 1.1
    */
   public byte[] getStrong() {
      return strong;
   }

   /**
    * Return the offset from where this checksum pair was generated.
    *
    * @return The offset.
    */
   public long getOffset() {
      return offset;
   }

   /**
    * Return the length of the data for which this checksum pair was
    * generated.
    *
    * @return The length.
    */
   public int getLength() {
      return length;
   }

   /**
    * Return the sequence number of this checksum pair, if any.
    *
    * @return The sequence number.
    */
   public int getSequence() {
      return seq;
   }

 // Public instance methods overriding java.lang.Object.
   // -------------------------------------------------------------------------

   public int hashCode() {
      return weak;
   }

   /**
    * We define equality for this object as equality between two weak
    * sums and equality between two strong sums.
    *
    * @param obj The Object to test.
    * @return True if both checksum pairs are equal.
    */
   public boolean equals(Object obj) {
      return weak == ((ChecksumPair) obj).weak &&
         Arrays.equals(strong, ((ChecksumPair) obj).strong);
   }

   /**
    * Returns a String representation of this pair.
    *
    * @return The String representation of this pair.
    * @since 1.2
    */
   public String toString() {
      StringBuffer buf = new StringBuffer();
      String s;
      s = Integer.toHexString(getWeak());
      for (int i = 0; i < 8 - s.length(); i++) {
         buf.append('0');
      }
      String weak = buf.toString() + s;
      return "len=" + length + " offset=" + offset + " weak=" + weak
         + " strong=" + Util.toHexString(strong);
   }
}
