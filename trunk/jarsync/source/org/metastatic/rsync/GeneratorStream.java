/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$
  
   GeneratorStream: streaming alternative to Generator.
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
   your version. */

package org.metastatic.rsync;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class GeneratorStream {

   // Constants and fields.
   // -----------------------------------------------------------------------

   /**
    * The configuration.
    */
   protected final Configuration config;

   /**
    * The list of {@link GeneratorListeners}.
    */
   protected final List listeners;

   /**
    * The intermediate byte buffer.
    */
   protected final byte[] buffer;

   /**
    * The current index in {@link #buffer}.
    */
   protected int ndx;

   /**
    * The number of bytes summed thusfar.
    */
   protected long count;

   // Constructor.
   // -----------------------------------------------------------------------

   public GeneratorStream(Configuration config) {
      this.config = (Configuration) config.clone();
      this.listeners = new LinkedList();
      buffer = new byte[config.blockLength];
      reset();
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   /**
    * Add a {@link GeneratorListener} to the list of listeners.
    *
    * @param listener The listener to add.
    */
   public void addListener(GeneratorListener listener) {
      if (listener == null)
         throw new IllegalArgumentException();
      listeners.add(listener);
   }

   /**
    * Remove a {@link GeneratorListener} from the list of listeners.
    *
    * @param listener The listener to add.
    * @return True if a listener was really removed (i.e. that the
    *         listener was in the list to begin with).
    */
   public boolean removeListener(GeneratorListener listener) {
      return listeners.remove(listener);
   }

   /**
    * Reset this generator, to be used for another data set.
    */
   public void reset() {
      ndx = 0;
      count = 0L;
   }

   /**
    * Update this generator with a single byte.
    *
    * @param b The next byte
    */
   public void update(byte b) {
      buffer[ndx++] = b;
      if (ndx == buffer.length) {
         ChecksumPair p = generateSum(buffer, 0, buffer.length);
         for (Iterator it = listeners.listIterator(); it.hasNext(); )
            ((GeneratorListener) it.next()).update(new GeneratorEvent(p));
         ndx = 0;
      }
   }

   /**
    * Update this generator with a portion of a byte array.
    *
    * @param buf The next bytes.
    * @param off The offset to begin at.
    * @param len The number of bytes to update.
    */
   public void update(byte[] buf, int off, int len) {
      int i = off;
      do {
         int l = Math.min(len - (i - off), buffer.length - ndx);
         System.arraycopy(buf, i, buffer, ndx, l);
         i += l;
         ndx += l;
         if (ndx == buffer.length) {
            ChecksumPair p = generateSum(buffer, 0, buffer.length);
            for (Iterator it = listeners.listIterator(); it.hasNext(); )
               ((GeneratorListener) it.next()).update(new GeneratorEvent(p));
            ndx = 0;
         }
      } while (i < off + len);
   }

   /**
    * Update this generator with a byte array.
    *
    * @param buf The next bytes.
    */
   public void update(byte[] buf) {
      update(buf, 0, buf.length);
   }

   /**
    * Finish generating checksums, flushing any buffered data and
    * resetting this instance.
    */
   public void doFinal() {
      if (ndx > 0) {
         ChecksumPair p = generateSum(buffer, 0, ndx);
         for (Iterator it = listeners.listIterator(); it.hasNext(); )
            ((GeneratorListener) it.next()).update(new GeneratorEvent(p));
      }
      reset();
   }

   // Own methods.
   // -----------------------------------------------------------------------

   /**
    * Generate a sum pair for a portion of a byte array.
    * 
    * @param buf The byte array to checksum.
    * @param off Where in <code>buf</code> to start.
    * @param len How many bytes to checksum.
    * @return A {@link ChecksumPair} for this byte array.
    */
   protected ChecksumPair generateSum(byte[] buf, int off, int len) {
      ChecksumPair p = new ChecksumPair();
      config.weakSum.check(buf, off, len);
      config.strongSum.update(buf, off, len);
      if (config.checksumSeed != null) {
         config.strongSum.update(config.checksumSeed, 0,
            config.checksumSeed.length);
      }
      p.weak = config.weakSum.getValue();
      p.strong = new byte[config.strongSumLength];
      System.arraycopy(config.strongSum.digest(), 0, p.strong, 0,
         config.strongSumLength);
      p.offset = count;
      p.length = len;
      count += len;
      return p;
   }
}
