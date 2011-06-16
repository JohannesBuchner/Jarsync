// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// DisconnectException -- Disconnect in SSH2 operations.
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
 * A disconnect exception is thrown whenever a SSH_DISCONNECT message is
 * received from the server. The chief difference in this class is that
 * it translates SSH_DISCONNECT "reason codes" to strings, and removes
 * nonprintable characters from the description received by the server.
 *
 * @version $Revision$
 */
public class
DisconnectException extends SSH2Exception implements SSH2Constants {

   // Constants and variables.
   // -----------------------------------------------------------------------

   protected int reason_code;
   protected String description;

   private DisconnectException() { }
   private DisconnectException(String msg) { }

   public DisconnectException(int reason_code, String desc) {
      this.reason_code = reason_code;
      description = desc;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public String getMessage() {
      String msg = "disconnect: ";
      msg += codeToMessage(reason_code);
      msg += ": ";
      msg += printableOnly(description);
      return msg;
   }

   // Own methods. ----------------------------------------------------------

   private static String codeToMessage(int code) {
      switch (code) {
         case SSH_DISCONNECT_HOST_NOT_ALLOWED_TO_CONNECT:
            return "host not allowed to connect";
         case SSH_DISCONNECT_PROTOCOL_ERROR:
            return "protocol error";
         case SSH_DISCONNECT_KEY_EXCHANGE_FAILED:
            return "key exchange failed";
         case SSH_DISCONNECT_RESERVED:
            return "reserved";
         case SSH_DISCONNECT_MAC_ERROR:
            return "message authentication code error";
         case SSH_DISCONNECT_COMPRESSION_ERROR:
            return "compression error";
         case SSH_DISCONNECT_SERVICE_NOT_AVAILABLE:
            return "service not available";
         case SSH_DISCONNECT_PROTOCOL_VERSION_NOT_SUPPORTED:
            return "protocol version not supported";
         case SSH_DISCONNECT_HOST_KEY_NOT_VERIFIABLE:
            return "host key not verifiable";
         case SSH_DISCONNECT_CONNECTION_LOST:
            return "connection lost";
         case SSH_DISCONNECT_BY_APPLICATION:
            return "disconnected by application";
         case SSH_DISCONNECT_TOO_MANY_CONNECTIONS:
            return "too many connections";
         case SSH_DISCONNECT_AUTH_CANCELLED_BY_USER:
            return "authentication cancelled by user";
         case SSH_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE:
            return "no more authentication methods";
         case SSH_DISCONNECT_ILLEGAL_USER_NAME:
            return "illegal user name";
      }
      return "";
   }

   private static String printableOnly(String s) {
      if (s == null) return s;
      StringBuffer str = new StringBuffer(s.length());
      for (int i = 0; i < s.length(); i++) {
         if (Character.isLetterOrDigit(s.charAt(i)) ||
             Character.isWhitespace(s.charAt(i)))
            str.append(s.charAt(i));
      }
      return str.toString();
   }
}
