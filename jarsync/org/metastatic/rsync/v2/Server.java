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
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.metastatic.rsync.*;

public class Server extends Protocol {

   // Constants and variables.
   // -----------------------------------------------------------------------

   public static final int STATE_PROTOCOL_BEGIN = 004;
   public static final int STATE_PROTOCOL_AUTH  = 010;

   protected String line = "";

   // Constructors.
   // -----------------------------------------------------------------------

   public Server() {
      super();
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   protected void updateProtocol(byte[] buf, int off, int len)
   throws IOException
   {
      switch (state & STATE_SPECIFIC_MASK) {
         case STATE_PROTOCOL_BEGIN:
            for (int i = 0; i < len; i++) {
               if ((char) buf[i] == '\n') {
                  if (!line.startsWith(AT_RSYNCD))
                     throw new ProtocolError("malformed greeting");
                  try {
                     remoteVersion = Integer.parseInt(
                        line.substring(AT_RSYNCD.length()));
                  } catch (NumberFormatException nfe) {
                     throw new ProtocolError(nfe.getMessage());
                  }
                  if (remoteVersion < MIN_PROTOCOL_VERSION)
                     throw new ProtocolException("unsupported version number");
                  state ^= (state & STATE_SPECIFIC_MASK) | STATE_PROTOCOL_AUTH;
               } else {
                  line += (char) buf[i];
               }
            }
            break;
         case STATE_PROTOCOL_AUTH:
      }
   }
}
