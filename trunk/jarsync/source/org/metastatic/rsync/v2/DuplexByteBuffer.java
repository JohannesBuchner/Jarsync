/* vim:set tabstop=3 expandtab tw=72:
   $Id$
  
   DuplexBuffer: multiplexed extension to ByteBuffer.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>
  
   This file is a part of Jarsync.
  
   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2, or (at your option) any
   later version.
  
   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
  
   You should have received a copy of the GNU General Public License
   along with Jarsync; see the file COPYING.  If not, write to the
  
      Free Software Foundation Inc.,
      59 Temple Place - Suite 330,
      Boston, MA 02111-1307
      USA  */

package org.metastatic.rsync.v2;

import java.nio.ByteBuffer;

public class DuplexByteBuffer implements MultiplexedIO
{

   // Constants and fields.
   // -----------------------------------------------------------------------

   protected final ByteBuffer buffer;

   protected final ByteBuffer outputBuffer;

   protected boolean duplex;

   // Constructors.
   // -----------------------------------------------------------------------

   public DuplexByteBuffer(ByteBuffer buffer) {
      this.buffer = buffer;
      outputBuffer = ByteBuffer.allocate(4092);
      duplex = false;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void setDuplex(boolean duplex) {
      flush();
      this.duplex = duplex;
   }

   public void put(byte[] buf, int off, int len) {
      int i = 0;
      while (i < len) {
         int r = outputBuffer.remaining();
         outputBuffer.put(buf, i+off, Math.min(r, len-i));
         i += Math.min(r, len);
         if (!outputBuffer.hasRemaining()) {
            flush();
         }
      }
   }

   public void put(byte[] buf) {
      put(buf, 0, buf.length);
   }

   public void put(byte b) {
      outputBuffer.put(b);
      if (!outputBuffer.hasRemaining())
         flush();
   }

   public void putInt(int i) {
      byte[] b = new byte[4];
      b[0] = (byte) (i & 0xFF);
      b[1] = (byte) (i >>>  8 & 0xFF);
      b[2] = (byte) (i >>> 16 & 0xFF);
      b[3] = (byte) (i >>> 24 & 0xFF);
      put(b);
   }

   public void putLong(long l) {
      if ((l & ~0x7fffffff) == 0) {
         putInt((int) l);
         return;
      }
      putInt(0xFFFFFFFF);
      byte[] b = new byte[8];
      b[0] = (byte) (l & 0xff);
      b[1] = (byte) (l >>>  8 & 0xff);
      b[2] = (byte) (l >>> 16 & 0xff);
      b[3] = (byte) (l >>> 24 & 0xff);
      b[4] = (byte) (l >>> 32 & 0xff);
      b[5] = (byte) (l >>> 40 & 0xff);
      b[6] = (byte) (l >>> 48 & 0xff);
      b[7] = (byte) (l >>> 56 & 0xff);
      put(b);
   }

   public void putString(String string) {
      try {
         putInt(string.length());
         put(string.getBytes("ISO-8859-1"));
      } catch (java.io.UnsupportedEncodingException x) {
      }
   }

   public void putShortString(String string) {
      try {
         put((byte) string.length());
         put(string.getBytes("ISO-8859-1"));
      } catch (java.io.UnsupportedEncodingException x) {
      }
   }

   public void putString(int logcode, String message) {
      if (duplex) {
         flush();
         try {
            byte[] buf = message.getBytes("ISO-8859-1");
            putLogcode(logcode, buf.length);
            buffer.put(buf);
         } catch (java.io.UnsupportedEncodingException x) {
         }
      }
   }

   public String toString() {
      return "pre_buf=" + outputBuffer.toString() + " buf=" +
         buffer.toString() + " duplex=" + duplex;
   }

   public void flush() {
      outputBuffer.flip();
      if (outputBuffer.hasRemaining()) {
         putLogcode(FNONE, outputBuffer.remaining());
         buffer.put(outputBuffer);
      }
      outputBuffer.compact();
   }

   // Own methods.
   // -----------------------------------------------------------------------

   protected void putLogcode(int logcode, int len) {
      if (duplex) {
         buffer.put((byte) (len & 0xFF));
         buffer.put((byte) (len >>>  8 & 0xFF));
         buffer.put((byte) (len >>> 16 & 0xFF));
         buffer.put((byte) (logcode + MPLEX_BASE & 0xFF));
      }
   }
}
