// :vim:set tw=72 expandtab tabstop=3:
// $Id$
//
// Derived from gnu.crypto.hash.BaseHash from the classpathx-crypto
// package.
//
// MessageDigest -- superclass for strong checksums.
// Copyright (C) 2001,2002, Free Software Foundation, Inc.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License or (at your option) any
// later version.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
// more details.
//
// You should have received a copy of the GNU General Public License along with
// this program; see the file COPYING.  If not, write to the
//
//    Free Software Foundation Inc.,
//    59 Temple Place - Suite 330,
//    Boston, MA 02111-1307
//    USA
//
// ----------------------------------------------------------------------------

package org.metastatic.rsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * <p>A base abstract class to facilitate hash implementations.</p>
 *
 * @version $Revision$
 */
public abstract class MessageDigest {

   // Constants and variables
   // -------------------------------------------------------------------------

   public static final String RCS_ID = "$Id$";

   private static int BUFFER_LENGTH = 20*1024;

   /** The name of this digest. */
   protected String name;

   /** The hash (output) size in bytes. */
   protected int hashSize;

   /** The hash (inner) block size in bytes. */
   protected int blockSize;

   /** Number of bytes processed so far. */
   protected long count;

   /** Temporary input buffer. */
   protected byte[] buffer;

   // Constructor(s)
   // -------------------------------------------------------------------------

   /** Trivial no-arguments constructor. */
   protected MessageDigest() { }

   // Instance methods
   // -------------------------------------------------------------------------

   /**
    * Return the cononical name of this message digest.
    *
    * @return This digest's name.
    */
   public String name() {
      return name;
   }

   /**
    * Return the size of the final hash, in bytes.
    *
    * @return The hash size.
    */
   public int hashSize() {
      return hashSize;
   }

   /**
    * Return the internal block size of this message digest.
    *
    * @return The block size.
    */
   public int blockSize() {
      return blockSize;
   }

   /**
    * Update the digest with a single byte.
    *
    * @param b The byte to add.
    */
   public void update(byte b) {
      // compute number of bytes still unhashed; ie. present in buffer
      int i = (int)(count % blockSize);
      count++;
      buffer[i] = b;
      if (i == (blockSize - 1)) {
         transform(buffer, 0);
      }
   }

   /**
    * Update the digest with a portion of a byte array.
    *
    * @param b The bytes to add.
    * @param offset From whence in <code>b</code> to start.
    * @param len The number of bytes to read from <code>b</code>.
    */
   public void update(byte[] b, int offset, int len) {
      int n = (int)(count % blockSize);
      count += len;
      int partLen = blockSize - n;
      int i = 0;

      if (len >= partLen) {
         System.arraycopy(b, offset, buffer, n, partLen);
         transform(buffer, 0);
         for (i = partLen; i + blockSize - 1 < len; i+= blockSize) {
            transform(b, offset + i);
         }
         n = 0;
      }

      if (i < len) {
         System.arraycopy(b, offset + i, buffer, n, len - i);
      }
   }

   /**
    * Finish processing the input and return the computed digest. After
    * calling this method this instance is {@link #reset()}.
    *
    * @return A byte array containing the computed digest.
    */
   public byte[] digest() {
      byte[] tail = padBuffer(); // pad remaining bytes in buffer
      update(tail, 0, tail.length); // last transform of a message
      byte[] result = getResult(); // make a result out of context

      reset(); // reset this instance for future re-use

      return result;
   }

   /**
    * Reset this instance for future re-use.
    */
   public void reset() {
      count = 0L;
      for (int i = 0; i < blockSize; ) {
         buffer[i++] = 0;
      }

      resetContext();
   }

   /**
    * Compute the digest for an entire file.
    *
    * @param f The file to digest.
    * @return The digest computed from <code>f</code>.
    * @throws IOException If reading from <code>f</code> fails.
    */
   public byte[] fileChecksum(File f) throws IOException {
      reset();
      FileInputStream in = new FileInputStream(f);
      byte[] buf = new byte[BUFFER_LENGTH];
      int len;
      while ((len = in.read(buf)) != -1) {
         update(buf, 0, len);
      }
      return digest();
   }

   // methods to be implemented by concrete subclasses ------------------------

   /**
    * Clone this instance. The internal state (the number of bytes read
    * and the digest thusfar computed) are copied into the new instance.
    *
    * @return A clone of this instance.
    */
   public abstract Object clone();

   /**
    * Perform a simple conformance test of this message digest.
    *
    * @return true If the internal self-test succeeds.
    */
   public abstract boolean selfTest();

   /**
    * <p>Returns the byte array to use as padding before completing a hash
    * operation.</p>
    *
    * @return The bytes to pad the remaining bytes in the buffer before
    *    completing a hash operation.
    */
   protected abstract byte[] padBuffer();

   /**
    * <p>Constructs the result from the contents of the current context.</p>
    *
    * @return the output of the completed hash operation.
    */
   protected abstract byte[] getResult();

   /** Resets the instance for future re-use. */
   protected abstract void resetContext();

   /**
    * <p>The block digest transformation per se.</p>
    *
    * @param in The <i>blockSize</i> long block, as an array of bytes to digest.
    * @param offset The index where the data to digest is located within the
    * input buffer.
    */
   protected abstract void transform(byte[] in, int offset);
}
