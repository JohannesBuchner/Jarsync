/* PacketOutputStream -- Binary packet output in SSH 2.
   Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>

This file is a part of HUSH, the Hopefully Uncomprehensible Shell.

HUSH is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your
option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.net.ssh2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.math.BigInteger;

import com.jcraft.jzlib.JZlib;

import org.metastatic.util.Util;

/**
 * This class facilitates output of SSH 2 data types, and, once they
 * have been negotiated, encryption, compression, and message
 * authentication.
 *
 * @version $Revision$
 */
public class PacketOutputStream extends OutputStream
{

  // Constants and variables.
  // -------------------------------------------------------------------------

  /** The size of our compression buffer. */
  static final int BUF_SIZE = 4096;

  /** Our configuration. */
  protected Configuration config;

  /** The sequence number of the current packet. */
  protected int sequence;

  /** Our payload, as it is being written. */
  protected ByteArrayOutputStream payload;

  /** The total number of bytes sent. */
  protected long total_raw_out;

  // Constructors.
  // -------------------------------------------------------------------------

  /**
   * Create a packet output stream with the default Configuration.
   */
  public PacketOutputStream()
  {
    this(new Configuration());
  }

  /**
   * Create a packet output stream with the specified Configuration.
   *
   * @param config The {@link Configuration}
   */
  public PacketOutputStream(Configuration config)
  {
    this.config = config;
    sequence = 0;
    payload = new ByteArrayOutputStream();
    total_raw_out = 0L;
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  /**
   * Get the currently-used configuration.
   *
   * @return The configuration.
   */
  public Configuration getConfig()
  {
    return config;
  }

  /**
   * Construct an appropriate binary packet as specified in
   * draft-ietf-secsh-transport-14.txt
   *
   * @return A byte array of the encoded packet, ready to be sent
   *    over the wire.
   * @throws org.metastatic.net.ssh2.SSH2Exception If compression is on
   *    but fails for some reason.
   */
  public byte[] toBinaryPacket() throws SSH2Exception
  {
    byte[] payload_bytes = payload.toByteArray();
    byte[] packet;
    int packet_length;
    int padding_length;
    byte[] padding = null;
    byte[] mac = null;

    // If compressing the payload, do it.
    if (config.flater != null)
      {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        byte[] buf = new byte[BUF_SIZE];
        config.flater.next_in = payload_bytes;
        config.flater.next_in_index = 0;
        config.flater.avail_in = payload_bytes.length;
        do
          {
            config.flater.next_out = buf;
            config.flater.next_out_index = 0;
            config.flater.avail_out = buf.length;
            int err = config.flater.deflate(JZlib.Z_PARTIAL_FLUSH);
            switch (err)
              {
              case JZlib.Z_OK:
                compressed.write(buf, 0, buf.length-config.flater.avail_out);
                break;
              default:
                throw new SSH2Exception("compression failed (" + err + ") : "
                                        + config.flater.msg);
              }
          }
        while (config.flater.avail_out == 0);
        payload_bytes = compressed.toByteArray();
      }

    // Packet must be either a multiple of 8 or of the cipher's block
    // size, whichever is larger.
    int s = 8;
    if (config.cipher != null)
      s = Math.max(8, config.cipher.currentBlockSize());
    padding_length = s * ((5+payload_bytes.length)/s+1)
      - (5+payload_bytes.length);
    if (padding_length < s) padding_length += s;
    padding = new byte[padding_length];
    if (config.random != null && config.cipher != null)
      config.random.nextBytes(padding);

    packet_length = 1+payload_bytes.length+padding.length;
    byte[] plain = new byte[packet_length+4];
    plain[0] = (byte) ((packet_length >>> 24) & 0xff);
    plain[1] = (byte) ((packet_length >>> 16) & 0xff);
    plain[2] = (byte) ((packet_length >>>  8) & 0xff);
    plain[3] = (byte) ( packet_length & 0xff);
    plain[4] = (byte) (padding.length & 0xff);
    System.arraycopy(payload_bytes, 0, plain, 5, payload_bytes.length);
    System.arraycopy(padding, 0, plain, 5+payload_bytes.length,
                     padding.length);

    // Compute the MAC if the MAC algorithm has been agreed upon.
    if (config.mac != null)
      {
        byte[] seq = { (byte) ((sequence>>>24)&0xff),
                       (byte) ((sequence>>>16)&0xff),
                       (byte) ((sequence>>>8)&0xff),
                       (byte) ( sequence&0xff) };
        config.mac.update(seq, 0, seq.length);
        config.mac.update(plain, 0, plain.length);
        mac = config.mac.digest();
        config.mac.reset();
      }

    // Encrypt the packet if we've agreed upon a cipher.
    byte[] encrypted = null;
    if (config.cipher != null)
      {
        final int bs = config.cipher.currentBlockSize();
        encrypted = new byte[plain.length];
        for (int i = 0; i < plain.length; i += bs)
          {
            config.cipher.update(plain, i, encrypted, i);
          }
      }
    else
      encrypted = plain;
    plain = null;

    // The packet is the concatenation of the encrypted packet and the MAC.
    packet = new byte[encrypted.length+((mac==null)?0:mac.length)];
    System.arraycopy(encrypted, 0, packet, 0, encrypted.length);
    if (mac != null)
      System.arraycopy(mac, 0, packet, encrypted.length, mac.length);
    encrypted = mac = null;
    sequence++;
    total_raw_out += packet.length;
    return packet;
  }

  /**
   * Retrieve the uncompressed, unencrypted payload.
   *
   * @return The raw payload.
   */
  public byte[] getPayload()
  {
    return payload.toByteArray();
  }

  /**
   * Reset the payload.
   */
  public void reset()
  {
    payload.reset();
  }

  /**
   * Write a single byte into the payload.
   *
   * @param b The byte.
   */
  public void write(int b)
  {
    payload.write(b);
  }

  /**
   * Write a byte array into the payload.
   *
   * @param b The bytes.
   */
  public void write(byte[] b)
  {
    payload.write(b, 0, b.length);
  }

  /**
   * Write a portion of a byte array into the payload.
   *
   * @param b The bytes.
   * @param off From whence in the byte array to start.
   * @param len The number of bytes to write.
   */
  public void write(byte[] b, int off, int len)
  {
    payload.write(b, off, len);
  }

  // SSH2 data types as defined in draft-ietf-secsh-architecture-12.txt

  /**
   * Write a multiple-precision integer to the payload, as an unsigned
   * 32-bit integer describing the length, then the bytes of the integer
   * in two's complement format.
   *
   * @param mpint The multiple-precision integer to write.
   */
  public void writeMPint(BigInteger mpint)
  {
    if (mpint.equals(BigInteger.valueOf(0)))
      writeUInt32(0);
    else
      writeString(mpint.toByteArray());
  }

  /**
   * Write a boolean value to the payload. This is the byte 0x01 if
   * true, 0x00 if false.
   *
   * @param b The boolean to write.
   */
  public void writeBoolean(boolean b)
  {
    if (b) write(1);
    else write(0);
  }

  /**
   * Write an int as an unsigned 32-bit integer.
   *
   * @param uint The integer to write.
   */
  public void writeUInt32(int uint)
  {
    byte[] b = new byte[] {
      (byte) (uint >>> 24), (byte) (uint >>> 16),
      (byte) (uint >>>  8), (byte)  uint
    };
    write(b);
  }

  /**
   * Write a long as an unsigned 64-bit integer.
   *
   * @param uint the integer to write.
   */
  public void writeUInt64(long uint)
  {
    byte[] b = new byte[] {
      (byte) (uint >>> 56), (byte) (uint >>> 48),
      (byte) (uint >>> 40), (byte) (uint >>> 36),
      (byte) (uint >>> 24), (byte) (uint >>> 16),
      (byte) (uint >>>  8), (byte)  uint
    };
    write(b);
  }

  /**
   * Write the length of the given byte array, followed by the array
   * itself.
   *
   * @param str The "string" to write.
   */
  public void writeString(byte[] str)
  {
    writeUInt32(str.length);
    write(str);
  }

  /**
   * Write the given string as an array of bytes, taking the encoding as
   * US-ASCII.
   *
   * @param str The string to write.
   */
  public void writeASCII(String str)
  {
    try
      {
        writeString(str.getBytes("US-ASCII"));
      }
    catch (java.io.UnsupportedEncodingException shouldNeverHappen)
      {
        throw new Error(shouldNeverHappen.toString());
      }
  }

  /**
   * Write the given string as an array of bytes, taking the encoding as
   * UTF-8
   *
   * @param str The string to write.
   */
  public void writeUTF8(String str)
  {
    try
      {
        writeString(str.getBytes("UTF-8"));
      }
    catch (java.io.UnsupportedEncodingException shouldNeverHappen)
      {
        throw new Error(shouldNeverHappen.toString());
      }
  }

  /**
   * Get the total number of bytes that have been written as binary
   * packets, through the {@link toBinaryPacket()} method.
   *
   * @return The number of byte written.
   */
  public long getTotalBytesOut()
  {
    return total_raw_out;
  }

  /**
   * Set the total number of bytes to be 0.
   */
  public void resetTotalBytesOut()
  {
    total_raw_out = 0;
  }
}
