// :vim:set tw=78 expandtab tabstop=3:
// $Id$
//
// ConsoleClient -- rsync console client.
// Copyright (C) 2001,2002  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of Jarsync.
//
// Jarsync is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2, or (at your option) any
// later version.
//
// Jarsync is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Jarsync; see the file COPYING.  If not, write to the
//
//    Free Software Foundation Inc.,
//    59 Temple Place - Suite 330,
//    Boston, MA 02111-1307
//    USA
//
// Linking this library statically or dynamically with other modules is
// making a combined work based on this library.  Thus, the terms and
// conditions of the GNU General Public License cover the whole
// combination.
//
// As a special exception, the copyright holders of this library give
// you permission to link this library with independent modules to
// produce an executable, regardless of the license terms of these
// independent modules, and to copy and distribute the resulting
// executable under terms of your choice, provided that you also meet,
// for each linked independent module, the terms and conditions of the
// license of that module.  An independent module is a module which is
// not derived from or based on this library.  If you modify this
// library, you may extend this exception to your version of the
// library, but you are not obligated to do so.  If you do not wish to
// do so, delete this exception statement from your version.
//
// ---------------------------------------------------------------------------

package org.metastatic.rsync.v2;

import java.util.Iterator;

import org.metastatic.rsync.*;

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

      if (c.authRequired()) {
         System.out.print("user: ");
         String username = Util.readLine(System.in);
         System.out.print("password: ");
         String password = Util.readLine(System.in);
         if (!c.authenticate(username, password)) {
            System.err.println("Authentication failed.");
            System.exit(1);
         }
      }

      for (Iterator i = c.getServerMessages().iterator(); i.hasNext(); ) {
         System.out.println(i.next());
      }

      if (module != null && !module.equals("#list")) {
         String[] sargv = new String[] {
            "--server", "--sender", "-r", ".", module + "/", ""
         };
         c.serverArgs(sargv);
         Rsync rs = c.startClient(new Configuration());
         rs.sendExcludeList(java.util.Collections.singletonList("/*/*"));
         //rs.sendExcludeList(null);
         rs.readStuff();
      }
   }
}
