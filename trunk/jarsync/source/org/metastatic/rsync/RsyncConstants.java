// vim:set tw=72 expandtab softtabstop=3 shiftwidth=3 tabstop=3:
// $Id$
//
// RsyncConstants: Useful constants in the Rsync algorithm.
// Copyright (C) 2001,2002  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of Jarsync.
//
// Jarsync is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2, or (at your option) any
// later version.
//
// Jarsync is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Jarsync; see the file COPYING.  If not, write to the
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
//
// --------------------------------------------------------------------

package org.metastatic.rsync;

/**
 * A number of useful constants in the Rsync algorithm.
 *
 * @version $Revision$
 */
public interface RsyncConstants {

   public static final String JARSYNC_VERSION = "0.0.4";

   /** The strong checksum length. */
   public static final int SUM_LENGTH = MD4.DIGEST_LENGTH;

   /** The default block size. */
   public static final int BLOCK_LENGTH = 700;

   /** Rdiff/rproxy default block length. */
   public static final int RDIFF_BLOCK_LENGTH = 2048;

   /** Rdiff/rproxy default sum length. */
   public static final int RDIFF_STRONG_LENGTH = 8;

   /** The default port for rsyncd connections. */
   public static final int RSYNCD_PORT = 873;

   /** The greeting. */
   public static final String RSYNCD_GREETING = "@RSYNCD: ";

   /** Authentication required. */
   public static final String RSYNCD_AUTHREQD = "@RSYNCD: AUTHREQD ";

   /** OK. */
   public static final String RSYNCD_OK = "@RSYNCD: OK";

   /** Error. */
   public static final String AT_ERROR = "@ERROR";

   /** Exit message. */
   public static final String RSYNCD_EXIT = "@RSYNCD: EXIT";

   /** Rdiff/rproxy signature magic. */
   public static final int SIG_MAGIC = 0x72730136;

   /** Rdiff/rproxy delta magic. */
   public static final int DELTA_MAGIC = 0x72730236;

   public static final byte OP_END = 0x00;

   public static final byte OP_LITERAL_N1 = 0x41;
   public static final byte OP_LITERAL_N2 = 0x42;
   public static final byte OP_LITERAL_N4 = 0x43;
   public static final byte OP_LITERAL_N8 = 0x44;

   public static final byte OP_COPY_N4_N4 = 0x4f;

   public static final int FNONE  = 0;
   public static final int FERROR = 1;
   public static final int FINFO  = 2;
   public static final int FLOG   = 3;
   public static final int MPLEX_BASE = 7;
   public static final int OUTPUT_BUFFER_SIZE = 4092;
}
