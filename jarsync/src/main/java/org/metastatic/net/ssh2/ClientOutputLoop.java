// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// ClientOutputLoop -- Handle the channel connections.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of HUSH, the Hopefully Uncomprehensible Shell.
//
// HUSH is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your
// option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the
//
//    Free Software Foundation, Inc.,
//    59 Temple Place, Suite 330,
//    Boston, MA  02111-1307
//    USA
//
// ---------------------------------------------------------------------------

package org.metastatic.net.ssh2;

import java.io.IOException;
import java.io.OutputStream;

import java.util.LinkedList;

/**
 * This thread waits for channel data to become available, and sends it
 * out over the wire, first-in-first-out.
 *
 * @version $Revision$
 */
class ClientOutputLoop extends Thread implements SSH2Constants {

 // Constants and variables.
   // ------------------------------------------------------------------------

   protected LinkedList channel_queue;
   protected Connection conn;
   protected boolean frozen;

   // Constructors.
   // ------------------------------------------------------------------------

   ClientOutputLoop(Connection conn, LinkedList channel_queue) {
      this.channel_queue = channel_queue;
      this.conn = conn;
   }

   // Instance methods overriding java.lang.Thread
   // ------------------------------------------------------------------------

   public void run() {
      PacketOutputStream pout = conn.getPacketOutputStream();
      OutputStream out = conn.getOutputStream();
      frozen = false;
      Debug.debug("Client output loop started.");
      while (conn.connected()) {
         synchronized (channel_queue) {
            while (channel_queue.size() == 0) {
               try {
                  Debug.debug2("Waiting on " + channel_queue);
                  channel_queue.wait();
               } catch (InterruptedException ie) { }
            }
            while (frozen);  // wait.
         }
         synchronized (pout) {
            synchronized (out) {
               ChannelPacket p = (ChannelPacket) channel_queue.removeFirst();
               int rem = p.getChannel().getSendWindowSize();
               Debug.debug("window size=" + rem + " data size="
                  + p.getDataLength());
               if (p.getChannel() != Channel.CONTROL_CHANNEL
                     && p.consumesWindow() && rem < p.getDataLength()) {
                  // Put it back, and wait for more window space.
                  // Note that we could be smarter here.
                  channel_queue.addFirst(p);
                  continue;
               }
               try {
                  Debug.debug("Writing packet.");
                  pout.reset();
                  pout.write(p.getPayload());
                  out.write(pout.toBinaryPacket());
               } catch (IOException ioe) { }
               if (p.consumesWindow()) {
                  p.getChannel().adjustSendWindowSize(-p.getDataLength());
               }
            }
         }
      }
      Debug.debug("Client output loop ending.");
   }

   void freeze() {
      frozen = true;
   }

   void unfreeze() {
      frozen = false;
   }
}
