// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// ChannelException -- Channel open failure in SSH 2.
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

public class ChannelException extends SSH2Exception implements SSH2Constants {

   // Constants and variables.
   // -----------------------------------------------------------------------

   /** This exception's reason code. */
   protected int reason_code;

   /** This exception's description. */
   protected String description;

   // Constructors.
   // -----------------------------------------------------------------------

   // This class cannot be instantiated as a normal exception.
   private ChannelException() { }
   private ChannelException(String msg) { }

   public ChannelException(int reason_code, String desc) {
      this.reason_code = reason_code;
      description = desc;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public String getMessage() {
      String msg = "channel open failed: ";
      msg += codeToMessage(reason_code);
      msg += ": ";
      msg += printableOnly(description);
      return msg;
   }

   // Own methods. ----------------------------------------------------------

   /**
    * Translate a reason code to a text message. TODO: internationalize
    * these messages.
    *
    * @param code The reason code.
    * @return A printable message for the reason code.
    */
   private static String codeToMessage(int code) {
      switch (code) {
         case SSH_OPEN_ADMINISTRATIVELY_PROHIBITED:
            return "administratively prohibited";
         case SSH_OPEN_CONNECT_FAILED:
            return "connect failed";
         case SSH_OPEN_UNKNOWN_CHANNEL_TYPE:
            return "unknown channel type";
         case SSH_OPEN_RESOURCE_SHORTAGE:
            return "resource shortage";
      }
      return "unknown reason";
   }

   /**
    * Remove any nonprintable characters from the string.
    *
    * @param s The source string.
    * @return The string argument, without nonprintable characters.
    */
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
