/* vim:set tw=72 expandtab softtabstop=3 shiftwidth=3 tabstop=3:
   $Id$

   Constants: constants used in rsync.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync.

   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2, or (at your option) any
   later version.

   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jarsync; see the file COPYING.  If not, write to the

      Free Software Foundation Inc.,
      59 Temple Place - Suite 330,
      Boston, MA 02111-1307
      USA  */

package org.metastatic.rsync.v2;

import org.metastatic.rsync.MD4;

/**
 * A number of useful constants in rsync.
 *
 * @version $Revision$
 */
public interface Constants {

   /** The default block size. */
   public static final int BLOCK_LENGTH = 700;

   /** The chunk size. */
   public static final int CHUNK_SIZE = 32768;

   /** The size of MD4 checksums. */
   public static final int SUM_LENGTH = MD4.DIGEST_LENGTH;

   /** The default port for rsyncd connections. */
   public static final int RSYNCD_PORT = 873;

   public static final int PROTOCOL_VERSION = 26;
   public static final int MIN_PROTOCOL_VERSION = 15;
   public static final int MAX_PROTOCOL_VERSION = 30;

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

   public static final int MAXPATHLEN = 1024;

   /* Flist flags. */
   public static final int FLAG_DELETE = (1<<0);
   public static final int SAME_MODE   = (1<<1);
   public static final int SAME_RDEV   = (1<<2);
   public static final int SAME_UID    = (1<<3);
   public static final int SAME_GID    = (1<<4);
   public static final int SAME_DIR    = (1<<5);
   public static final int SAME_NAME   = SAME_DIR;
   public static final int LONG_NAME   = (1<<6);
   public static final int SAME_TIME   = (1<<7);

   /* Mode flags. */
   public static final int _S_IFMT  = 0170000;
   public static final int _S_IFLNK = 0120000;
   public static final int _S_IFREG = 0100000;

   /* Nonblocking I/O states. */

   public static final int INPUT_MASK  = 0x0F;
   public static final int OUTPUT_MASK = 0xF0;

   public static final int STATE_SETUP_PROTOCOL  = 0xFF;
   public static final int STATE_RECEIVE_EXCLUDE = 0x01;
   public static final int STATE_SEND_FLIST      = 0x10;
   public static final int STATE_SENDER          = 0x22;
   public static final int STATE_SENDER_INPUT    = 0x02;
   public static final int STATE_SENDER_OUTPUT   = 0x20;
   public static final int STATE_RECEIVER        = 0x33;
   public static final int STATE_RECEIVER_INPUT  = 0x03;
   public static final int STATE_RECEIVER_OUTPUT = 0x30;
   public static final int STATE_DONE            = 0x44;
   public static final int STATE_INPUT_DONE      = 0x04;
   public static final int STATE_OUTPUT_DONE     = 0x40;

   // Protocol setup.
   public static final int SETUP_READ_GREETING     = 0;
   public static final int SETUP_READ_MODULE       = 1;
   public static final int SETUP_READ_AUTH         = 2;
   public static final int SETUP_READ_OPTIONS      = 3;
   public static final int SETUP_READ_DONE         = 4;
   public static final int SETUP_WRITE_GREETING    = 0;
   public static final int SETUP_WRITE_MODULES     = 1;
   public static final int SETUP_WRITE_CHALLENGE   = 2;
   public static final int SETUP_WRITE_OK          = 3;
   public static final int SETUP_WRITE_ERROR       = 4;
   public static final int SETUP_WRITE_WAIT        = 5;
   public static final int SETUP_WRITE_DONE        = 6;

   // File list states.
   public static final int FLIST_RECEIVE_FILES = 0x01;
   public static final int FLIST_RECEIVE_UIDS  = 0x02;
   public static final int FLIST_RECEIVE_GIDS  = 0x03;
   public static final int FLIST_RECEIVE_DONE  = 0x04;
   public static final int FLIST_SEND_FILES    = 0x10;
   public static final int FLIST_SEND_UIDS     = 0x20;
   public static final int FLIST_SEND_GIDS     = 0x30;
   public static final int FLIST_SEND_DONE     = 0x40;

   // Sender states.
   public static final int SENDER_WAIT          = 0x00;
   public static final int SENDER_DONE          = 0xFF;
   public static final int SENDER_RECEIVE_INDEX = 0x01;
   public static final int SENDER_RECEIVE_SUMS  = 0x02;
   public static final int SENDER_SEND_DELTAS   = 0x10;
   public static final int SENDER_SEND_FILE     = 0x20;
}
