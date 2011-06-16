// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// ChannelListener -- Standard interface for receiving input.
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
 * A channel listener is the recipient of multiplexed channel data in
 * the SSH 2 protocol. An implementation of this interface is the
 * <i>input</i> receiver for a particular channel. The exact meaning of
 * channel events is channel-specific.
 *
 * @version $Revision$
 */
public interface ChannelListener extends java.util.EventListener {

   /**
    * This method is called when a channel is newly created. After this
    * method returns channel input will be sent to this listener, and
    * the {@link Channel} passed to this method may be used for output.
    *
    * @param c The channel to be used for output.
    */
   public void startInput(Channel c);

   /**
    * This method is called when the request associated with this
    * listener failed.
    *
    * @param e The exception describing the failure.
    */
   public void openFailed(SSH2Exception e);

   /**
    * Normal data. Implementations of this method should take this data
    * as normal for whatever the underlying application is. This is, for
    * example, the standard output stream for remote applications.
    * 
    * @param e The event itself.
    */
   public void input(ChannelEvent e);

   /**
    * Extended data. In practice, the type is always STDERR data, and is
    * the standard error stream for remote applications.
    *
    * @param e The event itself.
    * @param type The type of data.
    */
   public void extended(ChannelEvent e, int type);

   /**
    * A positive reply to a previously-made request.
    */
   public void requestSuccess(ChannelEvent e);

   /**
    * A negative reply to a previously-made request.
    */
   public void requestFailure(ChannelEvent e);

   /**
    * An exit signal, with status, from a remote process.
    */
   public void exit(int status);

   /**
    * Signals that the end-of-file marker has been received.
    */
   public void eof();

   /**
    * Close this listener's channel. This method is either called to
    * <i>request</i> to close this channel, after which the client
    * should send a {@link SSH2Constants#SSH_MSG_CHANNEL_CLOSE} in reply,
    * or this is called as a <i>reply</i> to a previously-sent {@link
    * SSH2Constants#SSH_MSG_CHANNEL_CLOSE}.
    *
    * @param channel The channel number being closed.
    */
   public void closeChannel(int channel);
}
