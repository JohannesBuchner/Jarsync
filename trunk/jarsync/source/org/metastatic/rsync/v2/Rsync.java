// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// Rsync -- rsync-2.* protocol operations.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
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
// --------------------------------------------------------------------------

package org.metastatic.rsync.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Iterator;
import java.util.List;

import org.metastatic.rsync.*;

public class Rsync implements RsyncConstants {

   // Constants and variables.
   // -----------------------------------------------------------------------

   /** Our multiplexed input stream. */
   protected MultiplexedInputStream min;

   /** Our multiplexed output stream. */
   protected MultiplexedOutputStream mout;

   /** Our configuration. */
   protected Configuration config;

   /** The remote protocol version. */
   protected int remoteVersion;

   // Constructors.
   // -----------------------------------------------------------------------

   /**
    * Create a new Rsync object.
    *
    * @param in The underlying input stream.
    * @param out The underlying output stream.
    * @param config The configuration to use.
    * @param remoteVersion The remote protocol version.
    */
   public Rsync(InputStream in, OutputStream out, Configuration config,
                int remoteVersion, boolean amServer)
   {
      if (remoteVersion >= 23) {
         if (amServer) {
            min = new MultiplexedInputStream(in, false);
            mout = new MultiplexedOutputStream(out, true);
         } else {
            min = new MultiplexedInputStream(in, true);
            mout = new MultiplexedOutputStream(out, false);
         }
      } else {
         min = new MultiplexedInputStream(in, false);
         mout = new MultiplexedOutputStream(out, false);
      }
      this.config = config;
      this.remoteVersion = remoteVersion;
      Logger.addErrorStream(System.err);
      Logger.addInfoStream(System.err);
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void sendExcludeList(List exclude) throws IOException {
      if (exclude != null && exclude.size() > 0) {
         for (Iterator i = exclude.iterator(); i.hasNext(); ) {
            String pattern = (String) i.next();
            if (pattern.startsWith("+ ") && remoteVersion < 19) {
               throw new
                  IOException("remote rsync does not support include syntax");
            }
            System.out.println("exlcude pattern= " + pattern + " with length= "
               + pattern.length());
            mout.writeInt(pattern.length());
            mout.writeString(pattern);
         }
      }
      System.out.print("Writing naught... ");
      mout.writeInt(0);
      System.out.print("flushing... ");
      mout.flush();
      System.out.println("done");
   }

   public void readStuff() throws IOException {
      byte[] buf = new byte[1024];
      int count = 0;
      while ((count = min.read(buf, 0, 1024)) > 0) {
         for (int i = 0; i < 1024; i++) {
            switch (buf[i]) {
               case 0x07: System.out.print("\\a"); break;
               case 0x08: System.out.print("\\b"); break;
               case 0x09: System.out.print("\\t"); break;
               case 0x0a: System.out.print("\\n"); break;
               case 0x0b: System.out.print("\\v"); break;
               case 0x0c: System.out.print("\\f"); break;
               case 0x0d: System.out.print("\\r"); break;
            }
            if ((buf[i] >= 0 && buf[i] <= 6) || buf[i] == 0x0e || buf[i] == 0x0f) {
               System.out.print(" " + Integer.toHexString(buf[i]&0xff));
            } else if (buf[i] >= 0x10 && buf[i] <= 0x20 || buf[i] >= 0x7f) {
               System.out.println(Integer.toHexString(buf[i]&0xff));
            } else if (buf[i] >= '!' && buf[i] <= '~') {
               System.out.print(" " + (char)buf[i]);
            }
            if (++count % 8 == 0) {
               System.out.println();
            } else {
               System.out.print(' ');
            }
         }
      }
   }

   public void sendFileList(List files) throws IOException {

   }

   public void sendFiles(List files) throws IOException {
      int phase = 0, i;

      while (true) {
         int offset = 0;

         i = min.readInt();
         if (i == -1) {
            if (phase == 0 && remoteVersion >= 13) {
               phase++;
               config.setStrongSumLength(SUM_LENGTH);
               mout.writeInt(-1);
               continue;
            }
            break;
         }

         if (i < 0 || i > files.size()) {
            //throw new 
         }
      }
   }
}
