/* Main.java -- main entry point.
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

import java.io.FileWriter;
import java.io.IOException;

import java.security.Security;
import java.util.Iterator;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import org.metastatic.rsync.JarsyncProvider;
import org.metastatic.rsync.ParameterException;
import org.metastatic.rsync.Parameters;

import HTTPClient.HTTPConnection;

public final class Main {

   private static final Logger logger =
      Logger.getLogger("org.metastatic.vwdiff");

   private static String conffile;
   private static boolean virgin = true;

   static MemoryAppender memoryAppender;

   // Class methods.
   // -----------------------------------------------------------------------

   public static void main(String[] argv) throws Throwable {
      Security.addProvider(new JarsyncProvider());
      HTTPConnection.setDefaultTimeout(300000);
      LongOpt[] longopts = {
         new LongOpt("config",  LongOpt.REQUIRED_ARGUMENT, null, 'c'),
         new LongOpt("help",    LongOpt.NO_ARGUMENT,       null, 'h'),
         new LongOpt("version", LongOpt.NO_ARGUMENT,       null, 'v')
      };
      Getopt g = new Getopt("vwdiff", argv, "c:hv", longopts);
      int c;
      conffile = "vwdiff.conf";

      while ((c = g.getopt()) != -1) switch (c) {
         case 'c':
            conffile = g.getOptarg();
            break;
         case 'h':
            usage();
            System.exit(0);
         case 'v':
            version();
            System.exit(0);
         case '?':
            System.err.println("Try `vwdiff --help' for more info.");
            System.exit(1);
      }

      Config conf = new Config();
      try {
         loadConfig(conf);
      } catch (IOException ioe) {
         System.err.println("Could not load config file " + conffile
            + ": " + ioe.getMessage());
         System.exit(1);
      }
      virgin = false; // w00t!!

      try {
         PatternLayout layout = new PatternLayout("%d: %m%n");
         if (conf.debug) {
            logger.addAppender(new ConsoleAppender(layout));
         } else if (conf.logfile != null) {
            logger.addAppender(new WriterAppender(layout,
               new FileWriter(conf.logfile)));
         }
         memoryAppender = new MemoryAppender(conf.logsize, layout);
         logger.addAppender(memoryAppender);
      } catch (IOException ioe) {
         System.err.println("Error creating logger: " + ioe.getMessage());
         System.exit(1);
      }

      try {
         Thread server = new Thread(new Server(conf));
         server.start();
      } catch (IOException ioe) {
         System.err.println("Error binding server: " + ioe.getMessage());
         System.exit(1);
      }

      try {
         conf.mergeSaved();
      } catch (IOException ioe) {
         logger.warn("could not load stored data: " + ioe.getMessage());
      }

      while (true) {
         try {
            logger.info("beginning run");
            long now = System.currentTimeMillis();
            for (Iterator i = conf.targets.values().iterator(); i.hasNext(); ) {
               Target t = (Target) i.next();
               t.update(false, false);
            }
            try {
               conf.store();
            } catch (Exception e) {
               logger.warn("could not store data: " + e.getMessage());
            }
            try {
               long elapsed = System.currentTimeMillis() - now;
               logger.info("ended run; elapsed time=" + (elapsed / 1000) + " seconds");
               if (elapsed < 300000)
                  Thread.sleep(300000 - elapsed);
            } catch (InterruptedException ie) {
            }
         } catch (Exception x) {
            logger.fatal("FATAL: " + x);
            System.exit(1);
         }
      }
   }

   static void loadConfig(Config conf) throws IOException {
      Parameters p = new Parameters(conf);
      p.begin(conffile);
      while (true) {
         try {
            p.parse();
            break;
         } catch (ParameterException pe) {
            if (virgin) {
               System.err.println("WARNING: bad configuration file: " +
                                  pe.getMessage());
            }
         } catch (IllegalArgumentException iae) {
            if (virgin) {
               System.err.println("WARNING: bad configuration file: " +
                                  iae.getMessage());
            }
         }
      }
      conf.current = null;
   }

   private static void usage() {
      System.out.println("usage: vwdiff [options]");
      System.out.println();
      System.out.println("  -c, --config=CONFIG        Read configuration from CONFIG.");
      System.out.println("  -h, --help                 Show this message and exit.");
      System.out.println("  -v, --version              Show version information and exit.");
   }

   private static void version() {
      System.out.println("vwdiff version " + version.VERSION +
         " (Jarsync " + org.metastatic.rsync.version.VERSION + ")");
      System.out.println();
      System.out.println("Jarsync comes with NO WARRANTY, to the extent permitted by law.");
      System.out.println("You may redistribute copies of Jarsync under the terms of the GNU");
      System.out.println("General Public License.  See the file `COPYING' for details.");
   }
}
