// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// SessionSpec -- Type-specific spec.
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
// ---------------------------------------------------------------------------

package org.metastatic.net.ssh2;

/**
 * Sessions are for executing remote commands or running interactive
 * shells. Session channels require nothing in particular when they are
 * created.
 *
 * @version $Revision$
 */
public class SessionSpec extends Channel.Spec implements SSH2Constants {

   // Constants and variables.
   // -----------------------------------------------------------------------

   /**
    * This specification's type.
    */
   public static final Channel.Type TYPE = Channel.Type.SESSION;

   /**
    * Maximum packet size. A lower number here helps interactivity.
    */
   protected static final int MAX_PACKET = 512;

   // Constructors.
   // -----------------------------------------------------------------------

   /** Trivial 0-arguments constructor. */
   public SessionSpec() { }

   // Implementations of abstract methods in Channel.Spec
   // -----------------------------------------------------------------------

   public Channel.Type getType() {
      return TYPE;
   }

   public int maxPacketSize() {
      return MAX_PACKET;
   }

   public int initWindowSize() {
      return MAX_PACKET;
   }

   public int windowIncrement() {
      return initWindowSize();
   }

   /** Empty. */
   public void writeTypeSpecific(PacketOutputStream pout) { }

   /** Empty. */
   public void readTypeSpecific(PacketInputStream pin) { }
}
