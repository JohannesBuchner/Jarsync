/* MultiplexedOutputStream -- Multiplexed output.
   $Id$

Copyright (C) 2002,2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2, or (at your option) any
later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with Jarsync; see the file COPYING.  If not, write to the

   Free Software Foundation Inc.,
   59 Temple Place - Suite 330,
   Boston, MA 02111-1307
   USA  */


package org.metastatic.rsync.v2;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import org.metastatic.rsync.*;

/**
 * Multiplexed output for servers.
 *
 * @version $Revision$
 */
public class MultiplexedOutputStream extends OutputStream
  implements MultiplexedIO
{

  // Constants and variables.
  // -----------------------------------------------------------------------

  private static final Logger logger =
    Logger.getLogger(MultiplexedInputStream.class.getName());

  /** The underlying output stream. */
  private OutputStream out;

  /** Our output buffer. */
  private byte[] outputBuffer;

  /** The number of bytes written to the buffer. */
  private int bufferCount;

  /** Whether or not to actually multiplex. */
  private boolean multiplex;

  protected Statistics stats;

  // Constructors.
  // -----------------------------------------------------------------------

  public MultiplexedOutputStream(OutputStream out, boolean multiplex)
  {
    this.out = out;
    this.multiplex = multiplex;
    outputBuffer = new byte[4092];
    bufferCount = 0;
    stats = new Statistics();
  }

  // Instance methods.
  // -----------------------------------------------------------------------

  public void setOutputStream(OutputStream out)
  {
    this.out = out;
  }

  public void setStats(Statistics stats)
  {
    if (stats != null) this.stats = stats;
  }

  public Statistics getStats()
  {
    return stats;
  }

  /**
   * Set multiplexing value.
   *
   * @param multiplex The new multiplex value.
   */
  public void setMultiplex(boolean multiplex)
  {
    this.multiplex = multiplex;
  }

  /**
   * Write a message to the multiplexed error stream. If this object
   * was not configured with to multiplex messages, then this method
   * does nothing.
   *
   * @param logcode The log code.
   * @param message The message.
   * @throws IOException If an I/O error occurs.
   */
  public void writeMessage(int logcode, String message) throws IOException
  {
    if (!multiplex) return;
    flush();
    write(logcode, message.getBytes("US-ASCII"));
  }

  /**
   * Write any buffered bytes to the stream.
   *
   * @throws IOException If an I/O error occurs.
   */
  public void flush() throws IOException
  {
    if (multiplex)
      {
        if (bufferCount > 0)
          write(FNONE, outputBuffer, 0, bufferCount);
        bufferCount = 0;
      }
    out.flush();
  }

  /**
   * Writes a byte array to the stream.
   *
   * @param buf The bytes to write.
   * @throws IOException If an I/O error occurs.
   */
  public void write(byte[] buf) throws IOException
  {
    write(buf, 0, buf.length);
  }

  /**
   * Writes a portion of a byte array to the stream.
   *
   * @param buf The bytes to write.
   * @param off The offset from whence to begin.
   * @param len The number of bytes to write.
   * @throws IOException If an I/O error occurs.
   */
  public void write(byte[] buf, int off, int len) throws IOException
  {
    //logger.debug("writing " + len + " bytes");
    if (multiplex)
      {
        while (bufferCount + len >= outputBuffer.length)
          {
            int count = Math.min(outputBuffer.length - bufferCount, len);
            System.arraycopy(buf, off, outputBuffer, bufferCount, count);
            flush();
            off += count;
            len -= count;
            stats.total_written += count;
            bufferCount = 0;
          }
        System.arraycopy(buf, off, outputBuffer, bufferCount, len);
        bufferCount += len;
        if (bufferCount == outputBuffer.length)
          {
            flush();
          }
      }
    else
      {
        // We automatically flush the output if we are not multiplexing
        // (we must explicitly flush multiplexed output because the tag
        // bytes would clutter the stream if we did it automatically).
        out.write(buf, off, len);
        //        out.flush();
        stats.total_written += len;
      }
  }

  /**
   * Write a single byte to the stream.
   *
   * @param buf The byte to write.
   * @throws IOException If an I/O error occurs.
   */
  public void write(int b) throws IOException
  {
    byte[] buf = new byte[] { (byte) b };
    write(buf);
  }

  /**
   * Write a four-byte integer to the stream in little-endian byte
   * order.
   *
   * @param i The integer to write.
   * @throws IOException If an I/O error occurs.
   */
  public void writeInt(int i) throws IOException
  {
    byte[] b = new byte[4];
    b[0] = (byte)  i;
    b[1] = (byte) (i >>>  8);
    b[2] = (byte) (i >>> 16);
    b[3] = (byte) (i >>> 24);
    logger.debug("writing int=" + i + " " + Util.toHexString(b));
    write(b);
  }

  /**
   * Write a long integer to the stream, in little-endian order,
   * writing four bytes if the value fits into four bytes, otherwise
   * writing eight bytes.
   *
   * @param l The long integer to write.
   * @throws IOException If an I/O error occurs.
   */
  public void writeLong(long l) throws IOException
  {
    if ((l & ~0x7fffffff) == 0)
      {
        writeInt((int) l);
        return;
      }
    writeInt(0xffffffff);
    byte[] b = new byte[8];
    b[0] = (byte)  l;
    b[1] = (byte) (l >>>  8);
    b[2] = (byte) (l >>> 16);
    b[3] = (byte) (l >>> 24);
    b[4] = (byte) (l >>> 32);
    b[5] = (byte) (l >>> 40);
    b[6] = (byte) (l >>> 48);
    b[7] = (byte) (l >>> 56);
    write(b);
  }

  public void writeString(String s) throws IOException
  {
    write(s.getBytes("US-ASCII"));
  }

  protected void write(int logcode, byte[] buf) throws IOException
  {
    write(logcode, buf, 0, buf.length);
  }

  protected synchronized void write(int logcode, byte[] buf, int off, int len)
    throws IOException
  {
    byte[] code = new byte[4];
    code[0] = (byte) (len & 0xff);
    code[1] = (byte) (len >>>  8 & 0xff);
    code[2] = (byte) (len >>> 16 & 0xff);
    code[3] = (byte) (logcode + MPLEX_BASE & 0xff);

    out.write(code, 0, 4);
    out.write(buf, off, len);
    //logger.debug("Wrote " + (len+4) + " byte packet:");
    //logger.debug("\t" + Util.toHexString(code) +
    //             Util.toHexString(buf, 0, len));
  }
}
