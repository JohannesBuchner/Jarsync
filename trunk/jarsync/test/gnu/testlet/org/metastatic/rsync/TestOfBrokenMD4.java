// based on TestOfMD4 from GNU Crypto

package gnu.testlet.org.metastatic.rsync;

// ----------------------------------------------------------------------------
// $Id$
//
// Copyright (C) 2001, 2002, Free Software Foundation, Inc.
//
// This file is part of GNU Crypto.
//
// GNU Crypto is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2, or (at your option)
// any later version.
//
// GNU Crypto is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; see the file COPYING.  If not, write to the
//
//    Free Software Foundation Inc.,
//    59 Temple Place - Suite 330,
//    Boston, MA 02111-1307
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
// ----------------------------------------------------------------------------

// Tags: JARSYNC

import gnu.testlet.Testlet;
import gnu.testlet.TestHarness;
import java.security.MessageDigest;
import java.security.Security;
import org.metastatic.rsync.JarsyncProvider;
import org.metastatic.rsync.Util;

/**
 * <p>Conformance tests for the 'broken' MD4 implementation.</p>
 *
 * @version $Revision$
 */
public class TestOfBrokenMD4 implements Testlet {

   // Constants and variables
   // -------------------------------------------------------------------------

   private MessageDigest algorithm, clone;

   // Constructor(s)
   // -------------------------------------------------------------------------

   // default 0-arguments constructor

   // Class methods
   // -------------------------------------------------------------------------

   // Instance methods
   // -------------------------------------------------------------------------

   public void test(TestHarness harness) {
      harness.checkPoint("TestOfMD4");

      try {
         Security.addProvider(new JarsyncProvider());
         algorithm = MessageDigest.getInstance("BrokenMD4", "JARSYNC");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestOfMD4.provider");
         throw new Error(x);
      }

      // Test vectors generated from rsync 2.5.5's mdfour.

      // Input a multiple of the block size is not padded.
      try {
         for (int i = 0; i < 64; i++) algorithm.update((byte) 'a');
         byte[] md = algorithm.digest();
         String exp = "755cd64425f260e356f5303ee82a2d5f";
         harness.check(exp.equals(Util.toHexString(md)), "testSixtyFourA");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestOfMD4.provider");
      } 

      // Input >= 2^32 bits has bad padding.
      try {
         harness.verbose("NOTE: This test may take a while.");
         for (int i = 0; i < 536870913; i++) algorithm.update((byte) 'a');
         byte[] md = algorithm.digest();
         String exp = "b6cea9f528a85963f7529a9e3a2153db";
         harness.check(exp.equals(Util.toHexString(md)), "test536870913A");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestOfMD4.provider");
      } 

      try {
         byte[] md = algorithm.digest("a".getBytes());
         String exp = "bde52cb31de33e46245e05fbdbd6fb24";
         harness.check(exp.equals(Util.toHexString(md)), "testA");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestOfMD4.testA");
      }

      try {
         byte[] md = algorithm.digest("abc".getBytes());
         String exp = "a448017aaf21d8525fc10ae87aa6729d";
         harness.check(exp.equals(Util.toHexString(md)), "testABC");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestOfMD4.testABC");
      }

      try {
         byte[] md = algorithm.digest("message digest".getBytes());
         String exp = "d9130a8164549fe818874806e1c7014b";
         harness.check(exp.equals(Util.toHexString(md)), "testMessageDigest");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestOfMD4.testMessageDigest");
      }

      try {
         byte[] md = algorithm.digest("abcdefghijklmnopqrstuvwxyz".getBytes());
         String exp = "d79e1c308aa5bbcdeea8ed63df412da9";
         harness.check(exp.equals(Util.toHexString(md)), "testAlphabet");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestOfMD4.testAlphabet");
      }

      try {
         byte[] md = algorithm.digest("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".getBytes());
         String exp = "043f8582f241db351ce627e153e7f0e4";
         harness.check(exp.equals(Util.toHexString(md)), "testAsciiSubset");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestOfMD4.testAsciiSubset");
      }

      try {
         byte[] md = algorithm.digest("12345678901234567890123456789012345678901234567890123456789012345678901234567890".getBytes());
         String exp = "e33b4ddc9c38f2199c3e7b164fcc0536";
         harness.check(exp.equals(Util.toHexString(md)), "testEightyNumerics");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestOfMD4.testEightyNumerics");
      }

      try {
         algorithm.update("a".getBytes(), 0, 1);
         clone = (MessageDigest) algorithm.clone();
         byte[] md = algorithm.digest();
         String exp = "bde52cb31de33e46245e05fbdbd6fb24";
         harness.check(exp.equals(Util.toHexString(md)), "testCloning #1");

         clone.update("bc".getBytes(), 0, 2);
         md = clone.digest();
         exp = "a448017aaf21d8525fc10ae87aa6729d";
         harness.check(exp.equals(Util.toHexString(md)), "testCloning #2");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestOfMD4.testCloning");
      }
   }
}
