// $Id$
// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
//
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// Parts derived from the GNU Classpath Extensions Cryptography library:
// Copyright (C) 2001-2002, Free Software Foundation, Inc.
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
// As a special exception, if you link this library with other files to
// produce an executable, this library does not by itself cause the
// resulting executable to be covered by the GNU General Public License.
// This exception does not however invalidate any other reasons why the
// executable file might be covered by the GNU General Public License.
// 
// ----------------------------------------------------------------------------

package org.metastatic.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * <p>A collection of utility methods used throughout this project.</p>
 *
 * @version $Revision$
 */
public class Util {

   // Constants and variables
   // -------------------------------------------------------------------------

   private static final char[] HEX_DIGITS = {
      '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'
   };

   // Constructor(s)
   // -------------------------------------------------------------------------

   /** Trivial constructor to enforce Singleton pattern. */
   private Util() {
      super();
   }

   // Class methods
   // -------------------------------------------------------------------------

   /**
    * <p>Returns <code>true</code> if the two designated byte arrays are
    * (a) non-null, (b) of the same length, and (c) contain the same values.</p>
    *
    * @param a the first byte array.
    * @param b the second byte array.
    * @return <code>true</code> if the two designated arrays contain the same
    * values. Returns <code>false</code> otherwise.
    */
   public static boolean areEqual(byte[] a, byte[] b) {
      if (a == null || b == null) {
         return false;
      }
      int aLength = a.length;
      if (aLength != b.length) {
         return false;
      }
      for (int i = 0; i < aLength; i++) {
         if (a[i] != b[i]) {
            return false;
         }
      }
      return true;
   }

   /**
    * <p>Returns a string of hexadecimal digits from a byte array. Each byte is
    * converted to 2 hex symbols; zero(es) included.</p>
    *
    * <p>This method calls the method with same name and three arguments as:</p>
    *
    * <pre>
    *    toString(ba, 0, ba.length);
    * </pre>
    *
    * @param ba the byte array to convert.
    * @return a string of hexadecimal characters (two for each byte)
    * representing the designated input byte array.
    */
   public static String toString(byte[] ba) {
      return toString(ba, 0, ba.length);
   }

   /**
    * <p>Returns a string of hexadecimal digits from a byte array, starting at
    * <code>offset</code> and consisting of <code>length</code> bytes. Each byte
    * is converted to 2 hex symbols; zero(es) included.
    *
    * @param ba the byte array to convert.
    * @param offset the index from which to start considering the bytes to
    * convert.
    * @param length the count of bytes, starting from the designated offset to
    * convert.
    * @return a string of hexadecimal characters (two for each byte)
    * representing the designated input byte sub-array.
    */
   public static final String toString(byte[] ba, int offset, int length) {
      char[] buf = new char[length * 2];
      for (int i = 0, j = 0, k; i < length; ) {
         k = ba[offset + i++];
         buf[j++] = HEX_DIGITS[(k >>> 4) & 0x0F];
         buf[j++] = HEX_DIGITS[ k        & 0x0F];
      }
      return new String(buf);
   }

   /**
    * <p>Returns a string of hexadecimal digits from a byte array,
    * separating each byte with the specified character.</p>
    *
    * @param ba  The byte array to convert.
    * @param off From whence in the byte array to start.
    * @param len The number of bytes to convert.
    * @param sep The character to separate individual bytes with.
    * @return The string of hexadecimal characters, two per byte, with
    *    every third character being the separator character.
    */
   public static final String toString(byte[] ba, int off, int len, char sep) {
      char[] buf = new char[len * 3 - 1];
      for (int i = 0, j = 0, k; i < len; ) {
         k = ba[off + i++];
         buf[j++] = HEX_DIGITS[(k >>> 4) & 0x0F];
         buf[j++] = HEX_DIGITS[ k        & 0x0F];
         if (j < buf.length)
            buf[j++] = sep;
      }
      return new String(buf);
   }

   public static final String toString(byte[] ba, char sep) {
      return toString(ba, 0, ba.length, sep);
   }

   /**
    * <p>Returns a byte array from a string of hexadecimal digits.</p>
    *
    * @param s a string of hexadecimal ASCII characters
    * @return the decoded byte array from the input hexadecimal string.
    */
   public static byte[] toBytesFromString(String s) {
      int limit = s.length();
      byte[] result = new byte[((limit + 1) / 2)];
      int i = 0, j = 0;
      if ((limit % 2) == 1) {
         result[j++] = (byte) fromDigit(s.charAt(i++));
      }
      while (i < limit) {
         result[j++] = (byte)(
               (fromDigit(s.charAt(i++)) << 4) | fromDigit(s.charAt(i++)));
      }
      return result;
   }

   /**
    * <p>Returns a number from <code>0</code> to <code>15</code> corresponding
    * to the designated hexadecimal digit.</p>
    *
    * @param c a hexadecimal ASCII symbol.
    */
   public static int fromDigit(char c) {
      if (c >= '0' && c <= '9') {
         return c - '0';
      } else if (c >= 'A' && c <= 'F') {
         return c - 'A' + 10;
      } else if (c >= 'a' && c <= 'f') {
         return c - 'a' + 10;
      } else
         throw new IllegalArgumentException("Invalid hexadecimal digit: " + c);
   }

   /**
    * <p>Returns a string of 8 hexadecimal digits (most significant digit first)
    * corresponding to the unsigned integer <code>n</code>.</p>
    *
    * @param n the unsigned integer to convert.
    * @return a hexadecimal string 8-character long.
    */
   public static String toString(int n) {
      char[] buf = new char[8];
      for (int i = 7; i >= 0; i--) {
         buf[i] = HEX_DIGITS[n & 0x0F];
         n >>>= 4;
      }
      return new String(buf);
   }

   /**
    * <p>Returns a string of hexadecimal digits from an integer array. Each int
    * is converted to 4 hex symbols.</p>
    */
   public static String toString(int[] ia) {
      int length = ia.length;
      char[] buf = new char[length * 8];
      for (int i = 0, j = 0, k; i < length; i++) {
         k = ia[i];
         buf[j++] = HEX_DIGITS[(k >>> 28) & 0x0F];
         buf[j++] = HEX_DIGITS[(k >>> 24) & 0x0F];
         buf[j++] = HEX_DIGITS[(k >>> 20) & 0x0F];
         buf[j++] = HEX_DIGITS[(k >>> 16) & 0x0F];
         buf[j++] = HEX_DIGITS[(k >>> 12) & 0x0F];
         buf[j++] = HEX_DIGITS[(k >>>  8) & 0x0F];
         buf[j++] = HEX_DIGITS[(k >>>  4) & 0x0F];
         buf[j++] = HEX_DIGITS[ k         & 0x0F];
      }
      return new String(buf);
   }

   /**
    * <p>Returns a string of 16 hexadecimal digits (most significant digit
    * first) corresponding to the unsigned long <code>n</code>.</p>
    *
    * @param n the unsigned long to convert.
    * @return a hexadecimal string 16-character long.
    */
   public static String toString(long n) {
      char[] b = new char[16];
      for (int i = 15; i >= 0; i--) {
         b[i] = HEX_DIGITS[(int)(n & 0x0FL)];
         n >>>= 4;
      }
      return new String(b);
   }

   /**
    * <p>Similar to the <code>toString()</code> method except that the Unicode
    * escape character is inserted before every pair of bytes. Useful to
    * externalise byte arrays that will be constructed later from such strings;
    * eg. s-box values.</p>
    *
    * @exception ArrayIndexOutOfBoundsException if the length is odd.
    */
   public static String toUnicodeString(byte[] ba) {
      return toUnicodeString(ba, 0, ba.length);
   }

   /**
    * <p>Similar to the <code>toString()</code> method except that the Unicode
    * escape character is inserted before every pair of bytes. Useful to
    * externalise byte arrays that will be constructed later from such strings;
    * eg. s-box values.</p>
    *
    * @exception ArrayIndexOutOfBoundsException if the length is odd.
    */
   public static final String
   toUnicodeString(byte[] ba, int offset, int length) {
      StringBuffer sb = new StringBuffer();
      int i = 0;
      int j = 0;
      int k;
      sb.append('\n').append("\"");
      while (i < length) {
         sb.append("\\u");

         k = ba[offset + i++];
         sb.append(HEX_DIGITS[(k >>> 4) & 0x0F]);
         sb.append(HEX_DIGITS[ k        & 0x0F]);

         k = ba[offset + i++];
         sb.append(HEX_DIGITS[(k >>> 4) & 0x0F]);
         sb.append(HEX_DIGITS[ k        & 0x0F]);

         if ((++j % 8) == 0) {
            sb.append("\"+").append('\n').append("\"");
         }
      }
      sb.append("\"").append('\n');
      return sb.toString();
   }

   /**
    * <p>Similar to the <code>toString()</code> method except that the Unicode
    * escape character is inserted before every pair of bytes. Useful to
    * externalise integer arrays that will be constructed later from such
    * strings; eg. s-box values.</p>
    *
    * @exception ArrayIndexOutOfBoundsException if the length is not a multiple
    * of 4.
    */
   public static String toUnicodeString(int[] ia) {
      StringBuffer sb = new StringBuffer();
      int i = 0;
      int j = 0;
      int k;
      sb.append('\n').append("\"");
      while (i < ia.length) {
         k = ia[i++];
         sb.append("\\u");
         sb.append(HEX_DIGITS[(k >>> 28) & 0x0F]);
         sb.append(HEX_DIGITS[(k >>> 24) & 0x0F]);
         sb.append(HEX_DIGITS[(k >>> 20) & 0x0F]);
         sb.append(HEX_DIGITS[(k >>> 16) & 0x0F]);
         sb.append("\\u");
         sb.append(HEX_DIGITS[(k >>> 12) & 0x0F]);
         sb.append(HEX_DIGITS[(k >>>  8) & 0x0F]);
         sb.append(HEX_DIGITS[(k >>>  4) & 0x0F]);
         sb.append(HEX_DIGITS[ k         & 0x0F]);

         if ((++j % 4) == 0) {
            sb.append("\"+").append('\n').append("\"");
         }
      }
      sb.append("\"").append('\n');
      return sb.toString();
   }

   public static byte[] toBytesFromUnicode(String s) {
      int limit = s.length() * 2;
      byte[] result = new byte[limit];
      char c;
      for (int i = 0; i < limit; i++) {
         c = s.charAt(i >>> 1);
         result[i] = (byte)(((i & 1) == 0) ? c >>> 8 : c);
      }

      return result;
   }

   /**
    * Encode an ASN.1 DSS signature as an SSH v.2 DSS blob (two 80-bit
    * multiple-precision integers, without lengths, separators, etc.).
    *
    * @param asn1 The ASN.1 encoded signature.
    * @return The blob.
    * @throws java.lang.IllegalArgumentException If <code>asn1</code> is
    *    not a well-formed ASN.1 sequence of two big integers.
    */
   public static byte[] ASN1ToDSSBlob(byte[] asn1)
   throws IllegalArgumentException {
      if (asn1[0] != 0x30 || asn1[2] != 0x02)
         throw new IllegalArgumentException("Poorly formed ASN.1 sequence");
      byte[] blob = new byte[40];
      int len1 = asn1[3] & 0xff;
      if (asn1[4+len1] != 0x02)
         throw new IllegalArgumentException("Poorly formed ASN.1 sequence");
      if (len1 == 21)
         System.arraycopy(asn1, 5, blob, 0, 20);
      else
         System.arraycopy(asn1, 4, blob, 20-len1, len1);
      int len2 = asn1[len1+5] & 0xff;
      if (len2 == 21)
         System.arraycopy(asn1, 7+len1, blob, 20, 20);
      else
         System.arraycopy(asn1, 6+len1, blob, 40-len2, len2);
      return blob;   
   }

// Base64 encoding derived from ISC's DHCP.
// See http://www.isc.org/products/DHCP/.

/*
 * Copyright (c) 1996 by Internet Software Consortium.
 * 
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND INTERNET SOFTWARE CONSORTIUM DISCLAIMS
 * ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL INTERNET SOFTWARE
 * CONSORTIUM BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS 
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
 * SOFTWARE.
 */

/*
 * Portions Copyright (c) 1995 by International Business Machines, Inc.
 *
 * International Business Machines, Inc. (hereinafter called IBM) grants
 * permission under its copyrights to use, copy, modify, and distribute this
 * Software with or without fee, provided that the above copyright notice and
 * all paragraphs of this notice appear in all copies, and that the name of IBM
 * not be used in connection with the marketing of any product incorporating
 * the Software or modifications thereof, without specific, written prior
 * permission.
 *
 * To the extent it has a right to do so, IBM grants an immunity from suit
 * under its patents, if any, for the use, sale or manufacture of products to
 * the extent that such products are used for performing Domain Name System
 * dynamic updates in TCP/IP networks by means of the Software.  No immunity is
 * granted for any product per se or for any other function of any product.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", AND IBM DISCLAIMS ALL WARRANTIES,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  IN NO EVENT SHALL IBM BE LIABLE FOR ANY SPECIAL,
 * DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER ARISING
 * OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE, EVEN
 * IF IBM IS APPRISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

   /** Base-64 characters. */
   private static final String BASE_64 =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

   /** Base-64 padding character. */
   private static final char BASE_64_PAD = '=';

   /**
    * Base64 encode a byte array, returning the returning string.
    *
    * @param buf The byte array to encode.
    * @param tw  The total length of any line, 0 for unlimited.
    * @return <tt>buf</tt> encoded in Base64.
    */
   public static final String base64Encode(byte[] buf, int tw) {
      int srcLength = buf.length;
      byte[] input = new byte[3];
      int[] output = new int[4];
      StringBuffer out = new StringBuffer();
      int i = 0;
      int chars = 0;

      while (srcLength > 2) {
         input[0] = buf[i++];
         input[1] = buf[i++];
         input[2] = buf[i++];
         srcLength -= 3;

         output[0] = (input[0] & 0xff) >>> 2;
         output[1] = ((input[0] & 0x03) << 4) + ((input[1] & 0xff) >>> 4);
         output[2] = ((input[1] & 0x0f) << 2) + ((input[2] & 0xff) >>> 6);
         output[3] = input[2] & 0x3f;

         out.append(BASE_64.charAt(output[0]));
         if (tw > 0 && ++chars % tw == 0)
            out.append("\n");
         out.append(BASE_64.charAt(output[1]));
         if (tw > 0 && ++chars % tw == 0)
            out.append("\n");
         out.append(BASE_64.charAt(output[2]));
         if (tw > 0 && ++chars % tw == 0)
            out.append("\n");
         out.append(BASE_64.charAt(output[3]));
         if (tw > 0 && ++chars % tw == 0)
            out.append("\n");
      }

      if (srcLength != 0) {
         input[0] = input[1] = input[2] = 0;
         for (int j = 0; j < srcLength; j++)
            input[j] = buf[i+j];
         output[0] = (input[0] & 0xff) >>> 2;
         output[1] = ((input[0] & 0x03) << 4) + ((input[1] & 0xff) >>> 4);
         output[2] = ((input[1] & 0x0f) << 2) + ((input[2] & 0xff) >>> 6);

         out.append(BASE_64.charAt(output[0]));
         if (tw > 0 && ++chars % tw == 0)
            out.append("\n");
         out.append(BASE_64.charAt(output[1]));
         if (tw > 0 && ++chars % tw == 0)
            out.append("\n");
         if (srcLength == 1) {
            out.append(BASE_64_PAD);
         } else {
            out.append(BASE_64.charAt(output[2]));
         }
         if (tw > 0 && ++chars % tw == 0)
            out.append("\n");
         out.append(BASE_64_PAD);
         if (tw > 0 && ++chars % tw == 0)
            out.append("\n");
      }
      if (tw > 0)
         out.append("\n");

      return out.toString();
   }

   /**
    * Decode a Base-64 string into a byte array.
    *
    * @param b64 The Base-64 encoded string.
    * @return The decoded bytes.
    * @throws java.io.IOException If the argument is not a valid Base-64
    *    encoding.
    */
   public static byte[] base64Decode(String b64) throws IOException {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      int state = 0, i;
      byte temp = 0;

      for (i = 0; i < b64.length(); i++) {
         if (Character.isWhitespace(b64.charAt(i))) {
            continue;
         }
         if (b64.charAt(i) == BASE_64_PAD) {
            break;
         }

         int pos = BASE_64.indexOf(b64.charAt(i));
         if (pos < 0) {
            throw new IOException("non-Base64 character " + b64.charAt(i));
         }
         switch (state) {
            case 0:
               temp = (byte) (pos - BASE_64.indexOf('A') << 2);
               state = 1;
               break;

            case 1:
               temp |= (byte) (pos - BASE_64.indexOf('A') >>> 4);
               result.write(temp);
               temp = (byte) ((pos - BASE_64.indexOf('A') & 0x0f) << 4);
               state = 2;
               break;

            case 2:
               temp |= (byte) ((pos - BASE_64.indexOf('A') & 0x7f) >>> 2);
               result.write(temp);
               temp = (byte) ((pos - BASE_64.indexOf('A') & 0x03) << 6);
               state = 3;
               break;

            case 3:
               temp |= (byte) (pos - BASE_64.indexOf('A') & 0xff);
               result.write(temp);
               state = 0;
               break;

            default:
               throw new Error("this statement should be unreachable");
         }
      }

      if (i < b64.length() && b64.charAt(i) == BASE_64_PAD) {
         switch (state) {
            case 0:
            case 1:
               throw new IOException("malformed Base64 sequence");

            case 2:
               for ( ; i < b64.length(); i++) {
                  if (!Character.isWhitespace(b64.charAt(i))) {
                     break;
                  }
               }
               // We must see a second pad character here.
               if (b64.charAt(i) != BASE_64_PAD) {
                  throw new IOException("malformed Base64 sequence");
               }
               i++;
               // Fall-through.

            case 3:
               i++;
               for ( ; i < b64.length(); i++) {
                  // We should only see whitespace after this.
                  if (!Character.isWhitespace(b64.charAt(i))) {
                     System.err.println(b64.charAt(i));
                     throw new IOException("malformed Base64 sequence");
                  }
               }
         }
      } else {
         if (state != 0) {
            throw new IOException("malformed Base64 sequence");
         }
      }

      return result.toByteArray();
   }
}
