// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// MultiplexedOutputStream -- Multiplexed output.
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
import java.io.OutputStream;

import org.metastatic.rsync.*;

/**
 * Multiplexed output for servers.
 *
 * @version $Revision$
 */
public class
MultiplexedOutputStream extends OutputStream implements RsyncConstants {

   // Constants and variables.
   // -----------------------------------------------------------------------

   /** The underlying output stream. */
   protected OutputStream out;

   /** Our output buffer. */
   protected byte[] outputBuffer;

   /** The number of bytes written to the buffer. */
   protected int bufferCount;

   /** Whether or not to actually multiplex. */
   protected boolean multiplex;

   // Constructors.
   // -----------------------------------------------------------------------

   public MultiplexedOutputStream(OutputStream out, boolean multiplex) {
      this.out = out;
      this.multiplex = multiplex;
      outputBuffer = new byte[OUTPUT_BUFFER_SIZE];
      bufferCount = 0;
   }

   // Output methods.
   // -----------------------------------------------------------------------

   /**
    * Write a message to the multiplexed error stream.
    *
    * @param logcode The log code.
    * @param message The message.
    */
   public void writeMessage(int logcode, String message) throws IOException {
      if (!multiplex) return;
      flush();
      write(logcode, message.getBytes("US-ASCII"));
   }

   public void flush() throws IOException {
      if (bufferCount == 0) return;
      if (multiplex) {
         System.out.println("Writing " + bufferCount + " bytes.");
         write(FNONE, outputBuffer, 0, bufferCount);
      } else {
         out.write(outputBuffer, 0, bufferCount);
      }
      bufferCount = 0;
   }

   public void write(byte[] buf) throws IOException {
      write(buf, 0, buf.length);
   }

   public void write(byte[] buf, int off, int len) throws IOException {
      while (bufferCount + len >= outputBuffer.length) {
         int count = Math.min(outputBuffer.length - bufferCount, len);
         System.arraycopy(buf, off, outputBuffer, bufferCount, count);
         flush();
         off += count;
         len -= count;
         bufferCount = 0;
      }
      System.arraycopy(buf, off, outputBuffer, bufferCount, len);
      bufferCount += len;
      if (bufferCount == outputBuffer.length) {
         flush();
      }
   }

   public void write(int b) throws IOException {
      byte[] buf = new byte[] { (byte) b };
      write(buf);
   }

   public void writeInt(int i) throws IOException {
      System.out.println("writing int= " + i);
      byte[] b = new byte[4];
      b[0] = (byte) (i & 0xff);
      b[1] = (byte) (i >>>  8 & 0xff);
      b[2] = (byte) (i >>> 16 & 0xff);
      b[3] = (byte) (i >>> 24 & 0xff);
      write(b);
   }

   public void writeLong(long l) throws IOException {
      if ((l & ~0x7fffffff) == 0) {
         writeInt((int) l);
         return;
      }
      writeInt(0xffffffff);
      byte[] b = new byte[8];
      b[0] = (byte) (l & 0xff);
      b[1] = (byte) (l >>>  8 & 0xff);
      b[2] = (byte) (l >>> 16 & 0xff);
      b[3] = (byte) (l >>> 24 & 0xff);
      b[4] = (byte) (l >>> 32 & 0xff);
      b[5] = (byte) (l >>> 40 & 0xff);
      b[6] = (byte) (l >>> 48 & 0xff);
      b[7] = (byte) (l >>> 56 & 0xff);
      write(b);
   }

   public void writeString(String s) throws IOException {
      write(s.getBytes("US-ASCII"));
   }

   protected void write(int logcode, byte[] buf) throws IOException {
      write(logcode, buf, 0, buf.length);
   }

   protected synchronized void
   write(int logcode, byte[] buf, int off, int len) throws IOException {
      byte[] code = new byte[4];
      code[0] = (byte) (len & 0xff);
      code[1] = (byte) (len >>>  8 & 0xff);
      code[2] = (byte) (len >>> 16 & 0xff);
      code[3] = (byte) (logcode + MPLEX_BASE & 0xff);

      out.write(code, 0, 4);
      out.write(buf, off, len);
      System.out.println("Wrote " + (len+4) + " byte packet:");
      System.out.println("\t" + Util.toHexString(code) +
         Util.toHexString(buf, 0, len));
   }
}
