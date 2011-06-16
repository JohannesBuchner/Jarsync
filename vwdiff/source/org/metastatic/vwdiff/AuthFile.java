/* Config.java -- configuration file reader.
   vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of vwdiff.

Vwdiff is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your
option) any later version.

Vwdiff is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License along
with vwdiff; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.vwdiff;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuthFile {

   // Fields.
   // -----------------------------------------------------------------------

   private static final String VWDIFF_REALM = "vwdiff";
   private MessageDigest md5;
   private HashMap users;

   // Constructors.
   // -----------------------------------------------------------------------

   public AuthFile() {
      try {
         md5 = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException nsae) {
         throw new Error(nsae);
      }
      users = new HashMap();
   }

   // Class methods.
   // -----------------------------------------------------------------------

   public static void main(String[] argv) throws Throwable {
      AuthFile file = new AuthFile();
      boolean create = false;
      if (argv.length < 2) {
         System.err.println("Usage: " + AuthFile.class.getName() +
            " [-c] passwordfile username");
         System.err.println("The -c flag creates a new file.");
         System.exit(1);
      }
      int i = 0;
      if (argv[0].equals("-c")) {
         if (argv.length < 3) {
            System.err.println("Usage: " + AuthFile.class.getName() +
               " [-c] passwordfile username");
            System.err.println("The -c flag creates a new file.");
            System.exit(1);
         }
         create = true;
         i++;
      }
      String passfile = argv[i++];
      String username = argv[i];
      if (!create) try {
         file.load(new File(passfile));
      } catch (IOException ioe) {
         System.err.println("Could not open passwd file " + 
            passfile + " for reading.");
         System.err.println("Use -c option to create new one.");
         System.exit(1);
      }
      LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
      if (file.users.containsKey(username))
         System.out.println("Changing password for user " + username);
      else
         System.out.println("Adding password for user " + username);
      System.out.print("New password: ");
      String pass1 = in.readLine();
      System.out.print("Re-type new password: ");
      String pass2 = in.readLine();
      if (!pass1.equals(pass2)) {
         System.err.println("They don't match, sorry.");
         System.exit(1);
      }
      file.setUser(username, pass1);
      try {
         file.store(new File(passfile));
      } catch (IOException ioe) {
         System.err.println("Could not write " + passfile + ": " +
            ioe.getMessage());
         System.exit(1);
      }
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public synchronized void load(File file) throws IOException {
      LineNumberReader in = new LineNumberReader(new FileReader(file));
      String line;
      while ((line = in.readLine()) != null) {
         int i = line.indexOf(":");
         if (i < 0) continue;
         String user = line.substring(0, i);
         i = line.indexOf(":", user.length() + 1);
         if (i < 0) continue;
         String realm = line.substring(user.length() + 1, i);
         if (!realm.equals(VWDIFF_REALM))
            continue;
         i = line.lastIndexOf(":");
         if (i < 0) continue;
         byte[] hash = bytesFromString(line.substring(i+1));
         users.put(user, hash);
      }
      in.close();
   }

   public synchronized void store(File file) throws IOException {
      FileWriter out = new FileWriter(file);
      for (Iterator it = users.entrySet().iterator(); it.hasNext(); ) {
         Map.Entry entry = (Map.Entry) it.next();
         String user = (String) entry.getKey();
         byte[] hash = (byte[]) entry.getValue();
         out.write(user);
         out.write(':');
         out.write(VWDIFF_REALM);
         out.write(':');
         out.write(toString(hash));
         out.write('\n');
      }
      out.close();
   }

   public synchronized byte[] getUserHash(String user) {
      byte[] b = (byte[]) users.get(user);
      return b != null ? (byte[]) b.clone() : null;
   }

   public synchronized void setUser(String username, String password) {
      md5.reset();
      try {
         md5.update(username.getBytes("UTF-8"));
         md5.update((byte) ':');
         md5.update(VWDIFF_REALM.getBytes("UTF-8"));
         md5.update((byte) ':');
         md5.update(password.getBytes("UTF-8"));
         users.put(username, md5.digest());
      } catch (java.io.UnsupportedEncodingException shouldNotHappen) {
      }
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private static byte[] bytesFromString(String str) {
      byte[] b = new byte[str.length() / 2];
      for (int i = 0, j = 0; i+1 < str.length() && j < b.length; ) {
         b[j  ]  = (byte) (hexDigit(str.charAt(i++)) << 4);
         b[j++] |= (byte)  hexDigit(str.charAt(i++));
      }
      return b;
   }

   private static int hexDigit(char c) {
      if (c >= '0' && c <= '9') {
         return c - '0';
      } else if (c >= 'a' && c <= 'f') {
         return (c - 'a') + 10;
      } else if (c >= 'A' && c <= 'F') {
         return (c - 'A') + 10;
      }
      return 0;
   }

   private static final char[] HEX = ("0123456789abcdef").toCharArray();

   private static String toString(byte[] b) {
      char[] c = new char[b.length * 2];
      for (int i = 0, j = 0; i < b.length; i++) {
         c[j++] = HEX[(b[i] >> 4) & 0x0F];
         c[j++] = HEX[ b[i] & 0x0F];
      }
      return new String(c);
   }
}
