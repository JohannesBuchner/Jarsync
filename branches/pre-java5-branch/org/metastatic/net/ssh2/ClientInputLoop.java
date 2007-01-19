/* ClientInputLoop -- Handle asynchonous input from the server.
   Copyright (C) 2002,2003  Casey Marshall <rsdio@metastatic.org>

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

import java.io.IOException;
import java.util.Map;

import org.metastatic.util.Reseeder;

/**
 * This class handles all asynchronous input in the SSH 2 protocol, once
 * the connection and authentication has been negotiated. Once this
 * method starts, nothing else reads input from the server.
 *
 * @version $Revision$
 */
class ClientInputLoop extends Thread implements SSH2Constants
{

  // Constants and variables.
  // -------------------------------------------------------------------------

  /** The connection to receive data from. */
  Connection conn;

  /** A map of channel numbers to ChannelListeners. */
  Map consumers;

  /** The reseeder. */
  Reseeder reseeder;

  X11Params x11fwd;

  // Constructor.
  // -------------------------------------------------------------------------

  /**
   * Create a new input loop thread, for an already-initialized
   * connection and a map of consumers.
   */
  ClientInputLoop(Connection conn, Map consumers, Reseeder reseeder)
  {
    this.conn = conn;
    this.consumers = consumers;
    this.reseeder = reseeder;
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public void run()
  {
    PacketInputStream pin = conn.getPacketInputStream();
    Channel c = null;
    ChannelListener cl = null;
    Channel.Spec spec;
    Integer recip;
    byte[] data;
    long data_read_so_far = 0;
    Debug.debug("Client input loop starting.");

    while (conn.connected())
      {
        synchronized (pin)
          {
            try
              {
                Debug.debug("Starting packet");
                pin.startPacket();
                byte msg_type = (byte) pin.read();
                Debug.debug("Read packet type " + msg_type);
                if (reseeder != null)
                  reseeder.touch();
                switch (msg_type)
                  {
                  case SSH_MSG_KEXINIT:
                    Debug.debug("MSG_KEXINIT: doing rekey.");
                    conn.receiveKeyExchange();
                    synchronized (conn.getPacketOutputStream())
                      {
                        conn.sendKeyExchange();
                        conn.sendKeyExchangeDH();
                      }
                    continue;

                  case SSH_MSG_KEXDH_REPLY:
                    Debug.debug("MSG_KEXDH_REPLY");
                    conn.receiveKeyExchangeDH();
                    synchronized (conn.getPacketOutputStream())
                      {
                        conn.newKeys();
                      }
                    continue;

                  case SSH_MSG_NEWKEYS:
                    Debug.debug("MSG_NEWKEYS");
                    pin.endPacket();
                    synchronized (conn.getPacketOutputStream())
                      {
                        conn.setupAlgorithms();
                      }
                    conn.unfreezeChannelOutput();
                    continue;

                  case SSH_MSG_GLOBAL_REQUEST:
                    Debug.warning("UNIMPLEMENTED: GLOBAL_REQUEST");
                    break;
                  case SSH_MSG_REQUEST_SUCCESS:
                    Debug.warning("UNIMPLEMENTED: REQUEST_SUCCESS");
                    break;
                  case SSH_MSG_REQUEST_FAILURE:
                    Debug.warning("UNIMPLEMENTED: REQUEST_FAILURE");
                    break;

                  case SSH_MSG_CHANNEL_REQUEST:
                    Debug.debug("CHANNEL_REQUEST");
                    recip = new Integer(pin.readUInt32());
                    try
                      {
                        c = (Channel) consumers.get(recip);
                        if (c == null) break;
                        cl = (ChannelListener) consumers.get(c);
                      }
                    catch (ClassCastException cce)
                      {
                      }
                    String request = pin.readASCII();
                    boolean want_reply = pin.readBoolean();
                    if (request.equals("exit-status"))
                      {
                        int status = pin.readUInt32();
                        Debug.debug("exit status=" + status);
                        cl.exit(status);
                      }
                    break;

                  case SSH_MSG_CHANNEL_OPEN:
                    {
                      Debug.debug("CHANNEL_OPEN");
                      /*
                        String type = pin.readASCII();
                        int send = pin.readUInt32();
                        int init_win = pin.readUInt32();
                        int max_pack = pin.readUInt32();
                        if (type.equals("x11")) {
                        if (x11fwd != null && x11fwd.accepting) {
                        openX11(send, init_win, max_pack);
                        } else {
                        conn.requestChannelFailure(send,
                        SSH_OPEN_ADMINISTRATIVELY_PROHIBITED,
                        "not accepting X11 connections", "");
                        }
                        } else {
                        conn.requestChannelFailure(send,
                        SSH_OPEN_UNKNOWN_CHANNEL_TYPE,
                        "unknown or unimplemented channel type", "");
                        }
                        */
                      break;
                    }

                  case SSH_MSG_CHANNEL_OPEN_CONFIRMATION:
                    Debug.debug("OPEN_CONFIRMATION");
                    recip = new Integer((int) pin.readUInt32());
                    int send = (int) pin.readUInt32();
                    int init_win = (int) pin.readUInt32();
                    int max_packet = (int) pin.readUInt32();
                    Debug.debug(" sender channel=" + send +
                                " init_window=" + init_win +
                                " max_packet=" + max_packet);
                    try
                      {
                        spec = (Channel.Spec) consumers.get(recip);
                        if (spec == null)
                          break;
                        cl = (ChannelListener) consumers.get(spec);
                        if (cl == null)
                          break;
                      }
                    catch (ClassCastException cce)
                      {
                        break;
                      }
                    spec.readTypeSpecific(pin);
                    c = new Channel(send, conn, init_win, max_packet,
                                    spec.initWindowSize(), spec.maxPacketSize(),
                                    spec.windowIncrement());
                    consumers.put(recip, c);
                    consumers.put(c, cl);
                    cl.startInput(c);
                    break;

                  case SSH_MSG_CHANNEL_OPEN_FAILURE:
                    Debug.debug("OPEN_FAILURE");
                    recip = new Integer((int) pin.readUInt32());
                    int reason = (int) pin.readUInt32();
                    String msg = pin.readUTF8();
                    pin.readASCII();
                    try
                      {
                        cl = (ChannelListener) consumers.get(recip);
                        if (cl == null)
                          break;
                      }
                    catch (ClassCastException cce)
                      {
                        break;
                      }
                    cl.openFailed(new ChannelException(reason, msg));
                    break;

                  case SSH_MSG_CHANNEL_WINDOW_ADJUST:
                    recip = new Integer((int) pin.readUInt32());
                    int increment = pin.readUInt32();
                    Debug.debug("WINDOW_ADJUST plus=" + increment);
                    c = (Channel) consumers.get(recip);
                    if (c == null) break;
                    c.adjustSendWindowSize(increment);
                    break;

                  case SSH_MSG_CHANNEL_DATA:
                    Debug.debug("CHANNEL_DATA");
                    recip = new Integer((int) pin.readUInt32());
                    data = pin.readString();
                    c = (Channel) consumers.get(recip);
                    c.adjustReceiveWindowSize(-data.length);
                    if (c.getReceiveWindowSize() < 0)
                      {
                        conn.requestWindowAdjust(c);
                      }
                    Debug.debug("channel " + recip + " window size now "
                                + c.getReceiveWindowSize());
                    data_read_so_far += data.length;
                    Debug.debug("read " + data_read_so_far +
                                " bytes on channel " + recip);
                    if (c == null) break;
                    cl = (ChannelListener) consumers.get(c);
                    if (cl != null)
                      {
                        cl.input(new ChannelEvent(this, data));
                      }
                    break;

                  case SSH_MSG_CHANNEL_EXTENDED_DATA:
                    Debug.debug("CHANNEL_EXTENDED_DATA");
                    recip = new Integer(pin.readUInt32());
                    int type = pin.readUInt32();
                    data = pin.readString();
                    c = (Channel) consumers.get(recip);
                    if (c == null) break;
                    c.adjustReceiveWindowSize(-data.length);
                    if (c.getReceiveWindowSize() < 0)
                      {
                        conn.requestWindowAdjust(c);
                      }
                    Debug.debug("channel " + recip + " window size now "
                                + c.getReceiveWindowSize());
                    data_read_so_far += data.length;
                    Debug.debug("read " + data_read_so_far +
                                " bytes on channel " + recip);
                    cl = (ChannelListener) consumers.get(c);
                    if (cl != null)
                      {
                        cl.extended(new ChannelEvent(this, data), type);
                      }
                    break;

                  case SSH_MSG_CHANNEL_EOF:
                    Debug.debug("CHANNEL_EOF");
                    recip = new Integer(pin.readUInt32());
                    try
                      {
                        c = (Channel) consumers.get(recip);
                        if (c == null) break;
                        cl = (ChannelListener) consumers.get(c);
                        if (cl == null) break;
                        cl.eof();
                      }
                    catch (ClassCastException cce)
                      {
                      }
                    break;

                  case SSH_MSG_CHANNEL_CLOSE:
                    Debug.debug("CHANNEL_CLOSE");
                    recip = new Integer((int) pin.readUInt32());
                    c = (Channel) consumers.get(recip);
                    if (c == null) break;
                    cl = (ChannelListener) consumers.get(c);
                    if (cl != null)
                      {
                        cl.closeChannel(recip.intValue());
                      }
                    consumers.remove(c);
                    consumers.remove(recip);
                    break;

                  case SSH_MSG_CHANNEL_SUCCESS:
                    Debug.debug("CHANNEL_SUCCESS");
                    recip = new Integer(pin.readUInt32());
                    c = (Channel) consumers.get(recip);
                    if (c == null) break;
                    cl = (ChannelListener) consumers.get(c);
                    if (cl != null)
                      {
                        cl.requestSuccess(new ChannelEvent(this));
                      }
                    break;

                  case SSH_MSG_CHANNEL_FAILURE:
                    Debug.debug("CHANNEL_FAILURE");
                    recip = new Integer(pin.readUInt32());
                    c = (Channel) consumers.get(recip);
                    if (c == null) break;
                    cl = (ChannelListener) consumers.get(c);
                    if (cl != null)
                      {
                        cl.requestFailure(new ChannelEvent(this));
                      }
                    break;

                  case SSH_MSG_IGNORE:
                    Debug.debug("MSG_IGNORE");
                    pin.readASCII();
                    break;

                  case SSH_MSG_DEBUG:
                    pin.readBoolean();
                    Debug.debug("MSG_DEBUG: " + pin.readASCII());
                    pin.readASCII();
                    break;

                  case SSH_MSG_DISCONNECT:
                    reason = pin.readUInt32();
                    msg = pin.readUTF8();
                    pin.readASCII();
                    Debug.debug("MSG_DISCONNECT: " + reason + " " + msg);
                    throw new DisconnectException(reason, msg);

                  default:
                    Debug.debug("UNKNOWN PACKET TYPE " + msg_type);
                    //conn.requestUnsupported(pin.getSequence());
                  }
                pin.endPacket();
              }
            catch (MACValidationException mve)
              {
                try
                  {
                    conn.disconnect(SSH_DISCONNECT_MAC_ERROR,
                                    "MAC could not be validated.");
                  }
                catch (IOException ioe)
                  {
                  }
              }
            catch (IOException ioe)
              {
                if (conn.connected())
                  {
                    ioe.printStackTrace();
                    System.exit(1);
                  }
                return;
              }
          }
      }
    Debug.debug("Client input loop ending.");
  }

/*
   private void openX11(int send, int init_win, int max_packet) {
      Integer n = null;
      try {
         n = conn.allocateChannel();
      } catch (SSH2Exception e) {
         conn.
      }
      Channel
      X11Forwarding x11 = new X11Forwarding(
   }
 */
}
