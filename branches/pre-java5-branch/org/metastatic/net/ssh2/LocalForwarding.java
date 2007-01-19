// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// LocalForwarding -- TCP/IP forwarding local->remote.
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
// --------------------------------------------------------------------------

package org.metastatic.net.ssh2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import java.util.HashSet;
import java.util.Iterator;

/**
 * This class implements <i>local to remote</i> TCP/IP forwarding. This
 * essentially means that a simple server will operate on the specified
 * port, taking input coming over that port, sending it over the secure
 * channel, then the server will forward it to its destination port.
 *
 * @version $Revision$
 */
class LocalForwarding extends Thread {

   // Constants and fields.
   // -----------------------------------------------------------------------

   protected static final int WAIT_TIMEOUT = 300000;

   /** The server socket that accepts connections. */
   protected ServerSocket socket;

   /** The SSH connection that we forward over. */
   protected Connection conn;

   /** True when we are alive. */
   protected boolean running;

   protected HashSet servers;

   // Constructors.
   // -----------------------------------------------------------------------

   LocalForwarding(int local_port, String host, int remote_port,
      Connection conn)
   throws IOException
   {
      this.conn = conn;
      socket = new ServerSocket(local_port);
      socket.setSoTimeout(WAIT_TIMEOUT);
      servers = new HashSet();
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void run() {
      running = true;
      while (running) {
         Socket s = null;
         try {
            s = socket.accept();
         } catch (SocketTimeoutException ste) {
            continue;
         } catch (SecurityException se) {
            break;
         } catch (IOException ioe) {
            break;
         }
         try {
            Server server = new Server(s, this);
            conn.newChannel(new DirectSpec(), server);
            servers.add(server);
         } catch (IOException ioe) {
         }
      }
      stopServers();
   }

   void stopServers() {
      for (Iterator it = servers.iterator(); it.hasNext(); ) {
         ((Server) it.next()).closeServer();
      }
   }

   void serverClosed(Server s) {
      servers.remove(s);
   }

 // Inner classes.
   // -----------------------------------------------------------------------

   private class Server extends Thread implements ChannelListener {

      // Constants and fields.
      // --------------------------------------------------------------------

      protected static final int BUFFER_SIZE = 4096;

      protected boolean running;

      protected Socket socket;

      protected InputStream in;

      protected OutputStream out;

      protected Channel c;

      protected LocalForwarding forwarding;

      protected boolean i_sent_close;

      // Constructors.
      // --------------------------------------------------------------------

      Server(Socket socket, LocalForwarding forwarding) throws IOException {
         this.socket = socket;
         in = socket.getInputStream();
         out = socket.getOutputStream();
         this.forwarding = forwarding;
         i_sent_close = false;
      }

      // Instance methods.
      // --------------------------------------------------------------------

      void closeServer() {
         running = false;
         try {
            socket.close();
         } catch (IOException ioe) {
         }
      }

      // Methods overriding Thread.
      // --------------------------------------------------------------------

      public void run() {
         int len = 0;
         byte[] buffer = new byte[BUFFER_SIZE];
         while (running) {
            try {
               len = in.read(buffer);
               if (len == -1) {
                  closeChannel(0);
               }
            } catch (IOException ioe) {
            }
            c.writeData(buffer, 0, len);
         } 
      }

      // Methods implementing ChannelListener.
      // --------------------------------------------------------------------

      public void startInput(Channel c) {
         this.c = c;
         running = true;
         start();
      }

      public void openFailed(SSH2Exception ex) {
      }

      public void input(ChannelEvent e) {
         if (running) {
            try {
               out.write(e.getData());
            } catch (IOException ioe) {
            }
         }
      }

      public void extended(ChannelEvent e, int type) {
      }

      public void requestSuccess(ChannelEvent e) {
      }

      public void requestFailure(ChannelEvent e) {
      }

      public void exit(int status) {
      }

      public void eof() {
      }

      public void closeChannel(int channel) {
         if (!i_sent_close) {
            c.close();
         }
         i_sent_close = true;
         closeServer();
         forwarding.serverClosed(this);
      }
   }
}
