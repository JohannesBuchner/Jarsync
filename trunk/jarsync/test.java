/*
 * test.java -- Simple test for jarsync.
 * Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
 *
 * see COPYING for details.
 */

import java.io.*;
import java.util.Collection;
import java.util.Iterator;

import org.metastatic.rsync.*;

/**
 *
 */
public class test {

   public static void main(String[] argv) throws Exception {
      if (argv.length < 2) {
         System.err.println("usage: test new-file old-file");
         System.exit(1);
      }
      File newf = new File(argv[0]);
      File old = new File(argv[1]);

      FileInputStream oldIn = new FileInputStream(old);
      byte[] buf = new byte[10*1024*1024];
      int len = 0;
      long off = 0;
      Generator gen = new Generator();
      Collection sums = null;
      while((len = oldIn.read(buf)) != -1) {
         if (sums == null) {
            sums = gen.generateSums(buf, 0, len);
         } else {
            sums.addAll(gen.generateSums(buf, 0, len, off));
         }
         off += len;
      }
      System.out.println("Checksums=");
      for (Iterator i = sums.iterator(); i.hasNext(); ) {
         System.out.println(i.next());
      }
      System.out.println();
      oldIn.close();
      gen = null;
      off = 0;
      len = 0;

      Matcher m = new Matcher();
      TwoKeyMap map = m.buildHashtable(sums);
      System.out.println("Hashtable=");
      map.DEBUG_printTo(System.out);
      System.out.println();
      
      Collection deltas = null;
      FileInputStream newIn = new FileInputStream(newf);
      while((len = newIn.read(buf)) != -1) {
         if (deltas == null) {
            deltas = m.hashSearch(map, buf, 0, len, off);
         } else {
            deltas.addAll(m.hashSearch(map, buf, 0, len, off));
         }
         off += len;
      }
      System.out.println("Deltas=");
      for (Iterator i = deltas.iterator(); i.hasNext(); ) {
         System.out.println(i.next());
      }
      System.out.println();
      newIn.close();
   }
}
