// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// DSSParameters -- DSS public key authentication for SSH2.
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

import java.security.interfaces.DSAPublicKey;

import org.metastatic.util.Util;

/**
 * Concrete implementation of {@link PublicKeyParameters} for the
 * Digital Signature Standard (DSS).
 *
 * @version $Revision$
 */
public class DSSParameters extends PublicKeyParameters {

   // Constructors.
   // -----------------------------------------------------------------------

   public DSSParameters(String username, KeyPair keys, String service) {
      super(username, keys, service);
      alg_name = "ssh-dss";
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public byte[] makePublicKeyBlob() {
      PacketOutputStream pout = new PacketOutputStream();
      DSAPublicKey pubkey = (DSAPublicKey) keys.getPublic();
      pout.writeASCII(alg_name);
      pout.writeMPint(pubkey.getParams().getP());
      pout.writeMPint(pubkey.getParams().getQ());
      pout.writeMPint(pubkey.getParams().getG());
      pout.writeMPint(pubkey.getY());
      return pout.getPayload();
   }

   public byte[] makeSignature(byte[] data) throws SSH2Exception {
      try {
         PacketOutputStream pout = new PacketOutputStream(
            new Configuration());
         Signature s = Signature.getInstance("SHA1withDSA");
         s.initSign(keys.getPrivate());
         s.update(data);
         byte[] sig = s.sign();
         pout.writeASCII(alg_name);
         pout.writeString(ASN1ToDSSBlob(sig));
         return pout.getPayload();
      } catch (NoSuchAlgorithmException nsae) {
         throw new SSH2Exception("no implementation of DSS available");
      } catch (InvalidKeyException ike) {
         throw new SSH2Exception("key pair is not a valid DSA key");
      } catch (SignatureException se) {
         throw new SSH2Exception("signing failed: " + se.getMessage());
      }
   }

   private byte[] ASN1ToDSSBlob(byte[] asn1) throws SSH2Exception {
      if (asn1[0] != 0x30 || asn1[2] != 0x02)
         throw new SSH2Exception("Poorly formed ASN.1 sequence");
      byte[] blob = new byte[40];
      int len1 = asn1[3] & 0xff;
      if (asn1[4+len1] != 0x02)
         throw new SSH2Exception("Poorly formed ASN.1 sequence");
      if (len1 == 21)
         System.arraycopy(asn1, 5, blob, 0, 20);
      else {
         System.arraycopy(asn1, 4, blob, 20-len1, len1);
      }
      int len2 = asn1[len1+5] & 0xff;
      if (len2 == 21)
         System.arraycopy(asn1, 7+len1, blob, 20, 20);
      else
         System.arraycopy(asn1, 6+len1, blob, 40-len2, len2);
      return blob;
   }
}
