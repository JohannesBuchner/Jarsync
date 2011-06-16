/* PacketInputStream -- Reads SSH 2 binary packets.
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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.math.BigInteger;

import com.jcraft.jzlib.JZlib;

import org.metastatic.util.Util;

/**
 * <p>This class provides the high-level methods needed to read data types
 * from SSH version 2.0's binary packet protocol. To use this class
 * properly requires an understanding of the underlying protocol,
 * defined in <a href="http://www.snailbook.com/docs/transport.txt">"SSH
 * Transport Layer Protocol"</a>.</p>
 *
 * <p>The basic process of reading packets is as follows:</p>
 *
 * <blockquote>
 * <p><code>packet.startPacket();<br>
 * byte msg_type = (byte) packet.read();<br>
 * zero or more calls to packet.read*()...<br>
 * packet.endPacket;</code></p>
 * </blockquote>
 *
 * <p>The exact sequence of the <code>read*()</code> methods depends on
 * the type of packet being read. Descriptions of the sequence of data
 * types for the various message types are described in</p>
 *
 * <ul>
 * <li><a href="http://www.snailbook.com/docs/userauth.txt">"SSH
 * Authentication Protoco"l</a></li>
 * <li><a href="http://www.snailbook.com/docs/connection.txt">"SSH
 * Connection Protocol"</a></li>
 * </ul>
 *
 * <p>The various data types that may be read from SSH binary packets
 * are described in <a
 * href="http://www.snailbook.com/docs/architecture.txt">"SSH Protocol
 * Architecture"</a></p>
 *
 * @version $Revision$
 */
public class PacketInputStream extends InputStream
{

  // Constants and variables.
  // -------------------------------------------------------------------------

  public static final int MAX_PAYLOAD_SIZE = 32773;

  /**
   * Our internal decompression buffer.
   */
  private static final int BUF_SIZE = 4096;

  /**
   * The current configuration of the packets being input. This includes
   * the cipher (which is in DECRYPT_MODE) and the inflater. The MAC is
   * also active, to get access to the mac length.
   */
  Configuration config;

  /**
   * The source of data coming in, which may be encrypted and
   * compressed.
   */
  InputStream in;

  /**
   * The buffer into which unencrypted, uncompressed data is placed.
   */
  protected byte[] buffer;

  protected int mark;

  protected int limit;

  /**
   * The uncompressed, unencrypted payload that has been read thusfar.
   */
  ByteArrayOutputStream payload;

  /**
   * The compressed payload if it was compressed.
   */
  protected ByteArrayOutputStream compressed_payload;

  /**
   * The sequence number of the packet being read.
   */
  protected int sequence;

  /**
   * The length of the packet being read.
   */
  protected int packet_length;

  /**
   * The length of the padding.
   */
  protected int padding_length;

  /**
   * The number of bytes read from this packet thusfar.
   */
  protected int total_in;

  /**
   * Total number of raw bytes read.
   */
  protected int raw_in;

  /**
   * The total number of bytes read from the server.
   */
  protected long total_raw_in;

  protected int inflated_length;

  protected boolean reading_mac;

  final protected byte[] intBuffer = new byte[4];
  final protected byte[] longBuffer = new byte[8];

  // Public constructor.
  // -------------------------------------------------------------------------

  /**
   * Create a new packet input stream, with a given configuration and a
   * source to read packet data from.
   */
  public PacketInputStream(Configuration config, InputStream in)
  {
    this.config = config;
    this.in = in;
    payload = new ByteArrayOutputStream();
    compressed_payload = new ByteArrayOutputStream();
    buffer = new byte[MAX_PAYLOAD_SIZE];
    mark = limit = 0;
    packet_length = padding_length = -1;
    sequence = 0;
    total_in = 0;
    raw_in = 0;
    total_raw_in = 0L;
    inflated_length = 0;
    reading_mac = false;
  }

  public PacketInputStream(InputStream in)
  {
    this(new Configuration(), in);
  }

// Instance methods overriding InputStream.
  // -------------------------------------------------------------------------

  /**
   * Return the number of bytes that may be read without blocking.
   */
  public int available() throws IOException
  {
    return limit - mark;
  }

  /**
   * Read a single byte from the packet.
   */
  public int read() throws IOException, IllegalStateException
  {
    while (available() == 0)
      update();
    if (mark + 1 > limit)
      {
        throw new IOException("Buffer underflow.");
      }
    int b = buffer[mark++] & 0xff;
    if (packet_length != -1 && padding_length != -1)
      {
        payload.write(b);
        total_in++;
      }
    Debug.debug2("Read one byte, available=" + available());
    return b;
  }

  /**
   * Read an array of bytes from the packet.
   */
  public int read(byte[] buf) throws IOException, IllegalStateException
  {
    return read(buf, 0, buf.length);
  }

  /**
   * Read an array of bytes into a specified portion of an array.
   */
  public int read(byte[] buf, int off, int len)
    throws IOException, IllegalStateException
  {
    while (available() < len)
      update();
    if (mark + len > limit)
      {
        throw new IOException("Buffer underflow.");
      }
    System.arraycopy(buffer, mark, buf, off, len);
    mark += len;
    if (packet_length != -1 && padding_length != -1
        && total_in < packet_length)
      {
        payload.write(buf, off, len);
        total_in += len;
      }
    Debug.debug2("Read " + len + " bytes, available=" + available());
    return len;
  }

// Public instance methods.
  // -------------------------------------------------------------------------

  /**
   * Begin a packet. This method will read in 8 bytes or the block size
   * of the cipher, whichever is larger, and will store the first five
   * unencrypted bytes into the packet_length and padding_length fields.
   */
  public void startPacket() throws IOException, IllegalStateException
  {
    raw_in = 0;
    packet_length = readUInt32();
    padding_length = read();
    total_in++;
    Debug.debug2("startPacket packet_length=" + packet_length
                 + " padding_length=" + padding_length);
    if (packet_length < 1 || packet_length > 35000)
      {
        throw new SSH2Exception("illegal packet length " + packet_length);
      }
  }

  /**
   * End the packet, discarding the padding bits, and validating the
   * MAC, if a MAC algorithm has been agreed upon.
   *
   * @throws SSH2Exception If the MAC cannot be validated. Further
   *   packets may still be read if MAC validation fails, but this is
   *   not recommended.
   */
  public void endPacket() throws IOException, IllegalStateException
  {
    Debug.debug2("Ending packet; raw_in=" + raw_in +
                 " packet_length=" + packet_length + " padding_length=" +
                 padding_length + " inflated_length=" + inflated_length);
    if (config.flater == null && total_in < packet_length - padding_length)
      {
        mark += packet_length - padding_length - total_in;
        Debug.debug2("Discarding " + (packet_length - padding_length
                                      - total_in));
      }
    else if (config.flater != null && total_in - 1 < inflated_length)
      {
        mark += inflated_length - total_in + 1;
        Debug.debug2("Discarding inflated=" + (inflated_length - total_in+1));
      }
    byte[] padding = new byte[padding_length];
    int len = read(padding);
    Debug.debug2(len + " bytes of padding read");
    if (config.mac != null)
      {
        byte[] mac = readMac();
        Debug.debug2("MAC: " + Util.toString(mac));
        config.mac.update((byte)(sequence >>> 24 & 0xff));
        config.mac.update((byte)(sequence >>> 16 & 0xff));
        config.mac.update((byte)(sequence >>>  8 & 0xff));
        config.mac.update((byte)(sequence & 0xff));
        config.mac.update((byte)(packet_length >>> 24 & 0xff));
        config.mac.update((byte)(packet_length >>> 16 & 0xff));
        config.mac.update((byte)(packet_length >>>  8 & 0xff));
        config.mac.update((byte)(packet_length & 0xff));
        config.mac.update((byte) padding_length);
        if (config.flater == null)
          {
            byte[] b = getPayload();
            config.mac.update(b, 0, b.length);
          }
        else
          {
            byte[] b = compressed_payload.toByteArray();
            config.mac.update(b, 0, b.length);
            config.mac.update(padding, 0, padding.length);
          }
        byte[] my_mac = config.mac.digest();
        config.mac.reset();
        Debug.debug2("my MAC: " + Util.toString(my_mac));
        Debug.debug2("MAC: " + Util.toString(mac));
        if (!java.util.Arrays.equals(mac, my_mac))
          {
            sequence++;
            payload.reset();
            if (config.flater != null)
              {
                compressed_payload.reset();
                inflated_length = 0;
              }
            packet_length = padding_length = -1;
            total_in = 0;
            mark = limit = 0;
            Debug.warning("MAC not validated.");
            throw new MACValidationException();
          }
        Debug.debug2("MAC validated: " + Util.toString(my_mac));
      }
    sequence++;
    payload.reset();
    if (config.flater != null)
      {
        compressed_payload.reset();
        inflated_length = 0;
      }
    packet_length = padding_length = -1;
    total_in = 0;
    mark = limit = 0;
  }

  /**
   * Get the decrypted, uncompressed payload.
   */
  public byte[] getPayload()
  {
    return payload.toByteArray();
  }

  /**
   * Get the packet's length, not counting the length field itself.
   */
  public int getPacketLength()
  {
    return packet_length;
  }

  /**
   * Get the length of the padding.
   */
  public int getPaddingLength()
  {
    return padding_length;
  }

  /**
   * Get the number of bytes read thusfar, not counting the four bytes
   * of the packet length field.
   */
  public int getTotalIn()
  {
    return total_in;
  }

  /**
   * Get the number of bytes left in the payload.
   */
  public int getRemaining()
  {
    return packet_length - total_in - padding_length;
  }

  /**
   * Read a boolean value from the packet (one byte).
   */
  public boolean readBoolean() throws IOException, IllegalStateException
  {
    int i = read();
    return (i != 0);
  }

  /**
   * Read an unsigned 32-bit integer from the packet.
   */
  public int readUInt32() throws IOException, IllegalStateException
  {
    read(intBuffer);
    Debug.debug2("read int=" + Util.toString(intBuffer));
    int i = ((intBuffer[0] & 0xff) << 24) | ((intBuffer[1] & 0xff) << 16)
          | ((intBuffer[2] & 0xff) <<  8) | ( intBuffer[3] & 0xff);
    Debug.debug2(" " + Integer.toHexString(i));
    return i;
  }

  /**
   * Read an unsigned 64-bit integer from the packet.
   */
  public long readUInt64() throws IOException, IllegalStateException
  {
    read(longBuffer);
    Debug.debug2(">>read long=" + Util.toString(longBuffer));
    long l = (longBuffer[0] << 56 & 0xff) | (longBuffer[1] << 48 & 0xff)
           | (longBuffer[2] << 40 & 0xff) | (longBuffer[3] << 32 & 0xff)
           | (longBuffer[4] << 24 & 0xff) | (longBuffer[5] << 16 & 0xff)
           | (longBuffer[6] <<  8 & 0xff) | (longBuffer[7] & 0xff);
    Debug.debug2(" " + Long.toHexString(l));
    return l;
  }

  /**
   * Read a "string" -- an arbitrary-length byte array.
   */
  public byte[] readString() throws IOException, IllegalStateException
  {
    int len = (int) readUInt32();
    byte[] buf = new byte[len];
    if (len == 0) return buf;
    read(buf);
    return buf;
  }

  /**
   * Read a string from the packet, encoded in US-ASCII.
   */
  public String readASCII() throws IOException, IllegalStateException
  {
    try
      {
        return new String(readString(), "US-ASCII");
      }
    catch (java.io.UnsupportedEncodingException shouldNeverHappen)
      {
        throw new Error(shouldNeverHappen.toString());
      }
  }

  /**
   * Read a string from the packet, encoded in UTF-8.
   */
  public String readUTF8() throws IOException, IllegalStateException
  {
    try
      {
        return new String(readString(), "UTF-8");
      }
    catch (java.io.UnsupportedEncodingException shouldNeverHappen)
      {
        throw new Error(shouldNeverHappen.toString());
      }
  }

  /**
   * Read the message authentication code from the packet. This method
   * reads the number of bytes specified by the {@link javax.crypto.Mac}
   * field of this packet's {@link #config}.
   *
   * @return The MAC.
   */
  protected byte[] readMac() throws IOException
  {
    if (config.mac == null) return null;
    reading_mac = true;
    byte[] buf = new byte[config.mac.macSize()];
    read(buf);
    reading_mac = false;
    return buf;
  }

  /**
   * Return the sequence number of the current packet. This value is
   * treated as an unsigned 32-bit integer (it is incremented by one for
   * every new packet, thus the returned value, although signed, is
   * treated as unsigned).
   *
   * @return The sequence number.
   */
  public int getSequence()
  {
    return sequence;
  }

  /**
   * Reset the internal payload buffer.
   */
  public void resetPayload()
  {
    payload.reset();
  }

  /**
   * Read an arbitrary-precision integer from the packet. This method
   * reads in a four-byte unsinged integer, then as many bytes as that
   * integer represents, then takes those bytes as a two's complement
   * signed integer.
   *
   * @return a {@link java.math.BigInteger} constructed from the bytes
   *   read in.
   */
  public BigInteger readMPint() throws IOException, IllegalStateException
  {
    return new BigInteger(readString());
  }

  public long getTotalBytesIn()
  {
    return total_raw_in;
  }

  public void resetTotalBytesIn()
  {
    total_raw_in = 0;
  }

// Own methods.
  // -------------------------------------------------------------------------

  protected void updateBuffer(byte[] buf, int off, int len)
    throws IOException
  {
    Debug.debug2("updateBuffer off=" + off + " len=" + len +
                 " buf.length=" + buf.length);
    if (limit + len > buffer.length)
      throw new IOException("buffer overflow");
    System.arraycopy(buf, off, buffer, limit, len);
    limit += len;
  }

  /**
   * Read some data from the source input stream, feeding it into the
   * cipher, and sending the decrypted bytes into our internal buffer.
   * If the payload is compressed, the entire payload will be read then
   * decompressed.
   *
   * @throws java.net.IOException If reading from the source input stream
   *   fails.
   * @throws org.metastatic.net.ssh.SSH2Exception If decompression fails.
   * @throws java.lang.IllegalStateException If decryption fails.
   */
  protected void update() throws IOException, IllegalStateException
  {
    if (reading_mac)
      {
        byte[] mac_buf = new byte[config.mac.macSize()];
        int mac_len = 0;
        int mac_off = 0;
        do
          {
            mac_len += in.read(mac_buf, mac_off, mac_buf.length-mac_off);
            mac_off = mac_len;
          }
        while (mac_len < mac_buf.length);
        updateBuffer(mac_buf, 0, mac_buf.length);
        total_raw_in += mac_len;
        return;
      }

    byte[] buf = null;
    if (config.cipher == null)
      {
        buf = new byte[8];
      }
    else
      {
        buf = new byte[Math.max(config.cipher.currentBlockSize(),8)];
      }
    int len = 0;
    int off = 0;
    //len = in.read(buf);
    while ((len += in.read(buf, off, buf.length - off)) < buf.length)
      {
        if (len <= 0)
          throw new EOFException("Unexpected EOF in input stream.");
        off += len;
        Debug.debug2(len + " bytes from stream");
      }
    raw_in += len;
    total_raw_in += len;
    byte[] buf2;

    Debug.debug2(len + " bytes from stream. " + raw_in +
                 " bytes so far");

    if (config.cipher != null)
      {
        if (raw_in <= packet_length+4 || packet_length == -1)
          {
            final int bs = config.cipher.currentBlockSize();
            buf2 = new byte[buf.length];
            for (int i = 0; i < len; i += bs)
              {
                config.cipher.update(buf, i, buf2, i);
              }
            buf = buf2;
            len = buf.length;
            Debug.debug2(len + " packet bytes");
            Debug.debug3("bytes=" + Util.toString(buf, 0, len));
         }
      }

    Debug.debug2("raw_in-len=" + (raw_in-len) +
                 " packet_length-padding_length+4=" +
                 (packet_length-padding_length+4));

    if (config.flater != null)
      {
        if (raw_in == len)
          {
            // beginning of packet
            updateBuffer(buf, 0, 5);
            Debug.debug2("start of packet; limit=" + limit);
            int l = ((buf[0] & 0xff) << 24) | ((buf[1] & 0xff) << 16)
                  | ((buf[2] & 0xff) <<  8) |  (buf[3] & 0xff);
            Debug.debug2("this packet length=" + l +
                         " this padding length=" + buf[4]);
            if (raw_in == l+4)
              {
                // a full, short packet.
                Debug.debug2("full, short packet.");
                Debug.debug2("buf.length=" + buf.length + " len-5-buf[4]="
                             + (len-5-buf[4]) + " buf[4]=" + buf[4]);
                compressed_payload.write(buf, 5, len-5-buf[4]);
                decompress();
                updateBuffer(buf, len-buf[4], buf[4]);
              }
            else if (raw_in > l - buf[4] + 4)
              {
                Debug.debug2("Short packet, some padding.");
                compressed_payload.write(buf, 5, l-buf[4]-1);
                decompress();
                updateBuffer(buf, 5+(l-buf[4]-1), len-(5+(l-buf[4]-1)));
              }
            else
              {
                compressed_payload.write(buf, 5, len-5);
              }
            return;
          }
        else if (raw_in < packet_length-padding_length+4)
          {
            // middle of payload
            Debug.debug2("middle of payload");
            compressed_payload.write(buf, 0, len);
            return;
          }
        else if (raw_in == packet_length-padding_length+4)
          {
            // end of payload
            Debug.debug2("end of payload");
            compressed_payload.write(buf, 0, len);
            decompress();
            return;
          }
        else if (raw_in > packet_length-padding_length+4
                 && raw_in-len < packet_length-padding_length+4)
          {
            // some payload, some padding
            Debug.debug2("some payload, some padding");
            compressed_payload.write(buf, 0,
              (packet_length-padding_length+4) - (raw_in-len));
            decompress();
            updateBuffer(buf, (packet_length-padding_length+4)-(raw_in-len),
                         len-((packet_length-padding_length+4)-(raw_in-len)));
            Debug.debug3("limit now=" + limit);
            return;
          }
        else
          {
            // Just padding
            if (inflated_length == 0)
              {
                decompress();
              }
            Debug.debug2("Just padding.");
            updateBuffer(buf, 0, len);
            return;
          }
      }

    if (len > 0)
      {
        updateBuffer(buf, 0, len);
      }
    Debug.debug3("available=" + available());
  }

  /**
   * Decompress a payload, using the JZlib implementation of zlib in
   * Z_PARTIAL_FLUSH mode.
   *
   * @throws java.io.IOException If the buffer overflows.
   * @throws org.metastatic.net.ssh.SSH2Exception If decompression fails.
   */
  private void decompress() throws IOException
  {
    byte[] buf2 = new byte[BUF_SIZE];
    config.flater.next_in = compressed_payload.toByteArray();
    config.flater.next_in_index = 0;
    config.flater.avail_in = compressed_payload.size();

    Debug.debug2("decompressing " + compressed_payload.size());

    for(;;)
      {
        config.flater.next_out = buf2;
        config.flater.next_out_index = 0;
        config.flater.avail_out = buf2.length;
        int err = config.flater.inflate(JZlib.Z_PARTIAL_FLUSH);
        switch (err)
          {
          case JZlib.Z_OK:
            updateBuffer(buf2, 0, buf2.length - config.flater.avail_out);
            inflated_length += buf2.length - config.flater.avail_out;
            Debug.debug3("decompressed="
                         + (buf2.length - config.flater.avail_out));
            break;
          case JZlib.Z_BUF_ERROR:
            return;
          default:
            throw new SSH2Exception("decompression failed (code " + err
                                    + "): " + config.flater.msg);
          }
      }
  }
}
