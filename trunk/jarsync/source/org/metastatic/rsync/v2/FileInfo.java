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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Basic information about files that are being sent over the wire.
 * Roughly analogous to 'struct file_struct' in rsync.h.
 *
 * @version $Revision$
 */
public class FileInfo implements Constants {

  // Fields.
  // -------------------------------------------------------------------------

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
  // -------------------------------------------------------------------------

  public FileInfo() { }

  public FileInfo(File f) throws IOException
  {
    f = f.getAbsoluteFile();
    if (!f.isDirectory())
      mode = _S_IFREG;
    modtime = f.lastModified();
    length = f.length();
    dirname = f.getParent();
    basename = f.getName();
  }

  // Class methods.
  // -------------------------------------------------------------------------

  public static boolean S_ISLNK(int mode) {
    return (mode & _S_IFMT) == _S_IFLNK;
  }

  public static boolean S_ISREG(int mode) {
    return (mode & _S_IFMT) == _S_IFREG;
  }

  // Intsance methods.
  // -------------------------------------------------------------------------

  public String permstring()
  {
    String perm_map = "rwxrwxrwx";
    StringBuffer buf = new StringBuffer("----------");
    for (int i = 0; i < 9; i++)
      {
        if ((mode & (1 << i)) != 0)
          buf.setCharAt(9-i, perm_map.charAt(8-i));
      }
    return buf.toString();
  }

  public String timestring()
  {
    Date date = new Date(modtime * 1000L);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    return sdf.format(date);
  }

  public String filename()
  {
    if (dirname != null)
      return dirname + File.separator + basename;
    else
      return basename;
  }

  public boolean S_ISLNK() {
    return S_ISLNK(mode);
  }

  public boolean S_ISREG() {
    return S_ISREG(mode);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(permstring());
    buf.append(' ');
    String len = String.valueOf(length);
    if (len.length() <= 10)
      buf.append("          ".substring(0, 10-len.length())).append(len);
    else
      buf.append(len);
    buf.append(' ');
    buf.append(timestring());
    buf.append(' ');
    buf.append(filename());
    if (S_ISLNK())
      buf.append(" -> " + link);
    return buf.toString();
  }
}
