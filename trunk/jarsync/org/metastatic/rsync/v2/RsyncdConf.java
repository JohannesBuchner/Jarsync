/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   RsyncdConf: config file reader.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync.

   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2, or (at your option) any
   later version.

   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jarsync; see the file COPYING.  If not, write to the

      Free Software Foundation Inc.,
      59 Temple Place - Suite 330,
      Boston, MA 02111-1307
      USA  */

package org.metastatic.rsync.v2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.metastatic.rsync.ParameterListener;

public class RsyncdConf implements ParameterListener {

   // Constants and variables.
   // -----------------------------------------------------------------------

   private String motdFile;

   private String pidFile;

   private String logFile;

   private Map modules;

   private Module current;

   // Constructors.
   // -----------------------------------------------------------------------

   public RsyncdConf() {
      modules = new HashMap();
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public Map getModules() {
      return modules;
   }

   public String getMOTDFile() {
      return motdFile;
   }

   public String getPIDFile() {
      return pidFile;
   }

   public String getLogFile() {
      return logFile;
   }

   public void beginSection(String name) {
      if (name.equals("#stats"))
         current = new StatsModule();
      else
         current = new Module(name);
      modules.put(name, current);
   }

   public void setParameter(String name, String value) {
      if (current == null) {
         if (name.equalsIgnoreCase("motd file"))
            motdFile = value;
         else if (name.equalsIgnoreCase("pid file"))
            pidFile = value;
         else if (name.equalsIgnoreCase("log file"))
            logFile = value;
      } else {
         if (name.equalsIgnoreCase("path"))
            current.path = value;
         else if (name.equalsIgnoreCase("comment"))
            current.comment = value;
         else if (name.equalsIgnoreCase("auth users"))
            current.users = commas(value);
         else if (name.equalsIgnoreCase("secrets file"))
            current.secretsFile = value;
         else if (name.equalsIgnoreCase("read only"))
            current.readOnly = toBoolean(value);
         else if (name.equalsIgnoreCase("list"))
            current.list = toBoolean(value);
         else if (name.equalsIgnoreCase("uid"))
            current.uid = value;
         else if (name.equalsIgnoreCase("gid"))
            current.gid = value;
         else if (name.equalsIgnoreCase("exclude"))
            current.exclude = value;
         else if (name.equalsIgnoreCase("exclude from"))
            current.excludeFrom = value;
         else if (name.equalsIgnoreCase("include"))
            current.include = value;
         else if (name.equalsIgnoreCase("include from"))
            current.includeFrom = value;
         else if (name.equalsIgnoreCase("strict modes"))
            current.strictModes = toBoolean(value);
         else if (name.equalsIgnoreCase("hosts allow"))
            current.hostsAllow = value;
         else if (name.equalsIgnoreCase("hosts deny"))
            current.hostsDeny = value;
         else if (name.equalsIgnoreCase("ignore errors"))
            current.ignoreErrors = toBoolean(value);
         else if (name.equalsIgnoreCase("ignore nonreadable"))
            current.ignoreNonReadable = toBoolean(value);
         else if (name.equalsIgnoreCase("transfer logging"))
            current.transferLogging = value;
         else if (name.equalsIgnoreCase("log format"))
            current.logFormat = value;
         else if (name.equalsIgnoreCase("timeout"))
            try {
               current.timeout = Integer.parseInt(value);
               System.err.println(current.timeout);
            } catch (NumberFormatException nfe) {
               System.err.println(nfe);
            }
         else if (name.equalsIgnoreCase("refuse options"))
            current.refuseOptions = value;
         else if (name.equalsIgnoreCase("dont compress"))
            current.dontCompress = value;
         else if (name.equalsIgnoreCase("max connections"))
            try {
               current.maxConnections = Integer.parseInt(value);
            } catch (NumberFormatException nfe) { }
         else
            System.err.println("extra parameter " + name);
      }
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private Set commas(String list) {
      StringTokenizer tok = new StringTokenizer(list, ",");
      HashSet result = new HashSet(tok.countTokens());
      while (tok.hasMoreTokens())
         result.add(tok.nextToken().trim());
      return result;
   }

   private static boolean toBoolean(String val) {
      val = val.toLowerCase();
      return val.equals("true") || val.equals("yes")
          || val.equals("t") || val.equals("y");
   }
}
