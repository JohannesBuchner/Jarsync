/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   NonblockingFileList: send and receive a list of files.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync.

   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2 of the License, or (at
   your option) any later version.

   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jarsync; if not, write to the

      Free Software Foundation, Inc.,
      59 Temple Place, Suite 330,
      Boston, MA  02111-1307
      USA  */

package org.metastatic.rsync.v2;

import java.io.FileInputStream ;
import java.io.IOException;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import java.security.MessageDigest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javaunix.UnixSystem;
import javaunix.io.UnixFile;

import org.apache.log4j.Logger;

/**
 * Class for sending and receiving file lists.
 */
final class NonblockingFileList implements NonblockingTool, Constants {

   // Constants and fields.
   // -----------------------------------------------------------------------

   private final Options options;
   private final String path;
   private final List argv;
   private final Logger logger;
   private final int remoteVersion;

   private final List files;
   private final Map uids;
   private final Map gids;

   private Iterator id_iterator;
   private int index;
   private FileInfo lastfile;
   private String lastname = "";
   private String lastdir = null;
   private int state;
   private int io_error = 0;

   private DuplexByteBuffer outBuffer;
   private ByteBuffer inBuffer;

   // Constructors.
   // -----------------------------------------------------------------------

   NonblockingFileList(Options options, String path, List argv,
                       int remoteVersion, Logger logger, int state)
   {
      this.options = options;
      this.path = path;
      this.argv = argv;
      this.remoteVersion = remoteVersion;
      this.logger = logger;
      this.state = state;
      files = new LinkedList();
      uids = new HashMap();
      gids = new HashMap();
      index = 0;
      lastfile = new FileInfo();
      lastfile.basename = "";
      lastfile.dirname = "";
      lastfile.mode = 0;
      lastfile.rdev = -1;
      lastfile.uid = -1;
      lastfile.gid = -1;
      lastfile.modtime = 0;
   }

   // NonblockingTool implementation.
   // -----------------------------------------------------------------------

   public boolean updateInput() throws Exception {
      switch (state & INPUT_MASK) {
         case FLIST_RECEIVE_FILES:
            getNextFile();
            return true;
         case FLIST_RECEIVE_UIDS:
         case FLIST_RECEIVE_GIDS:
         case FLIST_RECEIVE_DONE:
            if (remoteVersion >= 17)
               io_error = inBuffer.getInt();
            return false;
      }
      return false;
   }

   public boolean updateOutput() throws Exception {
      switch (state & OUTPUT_MASK) {
         case FLIST_SEND_FILES:
            sendNextFile();
            return true;
         case FLIST_SEND_UIDS:
            sendUidList();
            return true;
         case FLIST_SEND_GIDS:
            sendGidList();
            return true;
         case FLIST_SEND_DONE:
            outBuffer.put((byte) 0);
            if (remoteVersion >= 17)
               outBuffer.putInt(io_error);
            return false;
      }
      return false;
   }

   public void setBuffers(DuplexByteBuffer out, ByteBuffer in) {
      outBuffer = out;
      inBuffer = in;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   /**
    * Get the list of files, either read over the wire, or built in
    * memory.
    */
   List getFileList() {
      return files;
   }

   /**
    * Get the UID list, either read over the wire, or built in memory.
    */
   Map getUidList() {
      return uids;
   }

   /**
    * Get the GID list, either read over the wire, or built in memory.
    */
   Map getGidList() {
      return gids;
   }

   // Own methods.
   // -----------------------------------------------------------------------

   /**
    * Receives the next file entry from the buffer.
    */
   private void getNextFile() {
      int flags = inBuffer.get() & 0xFF;

      /*
       * We might not have enough bytes in the buffer, and we cannot
       * know how much this entry will need, so we take care reading the
       * data, and rewind if an underflow occurs.
       */
      int entryLen = 1;
      int l1 = 0, l2 = 0;
      FileInfo file = new FileInfo();

      if (flags == 0) {
         if (options.preserve_uid)
            state = FLIST_RECEIVE_UIDS;
         else if (options.preserve_gid)
            state = FLIST_RECEIVE_GIDS;
         else
            state = FLIST_RECEIVE_DONE;
         return;
      }
      try {
         if ((flags & SAME_NAME) != 0) {
            l1 = inBuffer.get() & 0xFF;
            entryLen++;
         }
         String thisname = null;
         if ((flags & LONG_NAME) != 0) {
            thisname = BufferUtil.getString(inBuffer, MAXPATHLEN);
            entryLen += 4 + thisname.length();
         } else {
            thisname = BufferUtil.getShortString(inBuffer);
            entryLen += 1 + thisname.length();
         }
         if (lastname.length() > 0 && l1 > 0)
            thisname = lastname.substring(0, l1) + thisname;

         thisname = thisname.replace('/', UnixFile.separatorChar);
         int p = 0;
         if ((p = thisname.lastIndexOf(UnixFile.separatorChar)) > 0) {
            if (lastdir != null && thisname.startsWith(lastdir)) {
               file.dirname = lastdir;
            } else {
               file.dirname = thisname.substring(0, p);
               lastdir = file.dirname;
            }
            file.basename = thisname.substring(p + 1);
         } else {
            file.dirname = "";
            file.basename = thisname;
         }

         file.flags = flags;
         file.length = BufferUtil.getLong(inBuffer);
         entryLen += (file.length > 0x7FFFFFFF) ? 12 : 4;
         if ((flags & SAME_TIME) == 0) {
            file.modtime = inBuffer.getInt() & 0xFFFFFFFFL;
            entryLen += 4;
         } else {
            file.modtime = lastfile.modtime;
         }
         if ((flags & SAME_MODE) == 0) {
            file.mode = inBuffer.getInt();
            entryLen += 4;
         } else {
            file.mode = lastfile.mode;
         }

         if (options.preserve_uid) {
            if ((flags & SAME_UID) == 0) {
               file.uid = inBuffer.getInt();
               entryLen += 4;
            } else {
               file.uid = lastfile.uid;
            }
         }
         if (options.preserve_gid) {
            if ((flags & SAME_GID) == 0) {
               file.gid = inBuffer.getInt();
               entryLen += 4;
            } else {
               file.gid = lastfile.gid;
            }
         }
         if (options.preserve_links && file.S_ISLNK()) {
            file.link = BufferUtil.getString(inBuffer, 0);
            entryLen += 4 + file.link.length();
         }
         if (options.always_checksum) {
            file.sum = new byte[SUM_LENGTH];
            if (remoteVersion < 21) {
               inBuffer.get(file.sum, 0, 2);
               entryLen += 2;
            } else {
               inBuffer.get(file.sum);
               entryLen += file.sum.length;
            }
         }

         lastfile = file;
         lastname = thisname;
         files.add(file);
         logger.debug("got file=" + file);
      } catch (BufferUnderflowException bue) {
         // rewind the buffer.
         inBuffer.position(inBuffer.position() - entryLen);
         throw bue;
      }
   }

   /**
    * Send the next file entry.
    */
   private void sendNextFile() throws Exception {
      if (argv.size() == 0) {
         state = FLIST_SEND_DONE;
         return;
      }
      UnixFile f = new UnixFile(path, (String) argv.get(index));
      if (f.isDirectory() && !options.recurse) {
         logger.info("skipping directory " + f.getName());
         if (++index < argv.size()) {
            if (!options.numeric_ids) {
               if (options.preserve_uid) {
                  id_iterator = uids.keySet().iterator();
                  state = FLIST_SEND_UIDS;
               } else if (options.preserve_gid) {
                  id_iterator = gids.keySet().iterator();
                  state = FLIST_SEND_GIDS;
               } else {
                  state = FLIST_SEND_DONE;
               }
            } else {
               state = FLIST_SEND_DONE;
            }
         }
         return;
      }

      FileInfo file = null;
      try {
         file = new FileInfo(f);
      } catch (IOException ioe) {
         io_error = 1;
         file = new FileInfo();
         file.dirname = f.getParent();
         file.basename = f.getName();
      }
      if (file.dirname.startsWith(path)) {
         file.dirname = file.dirname.substring(path.length());
      }
      if (options.always_checksum) {
         if (file.S_ISREG()) {
            try {
               file.sum = RsyncUtil.fileChecksum(f);
            } catch (Exception e) {
               io_error = 1;
               file.sum = new byte[SUM_LENGTH];
            }
         } else {
            file.sum = new byte[SUM_LENGTH];
         }
      }
      files.add(file);

      file.flags = 0;
      if (file.mode == lastfile.mode)
         file.flags |= SAME_MODE;
      if (file.rdev == lastfile.rdev)
         file.flags |= SAME_RDEV;
      if (file.uid == lastfile.uid)
         file.flags |= SAME_UID;
      if (file.gid == lastfile.gid)
         file.flags |= SAME_GID;
      if (file.modtime == lastfile.modtime)
         file.flags |= SAME_TIME;

      logger.debug("sending file entry: " + file);

      String fname = file.dirname;
      if (fname.startsWith(UnixFile.separator))
         fname = fname.substring(1);
      if (fname.length() > 0)
         fname += UnixFile.separator;
      fname += file.basename;
      int l1;
      for (l1 = 0; l1 < fname.length() && l1 < lastname.length() &&
         fname.charAt(l1) == lastname.charAt(l1) && l1 < 255;
         l1++);
      int l2 = fname.length() - l1;
      lastname = fname;

      if (l1 > 0)
         file.flags |= SAME_NAME;
      if (l2 > 255)
         file.flags |= LONG_NAME;

      if (file.flags == 0 && !f.isDirectory())
         file.flags = FLAG_DELETE;
      if (file.flags == 0)
         file.flags = LONG_NAME;

      outBuffer.put((byte) file.flags);
      if ((file.flags & SAME_NAME) != 0)
         outBuffer.put((byte) l1);
      if ((file.flags & LONG_NAME) != 0)
         outBuffer.putString(
            fname.substring(fname.length() - l2)
                 .replace(UnixFile.separatorChar, '/'));
      else
         outBuffer.putShortString(
            fname.substring(fname.length() - l2)
                 .replace(UnixFile.separatorChar, '/'));

      outBuffer.putLong(file.length);
      if ((file.flags & SAME_TIME) == 0)
         outBuffer.putInt((int) file.modtime);
      if ((file.flags & SAME_MODE) == 0)
         outBuffer.putInt(toWireMode(file.mode));
      if (options.preserve_uid && (file.flags & SAME_UID) == 0) {
         try {
            Integer uid = new Integer(file.uid);
            String user = UnixSystem.getPasswordByUid(file.uid).pw_name;
            if (!uids.containsKey(uid))
               uids.put(uid, user);
         } catch (IOException ioe) {
            io_error = 1;
         }
         outBuffer.putInt(file.uid);
      }
      if (options.preserve_gid && (file.flags & SAME_GID) == 0) {
         try {
            Integer gid = new Integer(file.gid);
            String group = UnixSystem.getGroupByGid(file.gid).gr_name;
            if (!gids.containsKey(gid))
               gids.put(gid, group);
         } catch (IOException ioe) {
            io_error = 1;
         }
         outBuffer.putInt(file.gid);
      }
      if (options.preserve_links && file.S_ISLNK()) {
         outBuffer.putString(file.link);
      }

      if (options.always_checksum) {
         if (remoteVersion < 21)
            outBuffer.put(file.sum, 0, 2);
         else
            outBuffer.put(file.sum);
      }

      if (f.isDirectory() && options.recurse)
         expandDirectory(f);

      if (++index == argv.size()) {
         if (!options.numeric_ids) {
            if (options.preserve_uid) {
               id_iterator = uids.keySet().iterator();
               state = FLIST_SEND_UIDS;
            } else if (options.preserve_gid) {
               id_iterator = gids.keySet().iterator();
               state = FLIST_SEND_GIDS;
            } else {
               state = FLIST_SEND_DONE;
            }
         } else {
            state = FLIST_SEND_DONE;
         }
      }
   }

   /**
    * Sends the next UID in the list.
    */
   private void sendUidList() throws Exception {
      if (!id_iterator.hasNext()) {
         outBuffer.putInt(0);
         if (options.preserve_gid) {
            id_iterator = gids.keySet().iterator();
            state = FLIST_SEND_GIDS;
         } else {
            state = FLIST_SEND_DONE;
         }
         return;
      }
      Integer uid = (Integer) id_iterator.next();
      outBuffer.putInt(uid.intValue());
      outBuffer.putShortString((String) uids.get(uid));
   }

   /**
    * Sends the next GID in the list.
    */
   private void sendGidList() throws Exception {
      if (!id_iterator.hasNext()) {
         outBuffer.putInt(0);
         state = FLIST_SEND_DONE;
         return;
      }
      Integer gid = (Integer) id_iterator.next();
      outBuffer.putInt(gid.intValue());
      outBuffer.putShortString((String) gids.get(gid));
   }

   /**
    * Expand the given directory, adding its entries (minus ones we
    * ignore) to the list.
    */
   private void expandDirectory(UnixFile dir) {
      String dirname = dir.getPath();
      logger.debug("dirname=" + dirname);
      if (dirname.startsWith(path))
         dirname = dirname.substring(path.length());
      if (dirname.startsWith(UnixFile.separator))
         dirname = dirname.substring(1);
      if (dirname.startsWith("."))
         dirname = dirname.substring(1);
      logger.debug("dirname=" + dirname);
      String[] list = dir.list(new Glob(options.exclude, false,
         path + UnixFile.separator + argv.get(0)));
      if (list != null) {
         for (int i = 0; i < list.length; i++) {
            argv.add(index+1, dirname + UnixFile.separator + list[i]);
         }
      }
   }

   private static int toWireMode(int mode) {
      if (FileInfo.S_ISLNK(mode))
         return (mode & ~(_S_IFMT)) | 0120000;
      return mode;
   }
}
