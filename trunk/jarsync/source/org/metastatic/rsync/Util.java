// vim:set tw=72 expandtab softtabstop=3 shiftwidth=3 tabstop=3:
// $Id$
//
// Parts of this file (the toHexString methods) are derived from the
// gnu.crypto.util.Util class in GNU Crypto.
//
// Util -- Basic utility functions.
// Copyright (C) 2001,2002  The Free Software Foundation, Inc.
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A number of useful, static methods.
 *
 * @version $Revision$
 */
public final class Util {

   // Constants and variables.
   // -----------------------------------------------------------------------

   /** The characters for Base64 encoding. */
   public static final String BASE_64 =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

   // Constructors.
   // -----------------------------------------------------------------------

   /** This class cannot be instantiated. */
   private Util() { }

   // Class methods.
   // -----------------------------------------------------------------------

   /**
    * Base-64 encode a byte array, returning the returning string.
    *
    * <p>Note that this method exists merely to be compatible with the
    * challenge-response authentication method of rsyncd. It is
    * <em>not</em> technincally a Base-64 encoder.
    *
    * @param buf The byte array to encode.
    * @return <tt>buf</tt> encoded in Base64.
    */
   public static final String base64(byte[] buf) {
      int bitOffset, byteOffset, index = 0;
      int bytes = (buf.length*8 + 5) / 6;
      StringBuffer out = new StringBuffer(bytes);

      for (int i = 0; i < bytes; i++) {
         byteOffset = (i*6)/8;
         bitOffset = (i*6)%8;
         if (bitOffset < 3) {
            index = (buf[byteOffset]>>>(2-bitOffset)) & 0x3f;
         } else {
            index = (buf[byteOffset] <<(bitOffset-2)) & 0x3f;
            if (byteOffset + 1 < buf.length) {
               index |= (buf[byteOffset+1]&0xff) >>> (8-(bitOffset-2));
            }
         }
         out.append(BASE_64.charAt(index));
      }

      return out.toString();
   }

   /**
    * Write a String as a sequece of ASCII bytes.
    *
    * @param out   The {@link java.io.OutputStream} to write to.
    * @param ascii The ASCII string to write.
    * @throws java.io.IOException If writing fails.
    */
   public static void
   writeASCII(OutputStream out, String ascii) throws IOException {
      try {
         out.write(ascii.getBytes("US-ASCII"));
      } catch (java.io.UnsupportedEncodingException shouldNotHappen) { }
   }

   /**
    * Read up to a '\n' or '\r', and return the resulting string. The
    * input is assumed to be ISO-8859-1.
    *
    * @param in The {@link java.io.InputStream} to read from.
    * @return The line read, without the line terminator.
    */
   public static String readLine(InputStream in) throws IOException {
      StringBuffer s = new StringBuffer();
      int c = in.read();
      while (c != -1 && c != '\n' && c != '\r') {
         s.append((char) (c&0xff));
         c = in.read();
      }
      if (c == -1) {
         throw new EOFException();
      }
      return s.toString();
   }

   // From gnu.crypto.util.Util

   /** Hexadecimal digits. */
   private static final char[] HEX_DIGITS = {
      '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'
   };

   /**
    * Convert a byte array to a big-endian ordered hexadecimal string.
    *
    * @param b The bytes to convert.
    * @return A hexadecimal representation to <tt>b</tt>.
    */
   public static String toHexString(byte[] b) {
      return toHexString(b, 0, b.length);
   }
 
   /**
    * Convert a byte array to a big-endian ordered hexadecimal string.
    *
    * @param b The bytes to convert.
    * @return A hexadecimal representation to <tt>b</tt>.
    */
   public static String toHexString(byte[] b, int off, int len) {
      char[] buf = new char[len * 2];
      for (int i = 0, j = 0, k; i < len; ) {
         k = b[off + i++];
         buf[j++] = HEX_DIGITS[(k >>> 4) & 0x0F];
         buf[j++] = HEX_DIGITS[ k        & 0x0F];
      }
      return new String(buf);
   }
}
