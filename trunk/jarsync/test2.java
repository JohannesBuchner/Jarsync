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
public class test2 {

   public static void main(String[] argv) throws Exception {
      if (argv.length < 3) {
         System.err.println("usage: test new-file old-file output-file");
         System.exit(1);
      }
      File newf = new File(argv[0]);
      File old = new File(argv[1]);
      File reconstructed = new File(argv[2]);
      Configuration config = new Configuration(
         java.security.MessageDigest.getInstance("MD4"), new Checksum32());

      Generator gen = new Generator(config);
      Collection sums = gen.generateSums(old);
      System.out.println("Checksums=");
      for (Iterator i = sums.iterator(); i.hasNext(); ) {
         System.out.println(i.next());
      }
      System.out.println();
      gen = null;

      Matcher m = new Matcher(config);
//      TwoKeyMap map = m.buildHashtable(sums);
//      System.out.println("Hashtable=");
//      map.DEBUG_printTo(System.out);
//      System.out.println();
      
      Collection deltas = m.hashSearch(sums, newf);
      System.out.println("Deltas=");
      for (Iterator i = deltas.iterator(); i.hasNext(); ) {
         System.out.println(i.next());
      }
      System.out.println();

      Rebuilder.rebuildFile(old, reconstructed, deltas);
   }
}
