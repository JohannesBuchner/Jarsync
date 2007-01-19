// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// ChannelPacket -- A packet to be sent over the wire.
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

/**
 * <p>This is a packet as it waits in the queue to be sent over the wire.
 * It contains only two fields, the {@link #channel}, which provides the
 * SSH2 channel's recipient ID and window size, and the {@link
 * #payload}, which is the raw data to be sent over the connection. This
 * class gives merely the convenience of having both in the same place.</p>
 */
class ChannelPacket {

   // Constants and variables.
   // -----------------------------------------------------------------------

   /**
    * The channel this packet will be sent over.
    */
   protected Channel channel;

   /**
    * The payload of this packet.
    */
   protected byte[] payload;

   /**
    * The size of window-consumable data.
    */
   protected int data_length;

   ChannelPacket(Channel channel, byte[] payload, int data_length) {
      this.channel = channel;
      this.payload = payload;
      this.data_length = data_length;
   }

   Channel getChannel() {
      return channel;
   }

   byte[] getPayload() {
      return payload;
   }

   int getDataLength() {
      return data_length;
   }

   boolean consumesWindow() {
      return data_length > 0;
   }
}
