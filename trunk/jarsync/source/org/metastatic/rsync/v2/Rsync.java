// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// Rsync -- rsync-2.* protocol operations.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
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
// --------------------------------------------------------------------------

package org.metastatic.rsync.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Iterator;
import java.util.LinkedList;
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

   // File receiving variables

   protected long lastTime;
   

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
         for (int i = 0; i < count && i < 1024; i++) {
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

   public List receiveFileList() throws IOException {
      List files = new LinkedList();

      for (int flags = min.read(); flags != 0; flags = min.read()) {
         
      }
      return null;
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

 // Own methods.
   // -----------------------------------------------------------------------

   protected UnixFile reciveFile(int flags) throws IOException {
      UnixFile file = new UnixFile();
      int l1 = 0, l2 = 0;

      if ((flags & SAME_NAME) != 0) {
         l1 = min.read();
      }

      if ((flags & LONG_NAME) != 0) {
         l2 = readInt();
      } else {
         l2 = read();
      }
   }
}
