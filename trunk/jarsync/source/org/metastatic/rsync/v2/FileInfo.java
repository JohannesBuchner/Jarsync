/* :vim:set tw=78 expandtab tabstop=3:
   $Id$

   FileInfo: information about files being tranferred.
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

/*
 * Based on rsync-2.5.5.
 * 
 * Rsync Copyright (C) 1992-2001 Andrew Tridgell
 *                     1996 Paul Mackerras
 *                     2001, 2002 Martin Pool
 *                     and others.
 */

package org.metastatic.rsync.v2;

import javaunix.io.FileStatus;
import javaunix.io.UnixFile;

/**
 * Basic information about files that are being sent over the wire.
 * Roughly analogous to 'struct file_struct' in rsync.h.
 *
 * @version $Revision$
 */
public class FileInfo implements Constants {

   // Fields.
   // -----------------------------------------------------------------------

   public int flags;
   public long modtime;
   public long length;
   public int inode;
   public int rdev;
   public int mode;
   public int uid;
   public int gid;
   public String basename;
   public String dirname;
   public String link;
   public byte[] sum;

   // Constructors.
   // -----------------------------------------------------------------------

   public FileInfo() { }

   public FileInfo(UnixFile file) throws java.io.IOException {
      FileStatus fstat = file.getFileStatus();
      modtime = fstat.st_mtime;
      length = file.length();
      mode = fstat.st_mode;
      uid = fstat.st_uid;
      gid = fstat.st_gid;
      inode = fstat.st_ino;
      rdev = fstat.st_rdev;
      if (fstat.isLnk()) {
         link = UnixFile.readLink(file.getPath());
      }
      dirname = file.getParent();
      basename = file.getName();
   }

   // Class methods.
   // -----------------------------------------------------------------------

   public static boolean S_ISLNK(int mode) {
      return (mode & _S_IFMT) == _S_IFLNK;
   }

   public static boolean S_ISREG(int mode) {
      return (mode & _S_IFMT) == _S_IFREG;
   }

   // Intsance methods.
   // -----------------------------------------------------------------------

   public boolean S_ISLNK() {
      return S_ISLNK(mode);
   }

   public boolean S_ISREG() {
      return S_ISREG(mode);
   }

   public String toString() {
      String s = dirname + UnixFile.separator + basename + " flags="
         + Integer.toBinaryString(flags) + " modtime=" + modtime +
         " length=" + length + " mode=" + Integer.toOctalString(mode) +
         " uid=" + uid + " gid=" + gid + " inode=" + inode + " rdev=" + rdev;
      if (S_ISLNK())
         s += " link=" + link;
      return s;
   }
}
