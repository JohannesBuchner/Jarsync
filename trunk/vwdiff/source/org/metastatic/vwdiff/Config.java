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

import java.awt.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.metastatic.rsync.ParameterException;
import org.metastatic.rsync.ParameterListener;

class Config implements ParameterListener {

   // Fields.
   // -----------------------------------------------------------------------

   int port;
   String bindAddress;
   AuthFile passwdFile;
   String logfile;
   String datafile;
   URL stylesheet;
   boolean debug;

   LinkedHashMap targets;
   private Target current;

   // Constructor.
   // -----------------------------------------------------------------------

   public Config() {
      port = 8008;
      debug = false;
      targets = new LinkedHashMap();
      datafile = "vwdiff.dat.gz";
   }

   // ParameterListener methods.
   // -----------------------------------------------------------------------

   public void beginSection(String name) {
      if (targets.containsKey(name)) {
         current = (Target) targets.get(name);
      } else {
         current = new Target(name);
         targets.put(name, current);
      }
   }

   public void setParameter(String name, String value) {
      if (current == null) {
         if (name.equalsIgnoreCase("port")) {
            try {
               port = Integer.parseInt(value);
               if (port <= 0)
                  throw new IllegalArgumentException("bad port");
            } catch (NumberFormatException nfe) {
               throw new IllegalArgumentException("bad port");
            }
         } else if (name.equalsIgnoreCase("bind address")) {
            bindAddress = value;
         } else if (name.equalsIgnoreCase("auth file")) {
            try {
               passwdFile = new AuthFile();
               passwdFile.load(new File(value));
            } catch (IOException ioe) {
               passwdFile = null;
               throw new IllegalArgumentException(ioe.getMessage());
            }
         } else if (name.equalsIgnoreCase("log file")) {
            logfile = value;
         } else if (name.equalsIgnoreCase("data file")) {
            datafile = value;
         } else if (name.equalsIgnoreCase("stylesheet")) {
            try {
               stylesheet = new URL(value);
            } catch (MalformedURLException mue) {
               throw new IllegalArgumentException("bad stylesheet URL: " +
                                                  mue.getMessage());
            }
         } else if (name.equalsIgnoreCase("debug")) {
            debug = value.equalsIgnoreCase("true")
                 || value.equalsIgnoreCase("yes");
         } else
            throw new IllegalArgumentException("bad global parameter " + name);
      } else {
         if (name.equalsIgnoreCase("url")) {
            try {
               current.setURL(new URL(value));
            } catch (MalformedURLException mue) {
               throw new IllegalArgumentException("malformed URL: "
                  + mue.getMessage());
            }
         } else if (name.equalsIgnoreCase("frequency")) {
            current.setFrequency(makeFreak(value));
         } else if (name.equalsIgnoreCase("threshold")) {
            try {
               double thold = Double.parseDouble(value);
               if (thold < 0.0 || thold > 100.0)
                  throw new IllegalArgumentException("invalid threshold");
               current.setThreshold(thold);
            } catch (NumberFormatException nfe) {
               throw new IllegalArgumentException("invalid threshold");
            }
         } else if (name.equalsIgnoreCase("block size")) {
            try {
               current.getConfig().blockLength = Integer.parseInt(value);
               if (current.getConfig().blockLength <= 0)
                  throw new IllegalArgumentException("block size can't be negative");
            } catch (NumberFormatException nfe) {
               throw new IllegalArgumentException("malformed block size");
            }
         } else if (name.equalsIgnoreCase("hash")) {
            try {
               current.getConfig().strongSum = MessageDigest.getInstance(value);
            } catch (NoSuchAlgorithmException nsae) {
               throw new IllegalArgumentException("no such digest: " + value);
            }
         } else if (name.equalsIgnoreCase("hash size")) {
            try {
               current.getConfig().strongSumLength = Integer.parseInt(value);
               if (current.getConfig().strongSumLength <= 0)
                  throw new IllegalArgumentException("sum size can't be negative");
            } catch (NumberFormatException nfe) {
               throw new IllegalArgumentException("malformed sum size");
            }
         } else if (name.equalsIgnoreCase("HTTP user")) {
            current.setHTTPUser(value);
         } else if (name.equalsIgnoreCase("HTTP password")) {
            current.setHTTPPassword(value.toCharArray());
         } else if (name.equalsIgnoreCase("image width")) {
            try {
               int width = Integer.parseInt(value);
               if (width <= 0)
                  throw new IllegalArgumentException("bad image width");
               current.setWidth(width);
            } catch (NumberFormatException nfe) {
               throw new IllegalArgumentException("bad image width");
            }
         } else if (name.equalsIgnoreCase("color")) {
            current.setColor(makeColor(value));
         } else if (name.equalsIgnoreCase("new data color")) {
            current.setNewColor(makeColor(value));
         } else if (name.equalsIgnoreCase("moved data color")) {
            current.setMovedColor(makeColor(value));
         } else
            throw new IllegalArgumentException("bad parameter: " + name);
      }
   }

   public void mergeSaved() throws IOException, ClassNotFoundException {
      ObjectInputStream oin =
         new ObjectInputStream(new GZIPInputStream(new FileInputStream(datafile)));
      LinkedHashMap old = (LinkedHashMap) oin.readObject();
      for (Iterator it = old.keySet().iterator(); it.hasNext(); ) {
         String name = (String) it.next();
         if (targets.containsKey(name)) {
            Target t1 = (Target) targets.get(name);
            Target t2 = (Target) old.get(name);
            t1.setBasis(t2.getBasis());
            t1.setImage(t2.getImage());
            t1.setLastUpdate(t2.getLastUpdate());
            t1.setLastAccess(t2.getLastAccess());
            t1.setLength(t2.getLength());
            t1.setNewLength(t2.getNewLength());
            t1.setMovedLength(t2.getMovedLength());
         }
      }
   }

   public void store() throws IOException {
      ObjectOutputStream oout =
         new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(datafile)));
      oout.writeObject(targets);
      oout.flush();
      oout.close();
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private long makeFreak(String str) {
      long mult = 1, ret = 1;
      if (str.endsWith("d") || str.endsWith("D")) {
         mult = 24L;
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
      return ret * mult * 3600000L;
   }

   private Color makeColor(String name) {
      if (name.charAt(0) == '#') {
         try {
            return new Color(Integer.parseInt(name.substring(1), 16));
         } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("malformed color spec: "
                                               + name);
         }
      } else if (name.charAt(0) == '@') {
         try {
            return new Color(Integer.parseInt(name.substring(1), 16), true);
         } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("malformed color spec: "
                                               + name);
         }
      } else {
         Color c = Color.getColor(name);
         if (c == null)
            throw new IllegalArgumentException("no such color: " + name);
         return c;
      }
   }
}
