// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// RsyncConstants: Useful constants in the Rsync algorithm.
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

/**
 * A number of useful constants in the Rsync algorithm.
 *
 * @version $Revision$
 */
public interface RsyncConstants {

   public static final String JARSYNC_VERSION = "0.0.2";

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
}
