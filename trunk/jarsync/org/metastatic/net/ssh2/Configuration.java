/* Configuration -- Configuration for reading/writing SSH2 packets.
   Copyright (C) 2002, 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of HUSH, the Hopefully Uncomprehensible Shell.

HUSH is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your
option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.net.ssh2;

import java.security.Provider;
import java.security.Security;
import java.security.SecureRandom;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gnu.crypto.mac.IMac;
import gnu.crypto.mode.IMode;

import com.jcraft.jzlib.ZStream;

public class Configuration
{

  // Constants and variables.
  // -------------------------------------------------------------------------

  IMode cipher;
  SecureRandom random;
  IMac mac;
  ZStream flater;
  byte[] session_id;

  // Constructors.
  // -------------------------------------------------------------------------

  /**
   * Trivial 0-arguments constructor.
   */
  Configuration()
  {
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public byte[] getSessionID()
  {
    return session_id;
  }

  // Algorithms.
  // -------------------------------------------------------------------------

  public static final List CIPHER_ALGS;
  public static final Map CIPHER_KEY_LENGTHS;
  public static final Map CIPHER_IV_LENGTHS;
  public static final List MAC_ALGS;
  public static final Map MAC_KEY_LENGTHS;
  public static final List COMPRESSION_ALGS =
    Arrays.asList(new String[] { "none", "zlib" } );
  public static final String KEX_ALG = "diffie-hellman-group1-sha1";
  public static final List PUB_KEY_ALGS =
    Arrays.asList(new String[] { "ssh-rsa", "ssh-dss" } );

  static
  {
    List l = new LinkedList();
    Map m1 = new HashMap();
    Map m2 = new HashMap();

    l.add("aes256-cbc");
    l.add("aes192-cbc");
    l.add("aes128-cbc");
    m1.put("aes256-cbc", new Integer(32));
    m1.put("aes192-cbc", new Integer(24));
    m1.put("aes128-cbc", new Integer(16));
    m2.put("aes256-cbc", new Integer(16));
    m2.put("aes192-cbc", new Integer(16));
    m2.put("aes128-cbc", new Integer(16));

    l.add("serpent256-cbc");
    l.add("serpent192-cbc");
    l.add("serpent128-cbc");
    m1.put("serpent256-cbc", new Integer(32));
    m1.put("serpent192-cbc", new Integer(24));
    m1.put("serpent128-cbc", new Integer(16));
    m2.put("serpent256-cbc", new Integer(16));
    m2.put("serpent192-cbc", new Integer(16));
    m2.put("serpent128-cbc", new Integer(16));

    l.add("twofish-cbc");
    l.add("twofish256-cbc");
    l.add("twofish192-cbc");
    l.add("twofish128-cbc");
    m1.put("twofish-cbc", new Integer(32));
    m1.put("twofish256-cbc", new Integer(32));
    m1.put("twofish192-cbc", new Integer(24));
    m1.put("twofish128-cbc", new Integer(16));
    m2.put("twofish-cbc", new Integer(16));
    m2.put("twofish256-cbc", new Integer(16));
    m2.put("twofish192-cbc", new Integer(16));
    m2.put("twofish128-cbc", new Integer(16));

    l.add("blowfish-cbc");
    m1.put("blowfish-cbc", new Integer(16));
    m2.put("blowfish-cbc", new Integer(8));

    l.add("3des-cbc");
    m1.put("3des-cbc", new Integer(24));
    m2.put("3des-cbc", new Integer(8));

    l.add("cast128-cbc");
    m1.put("cast128-cbc", new Integer(16));
    m2.put("cast128-cbc", new Integer(8));
    l.add("none");

    CIPHER_ALGS = Collections.unmodifiableList(l);
    CIPHER_KEY_LENGTHS = Collections.unmodifiableMap(m1);
    CIPHER_IV_LENGTHS = Collections.unmodifiableMap(m2);

    l = new LinkedList(); m2 = null;
    m1 = new HashMap();

    l.add("hmac-sha1");
    l.add("hmac-sha1-96");
    m1.put("hmac-sha1", new Integer(20));
    m1.put("hmac-sha1-96", new Integer(20));
    l.add("hmac-md5");
    l.add("hmac-md5-96");
    m1.put("hmac-md5", new Integer(16));
    m1.put("hmac-md5-96", new Integer(16));
    l.add("none");
    MAC_ALGS = Collections.unmodifiableList(l);
    MAC_KEY_LENGTHS = Collections.unmodifiableMap(m1);
  }
}
