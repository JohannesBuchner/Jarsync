/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   FileList -- Send and recieve lists of files.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync.

   Jarsync is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   Jarsync is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jarsync; if not, write to the

      Free Software Foundation, Inc.,
      59 Temple Place, Suite 330,
      Boston, MA  02111-1307
      USA  */

/*
 * Based on rsync-2.5.5.
 * 
 * Rsync Copyright (C) 1992-2001 Andrew Tridgell
 *                     1996 Paul Mackerras
 *                     2001, 2002 Martin Pool
 *                     and others.
 */

package org.metastatic.rsync.v2;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import javaunix.io.UnixFile;

public class FileList implements Constants {

   // Constants and fields.
   // -----------------------------------------------------------------------

   private static final Logger logger =
      Logger.getLogger(FileList.class.getName());

   private final MultiplexedInputStream in;
   private final MultiplexedOutputStream out;

   private final int remoteVersion;

   private Statistics stats;

   private final Options options;

   private long last_time;
   private int last_mode;
   private int last_uid;
   private int last_gid;
   private String lastname = "";
   private String lastdir  = "";

   // Constrctor.
   // -----------------------------------------------------------------------

   public FileList(MultiplexedInputStream in, MultiplexedOutputStream out,
                   int remoteVersion, boolean amServer, Options options)
   {
      this.in = in;
      this.out = out;
      if (amServer)
         logger.addAppender(new RsyncAppender(out));
      this.remoteVersion = remoteVersion;
      this.options = options;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public Statistics getStatistics() {
      return stats;
   }

   public void setStatistics(Statistics newStats) {
      if (newStats != null) stats = newStats;
   }

   public List recieveFileList() throws IOException {
      List flist = new LinkedList();
      byte flags;

      for (flags = (byte)in.read(); flags != 0; flags = (byte)in.read()) {
         FileInfo f = receiveFileEntry(flags);
         if (f.S_ISREG())
            stats.total_size += f.length;
         flist.add(f);
      }

      return flist;
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private FileInfo receiveFileEntry(byte flags) throws IOException {
      FileInfo file = new FileInfo();
      int l1 = 0, l2 = 0;
      String thisname;

      if ((flags & SAME_NAME) != 0)
         l1 = in.read();

      if ((flags & LONG_NAME) != 0)
         l2 = in.readInt();
      else
         l2 = in.read();

      if (l2 >= MAXPATHLEN - l1) {
         logger.fatal("overflow: flags=" + Integer.toHexString(flags)
            + " l1=" + l1 + " l2=" + l2 + " lastname=" + lastname);
         throw new IOException("buffer overflow in readFileEntry");
      }

      thisname = lastname.substring(0, l1);
      thisname += in.readString(l2);

      thisname = thisname.replace('/', File.separatorChar);
      lastname = thisname;
      /* thisname = cleanFname(thisname); */
      /* sanitize_path ... */

      int p;
      if ((p = thisname.lastIndexOf(File.separatorChar)) >= 0) {
         if (thisname.startsWith(lastdir)) {
            file.dirname = lastdir;
         } else {
            file.dirname = thisname.substring(0, p).intern();
            lastdir = file.dirname;
         }
         file.basename = thisname.substring(p+1);
      }
      file.flags = flags;
      file.length = in.readLong();
      file.modtime = (flags & SAME_TIME) != 0 ? last_time : in.readInt();
      file.mode = (flags & SAME_MODE) != 0 ? last_mode : in.readInt();
      if (options.preserve_uid)
         file.uid = (flags & SAME_UID) != 0 ? last_uid : in.readInt();
      if (options.preserve_gid)
         file.uid = (flags & SAME_GID) != 0 ? last_gid : in.readInt();
      /* preserve devices XXX */
      if (options.preserve_links && file.S_ISLNK()) {
         int l = in.readInt();
         if (l < 0) {
            logger.fatal("overflow: l=" + l);
            throw new IOException("buffer overflow in readFileEntry");
         }
         file.link = in.readString(l);
      }
      /* XXX hard links */

      if (options.always_checksum) {
         if (remoteVersion < 21) {
            in.read(file.sum, 0, 2);
         } else {
            in.read(file.sum);
         }
      }

      last_mode = file.mode;
      last_uid = file.uid;
      last_gid = file.gid;
      last_time = file.modtime;

      if (!options.preserve_perms)
         file.mode &= ~options.orig_umask;

      return file;
   }
}
