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

import java.net.MalformedURLException;
import java.net.URL;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HashMap;

import org.metastatic.rsync.ParameterException;
import org.metastatic.rsync.ParameterListener;

class Config implements ParameterListener {

   // Fields.
   // -----------------------------------------------------------------------

   int port;
   String bindAddress;
   File dataDir;
   boolean debug;

   HashMap targets;
   Target current;

   // Constructor.
   // -----------------------------------------------------------------------

   public Config() {
      port = 8080;
      bindAddress = null;
      dataDir = new File(System.getProperty("user.dir"));
      debug = false;
      targets = new HashMap();
   }

   // ParameterListener methods.
   // -----------------------------------------------------------------------

   public void beginSection(String name) {
      current = new Target(name);
   }

   public void setParameter(String name, String value) {
      if (current == null) {
         if (name.equalsIgnoreCase("port")) {
            try {
               port = Integer.parseInt(value);
               if (port <= 0)
                  throw new IllegalArgumentException("port can't be negative");
            } catch (NumberFormatException nfe) {
               throw new IllegalArgumentException("malformed port number");
            }
         } else if (name.equalsIgnoreCase("address")) {
            bindAddress = value;
         } else if (name.equalsIgnoreCase("data directory")) {
            dataDir = new File(value);
         } else if (name.equalsIgnoreCase("debug")) {
            debug = value.equalsIgnoreCase("true")
                 || value.equalsIgnoreCase("yes");
         } else
            throw new IllegalArgumentException("bad global parameter " + name);
      } else {
         if (name.equalsIgnoreCase("url")) {
            try {
               current.url = new URL(value);
            } catch (MalformedURLException mue) {
               throw new IllegalArgumentException("malformed URL: "
                  + mue.getMessage());
            }
         } else if (name.equalsIgnoreCase("frequency")) {
            current.frequency = makeFreak(value);
         } else if (name.equalsIgnoreCase("block size")) {
            try {
               current.config.blockLength = Integer.parseInt(value);
               if (current.config.blockLength <= 0)
                  throw new IllegalArgumentException("block size can't be negative");
            } catch (NumberFormatException nfe) {
               throw new IllegalArgumentException("malformed block size");
            }
         } else if (name.equalsIgnoreCase("hash")) {
            try {
               current.config.strongSum = MessageDigest.getInstance(value);
            } catch (NoSuchAlgorithmException nsae) {
               throw new IllegalArgumentException("no such digest: " + value);
            }
         } else if (name.equalsIgnoreCase("hash size")) {
            try {
               current.config.strongSumLength = Integer.parseInt(value);
               if (current.config.strongSumLength <= 0)
                  throw new IllegalArgumentException("sum size can't be negative");
            } catch (NumberFormatException nfe) {
               throw new IllegalArgumentException("malformed sum size");
            }
         } else
            throw new IllegalArgumentException("bad parameter: " + name);
      }
   }

   private int makeFreak(String str) {
      int mult = 1, ret = 0;
      if (str.endsWith("d") || str.endsWith("D")) {
         mult = 24;
         str = str.substring(0, str.length() - 1);
      } else if (str.endsWith("h") || str.endsWith("H")) {
         str = str.substring(0, str.length() - 1);
      }
      try {
         ret = Integer.parseInt(str);
      } catch (NumberFormatException nfe) {
         throw new IllegalArgumentException("bad frequency");
      }
      if (ret <= 0)
         throw new IllegalArgumentException("bad frequency");
      return ret * mult;
   }
}
