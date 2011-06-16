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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SSLUtil {

	public static final int BACKLOG = 3;

	// Constructor.
	// -------------------------------------------------------------------------

	private SSLUtil() {
	}

	// Class methods.
	// -------------------------------------------------------------------------

	public static Socket wrapSocket(Socket socket) throws IOException {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, null, null);
			SSLSocketFactory f = ctx.getSocketFactory();
			return f.createSocket(socket,
				socket.getInetAddress().getHostName(), socket.getPort(), true);
		} catch (NoSuchAlgorithmException nsae) {
			throw new IOException("TLS not available");
		} catch (KeyManagementException e) {
			throw new IOException("TLS not available");
		}
	}

	public static ServerSocket wrapServerSocket(ServerSocket socket)
		throws IOException {
		ServerSocket s;
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, null, null);
			SSLServerSocketFactory f = ctx.getServerSocketFactory();
			s =
				f.createServerSocket(socket.getLocalPort(), BACKLOG,
					socket.getInetAddress());
		} catch (NoSuchAlgorithmException nsae) {
			throw new IOException("TLS not available");
		} catch (KeyManagementException e) {
			throw new IOException("TLS not available");
		}
		((SSLServerSocket) s).setUseClientMode(false);
		return s;
	}
}
