// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// ChecksumPair -- A pair of weak, strong checksums.
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
   Integer weak;

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
   Long offset;

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
    */
   public ChecksumPair(int weak, byte[] strong, long offset) {
      this(new Integer(weak), strong, new Long(offset));
   }

   /**
    * Create a new checksum pair.
    *
    * @param weak The weak, rolling checksum.
    * @param strong The strong checksum.
    * @param offset The offset at which this checksum was computed.
    */
   public ChecksumPair(Integer weak, byte[] strong, Long offset) {
      this.weak = weak;
      this.strong = (byte[]) strong.clone();
      this.offset = offset;
   }

   /**
    * Create a new checksum pair with no associated offset.
    *
    * @param weak The weak checksum.
    * @param strong The strong checksum.
    */
   public ChecksumPair(int weak, byte[] strong) {
      this(weak, strong, -1L);
   }

   /**
    * Create a new checksum pair with no associated offset.
    *
    * @param weak The weak checksum.
    * @param strong The strong checksum.
    */
   public ChecksumPair(Integer weak, byte[] strong) {
      this(weak, strong, new Long(-1L));
   }

   public ChecksumPair() { }

 // Instance methods.
   // -------------------------------------------------------------------------

   /**
    * Get the weak checksum.
    *
    * @return The weak checksum.
    * @since 1.1
    */
   public Integer getWeak() {
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

   public Long getOffset() {
      return offset;
   }

 // Public instance methods overriding java.lang.Object.
   // -------------------------------------------------------------------------

   /**
    * Two checksum pairs are regared as equal if their respective
    * values are equal.
    *
    * @param obj The Object to test.
    * @return True if both checksum pairs are equal.
    */
   public boolean equals(Object obj) {
      return (getWeak() == ((ChecksumPair) obj).getWeak()) &&
         Arrays.equals(getStrong(), ((ChecksumPair) obj).getStrong());
   }

   /**
    * Returns a pair of hexadecimal strings of the form:
    * <blockquote>
    * <code>{ weak, strong }</code>
    * </blockquote>
    *
    * @return The String representation of this pair.
    * @since 1.2
    */
   public String toString() {
      String weak = new String();
      String strong = new String();
      String s;
      s = Integer.toHexString(getWeak().intValue());
      for (int i = 0; i < 8 - s.length(); i++) {
         weak = weak + "0";
      }
      weak = weak + s;
      byte[] digest = getStrong();
      for (int i = 0; i < digest.length; i++) {
         s = Integer.toHexString(((int) digest[i]) & 0xff);
         strong += (s.length() == 2) ? (s) : ("0" + s);
      }
      return "{ " + weak + ", " + strong + " }";
   }
}
