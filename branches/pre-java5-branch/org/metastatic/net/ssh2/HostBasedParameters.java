// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// HostBasedParameters -- Base class for hostbased authentication.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of HUSH, the Hopefully Uncomprehensible Shell. 
//
// HUSH is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your
// option) any later version.
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
// --------------------------------------------------------------------------

package org.metastatic.net.ssh2;

public abstract class HostBasedParameters
extends Authentication.Parameters implements SSH2Constants {

   // Constants and variables.
   // -----------------------------------------------------------------------

   public static final Authentication.Type TYPE =
      Authentication.Type.HOST_BASED;

   protected HostKeys keys;

   // Constructors.
   // -----------------------------------------------------------------------

   protected HostBasedParameters(String username, String service) {
      super(username, service);
      keys = new HostKeys();
   }

   // Instance methods implementing Authentication.Paramteters.
   // -----------------------------------------------------------------------

   public Authentication.Type getType() {
      return TYPE;
   }

   public void
   writeRequestPacket(PacketOutputStream pout) throws SSH2Exception {
      pout.reset();
      pout.write(SSH_MSG_USERAUTH_REQUEST);
      pout.writeUTF8(username);
      pout.writeASCII(service);
      pout.writeASCII(TYPE.getName());
      pout.writeASCII(keys.getAlgName());
      pout.writeString(keys.makePublicKeyBlob());
      pout.writeASCII(keys.hostname());
      pout.writeUTF8(System.getProperty("user.name"));
      PacketOutputStream sig_data = new PacketOutputStream();
      sig_data.writeString(pout.getConfig().getSessionID());
      sig_data.write(SSH_MSG_USERAUTH_REQUEST);
      sig_data.writeUTF8(username);
      sig_data.writeASCII(service);
      sig_data.writeASCII(TYPE.getName());
      sig_data.writeASCII(keys.getAlgName());
      sig_data.writeString(keys.makePublicKeyBlob());
      sig_data.writeASCII(keys.hostname());
      sig_data.writeUTF8(System.getProperty("user.name"));
      pout.writeString(keys.makeSignature(sig_data.getPayload()));
   }
}
