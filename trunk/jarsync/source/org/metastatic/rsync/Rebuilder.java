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

import java.util.*;

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

   public static void rebuildFileInPlace(File file, Collection deltas)
   throws IOException {
      RandomAccessFile f = new RandomAccessFile(file, "rw");
      List offsets = new LinkedList();
      List dataBlocks = new LinkedList();
      List conflicts = new LinkedList();
      TreeMap digraph = new TreeMap(new OffsetComparator());
      long newFileLength = 0;

      for (Iterator i = deltas.iterator(); i.hasNext(); ) {
         Object o = i.next();
         if (o instanceof Offsets) {
            offsets.add(o);
            digraph.put(o, new HashSet());
            newFileLength = Math.max(newFileLength,
               ((Offsets) o).getNewOffset()+((Offsets) o).getBlockLength());
         } else if (o instanceof DataBlock) {
            dataBlocks.add(o);
            newFileLength = Math.max(newFileLength,
               ((DataBlock) o).getOffset()+((DataBlock) o).getBlockLength());
         }
      }

      // build the digraph
      for (Iterator i = offsets.iterator(); i.hasNext(); ) {
         Offsets o1 = (Offsets) i.next();
         Set adj = (Set) digraph.get(o1);
         for (Iterator j = offsets.iterator(); j.hasNext(); ) {
            Offsets o2 = (Offsets) j.next();
            if (o1 == o2) continue;
            if (conflict(o1, o2)) {
               System.err.println("These conflict: " + o1 + " and " + o2);
               adj.add(o2);
            }
         }
      }

      // Sort the digraph topologically, removing nodes that cause cycles.
      TopologicalSorter ts = new TopologicalSorter(digraph);
      ts.sort();

      for (Iterator i = ts.getCycleNodes().iterator(); i.hasNext(); ) {
         Offsets o = (Offsets) i.next();
         System.err.println(">RFIP: conflicting offsets: " + o);
         byte[] buf = new byte[o.getBlockLength()];
         f.seek(o.getOldOffset());
         f.read(buf);
         dataBlocks.add(new DataBlock(o.getNewOffset(), buf));
      }

      for (Iterator i = ts.getFinished().iterator(); i.hasNext(); ) {
         Offsets o = (Offsets) i.next();
         System.err.println(">RFIP: offsets: " + o);
         byte[] buf = new byte[o.getBlockLength()];
         f.seek(o.getOldOffset());
         f.read(buf);
         f.seek(o.getNewOffset());
         f.write(buf);
      }

      for (Iterator i = dataBlocks.iterator(); i.hasNext(); ) {
         DataBlock db = (DataBlock) i.next();
         System.err.println(">RFIP: data block: " + db);
         f.seek(db.getOffset());
         f.write(db.getData());
      }
      if (f.length() > newFileLength) {
         f.setLength(newFileLength);
      }

      f.close();
   }

   private static boolean conflict(Offsets o1, Offsets o2) {
      return (o1.getNewOffset() >= o2.getOldOffset()
           && o1.getNewOffset() <= o2.getOldOffset()+o2.getBlockLength())
         ||  (o1.getNewOffset()+o1.getBlockLength() >= o2.getOldOffset()
           && o1.getNewOffset()+o1.getBlockLength() <= o2.getOldOffset()
               + o2.getBlockLength());
   }

 // Inner classes.
   // -----------------------------------------------------------------------

   private static class TopologicalSorter {

      // Constants and variables.

      private static final String WHITE = "white";
      private static final String GRAY  = "gray";
      private static final String BLACK = "black";

      private Map graph;
      private Map colors;
      private List finished;
      private List cycleNodes;

      // Constructor.
      
      TopologicalSorter(Map graph) {
         this.graph = graph;
         colors = new HashMap();
         finished = new LinkedList();
         cycleNodes = new LinkedList();
      }

      // Instance methods.

      void sort() {
         DFS();
      }

      List getFinished() {
         return finished;
      }

      List getCycleNodes() {
         return cycleNodes;
      }

      // Own methods.

      private void DFS() {
         for (Iterator i = graph.keySet().iterator(); i.hasNext(); ) {
            colors.put(i.next(), WHITE);
         }
         for (Iterator i = graph.keySet().iterator(); i.hasNext(); ) {
            Object u = i.next();
            if (colors.get(u).equals(WHITE)) {
               System.err.println(">>DFS: visiting " + u);
               DFSVisit(u);
            }
         }
      }

      private void DFSVisit(Object u) {
         colors.put(u, GRAY);
         for (Iterator i = ((Set) graph.get(u)).iterator(); i.hasNext(); ) {
            Object v = i.next();
            if (colors.get(v).equals(WHITE)) {
               DFSVisit(v);
            } else if (colors.get(v).equals(GRAY)) {
               cycleNodes.add(u);
            }
         }
         colors.put(u, BLACK);
         if (!cycleNodes.contains(u)) {
            finished.add(u);
         }
      }
   }

   /**
    * Sort Offsets objects by increasing write offset.
    */
   private static class OffsetComparator implements Comparator {
      public OffsetComparator() { }

      public int compare(Object o1, Object o2) {
         return (int) (((Offsets) o1).getNewOffset() -
            ((Offsets) o2).getNewOffset());
      }

      public boolean equals(Object o) {
         return (o instanceof OffsetComparator);
      }
   }
}
