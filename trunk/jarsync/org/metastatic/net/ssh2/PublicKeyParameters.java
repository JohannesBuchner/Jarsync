// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// PublicKeyAuthentication -- Public key authentication.
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

import java.io.IOException;

import java.security.KeyPair;

/**
 * This is the base class for public key based authentication. Concrete
 * implementations merely implement the signature and key "blob" methods
 * below.
 *
 * @version $Revision$
 */
public abstract class
PublicKeyParameters extends Authentication.Parameters implements SSH2Constants
{

   // Constants and variables.
   // -----------------------------------------------------------------------

   /** This method's type. */
   public static final Authentication.Type TYPE =
      Authentication.Type.PUBLIC_KEY;

   /** The public/private key pair. */
   protected KeyPair keys;

   /** The algorithm name. */
   protected String alg_name;

   // Constructors.
   // -----------------------------------------------------------------------

   /**
    * Prepare a new public key authentication. Since this class (and its
    * subclasses) keeps a secret key, it should be used as carefully as
    * a secret key.
    *
    * @param username The user name.
    * @param keys     The public/private key pair.
    * @param service  The service to request.
    */
   protected
   PublicKeyParameters(String username, KeyPair keys, String service) {
      super(username, service);
      this.keys = keys;
   }

   // Instance methods implementing Authentication.Parameters.
   // -----------------------------------------------------------------------

   public Authentication.Type getType() {
      return TYPE;
   }

   public void
   writeRequestPacket(PacketOutputStream pout, int type) throws SSH2Exception {
      pout.reset();
      pout.write(SSH_MSG_USERAUTH_REQUEST);
      pout.writeUTF8(username);
      pout.writeASCII(service);
      pout.writeASCII(TYPE.getName());
      if (type == SEND_PUBLICKEY) {
         pout.writeBoolean(false);
         pout.writeASCII(alg_name);
         pout.writeString(makePublicKeyBlob());
      } else if (type == SEND_SIGNATURE) {
         pout.writeBoolean(true);
         pout.writeASCII(alg_name);
         pout.writeString(makePublicKeyBlob());
         PacketOutputStream sig_data = new PacketOutputStream();
         sig_data.writeString(pout.getConfig().getSessionID());
         sig_data.write(SSH_MSG_USERAUTH_REQUEST);
         sig_data.writeUTF8(username);
         sig_data.writeASCII(service);
         sig_data.writeASCII("publickey");
         sig_data.writeBoolean(true);
         sig_data.writeASCII(alg_name);
         sig_data.writeString(makePublicKeyBlob());
         pout.writeString(makeSignature(sig_data.getPayload()));
      }
   }

   // Methods to be implemented by concrete subclasses. ---------------------

   /**
    * Encode this public key as an algorithm-specific blob.
    * 
    * @return The encoded key.
    */
   public abstract byte[] makePublicKeyBlob();

   /**
    * Sign the byte array, returning the signature.
    *
    * @param b The data to sign.
    * @return The signature.
    */
   public abstract byte[] makeSignature(byte[] b) throws SSH2Exception;
}
