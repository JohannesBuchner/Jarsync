// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// SSH2Constants -- Message constants in the SSH version 2.0 protocol.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of HUSH, the Hopefully Uncomprehensible Shell.
//
// HUSH is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your
// option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the
//
//    Free Software Foundation, Inc.,
//    59 Temple Place, Suite 330,
//    Boston, MA  02111-1307
//    USA
//
// ---------------------------------------------------------------------------

package org.metastatic.net.ssh2;

import java.math.BigInteger;
import java.util.Map;

/**
 * This interface defines a number of constants that are used in
 * communicating with the server. It defines message types (the single
 * byte that appears at the beginning of a packet's payload), disconnect
 * and failure message numbers, and key-exchange constants.
 *
 * @version $Revision$
 */
public interface SSH2Constants {

   /* SSH Message numbers. */
   /* Generic transport layer. (1-19) */
   public static final byte SSH_MSG_DISCONNECT                =   1;
   public static final byte SSH_MSG_IGNORE                    =   2;
   public static final byte SSH_MSG_UNIMPLEMENTED             =   3;
   public static final byte SSH_MSG_DEBUG                     =   4;
   public static final byte SSH_MSG_SERVICE_REQUEST           =   5;
   public static final byte SSH_MSG_SERVICE_ACCEPT            =   6;

   /* Algorithm negotiation. (20-29) */
   public static final byte SSH_MSG_KEXINIT                   =  20;
   public static final byte SSH_MSG_NEWKEYS                   =  21;

   /* Key exchange method specific. (30-49) */
   public static final byte SSH_MSG_KEXDH_INIT                =  30;
   public static final byte SSH_MSG_KEXDH_REPLY               =  31;

   /* User authentication generic. (50-59) */
   public static final byte SSH_MSG_USERAUTH_REQUEST          =  50;
   public static final byte SSH_MSG_USERAUTH_FAILURE          =  51;
   public static final byte SSH_MSG_USERAUTH_SUCCESS          =  52;
   public static final byte SSH_MSG_USERAUTH_BANNER           =  53;

   /* User authentication method specific. (60-79) */
   public static final byte SSH_MSG_USERAUTH_PK_OK            =  60;

   public static final byte SSH_MSG_USERAUTH_PASSWD_CHANGEREQ =  60;

   /* Connection protocol generic. (80-89) */
   public static final byte SSH_MSG_GLOBAL_REQUEST            =  80;
   public static final byte SSH_MSG_REQUEST_SUCCESS           =  81;
   public static final byte SSH_MSG_REQUEST_FAILURE           =  82;

   /* Channel related messages. (90-127) */
   public static final byte SSH_MSG_CHANNEL_OPEN              =  90;
   public static final byte SSH_MSG_CHANNEL_OPEN_CONFIRMATION =  91;
   public static final byte SSH_MSG_CHANNEL_OPEN_FAILURE      =  92;
   public static final byte SSH_MSG_CHANNEL_WINDOW_ADJUST     =  93;
   public static final byte SSH_MSG_CHANNEL_DATA              =  94;
   public static final byte SSH_MSG_CHANNEL_EXTENDED_DATA     =  95;
   public static final byte SSH_MSG_CHANNEL_EOF               =  96;
   public static final byte SSH_MSG_CHANNEL_CLOSE             =  97;
   public static final byte SSH_MSG_CHANNEL_REQUEST           =  98;
   public static final byte SSH_MSG_CHANNEL_SUCCESS           =  99;
   public static final byte SSH_MSG_CHANNEL_FAILURE           = 100;

   /* Reserved. (128-191) */

   /* Local extensions. (192-255) */

   /* SSH_MSG_DISCONNECT reason codes */
   public static final int SSH_DISCONNECT_HOST_NOT_ALLOWED_TO_CONNECT    =  1;
   public static final int SSH_DISCONNECT_PROTOCOL_ERROR                 =  2;
   public static final int SSH_DISCONNECT_KEY_EXCHANGE_FAILED            =  3;
   public static final int SSH_DISCONNECT_RESERVED                       =  4;
   public static final int SSH_DISCONNECT_MAC_ERROR                      =  5;
   public static final int SSH_DISCONNECT_COMPRESSION_ERROR              =  6;
   public static final int SSH_DISCONNECT_SERVICE_NOT_AVAILABLE          =  7;
   public static final int SSH_DISCONNECT_PROTOCOL_VERSION_NOT_SUPPORTED =  8;
   public static final int SSH_DISCONNECT_HOST_KEY_NOT_VERIFIABLE        =  9;
   public static final int SSH_DISCONNECT_CONNECTION_LOST                = 10;
   public static final int SSH_DISCONNECT_BY_APPLICATION                 = 11;
   public static final int SSH_DISCONNECT_TOO_MANY_CONNECTIONS           = 12;
   public static final int SSH_DISCONNECT_AUTH_CANCELLED_BY_USER         = 13;
   public static final int SSH_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE = 14;
   public static final int SSH_DISCONNECT_ILLEGAL_USER_NAME              = 15;

   /* Channel open failure reason codes. */
   public static final int SSH_OPEN_ADMINISTRATIVELY_PROHIBITED = 1;
   public static final int SSH_OPEN_CONNECT_FAILED              = 2;
   public static final int SSH_OPEN_UNKNOWN_CHANNEL_TYPE        = 3;
   public static final int SSH_OPEN_RESOURCE_SHORTAGE           = 4;

   /* Extended data types. */
   public static final int SSH_EXTENDED_DATA_STDERR = 1;

   /* Pty request terminal encodings */
   public static final byte TTY_OP_END =   0;

   /* Control characters */
   public static final byte VINTR      =   1;
   public static final byte VQUIT      =   2;
   public static final byte VERASE     =   3;
   public static final byte VKILL      =   4;
   public static final byte VEOF       =   5;
   public static final byte VEOL       =   6;
   public static final byte VEOL2      =   7;
   public static final byte VSTART     =   8;
   public static final byte VSTOP      =   9;
   public static final byte VSUSP      =  10;
   public static final byte VDSUSP     =  11;
   public static final byte VREPRINT   =  12;
   public static final byte VWERASE    =  13;
   public static final byte VLNEXT     =  14;
   public static final byte VFLUSH     =  15;
   public static final byte VSWTCH     =  16;
   public static final byte VSTATUS    =  17;
   public static final byte VDISCARD   =  18;

   /* Input */
   public static final byte IGNPAR     =  30;
   public static final byte PARMRK     =  31;
   public static final byte INPCK      =  32;
   public static final byte ISTRIP     =  33;
   public static final byte INLCR      =  34;
   public static final byte IGNCR      =  35;
   public static final byte ICRNL      =  36;
   public static final byte IUCLC      =  37;
   public static final byte IXON       =  38;
   public static final byte IXANY      =  39;
   public static final byte IXOFF      =  40;
   public static final byte IMAXBEL    =  41;

   /* Local */
   public static final byte ISIG       =  50;
   public static final byte ICANON     =  51;
   public static final byte XCASE      =  52;
   public static final byte ECHO       =  53;
   public static final byte ECHOE      =  54;
   public static final byte ECHOK      =  55;
   public static final byte ECHONL     =  56;
   public static final byte NOFLSH     =  57;
   public static final byte TOSTOP     =  58;
   public static final byte IEXTEN     =  59;
   public static final byte ECHOCTL    =  60;
   public static final byte ECHOKE     =  61;
   public static final byte PENDIN     =  62;
   
   /* Output */
   public static final byte OPOST      =  70;
   public static final byte OLCUC      =  71;
   public static final byte ONLCR      =  72;
   public static final byte OCRNL      =  73;
   public static final byte ONOCR      =  74;
   public static final byte ONLRET     =  75;

   /* Control */
   public static final byte CS7        =  90;
   public static final byte CS8        =  91;
   public static final byte PARENB     =  92;
   public static final byte PARODD     =  93;

   /* Baud rates */
   public static final byte TTY_OP_ISPEED = (byte) 0x80;
   public static final byte TTY_OP_OSPEED = (byte) 0x81;

   /* Signal names. */
   public static final String SIG_ABRT = "ABRT";
   public static final String SIG_ALRM = "ALRM";
   public static final String SIG_FPE  =  "FPE";
   public static final String SIG_HUP  =  "HUP";
   public static final String SIG_ILL  =  "ILL";
   public static final String SIG_INT  =  "INT";
   public static final String SIG_KILL = "KILL";
   public static final String SIG_PIPE = "PIPE";
   public static final String SIG_QUIT = "QUIT";
   public static final String SIG_SEGV = "SEGV";
   public static final String SIG_TERM = "TERM";
   public static final String SIG_USR1 = "USR1";
   public static final String SIG_USR2 = "USR2";

   /** Prime p for Diffie-Hellman Group1 */
   public static final BigInteger DH_P = new BigInteger(
      "00ffffffffffffffffc90fdaa22168c234c4c6628b80dc1c" +
      "d129024e088a67cc74020bbea63b139b22514a08798e3404" +
      "ddef9519b3cd3a431b302b0a6df25f14374fe1356d6d51c2" +
      "45e485b576625e7ec6f44c42e9a637ed6b0bff5cb6f406b7" +
      "edee386bfb5a899fa5ae9f24117c4b1fe649286651ece653" +
      "81ffffffffffffffff", 16);

   /** Generator g for Diffie-Hellman Group1 */
   public static final BigInteger DH_G = BigInteger.valueOf(2);
}
