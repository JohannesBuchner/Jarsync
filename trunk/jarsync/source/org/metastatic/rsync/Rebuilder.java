// vim:set tabstop=3 expandtab tw=78:
// $Id$
//
// Rebuilder -- File reconstruction from deltas.
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
// --------------------------------------------------------------------------

package org.metastatic.rsync;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.Collection;
import java.util.Iterator;

public class Rebuilder {

   // Constants and variables.
   // -----------------------------------------------------------------------

   private static final String TMP_PREFIX = ".jarsync-";
   private static final String TMP_SUFFIX = ".temp";

   // Class methods.
   // -----------------------------------------------------------------------

   /**
    * Reconstruct a file into a new file created with {@link
    * java.io.File#createTempFile(java.lang.String,java.lang.String,java.io.File)}.
    * This file can then be renamed to the destination.
    */
   public static File rebuildFile(File oldFile, Collection deltas)
   throws IOException {
      File newFile = File.createTempFile(TMP_PREFIX, TMP_SUFFIX,
         oldFile.getParentFile());
      rebuildFile(oldFile, newFile, deltas);
      return newFile;
   }

   /**
    * Reconstruct a file into <code>newFile</code>.
    */
   public static void rebuildFile(File oldFile, File newFile, Collection deltas)
   throws IOException {
      RandomAccessFile out = new RandomAccessFile(newFile, "rw");
      RandomAccessFile in = new RandomAccessFile(oldFile, "r");

      for (Iterator i = deltas.iterator(); i.hasNext(); ) {
         Object o = i.next();
         if (o instanceof DataBlock) {
            long off = ((DataBlock) o).getOffset();
            out.seek(off);
            out.write(((DataBlock) o).getData());
         } else if (o instanceof Offsets) {
            int len = ((Offsets) o).getBlockLength();
            long off1 = ((Offsets) o).getOldOffset();
            long off2 = ((Offsets) o).getNewOffset();
            byte[] buf = new byte[len];
            in.seek(off1);
            in.read(buf);
            out.seek(off2);
            out.write(buf);
         }
      }

      in.close();
      out.close();
   }
}
