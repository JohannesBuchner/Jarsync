/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$
  
   Matcher: Hashtable generation and search.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>
  
   This file is a part of Jarsync
  
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

import java.io.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Methods for performing the checksum search. The result of a search
 * is a {@link java.util.List} of {@link Delta} objects that, when
 * applied to a method in {@link Rebuilder}, will reconstruct the new
 * version of the data.</p>
 *
 * @version $Revision$
 */
public final class Matcher {

   // Constants and variables.
   // -----------------------------------------------------------------

   /** The list of deltas being built. */
   protected final List deltas;

   /** The underlying matcher stream. */
   protected final MatcherStream matcher;

   /** The size of allocated byte arrays. */
   protected final int chunkSize;

   // Constructors.
   // -----------------------------------------------------------------

   /**
    * Create a matcher with the specified configuration.
    *
    * @param config The {@link Configuration} for this Matcher.
    */
   public Matcher(Configuration config) {
      deltas = new LinkedList();
      matcher = new MatcherStream(config);
      matcher.addListener(new Callback(deltas));
      chunkSize = config.chunkSize;
   }

 // Instance methods.
   // -----------------------------------------------------------------

   /**
    * Search the given byte buffer.
    *
    * @param sums The checksums to search for.
    * @param buf  The data buffer to search.
    * @param baseOffset The offset from whence <code>buf</code> came.
    * @return A collection of {@link Delta}s derived from this search.
    */
   public List hashSearch(List sums, byte[] buf) {
      return hashSearch(sums, buf, 0, buf.length);
   }

   /**
    * Search a portion of a byte buffer.
    *
    * @param sums The checksums to search for.
    * @param buf  The data buffer to search.
    * @param off  The offset in <code>buf</code> to begin.
    * @param len  The number of bytes to search from <code>buf</code>.
    * @param baseOffset The offset from whence <code>buf</code> came.
    * @return A collection of {@link Delta}s derived from this search.
    */
   public List hashSearch(List sums, byte[] buf, int off, int len) {
      deltas.clear();
      matcher.reset();
      matcher.setChecksums(sums);
      matcher.update(buf, off, len);
      matcher.doFinal();
      return new LinkedList(deltas);
   }

   /**
    * Search a file by name.
    *
    * @param sums The checksums to search for.
    * @param filename The name of the file to search.
    * @return A list of deltas derived from this search.
    * @throws IOException If <i>filename</i> cannot be read.
    */
   public List hashSearch(List sums, String filename) throws IOException {
      return hashSearch(sums, new FileInputStream(filename));
   }

   /**
    * Search a file.
    *
    * @param sums The checksums to search for.
    * @param f    The file to search.
    * @return A list of {@link Delta}s derived from this search.
    * @throws IOException If <i>f</i> cannot be read.
    */
   public List hashSearch(List sums, File f) throws IOException {
      return hashSearch(sums, new FileInputStream(f));
   }

   /**
    * Search an input stream.
    *
    * @param m  The {@link TwoKeyMap} to search.
    * @param in The input stream to search.
    * @return A collection of {@link Delta}s derived from this search.
    * @throws IOException If an exception occurs while reading.
    */
   public List hashSearch(List sums, InputStream in) throws IOException {
      deltas.clear();
      matcher.reset();
      matcher.setChecksums(sums);
      byte[] buffer = new byte[chunkSize];
      int len = 0, off = 0;
      while ((len = in.read(buffer)) != -1) {
         matcher.update(buffer, off, len);
         off += len;
      }
      matcher.doFinal();
      return new LinkedList(deltas);
   }

   // Inner classes.
   // -----------------------------------------------------------------------

   /**
    * Trivial implementation of a MatcherListener that simply adds
    * incoming deltas to a List.
    */
   private class Callback implements MatcherListener {

      // Fields.
      // --------------------------------------------------------------------

      private final List deltas;

      // Constructors.
      // --------------------------------------------------------------------

      Callback(List deltas) {
         this.deltas = deltas;
      }

      // Instance methods.
      // --------------------------------------------------------------------

      public void update(Delta d) {
         deltas.add(d);
      }
   }
}
