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

public final class Matcher implements RsyncConstants {

   // Constants and variables.
   // -----------------------------------------------------------------

   protected MD4 strongSum;
   protected RollingChecksum weakSum;
   protected int blockLength;

   // Constructors.
   // -----------------------------------------------------------------

   public Matcher() {
      this(BLOCK_LENGTH);
   }

   public Matcher(int blockLength) {
      this.blockLength = blockLength;
      strongSum = new MD4();
      weakSum = new RollingChecksum();
   }

   // Instance methods.
   // -----------------------------------------------------------------

   /**
    * Create a two-key map for the given collection of checksums.
    *
    */
   public TwoKeyMap buildHashtable(Collection sums) {
      TwoKeyMap m = new TwoKeyMap();
      for (Iterator i = sums.iterator(); i.hasNext(); ) {
         ChecksumPair pair = (ChecksumPair) i.next();
         m.put(pair, pair.getOffset());
      }
      return m;
   }

   public Collection hashSearch(TwoKeyMap m, Collection sums, File f)
   throws IOException
   {
      return hashSearch(m, sums, new FileInputStream(f));
   }

   public Collection hashSearch(TwoKeyMap m, Collection sums, InputStream in)
   throws IOException
   {
      //config.weakSum.reset();
      //do {
      //   
      //} (while bt != -1 && len == config.blockSize);
      return null;
   }

   public Collection hashSearch(TwoKeyMap m, byte[] buf, long baseOffset) {
      return hashSearch(m, buf, 0, buf.length, baseOffset);
   }

   public Collection
   hashSearch(TwoKeyMap m, byte[] buf, int off, int len, long baseOffset) {
      LinkedList deltas = new LinkedList();
      ChecksumPair p = new ChecksumPair();
      int n = Math.min(len, blockLength);
      Integer weak;
      Long oldOffset;
      int i = off, j = off;

      weakSum.reset();
      weakSum.check(buf, off, n);

      while (i < len + off) {
         weak = new Integer(weakSum.getValue());
         oldOffset = hashSearch(weak, buf, i, n, m);
         if (oldOffset != null) {
            System.err.println("third test succeeds; off=" + oldOffset);
            if (j == i && !deltas.isEmpty()) {
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
            n = Math.min(len - (i - off), blockLength);
            weakSum.check(buf, i, n);
         } else {
            if (i+n < len + off)
               weakSum.roll(buf[i+n]);
            else
               weakSum.trim();
            i++;
            n = Math.min(len - (i - off), blockLength);
         }
      }
      if (i != j) {
         byte[] b = new byte[i-j];
         System.arraycopy(buf, j, b, 0, b.length);
         deltas.add(new DataBlock(j+baseOffset, b));
      }
      return deltas;
   }

   public Long
   hashSearch(Integer weakSum, byte[] block, int off, int len, TwoKeyMap m) {
      if (m.containsKey(weakSum.intValue())) {
         System.err.println("first test succeeds; weak=" +
            Integer.toHexString(weakSum.intValue() & 0xffff));
         if (m.containsKey(weakSum)) {
            strongSum.reset();
            strongSum.update(block, off, len);
            byte[] digest = strongSum.digest();
            ChecksumPair pair = new ChecksumPair(weakSum, digest);
            System.err.println("second test succeeds; sums=" + pair);
            return (Long) m.get(new ChecksumPair(weakSum, digest));
         }
      }
      return null;
   }
}
