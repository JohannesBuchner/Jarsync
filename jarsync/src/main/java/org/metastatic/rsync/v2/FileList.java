/* FileList -- Send and recieve lists of files.
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import org.metastatic.rsync.Util;

public class FileList implements Constants
{

  // Constants and fields.
  // -------------------------------------------------------------------------

  private static final Logger logger = Logger.getLogger(FileList.class.getName());

  private final MultiplexedInputStream in;
  private final MultiplexedOutputStream out;

  private final int remoteVersion;

  private Statistics stats;

  private final Options options;

  private int last_time;
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
  // -------------------------------------------------------------------------

  public Statistics getStatistics()
  {
    return stats;
  }

  public void setStatistics(Statistics newStats)
  {
    if (newStats != null) stats = newStats;
  }

  public List receiveFileList() throws IOException
  {
    List flist = new LinkedList();
    byte flags;

    for (flags = (byte)in.read(); flags != 0; flags = (byte)in.read())
      {
        FileInfo f = receiveFileEntry(flags);
        if (f.S_ISREG())
          stats.total_size += f.length;
        flist.add(f);
      }
    if (options.preserve_uid)
      {
        int id;
        while ((id = in.readInt()) != 0)
          {
            int len = in.read();
            logger.debug("uidList " + id + " -> " + in.readString(len));
          }
      }
    if (options.preserve_uid)
      {
        int id;
        while ((id = in.readInt()) != 0)
          {
            int len = in.read();
            logger.debug("uidList " + id + " -> " + in.readString(len));
          }
      }
    logger.debug("io error flag="+in.readInt());

    return flist;
  }

  public void sendFileList(List flist) throws IOException
  {
    for (Iterator it = flist.iterator(); it.hasNext(); )
      {
        sendFileEntry((FileInfo) it.next());
      }
    out.write(0);
    if (options.preserve_uid)
      out.writeInt(0); // Dummy uid list.
    if (options.preserve_gid)
      out.writeInt(0); // Dummy gid list.
    if (remoteVersion >= 17)
      out.writeInt(0); // io_error flag
    out.flush();
  }

  public List createFileList(String[] argv, int off, int len)
    throws IOException
  {
    LinkedList files = new LinkedList();
    LinkedList dirs  = new LinkedList();
    Glob exclude = new Glob(options.exclude, false,
                            System.getProperty("user.dir"));
    logger.debug("createFileList off=" + off + " len=" + len);
    for (int i = 0; i < len; i++)
      {
        File f = new File(argv[i+off]);
        if (!f.exists())
          {
            logger.warn(argv[i+off] + ": no such file or directory");
            continue;
          }
        if (f.isDirectory())
          dirs.add(f);
        else
          files.add(new FileInfo(f));
      }

    if (options.recurse)
      {
        while (dirs.size() > 0)
          {
            File dir = (File) dirs.removeFirst();
            File[] f = dir.listFiles(exclude);
            for (int i = 0; i < f.length; i++)
              {
                if (f[i].isDirectory())
                  dirs.add(f[i]);
                else
                  files.add(new FileInfo(f[i]));
              }
          }
      }
    stats.num_files = files.size();
    return files;
  }

  /**
   * Maps a file list received from the remote side to its local equivalent,
   * depending upon the destination.
   */
  public void toLocalList(List flist, String destination)
  {
    File dest = new File(destination);
    if (dest.exists() && !dest.isDirectory() && flist.size() > 1)
      throw new IllegalArgumentException(dest + " is not a directory");

    for (Iterator i = flist.iterator(); i.hasNext(); )
      {
        FileInfo file = (FileInfo) i.next();
        if (options.only_existing)
          if (dest.exists() && dest.isDirectory() &&
              !new File(dest, file.basename).exists())
            i.remove();
        if (!dest.exists() || !dest.isDirectory())
          file.basename = dest.getName();
        if (!options.relative_paths)
          {
            if (dest.isDirectory())
              file.dirname = dest.getPath();
            else
              file.dirname = dest.getParent();
          }
      }
  }

  // Own methods.
  // -------------------------------------------------------------------------

  private FileInfo receiveFileEntry(byte flags) throws IOException
  {
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

    logger.debug("recieveFileEntry flags=" + flags + " l1=" + l1 + " l2=" + l2);
    logger.debug("lastname=" + lastname);

    thisname = lastname.substring(0, l1);
    thisname += in.readString(l2);

    logger.debug("thisname=" + thisname);

    thisname = thisname.replace('/', File.separatorChar);
    lastname = thisname;
    /* thisname = cleanFname(thisname); */
    /* sanitize_path ... */

    int p;
    if ((p = thisname.lastIndexOf(File.separatorChar)) >= 0)
      {
        if (thisname.startsWith(lastdir))
          {
            file.dirname = lastdir;
          }
        else
          {
            file.dirname = thisname.substring(0, p).intern();
            lastdir = file.dirname;
          }
        file.basename = thisname.substring(p+1);
      }
    else
      {
        file.basename = thisname;
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

    logger.debug("read fileinfo=" + file);

    return file;
  }

  private void sendFileEntry(FileInfo file) throws IOException
  {
    int l1 = 0, l2 = 0;
    int flags = 0;
    String fname = file.filename();
    fname = fname.replace(File.separatorChar, '/');

    if (file.mode == last_mode)
      flags |= SAME_MODE;
    if (options.preserve_uid && file.uid == last_uid)
      flags |= SAME_UID;
    if (options.preserve_gid && file.gid == last_gid)
      flags |= SAME_GID;
    if (file.modtime == last_time)
      flags |= SAME_TIME;

    if (lastname != null)
      for (l1 = 0; l1 < lastname.length() && l1 < fname.length() &&
             lastname.charAt(l1) == fname.charAt(l1) && l1 <= 255; l1++);

    l2 = fname.length() - l1;

    if (l1 > 0)
      flags |= SAME_NAME;
    if (l2 > 255)
      flags |= LONG_NAME;

    if (flags == 0 && !file.S_ISDIR())
      flags |= FLAG_DELETE;
    if (flags == 0)
      flags |= LONG_NAME;

    logger.debug("writing flags=" + Integer.toBinaryString(flags));
    out.write(flags);
    logger.debug("writing file name l1=" + l1 + " l2=" + l2 + " name=" + fname.substring(l1));
    if ((flags & SAME_NAME) != 0)
      out.write(l1);
    if ((flags & LONG_NAME) != 0)
      out.writeInt(l2);
    else
      out.write(l2);
    Util.writeASCII(out, fname.substring(l1));

    out.writeLong(file.length);
    if ((flags & SAME_TIME) == 0)
      out.writeInt(file.modtime);
    if ((flags & SAME_MODE) == 0)
      out.writeInt(file.mode);

    if (options.preserve_uid && (flags & SAME_UID) == 0)
      out.writeInt(file.uid);
    if (options.preserve_gid && (flags & SAME_GID) == 0)
      out.writeInt(file.gid);

    if (options.always_checksum)
      {
        if (file.sum == null)
          file.sum = RsyncUtil.fileChecksum(file.filename());
        if (remoteVersion < 21)
          out.write(file.sum, 0, 2);
        else
          out.write(file.sum);
      }

    out.flush();

    last_mode = file.mode;
    last_time = file.modtime;
    lastname = fname;
  }
}
