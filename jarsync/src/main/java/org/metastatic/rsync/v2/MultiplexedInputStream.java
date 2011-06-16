/* MultiplexedInputStream -- Multiplexed input.
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
import java.io.InputStream;

import org.apache.log4j.Logger;

import org.metastatic.rsync.Util;

/**
 * Handle multiplexed I/O for the rsync protocol.
 *
 * @version $Revision$
 */
public class MultiplexedInputStream extends InputStream
  implements MultiplexedIO
{

   // Constants and variables.
   // -----------------------------------------------------------------------

  private static Logger logger =
    Logger.getLogger(MultiplexedInputStream.class.getName());

  /** The underlying input stream. */
  protected InputStream in;

  /** Whether or not to actually multiplex. */
  protected boolean multiplex;

  /** Used in readUnbuffered. */
  protected int remaining;

  protected Statistics stats;

  // Constructors.
  // -----------------------------------------------------------------------

  public MultiplexedInputStream(InputStream in, boolean multiplex)
  {
    this.in = in;
    this.multiplex = multiplex;
    this.stats = new Statistics();
  }

  // Instance methods.
  // -----------------------------------------------------------------------

  public void setInputStream(InputStream in)
  {
    this.in = in;
  }

  public void setStats(Statistics stats)
  {
    if (stats != null) this.stats = stats;
  }

  public Statistics getStats()
  {
    return stats;
  }

  public void setMultiplex(boolean multiplex)
  {
    this.multiplex = multiplex;
  }

  public int read() throws IOException
  {
    byte[] b = new byte[1];
    read(b);
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
  public int read(byte[] buf, int off, int len) throws IOException
  {
    int ret = 0, total = 0;

    //logger.debug("want to read " + len + " bytes");
    while (total < len)
      {
        if (multiplex)
          {
            ret = readUnbuffered(buf, off+total, len-total);
          }
        else
          {
            ret = in.read(buf, off+total, len-total);
          }
        total += ret;
      }
    //logger.debug("read " + total + " bytes");
    stats.total_read += total;
    return total;
  }

  /**
   * Attempt to read as many bytes as will fit into the buffer,
   * blocking until it has been filled.
   *
   * @param buf The buffer to read into.
   */
  public int read(byte[] buf) throws IOException
  {
    return read(buf, 0, buf.length);
  }

  /**
   * Read <tt>len</tt> bytes, returning them as a {@link
   * java.lang.String} interpreted as US-ASCII.
   *
   * @param len The number of bytes to read.
   * @return An ASCII string of the bytes read.
   */
  public String readString(int len) throws IOException
  {
    if (len < 1)
      return null;
    byte[] buf = new byte[len];
    read(buf);
    return new String(buf, "US-ASCII");
  }

  /**
   * Read a little-endian ordered 32-bit integer.
   *
   * @return The integer read.
   */
  public int readInt() throws IOException
  {
    byte[] buf = new byte[4];
    read(buf);
    return ((buf[3] & 0xFF) << 24) | ((buf[2] & 0xFF) << 16)
         | ((buf[1] & 0xFF) <<  8) | (buf[0] & 0xFF);
  }

  /**
   * Read a little-endian ordered 64-bit long integer.
   *
   * @return The long integer read.
   */
  public long readLong() throws IOException
  {
    int ret = readInt();
    if (ret != 0xffffffff)
      {
        return ret;
      }
    byte[] buf = new byte[8];
    read(buf);
    return ((buf[7] & 0xFF) << 56) | ((buf[6] & 0xFF) << 48)
         | ((buf[5] & 0xFF) << 40) | ((buf[4] & 0xFF) << 32)
         | ((buf[3] & 0xFF) << 24) | ((buf[2] & 0xFF) << 16)
         | ((buf[1] & 0xFF) <<  8) | (buf[0] & 0xFF);
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
  protected int readUnbuffered(byte[] buf, int off, int len) throws IOException
  {
    int ret = 0;
    int tag;
    byte[] line = new byte[1024];

    while (ret == 0)
      {
        if (remaining > 0)
          {
            len = Math.min(len, remaining);
            ret = in.read(buf, off, len);
            off += ret;
            remaining -= ret;
            continue;
          }

        in.read(line, 0, 4);
        tag = line[3] & 0xFF;

        remaining  =  line[0] & 0xFF;
        remaining |= (line[1] & 0xFF) <<  8;
        remaining |= (line[2] & 0xFF) << 16;

        if (tag == MPLEX_BASE)
            continue;

        tag -= MPLEX_BASE;

        if (tag != FERROR && tag != FINFO)
          {
            throw new IOException("illegal tag " + tag);
          }

        if (remaining > line.length - 1)
          {
            logger.fatal("multiplexing overflow " + remaining);
            throw new IOException("multiplexing overflow " + remaining);
          }

        in.read(line, 0, remaining);
        String msg = new String(line, 0, remaining);
        if (msg.endsWith("\n"))
          msg = msg.substring(0, msg.length()-1);
        if (tag == FERROR)
          logger.error(msg);
        else
          logger.info(msg);
        remaining = 0;
      }
    return ret;
  }
}
