/* vim:set tabstop=3 expandtab tw=72:
   $Id$
  
   Server: server-specific protocol.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>
  
   This file is a part of Jarsync.
  
   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2, or (at your option) any
   later version.
  
   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
  
   You should have received a copy of the GNU General Public License
   along with Jarsync; see the file COPYING.  If not, write to the
  
      Free Software Foundation Inc.,
      59 Temple Place - Suite 330,
      Boston, MA 02111-1307
      USA  */

package org.metastatic.rsync.v2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

public class Server extends Protocol {

	// Constants and variables.
	// -----------------------------------------------------------------------

	public Server(String motd, Map modules, InetAddress client) {
		super(motd, modules, client);
	}

	public static final int STATE_PROTOCOL_BEGIN = 004;
	public static final int STATE_PROTOCOL_AUTH = 010;

	protected String line = "";

	// Constructors.
	// -----------------------------------------------------------------------

	// Instance methods.
	// -----------------------------------------------------------------------

	protected void updateProtocol(byte[] buf, int off, int len)
		throws IOException {
		switch (state & STATE_SETUP_PROTOCOL) {
			case (STATE_PROTOCOL_BEGIN):
				for (int i = 0; i < len; i++) {
					if ((char) buf[i] == '\n') {
						if (!line.startsWith(RSYNCD_GREETING))
							throw new ProtocolException("malformed greeting");
						try {
							remoteVersion =
								Integer.parseInt(line.substring(RSYNCD_GREETING
									.length()));
						} catch (NumberFormatException nfe) {
							throw new ProtocolException(nfe.getMessage());
						}
						if (remoteVersion < MIN_PROTOCOL_VERSION)
							throw new ProtocolException(
								"unsupported version number");
						state ^=
							(state & STATE_SETUP_PROTOCOL)
								| STATE_PROTOCOL_AUTH;
					} else {
						line += (char) buf[i];
					}
				}
				break;
			case STATE_PROTOCOL_AUTH:
		}
	}
}
