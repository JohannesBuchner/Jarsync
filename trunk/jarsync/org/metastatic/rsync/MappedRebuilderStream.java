/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   MappedRebuilderStream: memory-mapped streaming file reconstructor.
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
      USA

   Linking Jarsync statically or dynamically with other modules is
   making a combined work based on Jarsync.  Thus, the terms and
   conditions of the GNU General Public License cover the whole
   combination.

   As a special exception, the copyright holders of Jarsync give you
   permission to link Jarsync with independent modules to produce an
   executable, regardless of the license terms of these independent
   modules, and to copy and distribute the resulting executable under
   terms of your choice, provided that you also meet, for each linked
   independent module, the terms and conditions of the license of that
   module.  An independent module is a module which is not derived from
   or based on Jarsync.  If you modify Jarsync, you may extend this
   exception to your version of it, but you are not obligated to do so.
   If you do not wish to do so, delete this exception statement from
   your version.  */

package org.metastatic.rsync;

import java.io.File;
import java.io.IOException;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.util.Iterator;

/**
 * A version of {@link RebuilderStream} that maps large files to memory
 * using the NIO API. Small files are not mapped and automatically use
 * the superclass's implementation.
 */
public class MappedRebuilderStream extends RebuilderStream {

   // Constants and fields.
   // -----------------------------------------------------------------------

   /**
    * The default lower bound for files to map.
    */
   public static final int MAP_LIMIT = 32768;

   /**
    * The size of the map. If not specified, the entire file is mapped.
    */
   protected long mapSize;

   /**
    * The lower bound file length to map; files smaller than this will
    * not be mapped.
    */
   protected long mapLimit;

   /**
    * The mapped file, if any.
    */
   protected MappedByteBuffer mappedFile;

   /**
    * The current offset in the file where the region is mapped.
    */
   protected long mapOffset;

   // Constructors.
   // -----------------------------------------------------------------------

   /**
    * Create a new memory mapped rebuilder, with the default map limit
    * and a maximum map size of {@link java.lang.Integer#MAX_VALUE}.
    */
   public MappedRebuilderStream() {
      this(Integer.MAX_VALUE, MAP_LIMIT);
   }

   /**
    * Create a new memory mapped rebuilder with the given map limit and
    * a maximum map size of {@link java.lang.Integer#MAX_VALUE}.
    *
    * @param mapLimit The smallest file size to map.
    */
   public MappedRebuilderStream(long mapLimit) {
      this(Integer.MAX_VALUE, mapLimit);
   }

   /**
    * Create a new memory mapped rebuilder with the given map limit and
    * maximum map size.
    *
    * @param mapSize  The maximum size of map to create.
    * @param mapLimit The smallest file size to map.
    */
   public MappedRebuilderStream(long mapSize, long mapLimit) {
      super();
      this.mapSize = mapSize;
      this.mapLimit = mapLimit;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void setBasisFile(File file) throws IOException {
      super.setBasisFile(file);
      mappedFile = null;
      if (file != null && file.length() >= mapLimit) {
         mappedFile = basisFile.getChannel().map(FileChannel.MapMode.READ_ONLY,
            0, Math.min(mapSize, file.length()));
         mapOffset = 0;
      }
   }

   public void setBasisFile(String filename) throws IOException {
      super.setBasisFile(filename);
      mappedFile = null;
      if (basisFile != null && basisFile.length() >= mapLimit) {
         mappedFile = basisFile.getChannel().map(FileChannel.MapMode.READ_ONLY,
            0, Math.min(mapSize, basisFile.length()));
         mapOffset = 0;
      }
   }

   public void update(Delta delta) throws IOException, ListenerException {
      if (mappedFile == null) {
         super.update(delta);
      } else {
         RebuilderEvent event = null;
         if (delta instanceof DataBlock) {
            event = new RebuilderEvent(((DataBlock) delta).getData(),
               delta.getWriteOffset());
         } else {
            long offset = ((Offsets) delta).getOldOffset();
            if (offset+delta.getBlockLength() > mapOffset+mappedFile.capacity())
               remapFile(offset);
            byte[] buf = new byte[delta.getBlockLength()];
            mappedFile.position((int) (offset - mapOffset));
            mappedFile.get(buf);
            event = new RebuilderEvent(buf, delta.getWriteOffset());
         }
         for (Iterator i = listeners.iterator(); i.hasNext(); )
            ((RebuilderListener) i.next()).update(event);
      }
   }

   // Own methods.
   // -----------------------------------------------------------------------

   /**
    * Remap the file, if the read offset is not currently mapped.
    *
    * @param newOffset The new offset that needs to be read. The mapped
    *    region will have this offset in the middle of the buffer.
    */
   private void remapFile(long newOffset) throws IOException {
      long newLen = Math.min(mapSize, 2 * (basisFile.length() - newOffset));
      mapOffset = newOffset - (newLen / 2);
      if (mapOffset < 0)
         mapOffset = 0;
      mappedFile = basisFile.getChannel().map(
         FileChannel.MapMode.READ_ONLY, mapOffset, newLen);
   }
}
