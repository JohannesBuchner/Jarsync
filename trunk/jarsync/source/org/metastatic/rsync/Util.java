// :vim:set tw=78 expandtab tabstop=3:
// $Id$
//
// Util -- Basic utility functions.
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
// ---------------------------------------------------------------------------

package org.metastatic.rsync;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util {

   // Constants and variables.
   // -----------------------------------------------------------------------

   public static final String BASE_64 =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

   // Constructors.
   // -----------------------------------------------------------------------

   /** This class cannot be instantiated. */
   private Util() { }

   // Class methods.
   // -----------------------------------------------------------------------

   /**
    * Base64 encode a byte array, returning the returning string.
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
}
