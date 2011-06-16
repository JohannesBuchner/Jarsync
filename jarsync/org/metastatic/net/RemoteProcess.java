// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// RemoteProcess -- A process on another machine.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// This file is part of HUSH, the Hopefully Uncomprehensible Shell.
//
// HUSH is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your
// option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the
//
//    Free Software Foundation, Inc.,
//    59 Temple Place, Suite 330,
//    Boston, MA  02111-1307
//    USA
//
// --------------------------------------------------------------------------

package org.metastatic.net;

public abstract class RemoteProcess extends Process {
   protected String host;
   protected String[] argv;
   protected String[] envp;

   protected RemoteProcess(String host, String[] argv, String[] envp) {
      this.host = host;
      this.argv = (argv != null) ? (String[]) argv.clone() : null;
      this.envp = (envp != null) ? (String[]) envp.clone() : null;
   }

   // Class methods.
   // -----------------------------------------------------------------------

   public String getHost() {
      return host;
   }

   public String getCommand() {
      if (argv != null && argv.length != 0)
         return argv[0];
      return null;
   }

   public String[] getArgv() {
      if (argv != null)
         return (String[]) argv.clone();
      return null;
   }

   public String[] getEnvp() {
      if (envp != null)
         return (String[]) envp.clone();
      return null;
   }
}
