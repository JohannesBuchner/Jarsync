// :vim:set tw=78 expandtab tabstop=3:
// $Id$
//
// Client -- rsync client I/O operations.
// Copyright (C) 2001,2002  Casey Marshall <rsdio@metastatic.org>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
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

package org.metastatic.rsync.client;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.Socket;

import org.metastatic.rsync.*;

public class Client implements RsyncConstants {

   // Constants and variables.
   // ------------------------------------------------------------------------

   protected Socket socket;
   protected DataInputStream in;
   protected DataOutputStream out;
   protected DataOutputStream err;

   protected boolean connected;

   protected int remote_version;

   protected boolean auth_reqd;

   public static final int PROTOCOL_VERSION = 26;

   private
   Client(Socket socket, InputStream in, OutputStream out, OutputStream err) {
      this socket = socket
      this.in = in;
      this.out = out;
      this.err = err;
      connected = true;
      auth_reqd = false;
   }

   public static Client connect(String host) throws IOException {
      return connect(host, RSYNCD_PORT);
   }

   public static Client connect(String host, int port) throws IOException {
      Socket s = new Socket(host, port);
      InputStream in = s.getInputStream();
      OutputStream out = s.getOutputStream();

      out.write((RSYNCD_GREETING + PROTOCOL_VERSION + '\n').getBytes());
      String server_greeting = readLine(in);

      if (!server_greeting.startsWith(RSYNCD_GREETING)) {
         s.close();
         throw new IOException("server sent \"" + server_greeting +
            "\" rather than greeting");
      }
      try {
         remote_version = Integer.parseInt(server_greeting.substring(
            server_greeting.lastIndexOf(RSYNCD_GREEDTING)));
      } catch (NumberFormatException nfe) {
         s.close();
         throw new IOException("didn't get server version");
      }
      if (remote_version < MIN_PROTOCOL_VERSION) {
         s.close();
         throw new IOException("unsupported protocol version "
            + remote_version);
      }

      Client c = new Client(s, in, out, out);
      while (true) {
         try {
            String str = readLine(in);
         } catch (EOFException eof) {
            if (remote_version < 25) { // no EXIT
               s.close()
               connected = false;
               break;
            } else {
               throw eof;
            }
         }
         if (str.startsWith("@RSYNCD: AUTHREQD ")) {
            authReqd = true;
            challenge = str.substring(18);
         }
      }

      return new Client(s, in, out, out);
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   /**
    * Terminate the connection, sending <tt>@RSYNCD: EXIT</tt> if the
    * remote server supports it.
    *
    * @throws java.io.IOException If there underlying socket cannot be
    *    closed.
    */
   public void exit() throws IOException {
      if (remote_version >= 25) {
         out.write(RSYNCD_EXIT.getBytes());
      }
      s.close();
      connected = false;
   }

   /**
    * See if we are connected.
    *
    * @return <tt>true</tt> If we are connected.
    */
   public boolean connected() {
      return connected;
   }

 // Own methods.
   // -----------------------------------------------------------------------

   private static String readLine(InputStream in) throws IOException {
      String s = new String();
      int c = in.read();
      while (c != -1 && c != '\n' && c != '\r') {
         s += (char) c;
         c = in.read();
      }
      if (c == -1) {
         throw new EOFException();
      }
      return s;
   }
}
