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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

public final class Main {

   // Class methods.
   // -----------------------------------------------------------------------

   public static void main(String[] argv) throws Throwable {
      LongOpt[] longopts = {
         new LongOpt("config",  LongOpt.REQUIRED_ARGUMENT, null, 'c'),
         new LongOpt("help",    LongOpt.NO_ARGUMENT,       null, 'h'),
         new LongOpt("version", LongOpt.NO_ARGUMENT,       null, 'v')
      };
      Getopt g = new Getopt("vwdiff", argv, "c:hv", longopts);
      int c;
      String conffile = "vwdiff.conf";

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
