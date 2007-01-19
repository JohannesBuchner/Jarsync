// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// SFTPConstants -- Message constants for SFTP sessions.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of HUSH, the Hopefully Uncomprehensible Shell.
//
// HUSH is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your
// option) any later version.
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

package org.metastatic.net.ssh2;

public interface SFTPConstants {

   /* Message numbers. */
   public static final byte SSH_FXP_INIT           =    1;
   public static final byte SSH_FXP_VERSION        =    2;
   public static final byte SSH_FXP_OPEN           =    3;
   public static final byte SSH_FXP_CLOSE          =    4;
   public static final byte SSH_FXP_READ           =    5;
   public static final byte SSH_FXP_WRITE          =    6;
   public static final byte SSH_FXP_LSTAT          =    7;
   public static final byte SSH_FXP_FSTAT          =    8;
   public static final byte SSH_FXP_SETSTAT        =    9;
   public static final byte SSH_FXP_FSETSTAT       =   10;
   public static final byte SSH_FXP_OPENDIR        =   11;
   public static final byte SSH_FXP_READDIR        =   12;
   public static final byte SSH_FXP_REMOVE         =   13;
   public static final byte SSH_FXP_MKDIR          =   14;
   public static final byte SSH_FXP_RMDIR          =   15;
   public static final byte SSH_FXP_REALPATH       =   16;
   public static final byte SSH_FXP_STAT           =   17;
   public static final byte SSH_FXP_RENAME         =   18;
   public static final byte SSH_FXP_READLINK       =   19;
   public static final byte SSH_FXP_SYMLINK        =   20;
   public static final byte SSH_FXP_STATUS         =  101;
   public static final byte SSH_FXP_HANDLE         =  102;
   public static final byte SSH_FXP_DATA           =  103;
   public static final byte SSH_FXP_NAME           =  104;
   public static final byte SSH_FXP_ATTRS          =  105;
   public static final byte SSH_FXP_EXTENDED       =  -56;
   public static final byte SSH_FXP_EXTENDED_REPLY =  -55;

   /* File attributes. */
   public static final int SSH_FILEXFER_ATTR_SIZE        = 1 <<  0;
   public static final int SSH_FILEXFER_ATTR_UIDGID      = 1 <<  1;
   public static final int SSH_FILEXFER_ATTR_PERMISSIONS = 1 <<  2;
   public static final int SSH_FILEXFER_ATTR_ACMODTIME   = 1 <<  3;
   public static final int SSH_FILEXFER_ATTR_EXTENDED    = 1 << 31;

   /* File operations. */
   public static final int SSH_FXF_READ   = 1 << 0;
   public static final int SSH_FXF_WRITE  = 1 << 1;
   public static final int SSH_FXF_APPEND = 1 << 2;
   public static final int SSH_FXF_CREAT  = 1 << 3;
   public static final int SSH_FXF_TRUNC  = 1 << 4;
   public static final int SSH_FXF_EXCL   = 1 << 5;

   /* Status codes. */
   public static final int SSH_FX_OK                = 0;
   public static final int SSH_FX_EOF               = 1;
   public static final int SSH_FX_NO_SUCH_FILE      = 2;
   public static final int SSH_FX_PERMISSION_DENIED = 3;
   public static final int SSH_FX_FAILURE           = 4;
   public static final int SSH_FX_BAD_MESSAGE       = 5;
   public static final int SSH_FX_NO_CONNECTION     = 6;
   public static final int SSH_FX_CONNECTION_LOST   = 7;
   public static final int SSH_FX_OP_UNSUPPORTED    = 8;
}
