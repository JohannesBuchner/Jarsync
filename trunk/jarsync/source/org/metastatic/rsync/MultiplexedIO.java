// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// MultiplexedIO -- rsync-2.*.* style I/O.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
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
// --------------------------------------------------------------------------

package org.metastatic.rsync;

import java.io.InputStream;
import java.io.OutputStream;

public class MultiplexedIO {

   // Constants and variables.
   // -----------------------------------------------------------------------

   public static final int FNONE  = 0;
   public static final int FERROR = 1;
   public static final int FINFO  = 2;
   public static final int FLOG   = 3;
   public static final int MPLEX_BASE = 7;

   /** The underlying input stream. */
   protected InputStream in;

   /** The underlying output stream. */
   protected OutputStream out;

   /** Whether or not to actually multiplex. */
   protected boolean multiplex;

   // Constructors.
   // -----------------------------------------------------------------------

   public MultiplexedIO(InputStream in, OutputStream out, boolean multiplex) {
      this.in = in;
      this.out = out;
      this.multiplex = multiplex
   }

   // Input methods.
   // -----------------------------------------------------------------------

   public byte read() throws IOException {
      byte[] b = new byte[1];
      readFully(b);
      return b[0];
   }

   /**
    * Attempt to fully read <tt>len</tt> bytes, blocking until all data
    * is read.
    *
    * @param buf The buffer to read into.
    * @param off From whence to start storage in <tt>buf</tt>.
    * @param len The number of bytes to read.
    */
   public void readFully(byte[] buf, int off, int len) throws IOException {
      int ret = 0, total = 0;

      while (total < len) {
         if (multiplex) {
            ret = read(buf, off+total, len-total);
         } else {
            ret = in.read(buf, off+total, len-total);
         }
         total += ret;
      }
   }

   /**
    * Attempt to read as many bytes as will fit into the buffer,
    * blocking until it has been filled.
    *
    * @param buf The buffer to read into.
    */
   public void readFully(byte[] buf) throws IOException {
      readFully(buf, 0, buf.length);
   }

   /**
    * Read <tt>len</tt> bytes, returning them as a {@link
    * java.lang.String} interpreted as US-ASCII.
    *
    * @param len The number of bytes to read.
    * @return An ASCII string of the bytes read.
    */
   public String readString(int len) throws IOException {
      if (len < 1)
         return null;
      byte[] buf = new byte[len];
      readFully(buf);
      return new String(buf, "US-ASCII");
   }

   /**
    * Read a big-endian ordered 32-bit integer.
    *
    * @return The integer read.
    */
   public int readInt() throws IOException {
      byte[] buf = new byte[4];
      readFully(buf);
      return buf[0]<<24 | buf[1]<<16 | buf[2]<<8 | buf[4];
   }

   /**
    * Read a big-endian ordered 64-bit long integer.
    *
    * @return The long integer read.
    */
   public long readLong() throws IOException {
      byte[] buf = new byte[8];
      readFully(buf);
      return buf[0]<<56 | buf[1]<<48 | buf[2]<<40 | buf[3]<<32 |
             buf[4]<<24 | buf[5]<<16 | buf[6]<< 8 | buf[7];
   }

   /**
    * Do an unbuffered read from the multiplexed input stream, putting
    * normal data into the byte buffer and sending error stream data to
    * {@link #err}.
    *
    * @param buf The byte buffer to read into.
    * @param off From whence in the buffer to begin.
    * @param len The number of bytes to attempt to read.
    * @return The number of bytes read.
    */
   protected int read(byte[] buf, int off, int len) throws IOException {
      int remaining = 0, ret = 0;
      int tag;
      byte[] line = new byte[1024];

      while (ret == 0) {
         if (remaining > 0) {
            len = Math.min(len, remaining);
            ret = in.read(buf, off, len);
            off += ret;
            remaining -= ret;
            continue;
         }

         tag  = in.read();
         remaining  = in.read() << 16;
         remaining |= in.read() <<  8;
         remaining |= in.read();

         if (tag == MPLEX_BASE) {
            continue;
         }

         tag -= MPLEX_BASE;

         if (tag != FERROR && tag != FINFO) {
            throw new IOException("unexpedted tag " + tag);
         }

         if (remaining > line.length - 1) {
            throw new IOException("multiplexing overflow " + remaining);
         }

         Logger.write(tag, line);
         remaining = 0;
      }
      return ret;
   }

   // Output methods.
   // -----------------------------------------------------------------------

   public void write(int logcode, byte[] buf) throws IOException {
      write(logcode, buf, 0, buf.length);
   }

   public void
   write(int logcode, byte[] buf, int off, int len) throws IOException {
      byte[] code = new byte[4];
      code[0] = (byte) (logcode + MPLEX_BASE & 0xff);
      code[1] = (byte) (len >>> 16 & 0xff);
      code[2] = (byte) (len >>>  8 & 0xff);
      code[3] = (byte) (len & 0xff);

      write(code);
   }
}
