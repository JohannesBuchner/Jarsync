/* SSLUtil -- SSL socket utilites.
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with Jarsync; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.rsync.v2;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SSLUtil
{

  // Constructor.
  // -------------------------------------------------------------------------

  private SSLUtil() { }

  // Class methods.
  // -------------------------------------------------------------------------

  public static Socket wrapSocket(Socket socket) throws IOException
  {
    try
      {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        SSLSocketFactory f = ctx.getSocketFactory();
        return f.createSocket(socket, socket.getInetAddress().getHostName(),
                              socket.getPort(), true);
      }
    catch (NoSuchAlgorithmException nsae)
      {
        throw new IOException("TLS not available");
      }
  }

  public static Socket wrapServerSocket(Socket socket) throws IOException
  {
    Socket s = wrapSocket(socket);
    ((SSLSocket) s).setClientMode(false);
    return s;
  }
}
