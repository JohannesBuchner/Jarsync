// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// Matcher: Hashtable generation and search.
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
// --------------------------------------------------------------------

package org.metastatic.rsync;

import java.io.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Methods for performing the checksum search. The result of a search is
 * a {@link java.util.Collection} of {@link Delta} objects that, when
 * applied to a {@link Rebuilder}, will reconstruct the new version of
 * the data.
 *
 * @version $Revision$
 */
public final class Matcher implements RsyncConstants {

   // Constants and variables.
   // -----------------------------------------------------------------

   /** Our configuration. */
   protected Configuration config;

   /** The rolling checksum. */
   protected RollingChecksum weakSum;

   // Constructors.
   // -----------------------------------------------------------------

   /**
    * Create a matcher with the default configuration.
    */
   public Matcher() {
      this(new Configuration());
   }

   /**
    * Create a matcher with the specified configuration.
    */
   public Matcher(Configuration config) {
      this.config = config;
      weakSum = new RollingChecksum();
   }

 // Instance methods.
   // -----------------------------------------------------------------

   /**
    * Create a two-key map for the given collection of checksums.
    *
    * @param sums The {@link ChecksumPair}s to build the table from.
    * @return A {@link TwoKeyMap} built from the sums.
    */
   public TwoKeyMap buildHashtable(Collection sums) {
      TwoKeyMap m = new TwoKeyMap();
      for (Iterator i = sums.iterator(); i.hasNext(); ) {
         ChecksumPair pair = (ChecksumPair) i.next();
         m.put(pair, pair.getOffset());
      }
      return m;
   }

   /**
    * Search the given byte buffer.
    *
    * @param sums The checksums to search for.
    * @param buf  The data buffer to search.
    * @param baseOffset The offset from whence <code>buf</code> came.
    * @return A collection of {@link Delta}s derived from this search.
    */
   public Collection hashSearch(Collection sums, byte[] buf, long baseOffset) {
      return hashSearch(buildHashtable(sums), buf, baseOffset);
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
   public Collection hashSearch(Collection sums, byte[] buf, int off,
                                int len, long baseOffset)
   {
      return hashSearch(buildHashtable(sums), buf, off, len, baseOffset);
   }

   /**
    * Search a file.
    *
    * @param sums The checksums to search for.
    * @param f    The file to search.
    * @return A collection of {@link Delta}s derived from this search.
    */
   public Collection hashSearch(Collection sums, File f) throws IOException {
      return hashSearch(buildHashtable(sums), f);
   }

   /**
    * Create a collection of deltas derived from the given file.
    *
    * @param m The map of checksums to search for.
    * @param f The file to search.
    * @return A collection of {@link Delta}s derived from this search.
    */
   public Collection hashSearch(TwoKeyMap m, File f)
   throws IOException
   {
      return hashSearch(m, new RandomAccessFile(f, "r"));
   }

   /**
    * Create a collection of deltas derived from the given byte array.
    *
    * @param m The map of checksums to search for.
    * @param buf  The data buffer to search.
    * @param baseOffset The offset from whence <code>buf</code> came.
    * @return A collection of {@link Delta}s derived from this search.
    */
   public Collection hashSearch(TwoKeyMap m, byte[] buf, long baseOffset) {
      return hashSearch(m, buf, 0, buf.length, baseOffset);
   }

   /**
    * Create a collection of deltas derived from a portion of the given
    * byte array.
    *
    * @param m The map of checksums to search for.
    * @param buf  The data buffer to search.
    * @param off  The offset in <code>buf</code> to begin.
    * @param len  The number of bytes to search from <code>buf</code>.
    * @param baseOffset The offset from whence <code>buf</code> came.
    * @return A collection of {@link Delta}s derived from this search.
    */
   public Collection
   hashSearch(TwoKeyMap m, byte[] buf, int off, int len, long baseOffset) {
      LinkedList deltas = new LinkedList();
      ChecksumPair p = new ChecksumPair();
      int n = Math.min(len, config.blockLength);
      Integer weak;
      Long oldOffset;
      int i = off, j = off;

      weakSum.reset();
      weakSum.check(buf, off, n);

      while (i < len + off) {
         weak = new Integer(weakSum.getValue());
         oldOffset = hashSearch(weak, buf, i, n, m);
         if (oldOffset != null) {
            //System.err.println("third test succeeds; off=" + oldOffset);
            if (j == i && !deltas.isEmpty() && config.doRunLength) {
               Offsets o = (Offsets) deltas.getLast();
               o.setBlockLength(o.getBlockLength() + n);
            } else if (i != j) {
               byte[] b = new byte[i-j];
               System.arraycopy(buf, j, b, 0, b.length);
               deltas.add(new DataBlock(j+baseOffset, b));
               deltas.add(new Offsets(oldOffset.longValue(), i+baseOffset, n));
            } else {
               deltas.add(new Offsets(oldOffset.longValue(), i+baseOffset, n));
            }
            i += n;
            j = i;
            n = Math.min(len - (i - off), config.blockLength);
            weakSum.check(buf, i, n);
         } else {
            if (i+n < len + off)
               weakSum.roll(buf[i+n]);
            else
               weakSum.trim();
            i++;
            n = Math.min(len - (i - off), config.blockLength);
         }
      }
      if (i != j) {
         byte[] b = new byte[i-j];
         System.arraycopy(buf, j, b, 0, b.length);
         deltas.add(new DataBlock(j+baseOffset, b));
      }
      return deltas;
   }

 // Own methods
   // -----------------------------------------------------------------------

   /**
    * Search if a portion of the given byte array is in the map,
    * returning its original offset if it is.
    *
    * @param weakSum The weak sum to search for.
    * @param block   The block of bytes to search for.
    * @param off     The offset in the block to begin.
    * @param len     The number of bytes to read from the block.
    * @param m       The map to search.
    * @return The original offset of the given block if it was found in
    *    the map. null if it was not found.
    */
   protected Long
   hashSearch(Integer weakSum, byte[] block, int off, int len, TwoKeyMap m) {
      if (m.containsKey(weakSum.intValue())) {
         //System.err.println("first test succeeds; weak=" +
         //   Integer.toHexString(weakSum.intValue() & 0xffff));
         if (m.containsKey(weakSum)) {
            config.strongSum.reset();
            config.strongSum.update(block, off, len);
            byte[] digest = new byte[config.strongSumLength];
            System.arraycopy(config.strongSum.digest(), 0, digest, 0,
               digest.length);
            ChecksumPair pair = new ChecksumPair(weakSum, digest);
            //System.err.println("second test succeeds; sums=" + pair);
            return (Long) m.get(new ChecksumPair(weakSum, digest));
         }
      }
      return null;
   }

   /**
    * Search if a portion of the given file is in the map, returning its
    * original offset if it is.
    * 
    * @param weakSum The weak sum to search for.
    * @param f       The file to search.
    * @param off     The offset in the file to begin.
    * @param len     The number of bytes to read from the file.
    * @param m       The map to search.
    * @return The original offset of the given block if it was found in
    *    the map. null if it was not found.
    */
   protected Long hashSearch(Integer weakSum, RandomAccessFile f, long off,
      int len, TwoKeyMap m) throws IOException
   {
      if (m.containsKey(weakSum.intValue())) {
         //System.err.println("first test succeeds; weak=" +
         //   Integer.toHexString(weakSum.intValue() & 0xffff));
         if (m.containsKey(weakSum)) {
            byte[] buf = new byte[len];
            f.seek(off);
            int l = f.read(buf);
            config.strongSum.reset();
            config.strongSum.update(buf, 0, l);
            byte[] digest = new byte[config.strongSumLength];
            System.arraycopy(config.strongSum.digest(), 0, digest, 0,
               digest.length);
            ChecksumPair pair = new ChecksumPair(weakSum, digest);
            //System.err.println("second test succeeds; sums=" + pair);
            return (Long) m.get(new ChecksumPair(weakSum, digest));
         }
      }
      return null;
   }

   /**
    * Create a collection of deltas from the given RandomAccessFile.
    *
    * @param m The map to search.
    * @param f The file to search.
    * @return A collection of {@link Delta}s derived from this search.
    */
   protected Collection
   hashSearch(TwoKeyMap m, RandomAccessFile f) throws IOException {
      LinkedList deltas = new LinkedList();
      ChecksumPair p = new ChecksumPair();
      long len = f.length();
      int n = (int) Math.min(len, config.blockLength);
      byte[] buf = new byte[n];
      Integer weak;
      Long oldOffset;
      long i = 0, j = 0;

      weakSum.reset();
      f.read(buf, 0, n);
      weakSum.check(buf, 0, n);

      while (i < len) {
         weak = new Integer(weakSum.getValue());
         oldOffset = hashSearch(weak, f, i, n, m);
         if (oldOffset != null) {
            //System.err.println("third test succeeds; off=" + oldOffset);
            if (j == i && !deltas.isEmpty()) {
               Offsets o = (Offsets) deltas.getLast();
               o.setBlockLength(o.getBlockLength() + n);
            } else if (i != j) {
               byte[] b = new byte[(int) (i-j)];
               f.seek(j);
               f.read(b, 0, b.length);
               deltas.add(new DataBlock(j, b));
               deltas.add(new Offsets(oldOffset.longValue(), i, n));
               //f.seek(i+n);
            } else {
               deltas.add(new Offsets(oldOffset.longValue(), i, n));
            }
            i += n;
            j = i;
            n = (int) Math.min(len - i, config.blockLength);
            f.read(buf, 0, n);
            weakSum.check(buf, 0, n);
         } else {
            if (i+n < len)
               weakSum.roll((byte) f.read());
            else
               weakSum.trim();
            i++;
            n = (int) Math.min(len - i, config.blockLength);
         }
      }
      if (i != j) {
         byte[] b = new byte[(int) (i-j)];
         f.seek(j);
         f.read(b, 0, b.length);
         deltas.add(new DataBlock(j, b));
      }
      f.close();
      return deltas;
   }
}
