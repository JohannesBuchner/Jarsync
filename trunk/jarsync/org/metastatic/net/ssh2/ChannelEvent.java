// vim:set tw=72 expandtab shiftwidth=3 softtabstop=3 tabstop=3:
// $Id$
//
// ChannelEvent -- Input events.
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

import java.util.EventObject;

public class ChannelEvent extends EventObject {

   // Constants and variables.
   // ------------------------------------------------------------------------

   protected byte[] data;

   // Constructors.
   // ------------------------------------------------------------------------

   public ChannelEvent(Object source) {
      super(source);
   }

   public ChannelEvent(Object source, byte[] data) {
      super(source);
      this.data = (byte[]) data.clone();
   }

   // Instance methods.
   // ------------------------------------------------------------------------

   public byte[] getData() {
      return data;
   }
}
