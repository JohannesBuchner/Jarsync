// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// MultiplexedInputStream -- Multiplexed input.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
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
// --------------------------------------------------------------------------

package org.metastatic.rsync.v2;

import java.io.IOException;
import java.io.InputStream;

import org.metastatic.rsync.*;

/**
 * Handle multiplexed I/O for the rsync protocol.
 *
 * @version $Revision$
 */
public class
MultiplexedInputStream extends InputStream implements RsyncConstants {

   // Constants and variables.
   // -----------------------------------------------------------------------

   /** The underlying input stream. */
   protected InputStream in;

   /** Whether or not to actually multiplex. */
   protected boolean multiplex;

   // Constructors.
   // -----------------------------------------------------------------------

   public MultiplexedInputStream(InputStream in, boolean multiplex) {
      this.in = in;
      this.multiplex = multiplex;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public int read() throws IOException {
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
    * Read a little-endian ordered 32-bit integer.
    *
    * @return The integer read.
    */
   public int readInt() throws IOException {
      byte[] buf = new byte[4];
      readFully(buf);
      return buf[3]<<24 | buf[2]<<16 | buf[1]<<8 | buf[0];
   }

   /**
    * Read a little-endian ordered 64-bit long integer.
    *
    * @return The long integer read.
    */
   public long readLong() throws IOException {
      int ret = readInt();
      if (ret != 0xffffffff) {
         return ret;
      }
      byte[] buf = new byte[8];
      readFully(buf);
      return buf[7]<<56 | buf[6]<<48 | buf[5]<<40 | buf[4]<<32 |
             buf[3]<<24 | buf[2]<<16 | buf[1]<< 8 | buf[0];
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
   public int read(byte[] buf, int off, int len) throws IOException {
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
         System.out.println("mio.read().ret=" + 0);

         in.read(line, 0, 4);
         for (int i = 0; i < 4; i++)
            System.out.print(Integer.toHexString(line[i]));
         System.out.println();

         tag = line[3];
         System.out.println("tag=" + tag);

         remaining  = line[0];
         remaining |= line[1] <<  8;
         remaining |= line[2] << 16;
         System.out.println("remaining=" + remaining);

         if (tag == MPLEX_BASE) {
            continue;
         }

         tag -= MPLEX_BASE;

         if (tag != FERROR && tag != FINFO) {
            throw new IOException("unexpected tag " + tag);
         }

         if (remaining > line.length - 1) {
            throw new IOException("multiplexing overflow " + remaining);
         }

         in.read(line, 0, remaining);
         Logger.write(tag, line);
         remaining = 0;
      }
      return ret;
   }
}
