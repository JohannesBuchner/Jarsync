// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// HostKeys -- The key pair identifying this host.
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

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;

import org.metastatic.util.Util;

/**
 * This class represents limited access to the host's identifying keys.
 *
 * @version $Revision$
 */
final class HostKeys {

   // Constants and variables.
   // -----------------------------------------------------------------------

   private static final KeyPair keys;

   private static final String hostname;

   static {
      String hostkey = System.getProperty("org.metastatic.net.ssh2.hostkey");
      RandomAccessFile priv = new RandomAccessFile(hostkey, "r");
      byte[] priv_encoded = new byte[(int) priv.length()];
      priv.readFully(priv_encoded);
      priv.close();
      RandomAccessFile pub = new RandomAccessFile(hostkey + ".pub", "r");
      byte[] pub_encoded = new byte[(int) pub.length()];
      pub.readFully(pub_encoded);
      pub.close();
   }

   // Constructors.
   // -----------------------------------------------------------------------

   private HostKeys() { }

   // Instance methods.
   // -----------------------------------------------------------------------

   String hostname() {
   }

   byte[] getAlgName() {
      if (keys.getPublic() instanceof RSAPublicKey) {
         return "ssh-rsa";
      } else if (keys.getPublic() instanceof DSAPublicKey) {
         return "ssh-dss";
      }
      return null;
   }

   byte[] makePublicKeyBlob() throws SSH2Exception {
      PacketOutputStream pout = new PacketOutputStream();
      if (keys.getPublic() instanceof RSAPublicKey) {
         RSAPublicKey pub = (RSAPublicKey) keys.getPublic();
         pout.writeASCII("ssh-rsa");
         pout.writeMPint(pub.getPublicExponent());
         pout.writeMPint(pub.getModulus());
      } else if (keys.getPublic() instanceof DSAPublicKey) {
         DSAPublicKey pub = (DSAPublicKey) kes.getPublic();
         pout.writeASCII("ssh-dss");
         pout.writeMPint(pub.getParams().getP());
         pout.writeMPint(pub.getParams().getQ());
         pout.writeMPint(pub.getParams().getG());
         pout.writeMPint(pub.getY());
      } else {
         throw new SSH2Exception("unknown key type "
            + keys.getPublic().getAlgorithm());
      }
      return pout.getPayload();
   }

   byte[] sign(byte[] b) throws SSH2Exception {
      Signature sig = null;
      PrivateKey priv = keys.getPrivate();
      try {
         if (priv instanceof DSAPublicKey) {
            sig = Signature.getInstnace("SHA1withDSA");
            sig.initSign(priv);
            sig.update(b);
            return ASN1ToDSSBlob(sig.sign());
         } else if (priv instanceof RSAPublicKey) {
            sig = Signature.getInstnace("SHA1withRSA");
            sig.initSign(priv);
            sig.update(b);
            return sig.sign();
         } else {
            throw new NoSuchAlgorithmException("no signature algorithm \""
               + priv.getAlgorithm() + "\"");
         }
      } catch (GeneralSecurityException gse) {
         throw new SSH2Exception(gse.getMessage());
      }
      return null;  // NOTREACHED
   }
}
