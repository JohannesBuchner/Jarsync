// :vim:set tw=78 expandtab tabstop=3:
// $Id$
//
// ConsoleClient -- rsync console client.
// Copyright (C) 2001,2002  Casey Marshall <rsdio@metastatic.org>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
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
// ---------------------------------------------------------------------------

package org.metastatic.rsync.v2;

import java.util.Iterator;

import org.metastatic.rsync.Util;

public class ConsoleClient {

   public static void main(String[] argv) throws Throwable {
      if (argv.length < 1) {
         System.err.println("usage: ConsoleClient <host>[:port] [module]");
         System.exit(1);
      }
      String host = null;
      int port = 873;
      int ind = 0;
      if ((ind = argv[0].lastIndexOf(':')) < 0) {
         host = argv[0];
      } else {
         host = argv[0].substring(0, ind);
         if (ind < argv[0].length())
            port = Integer.parseInt(argv[0].substring(ind+1));
      }
      String module = null;
      if (argv.length >= 2) {
         module = argv[1];
      }
      SocketClient c = SocketClient.connect(host, port, module);
      System.out.print(c.getMOTD());
      if (c.authRequired()) {
         System.out.print("user: ");
         String user = Util.readLine(System.in);
         System.out.print("password: ");
         String pass = Util.readLine(System.in);
         if (!c.authenticate(user, pass)) {
            System.out.println("Authentication failed.");
            System.out.println(c.getError());
         }
      }

      if (module == null) {
         System.out.println("Modules:");
         for(Iterator modules = c.moduleList().iterator(); modules.hasNext(); )
            System.out.println(modules.next());
      } else if (c.connected()) {
         c.exit();
      }
   }
}
