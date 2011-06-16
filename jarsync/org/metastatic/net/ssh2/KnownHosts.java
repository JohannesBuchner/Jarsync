/* KnownHosts -- Stores and retrieves lists of known hosts.
   Copyright (C) 2002, 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of HUSH, the Hopefully Uncomprehensible Shell.

HUSH is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your
option) any later version.

HUSH is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with HUSH; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.net.ssh2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;

import java.math.BigInteger;

import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import gnu.crypto.key.dss.DSSPublicKey;
import gnu.crypto.key.rsa.GnuRSAPublicKey;

import org.metastatic.util.Util;

/**
 * A simple interface to <a
 * href="http://www.openssh.org/">OpenSSH</a>-style "known hosts" files.
 * These files contain three values, separated by spaces: (1) the host
 * name and the IP address, separated by a single comma, (2) the
 * algorithm name, and (3) the public key as an SSH "blob" encoded in
 * Base-64.
 *
 * @version $Revision$
 */
public class KnownHosts
{

  // Constants and fields.
  // -------------------------------------------------------------------------

  /** The mapping from aliases to keys. */
  private HashMap keys;

  // Constructors.
  // -------------------------------------------------------------------------

  /** Default 0-arguments constructor. */
  public KnownHosts()
  {
    keys = new HashMap();
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public void putKey(String keyAlg, String alias, PublicKey key)
  {
    HashMap m = (HashMap) keys.get(keyAlg);
    if (m == null)
      {
        m = new HashMap();
        keys.put(keyAlg, m);
      }
    m.put(alias, key);
  }

  public boolean containsAlias(String keyAlg, String alias)
  {
    HashMap m = (HashMap) keys.get(keyAlg);
    return m == null ? false : m.containsKey(alias);
  }

  public boolean containsKey(String keyAlg, String alias, PublicKey key)
  {
    HashMap m = (HashMap) keys.get(keyAlg);
    if (m == null)
      return false;
    PublicKey key2 = (PublicKey) m.get(alias);
    if (key2 == null)
      return false;
    if ((key2 instanceof DSAPublicKey) && (key instanceof DSAPublicKey))
      {
        DSAParams p1 = ((DSAPublicKey) key).getParams();
        DSAParams p2 = ((DSAPublicKey) key2).getParams();
        BigInteger y1 = ((DSAPublicKey) key).getY();
        BigInteger y2 = ((DSAPublicKey) key2).getY();
        if (p1.getG().equals(p2.getG()) && p1.getP().equals(p2.getP())
            && p1.getQ().equals(p2.getQ()) && y1.equals(y2))
          return true;
      }
    if ((key2 instanceof RSAPublicKey) && (key instanceof RSAPublicKey))
      {
        BigInteger e1 = ((RSAPublicKey) key).getPublicExponent();
        BigInteger e2 = ((RSAPublicKey) key2).getPublicExponent();
        BigInteger n1 = ((RSAPublicKey) key).getModulus();
        BigInteger n2 = ((RSAPublicKey) key2).getModulus();
        if (e1.equals(e2) && n1.equals(n2))
          return true;
      }
    return false;
  }

  public PublicKey getKey(String keyAlg, String alias)
  {
    HashMap m = (HashMap) keys.get(keyAlg);
    return m == null ? null : (PublicKey) m.get(alias);
  }

  public Iterator keys()
  {
    HashSet s = new HashSet();
    for (Iterator i = keys.keySet().iterator(); i.hasNext(); )
      {
        s.addAll(((HashMap) keys.get(i.next())).values());
      }
    return Collections.unmodifiableCollection(s).iterator();
  }

  public void load(InputStream in) throws IOException
  {
    LineNumberReader lines = new LineNumberReader(new InputStreamReader(in));
    ByteArrayInputStream bais = null;
    String line = null;
    int i = 1;

    while ((line = lines.readLine()) != null)
      {
        StringTokenizer tok = new StringTokenizer(line, " ");
        if (tok.countTokens() < 3)
          {
            throw new IOException("Badly formed key on line " + i);
          }
        String alias = tok.nextToken().toLowerCase();
        String alg = tok.nextToken();
        String base64 = tok.nextToken();

        bais = new ByteArrayInputStream(Util.base64Decode(base64));

        String alg2 = readASCII(bais);
        PublicKey key = null;
        if (!alg.equals(alg2))
          {
            throw new IOException("Badly formed key on line " + i);
          }
        if (alg2.equals("ssh-dss"))
          {
            BigInteger p = readMPint(bais);
            BigInteger q = readMPint(bais);
            BigInteger g = readMPint(bais);
            BigInteger y = readMPint(bais);
            key = new DSSPublicKey(p, q, g, y);
          }
        else if (alg2.equals("ssh-rsa"))
          {
            BigInteger e = readMPint(bais);
            BigInteger n = readMPint(bais);
            key = new GnuRSAPublicKey(n, e);
          }
        else
          {
            continue;
          }
        putKey(alg2, alias, key);
        i++;
      }
  }

  public void store(OutputStream out) throws IOException
  {
    for (Iterator algs = keys.keySet().iterator(); algs.hasNext(); )
      {
        String alg = (String) algs.next();
        HashMap m = (HashMap) keys.get(alg);
        for (Iterator aliases = m.keySet().iterator(); aliases.hasNext(); )
          {
            PacketOutputStream pout = new PacketOutputStream();
            String alias = (String) aliases.next();
            PublicKey key = (PublicKey) m.get(alias);
            String base64 = null;
            if (alg.equals("ssh-dss"))
              {
                pout.writeASCII(alg);
                pout.writeMPint(((DSAPublicKey) key).getParams().getP());
                pout.writeMPint(((DSAPublicKey) key).getParams().getQ());
                pout.writeMPint(((DSAPublicKey) key).getParams().getG());
                pout.writeMPint(((DSAPublicKey) key).getY());
                base64 = Util.base64Encode(pout.getPayload(), 0);
              }
            else if (alg.equals("ssh-rsa"))
              {
                pout.writeASCII(alg);
                pout.writeMPint(((RSAPublicKey) key).getPublicExponent());
                pout.writeMPint(((RSAPublicKey) key).getModulus());
                base64 = Util.base64Encode(pout.getPayload(), 0);
              }
            out.write(alias.getBytes());
            out.write(' ');
            out.write(alg.getBytes());
            out.write(' ');
            out.write(base64.getBytes());
            out.write('\n');
          }
      }
  }

  public String toString()
  {
    return keys.toString();
  }

  private String readASCII(InputStream in) throws IOException
  {
    int len = ((in.read() & 0xff) << 24) | ((in.read() & 0xff) << 16)
            | ((in.read() & 0xff) <<  8) | ( in.read() & 0xff);
    byte[] buf = new byte[len];
    in.read(buf);
    return new String(buf);
  }

  private BigInteger readMPint(InputStream in) throws IOException
  {
    int len = ((in.read() & 0xff) << 24) | ((in.read() & 0xff) << 16)
            | ((in.read() & 0xff) <<  8) | ( in.read() & 0xff);
    byte[] buf = new byte[len];
    in.read(buf);
    return new BigInteger(buf);
  }

  public static void main(String argv[]) throws Throwable
  {
    KnownHosts kh = new KnownHosts();
    kh.load(System.in);
    kh.store(System.out);
  }
}
