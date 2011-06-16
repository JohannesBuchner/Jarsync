/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
    $Id$

    BufferUtil: static methods for reading from a ByteBuffer.
    Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

    This file is a part of Jarsync.

    Jarsync is free software; you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Jarsync is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Jarsync; if not, write to the

       Free Software Foundation, Inc.,
       59 Temple Place, Suite 330,
       Boston, MA  02111-1307
       USA  */

package org.metastatic.rsync.v2;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class BufferUtil {

   // Class methods.
   // -----------------------------------------------------------------------

   /**
    * Read a string from the buffer.
    *
    * @param in        The input buffer.
    * @param maxLength The maximum number of bytes to read, or 0 if not
    *    limited.
    * @return The string.
    * @throws BufferOverflowException If the string length is greater
    *   than <i>maxLength</i>.
    * @throws BufferUnderflowException If there are not enough
    *   bytes available in the buffer.
    */
   public static String getString(ByteBuffer in, int maxLength) {
      int l = in.getInt();
      if (maxLength > 0 && l > maxLength)
         throw new BufferOverflowException();
      byte[] b = new byte[l];
      try {
         in.get(b);
         return new String(b, "ISO-8859-1");
      } catch (BufferUnderflowException bue) {
         in.position(in.position() - 4);
         throw bue;
      } catch (java.io.UnsupportedEncodingException shouldNotHappen) {
      }
      return null;
   }

   /**
    * Read a short string from the buffer.
    *
    * @param in        The input buffer.
    * @param maxLength The maximum number of bytes to read, or 0 if not
    *    limited.
    * @return The string.
    * @throws BufferUnderflowException If there are not enough
    *   bytes available in the buffer.
    */
   public static String getShortString(ByteBuffer in) {
      int l = in.get() & 0xFF;
      byte[] b = new byte[l];
      try {
         in.get(b);
         return new String(b, "ISO-8859-1");
      } catch (BufferUnderflowException bue) {
         in.position(in.position() - 1);
         throw bue;
      } catch (java.io.UnsupportedEncodingException shouldNotHappen) {
      }
      return null;
   }

   /**
    * Get a long integer from the byte buffer. The semantics of the
    * integer read are as follows:
    *
    * <ul>
    * <li>If the first four bytes do not equal the integer
    * <tt>0xFFFFFFFF</tt>, return these four bytes, and don't read
    * anything else.</li>
    * <li>Otherwise, read eight more bytes and return that as the long
    * integer.</li>
    * </ul>
    *
    * @param in The input buffer.
    * @return The long integer.
    * @throws BufferUnderflowException If there are not enough bytes in
    *   the buffer.
    */
   public static long getLong(ByteBuffer in) {
      int i = in.getInt();
      if (i != 0xFFFFFFFF)
         return i;
      try {
         return in.getLong();
      } catch (BufferUnderflowException bue) {
         in.position(in.position() - 4);
         throw bue;
      }
   }
}
