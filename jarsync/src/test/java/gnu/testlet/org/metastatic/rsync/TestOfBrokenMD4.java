/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   based on TestOfMD4 from GNU Crypto

   TestOfBrokenMD4: conformance tests for the "broken" MD4.
   Copyright (C) 2001, 2002, Free Software Foundation, Inc.
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
   your version.  */

package gnu.testlet.org.metastatic.rsync;

// ----------------------------------------------------------------------------
// $Id$
//
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

import java.security.MessageDigest;
import java.security.Security;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.metastatic.rsync.JarsyncProvider;
import org.metastatic.rsync.Util;

/**
 * <p>
 * Conformance tests for the 'broken' MD4 implementation.
 * </p>
 * 
 * @version $Revision$
 */
public class TestOfBrokenMD4
{
  private static final Logger log = Logger.getLogger(TestOfBrokenMD4.class);

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

  @Test
  public void test() throws Exception
  {
    log.debug("TestOfMD4");
    byte[] md;
    String exp;

    Security.addProvider(new JarsyncProvider());
    algorithm = MessageDigest.getInstance("BrokenMD4", "JARSYNC");

    // Test vectors generated from rsync 2.5.5's mdfour.

    // Input a multiple of the block size is not padded.
    for (int i = 0; i < 64; i++)
      algorithm.update((byte) 'a');
    md = algorithm.digest();
    exp = "755cd64425f260e356f5303ee82a2d5f";
    Assert.assertEquals("testSixtyFourA", exp, Util.toHexString(md));

    // Input >= 2^32 bits has bad padding.
    log.debug("NOTE: This test may take a while.");
    for (int i = 0; i < 536870913; i++)
      algorithm.update((byte) 'a');
    md = algorithm.digest();
    exp = "b6cea9f528a85963f7529a9e3a2153db";
    Assert.assertEquals("test536870913A", exp, Util.toHexString(md));

    md = algorithm.digest("a".getBytes());
    exp = "bde52cb31de33e46245e05fbdbd6fb24";
    Assert.assertEquals("testA", exp, Util.toHexString(md));

    md = algorithm.digest("abc".getBytes());
    exp = "a448017aaf21d8525fc10ae87aa6729d";
    Assert.assertEquals("testABC", exp, Util.toHexString(md));

    md = algorithm.digest("message digest".getBytes());
    exp = "d9130a8164549fe818874806e1c7014b";
    Assert.assertEquals("testMessageDigest", exp, Util.toHexString(md));

    md = algorithm.digest("abcdefghijklmnopqrstuvwxyz".getBytes());
    exp = "d79e1c308aa5bbcdeea8ed63df412da9";
    Assert.assertEquals("testAlphabet", exp, Util.toHexString(md));
    md = algorithm
        .digest("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            .getBytes());
    exp = "043f8582f241db351ce627e153e7f0e4";
    Assert.assertEquals("testAsciiSubset", exp, Util.toHexString(md));

    md = algorithm
        .digest("12345678901234567890123456789012345678901234567890123456789012345678901234567890"
            .getBytes());
    exp = "e33b4ddc9c38f2199c3e7b164fcc0536";
    Assert.assertEquals("testEightyNumerics", exp, Util.toHexString(md));

    algorithm.update("a".getBytes(), 0, 1);
    clone = (MessageDigest) algorithm.clone();
    md = algorithm.digest();
    exp = "bde52cb31de33e46245e05fbdbd6fb24";
    Assert.assertEquals("testCloning #1", exp, Util.toHexString(md));

    clone.update("bc".getBytes(), 0, 2);
    md = clone.digest();
    exp = "a448017aaf21d8525fc10ae87aa6729d";
    Assert.assertEquals("testCloning #2", exp, Util.toHexString(md));
  }
}
