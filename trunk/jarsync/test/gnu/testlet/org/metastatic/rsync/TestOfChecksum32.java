/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id $
  
   TestOfChecksum32: test of the rolling checksum.
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
   or based on Jarsync.  If you modify Jarsync you may extend this
   exception to your version of it, but you are not obligated to do so.
   If you do not wish to do so, delete this exception statement from
   your version.  */

// Tags: JARSYNC

package gnu.testlet.org.metastatic.rsync;

import gnu.testlet.Testlet;
import gnu.testlet.TestHarness;
import java.util.Random;
import org.metastatic.rsync.*;

/**
 * Conformance tests for the 32-bit rolling checksum.
 *
 * @version $Revision $
 */
public class TestOfChecksum32 implements Testlet {

   // Instance methods.
   // -----------------------------------------------------------------------

   public void test(TestHarness harness) {
      try {
         Checksum32 c = new Checksum32((short) 0);
         c.check("a".getBytes(), 0, 1);
         harness.check(c.getValue() == 0x610061, "TestA");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestA");
      }

      try {
         Checksum32 c = new Checksum32((short) 0);
         c.check("abc".getBytes(), 0, 3);
         harness.check(c.getValue() == 0x24a0126, "TestABC");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestABC");
      }

      try {
         Checksum32 c = new Checksum32((short) 0);
         c.check("abcdefghijklmnopqrstuvwxyz".getBytes(), 0, 26);
         harness.check(c.getValue() == 0x906c0b1f, "TestAlphabet");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestAlphabet");
      }

      try {
         Checksum32 c = new Checksum32((short) 0);
         c.check("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".getBytes(), 0, 62);
         harness.check(c.getValue() == 0xdf2c150b, "TestASCIISubset");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestASCIISubset");
      }

      try {
         Checksum32 c = new Checksum32((short) 0);
         c.check("Adler 32".getBytes(), 0, 8);
         harness.check(c.getValue() == 0xc05026d, "TestAdler32");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestAdler32");
      }

      try {
         Checksum32 c = new Checksum32((short) 0);
         c.check("01234567890123456789012345678901234567890123456789012345678901234567890123456789".getBytes(), 0, 80);
         harness.check(c.getValue() == 0x95e01068, "TestEightyNumerics");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestEightyNumerics");
      }

      try {
         Checksum32 c = new Checksum32((short) 0);
         c.check(new byte[] { 
            (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF
         }, 0, 4);
         harness.check(c.getValue() == 0xfdeaff38, "TestDEADBEEF");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestDEADBEEF");
      }

      try {
         Checksum32 c = new Checksum32((short) 0);
         c.check(new byte[] { 
            (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
         }, 0, 4);
         harness.check(c.getValue() == 0xfe54ff40, "TestCAFEBABE");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestCAFEBABE");
      }

      try {
         Checksum32 c = new Checksum32((short) 0);
         byte[] buf = new byte[256];
         for (int i = 0; i < 256; i++)
            buf[i] = (byte) i;
         c.check(buf, 0, 256);
         harness.check(c.getValue() == 0x6a80ff80, "TestAllByteValues");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestAllByteValues");
      }

      try {
         Random r = new Random();
         harness.checkPoint("testRoll");
         for (int i = 0; i < 10; i++) {
            Checksum32 c1 = new Checksum32((short) 0);
            Checksum32 c2 = new Checksum32((short) 0);
            byte[] buf1 = new byte[200 + r.nextInt(1200)];
            byte[] buf2 = new byte[buf1.length];
            r.nextBytes(buf1);
            r.nextBytes(buf2);
            c1.check(buf1, 0, buf1.length);
            c2.check(buf1, 0, buf1.length);
            c1.check(buf2, 0, buf2.length);
            for (int j = 0; j < buf2.length; j++) {
               c2.roll(buf2[j]);
            }
            harness.check(c1.getValue() == c2.getValue());
         }
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestRoll");
      }

      // CHAR_OFFSET = 31 ---------------------------------------------------

      try {
         Checksum32 c = new Checksum32((short) 31);
         c.check("a".getBytes(), 0, 1);
         harness.check(c.getValue() == 0x800080, "TestA");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestA");
      }

      try {
         Checksum32 c = new Checksum32((short) 31);
         c.check("abc".getBytes(), 0, 3);
         harness.check(c.getValue() == 0x3040183, "TestABC");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestABC");
      }

      try {
         Checksum32 c = new Checksum32((short) 31);
         c.check("abcdefghijklmnopqrstuvwxyz".getBytes(), 0, 26);
         harness.check(c.getValue() == 0xbaed0e45, "TestAlphabet");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestAlphabet");
      }

      try {
         Checksum32 c = new Checksum32((short) 31);
         c.check("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".getBytes(), 0, 62);
         harness.check(c.getValue() == 0xcbab1c8d, "TestASCIISubset");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestASCIISubset");
      }

      try {
         Checksum32 c = new Checksum32((short) 31);
         c.check("Adler 32".getBytes(), 0, 8);
         harness.check(c.getValue() == 0x10610365, "TestAdler32");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestAdler32");
      }

      try {
         Checksum32 c = new Checksum32((short) 31);
         c.check("01234567890123456789012345678901234567890123456789012345678901234567890123456789".getBytes(), 0, 80);
         harness.check(c.getValue() == 0x1e381a18, "TestEightyNumerics");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestEightyNumerics");
      }

      try {
         Checksum32 c = new Checksum32((short) 31);
         c.check(new byte[] { 
            (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF
         }, 0, 4);
         harness.check(c.getValue() == 0xff20ffb4, "TestDEADBEEF");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestDEADBEEF");
      }

      try {
         Checksum32 c = new Checksum32((short) 31);
         c.check(new byte[] { 
            (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
         }, 0, 4);
         harness.check(c.getValue() == 0xff8affbc, "TestCAFEBABE");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestCAFEBABE");
      }

      try {
         Checksum32 c = new Checksum32((short) 31);
         byte[] buf = new byte[256];
         for (int i = 0; i < 256; i++)
            buf[i] = (byte) i;
         c.check(buf, 0, 256);
         harness.check(c.getValue() == 0xfa001e80, "TestAllByteValues");
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestAllByteValues");
      }

      try {
         Random r = new Random();
         harness.checkPoint("testRoll");
         for (int i = 0; i < 10; i++) {
            Checksum32 c1 = new Checksum32((short) 31);
            Checksum32 c2 = new Checksum32((short) 31);
            byte[] buf1 = new byte[201 + r.nextInt(1200)];
            r.nextBytes(buf1);
            c1.check(buf1, 0, buf1.length-1);
            c2.check(buf1, 0, buf1.length-1);
            c1.check(buf1, 1, buf1.length-1);
            c2.roll(buf1[buf1.length-1]);
            harness.check(c1.getValue() == c2.getValue());
         }
      } catch (Exception x) {
         harness.debug(x);
         harness.fail("TestRoll");
      }
   }
}
