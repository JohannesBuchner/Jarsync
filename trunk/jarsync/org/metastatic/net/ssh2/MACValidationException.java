// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// MACValidationException -- Signals a failed MAC validation.
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
 * Signals that the MAC computed by the {@link PacketInputStream}
 * differs from the MAC sent by the server. While it is possible to
 * continue execution if this occurs, it is NOT recommended as it either
 * represents a corrupt data stream, or, more nefariously, alteration of
 * the data stream by a third party.
 *
 * @version $Revision$
 */
public class MACValidationException extends SSH2Exception {
   public MACValidationException() {
      super();
   }

   public MACValidationException(String msg) {
      super(msg);
   }
}
