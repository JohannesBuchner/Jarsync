// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// SimpleTest: real test of the algorithm.
// Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of Jarsync
//
// Jarsync is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2 of the License, or (at
// your option) any later version.
//
// Jarsync is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Jarsync; if not, write to the
//
//    Free Software Foundation, Inc.,
//    59 Temple Place, Suite 330,
//    Boston, MA  02111-1307
//    USA
//
// Linking this library statically or dynamically with other modules is
// making a combined work based on this library.  Thus, the terms and
// conditions of the GNU General Public License cover the whole
// combination.
//
// As a special exception, the copyright holders of this library give
// you permission to link this library with independent modules to
// produce an executable, regardless of the license terms of these
// independent modules, and to copy and distribute the resulting
// executable under terms of your choice, provided that you also meet,
// for each linked independent module, the terms and conditions of the
// license of that module.  An independent module is a module which is
// not derived from or based on this library.  If you modify this
// library, you may extend this exception to your version of the
// library, but you are not obligated to do so.  If you do not wish to
// do so, delete this exception statement from your version.
//
// --------------------------------------------------------------------------

// Tags: JARSYNC

package gnu.testlet.org.metastatic.rsync;

import gnu.testlet.Testlet;
import gnu.testlet.TestHarness;
import java.security.*;
import java.util.*;
import org.metastatic.rsync.*;

/**
 * Exercise the rsync algorithm 50 times, each time with randomly
 * generated data sets and differences, and with a random message digest
 * algorithm.
 *
 * @version $Revision $
 */
public class SimpleTest implements Testlet {

   // Fields.
   // -----------------------------------------------------------------------

   Random rand;

   // Constructor.
   // -----------------------------------------------------------------------

   public SimpleTest() {
      rand = new Random();
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void test(TestHarness harness) {
      String[] mds = getMessageDigests();
      harness.checkPoint("rsyncTest");
      Security.addProvider(new JarsyncProvider());
      Configuration conf = new Configuration();

      // Make sure we use our MD4 at least once!
      try {
         conf.strongSum = MessageDigest.getInstance("MD4", "JARSYNC");
      } catch (Exception x) {
         throw new Error(x);
      }
      conf.weakSum = new Checksum32();
      conf.strongSumLength = conf.strongSum.getDigestLength();

      for (int i = 0; i < 50; i++) {
         conf.blockLength = rand.nextInt(1400) + 250;
         byte[] n3w = new byte[rand.nextInt(1000000) + 500];
         rand.nextBytes(n3w);
         byte[] old = mutate(n3w, harness);
         harness.verbose("TEST #" + (i+1) + ": old data=" + old.length
            + " bytes, target data=" + n3w.length + " bytes, blocks="
            + conf.blockLength + " bytes, digest="
            + conf.strongSum.getAlgorithm());
         Generator gen = new Generator(conf);
         Collection sums = gen.generateSums(old);
         Matcher mat = new Matcher(conf);
         Collection deltas = mat.hashSearch(sums, n3w, 0);
         byte[] reconst = Rebuilder.rebuild(old, deltas);
         harness.check(Arrays.equals(n3w, reconst));
         try {
            conf.strongSum = MessageDigest.getInstance(
               mds[rand.nextInt(mds.length)]);
            conf.strongSumLength = conf.strongSum.getDigestLength();
         } catch (Exception x) {
            throw new Error(x);
         }
      }
   }

   // Own methods.
   // -----------------------------------------------------------------------

   /**
    * Derived from `mutate.pl' from librsync, (C) 1999, 2000 by Martin
    * Pool and (C) 1999 by Andrew Tridgell.
    */
   private byte[] mutate(byte[] b, TestHarness harness) {
      StringBuffer corpus = new StringBuffer(new String(b));
      int nmuts = 1 + rand.nextInt(30);
      harness.verbose("\t" + nmuts + " mutations");
      while (--nmuts > 0) {
         int from_off = rand.nextInt(corpus.length());
         int from_len = (int)(rand.nextDouble()
                      * rand.nextInt(corpus.length() - from_off));
         int to_off   = rand.nextInt(corpus.length());
         int to_len   = (int)(rand.nextDouble()
                      * rand.nextInt(corpus.length() - to_off));
         switch (rand.nextInt(3)) {
            case 0:
               harness.verbose("\tcopy and overwrite (" + from_off + ", "
                  + from_len + ") -> (" + to_off + ", " + to_len + ")");
               corpus.replace(to_off, to_off+to_len,
                  corpus.substring(from_off, from_off+from_len));
               break;
            case 1:
               harness.verbose("\tcopy and insert (" + from_off + ", "
                  + from_len + ") -> (" + to_off + ", " + to_len + ")");
               corpus.insert(to_off,
                  corpus.substring(from_off, from_off+from_len));
               break;
            case 2:
               harness.verbose("\tdelete (" + from_off + ", " + from_len + ")");
               corpus.delete(from_off, from_off + from_len);
               break;
         }
      }
      return corpus.toString().getBytes();
   }

   private String[] getMessageDigests() {
      HashSet algs = new HashSet();
      String[] tries = {
         "md2", "md4", "md5", "sha-1", "ripemd128", "ripemd160",
         "tiger", "whirlpool", "brokenmd4"
      };
      for (int i = 0; i < tries.length; i++) {
         try {
            MessageDigest.getInstance(tries[i]);
            algs.add(tries[i]);
         } catch (Exception x) { }
      }
      return (String[]) algs.toArray(tries);
   }
}
