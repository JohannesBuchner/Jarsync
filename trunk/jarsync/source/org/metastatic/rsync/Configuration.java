// vim:set tw=72 expandtab softtabstop=3 shiftwidth=3 tabstop=3:
// $Id$
//
// Configuration -- Wrapper around configuration data.
// Copyright (C) 2001,2002  Casey Marshall <rsdio@metastatic.org>
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
// ---------------------------------------------------------------------------

package org.metastatic.rsync;

/**
 * A Configuration is a mere collection of objects and values that
 * compose a particular configuration for the algorithm, for example the
 * message digest that computes the strong checksum.
 *
 * @author Casey Marshall
 * @version $Revision$
 */
public class Configuration implements RsyncConstants {

   // Constants and variables.
   // ------------------------------------------------------------------------

   /**
    * The message digest that computes the stronger checksum.
    */
   MessageDigest strongSum;

   /**
    * The length of blocks to checksum.
    */
   int blockLength;

   /**
    * The effective length of the strong sum.
    */
   int strongSumLength;

   /**
    * Whether or not to do run-length encoding when making Deltas.
    */
   boolean doRunLength;

   /**
    * The seed for the checksum, to perturb the strong checksum and help
    * avoid collisions in plain rsync (or in similar applicaitons).
    */
   byte[] checksumSeed;

   // Constructors.
   // ------------------------------------------------------------------------

   /**
    * Create a configuration using MD4 and a block length of 700.
    */
   public Configuration() {
      this(new MD4(), BLOCK_LENGTH);
   }

   /**
    * Create a configuration with a particular strong checksum and a
    * block length of 700.
    *
    * @param strongSum The {@link MessageDigest} to use.
    */
   public Configuration(MessageDigest strongSum) {
      this(strongSum, BLOCK_LENGTH);
   }

   /**
    * Create a configuration with a particular block length and the MD4
    * message digest algorithm.
    *
    * @param blockLength The size of blocks to checksum.
    */
   public Configuration(int blockLength) {
      this(new MD4(), blockLength);
   }

   /**
    * Create a configuration with a particular block length and message
    * digest.
    *
    * @param strongSum The {@link MessageDigest} to use.
    * @param blockLength The block length to use.
    */
   public Configuration(MessageDigest strongSum, int blockLength) {
      this.strongSum = strongSum;
      this.blockLength = blockLength;
      strongSumLength = SUM_LENGTH;
      doRunLength = false;
   }

   // Instance methods.
   // -------------------------------------------------------------------------

   /**
    * Set whether or not to do run-length encoding while generating
    * deltas.
    *
    * @param doRunLength Whether or not to do RLE.
    */
   public void setDoRunLength(boolean doRunLength) {
      this.doRunLength = doRunLength;
   }

   /**
    * Return whether or not to do run-length encoding.
    *
    * @return Whether or not to do RLE.
    */
   public boolean getDoRunLength() {
      return doRunLength;
   }

   /**
    * Set the effective length of the strong sum (the first
    * <code>strongSumLength</code> bytes of the strong sum).
    *
    * @param strongSumLength The sum length.
    */
   public void setStrongSumLength(int strongSumLength) {
      this.strongSumLength = strongSumLength;
   }

   /**
    * Get the effective length of the strong sum.
    *
    * @return The effective length of the strong sum.
    */
   public int getStrongSumLength() {
      return strongSumLength;
   }

   /**
    * Set the checksum seed. This can be <code>null</code> for the case
    * of no checksum. The byte array is clone()d if non-null.
    *
    * @param seed The seed.
    */
   public void setChecksumSeed(byte[] seed) {
      if (seed == null) {
         checksumSeed = null;
      } else {
         checksumSeed = (byte[]) seed.clone();
      }
   }

   /**
    * Get the checksum seed.
    *
    * @return The checksum seed, or <code>null</code> if there is none.
    */
   public byte[] getChecksumSeed() {
      return checksumSeed;
   }
}
