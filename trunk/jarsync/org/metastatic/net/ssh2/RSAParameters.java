// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// RSAParameters -- RSA public key parameters in SSH2.
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

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

import java.security.interfaces.RSAPublicKey;

/**
 * Concrete implementation of {@link PublicKeyParameters} for the RSA
 * signature algorithm.
 *
 * @version $Revision$
 */
public class RSAParameters extends PublicKeyParameters {

   // Constructors.
   // -----------------------------------------------------------------------

   public RSAParameters(String username, KeyPair keys, String service) {
      super(username, keys, service);
      alg_name = "ssh-rsa";
   }

   // Instance methods implementing PublicKeyParameters
   // -----------------------------------------------------------------------

   public byte[] makePublicKeyBlob() {
      PacketOutputStream pout = new PacketOutputStream();
      RSAPublicKey pubkey = (RSAPublicKey) keys.getPublic();
      pout.writeASCII(alg_name);
      pout.writeMPint(pubkey.getPublicExponent());
      pout.writeMPint(pubkey.getModulus());
      return pout.getPayload();
   }

   public byte[] makeSignature(byte[] data) throws SSH2Exception {
      try {
         PacketOutputStream pout = new PacketOutputStream(
            new Configuration());
         Signature s = Signature.getInstance("SHA1withRSA");
         s.initSign(keys.getPrivate());
         s.update(data);
         byte[] sig = s.sign();
         pout.writeASCII(alg_name);
         pout.writeString(sig);
         return pout.getPayload();
      } catch (NoSuchAlgorithmException nsae) {
         throw new SSH2Exception("no implementation of RSA available");
      } catch (InvalidKeyException ike) {
         throw new SSH2Exception("key pair is not a valid RSA key");
      } catch (SignatureException se) {
         throw new SSH2Exception("signing failed: " + se.getMessage());
      }
   }
}
