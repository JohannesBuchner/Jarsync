// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// Channel -- One of possibly many multiplexed data streams.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of HUSH, the Hopefully Uncomprehensible Shell.
//
// HUSH is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your
// option) any later version.
//
// HUSH is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// for more details.
//
// You should have received a copy of the GNU General Public License
// along with HUSH; if not, write to the
//
//    Free Software Foundation, Inc.,
//    59 Temple Place, Suite 330,
//    Boston, MA  02111-1307
//    USA
//
// ---------------------------------------------------------------------------

package org.metastatic.net.ssh2;

import java.awt.Dimension;

import org.metastatic.util.TerminalInfo;

/**
 * A channel in SSH 2 is a data stream that is multiplexed with other
 * data streams as they travel over the encrypted connection.
 *
 * @version $Revision$
 */
public class Channel {

   // Constants and variables.
   // ------------------------------------------------------------------------

   static final Channel CONTROL_CHANNEL = new Channel();

   /**
    * The server's channel to which this one connects.
    */
   protected int recipient_channel;

   /**
    * The established connection that this (and other) channels go
    * through.
    */
   protected Connection conn;

   /**
    * The maximum number of bytes that may be recieved over this channel
    * before the window must be resized.
    */
   protected int recv_window_size;

   /**
    * The maximum number of bytes that may be sent over this channel
    * before the window must be resized.
    */
   protected int send_window_size;

   /**
    * The maximum number of bytes that may be sent in any single packet.
    */
   protected int send_max_packet_size;

   /**
    * The maximum number of bytes that may be received in any single
    * packet.
    */
   protected int recv_max_packet_size;

   /**
    * The number of bytes the window is resized by.
    */
   protected int window_increment;

 // Constructors.
   // ------------------------------------------------------------------------

   private Channel() {
      this(-1, null, 0, 0, 0, 0, 0);
   }

   Channel(int recv_channel, Connection conn, int recv_window_size,
	   int recv_max_packet_size, int send_window_size,
      int send_max_packet_size, int window_increment)
   {
      this.recipient_channel = recv_channel;
      this.conn = conn;
      this.recv_window_size = recv_window_size;
      this.recv_max_packet_size = recv_max_packet_size;
      this.send_window_size = send_window_size;
      this.send_max_packet_size = send_max_packet_size;
      this.window_increment = window_increment;
   }

   // Instance methods.
   // ------------------------------------------------------------------------

   /** Get the remote channel number for this channel. */
   int getRecipientChannel() {
      return recipient_channel;
   }

   int getSendMaxPacketSize() {
      return send_max_packet_size;
   }

   int getReceiveMaxPacketSize() {
      return recv_max_packet_size;
   }

   /** Get the number of bytes left in our send window. */
   int getSendWindowSize() {
      return send_window_size;
   }

   /**
    * Set the number of bytes for our send window. This must only be
    * called after receiving an appropriate SSH_MSG_WINDOW_ADJUST from
    * the server.
    */
   void setSendWindowSize(int window_size) {
      send_window_size = window_size;
   }

   /** In/Decrement the send window. */
   void adjustSendWindowSize(int adjustment) {
      send_window_size += adjustment;
   }

   /** Get the number of bytes left in our receive window. */
   int getReceiveWindowSize() {
      return recv_window_size;
   }

   /**
    * Set the number of bytes for the receive window. This should be set
    * only after this window is depleted and after we have sent a
    * SSH_MSG_WINDOW_ADJUST to the server.
    */
   void setReceiveWindowSize(int window_size) {
      recv_window_size = window_size;
   }

   /** In/Decrement the receive window. */
   public void adjustReceiveWindowSize(int adjustment) {
      recv_window_size += adjustment;
   }

   /**
    * Get the number of bytes to add to the receive window when it needs
    * resizing.
    */
   int getWindowIncrement() {
      return window_increment;
   }

   /** Write an entire array of SSH_CHANNEL_DATA. */
   public void writeData(byte[] data) {
      conn.requestDataWrite(this, data, 0, data.length);
   }

   /** Write part of an array of SSH_CHANNEL_DATA. */
   public void writeData(byte[] data, int off, int len) {
      conn.requestDataWrite(this, data, off, len);
   }

   /** Write SSH_CHANNEL_EXTENDED_DATA with type STDERR. */
   public void writeStderrData(byte[] data) {
      conn.requestExtendedDataWrite(this, data,
         SSH2Constants.SSH_EXTENDED_DATA_STDERR);
   }

   /** Allocate a PTY on the server. */
   public void allocatePTY(TerminalInfo t) {
      conn.requestPTY(this, t);
   }

   /** Resize the terminal. */
   public void windowChange(Dimension chars, Dimension pixels) {
      conn.requestWindowChange(this, chars, pixels);
   }

   /** Start forwarding X11 connections. */
   public void forwardX11(boolean x, byte[] method, byte[] cookie, int y) {
      //conn.requestX11(this, x, method, cookie, y);
   }

   /** Set an environment variable. */
   public void env(String name, String value) {
      conn.requestEnv(this, name, value);
   }

   /** Start an interactive shell. */
   public void startShell() {
      conn.requestShell(this);
   }

   /** Execute a remote command. */
   public void exec(String command) {
      conn.requestExec(this, command);
   }

   /** Send a signal to a remote command. */
   public void signal(String signal) {
      conn.requestSignal(this, signal);
   }

   /** Send an end-of-file. */
   public void writeEOF() {
      conn.requestChannelEOF(this);
   }

   /** Exchange new keys. */
   public void rekey() {
      try {
         Debug.debug("there are " + conn.getInputStream().available()
            + " bytes still in the stream.");
      } catch (Exception e) {
         e.printStackTrace();
      }
      synchronized (conn.getPacketOutputStream()) {
         conn.freezeChannelOutput();
         try {
            conn.sendKeyExchange();
         } catch (Exception e) {
            Debug.fatal(e.getMessage());
            try {
               conn.disconnect(
                  SSH2Constants.SSH_DISCONNECT_KEY_EXCHANGE_FAILED);
            } catch (java.io.IOException ioe) { }
         }
      }
      Debug.debug("re-kex done.");
   }

   /** Disconnect, with a reason. */
   public void disconnect(int why) {
      try {
         conn.disconnect(why);
      } catch (java.io.IOException ioe) { }
   }

   /** Disconnect, with a reason and a message. */
   public void disconnect(int why, String message) {
      try {
         conn.disconnect(why, message);
      } catch (java.io.IOException ioe) { }
   }

   /** Close this channel. */
   public void close() {
      conn.requestChannelClose(this);
   }

   Connection getConnection() {
      return conn;
   }

 // Static inner classes.
   // ------------------------------------------------------------------------

   public static final class Type {
      protected String name;
      private Type(String name){this.name=name;}
      static final Type SESSION         = new Type("session");
      static final Type X11             = new Type("x11");
      static final Type FORWARDED_TCPIP = new Type("forwarded-tcpip");
      static final Type DIRECT_TCPIP    = new Type("direct-tcpip");
      String getName() { return name; }
   }

   public static abstract class Spec {
      public abstract Type getType();
      public abstract void writeTypeSpecific(PacketOutputStream p);
      public abstract void readTypeSpecific(PacketInputStream p)
         throws java.io.IOException;
      public abstract int initWindowSize();
      public abstract int maxPacketSize();
      public abstract int windowIncrement();
   }
}
