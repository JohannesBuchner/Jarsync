/* Options -- run-time option list.
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with Jarsync; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */

/*
 * Based on rsync-2.5.5.
 *
 * Rsync Copyright (C) 1992-2001 Andrew Tridgell
 *                     1996 Paul Mackerras
 *                     2001, 2002 Martin Pool
 *                     and others.
 */

package org.metastatic.rsync.v2;

import java.io.PrintStream;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

public class Options {

  // Constants and Fields.
  // -----------------------------------------------------------------------

  // Do you know how useful enums would be in Java? Very.

  public static final int OPT_VERSION = 1000;
  public static final int OPT_SUFFIX = 1001;
  public static final int OPT_SENDER = 1002;
  public static final int OPT_SERVER = 1003;
  public static final int OPT_EXCLUDE = 1004;
  public static final int OPT_EXCLUDE_FROM = 1005;
  public static final int OPT_DELETE = 1006;
  public static final int OPT_DELETE_EXCLUDED = 1007;
  public static final int OPT_NUMERIC_IDS = 1008;
  public static final int OPT_RSYNC_PATH = 1009;
  public static final int OPT_FORCE = 1010;
  public static final int OPT_TIMEOUT = 1011;
  public static final int OPT_DAEMON = 1012;
  public static final int OPT_CONFIG = 1013;
  public static final int OPT_PORT = 1014;
  public static final int OPT_INCLUDE = 1015;
  public static final int OPT_INCLUDE_FROM = 1016;
  public static final int OPT_STATS = 1017;
  public static final int OPT_PARTIAL = 1018;
  public static final int OPT_PROGRESS = 1019;
  public static final int OPT_COPY_UNSAFE_LINKS = 1020;
  public static final int OPT_SAFE_LINKS = 1021;
  public static final int OPT_COMPARE_DEST = 1022;
  public static final int OPT_LOG_FORMAT = 1023;
  public static final int OPT_PASSWORD_FILE = 1024;
  public static final int OPT_SIZE_ONLY = 1025;
  public static final int OPT_ADDRESS = 1026;
  public static final int OPT_DELETE_AFTER = 1027;
  public static final int OPT_EXISTING = 1028;
  public static final int OPT_MAX_DELETE = 1029;
  public static final int OPT_BACKUP_DIR = 1030;
  public static final int OPT_IGNORE_ERRORS = 1031;
  public static final int OPT_BWLIMIT = 1032;
  public static final int OPT_BLOCKING_IO = 1033;
  public static final int OPT_NO_BLOCKING_IO = 1034;
  public static final int OPT_WHOLE_FILE = 1035;
  public static final int OPT_NO_WHOLE_FILE = 1036;
  public static final int OPT_MODIFY_WINDOW = 1037;
  public static final int OPT_READ_BATCH = 1038;
  public static final int OPT_WRITE_BATCH = 1039;
  public static final int OPT_IGNORE_EXISTING = 1040;
  public static final int OPT_USE_SSL = 1041;
  public static final int OPT_KEYSTORE = 1042;

  public static final String OPTSTRING = "46abceghlnopqrtuvxzB:CDHILRST:W";
  public static final LongOpt[] LONGOPTS = {
    new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v'),
    new LongOpt("quiet", LongOpt.NO_ARGUMENT, null, 'q'),
    new LongOpt("checksum", LongOpt.NO_ARGUMENT, null, 'c'),
    new LongOpt("archive", LongOpt.NO_ARGUMENT, null, 'a'),
    new LongOpt("recursive", LongOpt.NO_ARGUMENT, null, 'r'),
    new LongOpt("relative", LongOpt.NO_ARGUMENT, null, 'R'),
    new LongOpt("backup", LongOpt.NO_ARGUMENT, null, 'b'),
    new LongOpt("backup-dir", LongOpt.REQUIRED_ARGUMENT, null, OPT_BACKUP_DIR),
    new LongOpt("suffix", LongOpt.REQUIRED_ARGUMENT, null, OPT_SUFFIX),
    new LongOpt("update", LongOpt.NO_ARGUMENT, null, 'u'),
    new LongOpt("links", LongOpt.NO_ARGUMENT, null, 'l'),
    new LongOpt("copy_links", LongOpt.NO_ARGUMENT, null, 'L'),
    new LongOpt("copy-unsafe-links", LongOpt.NO_ARGUMENT, null, OPT_COPY_UNSAFE_LINKS),
    new LongOpt("safe-links", LongOpt.NO_ARGUMENT, null, OPT_SAFE_LINKS),
    new LongOpt("hard-links", LongOpt.NO_ARGUMENT, null, 'H'),
    new LongOpt("perms", LongOpt.NO_ARGUMENT, null, 'p'),
    new LongOpt("owner", LongOpt.NO_ARGUMENT, null, 'o'),
    new LongOpt("group", LongOpt.NO_ARGUMENT, null, 'g'),
    new LongOpt("devices", LongOpt.NO_ARGUMENT, null, 'D'),
    new LongOpt("times", LongOpt.NO_ARGUMENT, null, 't'),
    new LongOpt("sparse", LongOpt.NO_ARGUMENT, null, 'S'),
    new LongOpt("dry-run", LongOpt.NO_ARGUMENT, null, 'n'),
    new LongOpt("whole-file", LongOpt.NO_ARGUMENT, null, 'W'),
    new LongOpt("no-whole-file", LongOpt.NO_ARGUMENT, null, OPT_NO_WHOLE_FILE),
    new LongOpt("one-file-system", LongOpt.NO_ARGUMENT, null, 'x'),
    new LongOpt("block-size", LongOpt.REQUIRED_ARGUMENT, null, 'B'),
    new LongOpt("rsh", LongOpt.REQUIRED_ARGUMENT, null, 'e'),
    new LongOpt("rsync-path", LongOpt.REQUIRED_ARGUMENT, null, OPT_RSYNC_PATH),
    new LongOpt("cvs-exclude", LongOpt.NO_ARGUMENT, null, 'C'),
    new LongOpt("existing", LongOpt.NO_ARGUMENT, null, OPT_EXISTING),
    new LongOpt("ignore-existing", LongOpt.NO_ARGUMENT, null, OPT_IGNORE_EXISTING),
    new LongOpt("delete", LongOpt.NO_ARGUMENT, null, OPT_DELETE),
    new LongOpt("delete-excluded", LongOpt.NO_ARGUMENT, null, OPT_DELETE_EXCLUDED),
    new LongOpt("delete-after", LongOpt.NO_ARGUMENT, null, OPT_DELETE_AFTER),
    new LongOpt("ignore-errors", LongOpt.NO_ARGUMENT, null, OPT_IGNORE_ERRORS),
    new LongOpt("max-delete", LongOpt.REQUIRED_ARGUMENT, null, OPT_MAX_DELETE),
    new LongOpt("partial", LongOpt.NO_ARGUMENT, null, OPT_PARTIAL),
    new LongOpt("force", LongOpt.NO_ARGUMENT, null, OPT_FORCE),
    new LongOpt("numeric-ids", LongOpt.NO_ARGUMENT, null, OPT_NUMERIC_IDS),
    new LongOpt("timeout", LongOpt.REQUIRED_ARGUMENT, null, OPT_TIMEOUT),
    new LongOpt("ignore-times", LongOpt.NO_ARGUMENT, null, 'I'),
    new LongOpt("size-only", LongOpt.NO_ARGUMENT, null, OPT_SIZE_ONLY),
    new LongOpt("modify-window", LongOpt.REQUIRED_ARGUMENT, null, OPT_MODIFY_WINDOW),
    new LongOpt("temp-dir", LongOpt.REQUIRED_ARGUMENT, null, 'T'),
    new LongOpt("compare-dest", LongOpt.REQUIRED_ARGUMENT, null, OPT_COMPARE_DEST),
    new LongOpt("compress", LongOpt.NO_ARGUMENT, null, 'z'),
    new LongOpt("exclude", LongOpt.REQUIRED_ARGUMENT, null, OPT_EXCLUDE),
    new LongOpt("exclude-from", LongOpt.REQUIRED_ARGUMENT, null, OPT_EXCLUDE_FROM),
    new LongOpt("include", LongOpt.REQUIRED_ARGUMENT, null, OPT_INCLUDE),
    new LongOpt("include-from", LongOpt.REQUIRED_ARGUMENT, null, OPT_INCLUDE_FROM),
    new LongOpt("version", LongOpt.NO_ARGUMENT, null, OPT_VERSION),
    new LongOpt("stats", LongOpt.NO_ARGUMENT, null, OPT_STATS),
    new LongOpt("progress", LongOpt.NO_ARGUMENT, null, OPT_PROGRESS),
    new LongOpt("password-file", LongOpt.REQUIRED_ARGUMENT, null, OPT_PASSWORD_FILE),
    new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
    new LongOpt("server", LongOpt.NO_ARGUMENT, null, OPT_SERVER),
    new LongOpt("sender", LongOpt.NO_ARGUMENT, null, OPT_SENDER),
    new LongOpt("ssl", LongOpt.NO_ARGUMENT, null, OPT_USE_SSL),
    new LongOpt("keystore", LongOpt.REQUIRED_ARGUMENT, null, OPT_KEYSTORE)
  };

  public boolean whole_file = false;
  public boolean no_whole_file = false;
  public boolean list_only = false;
  public boolean always_checksum = false;
  public boolean copy_links = false;
  public boolean preserve_links = false;
  public boolean preserve_hard_links = false;
  public boolean preserve_perms = false;
  public boolean preserve_devices = false;
  public boolean preserve_uid = false;
  public boolean preserve_gid = false;
  public boolean preserve_times = false;
  public boolean update_only = false;
  public boolean cvs_exclude = false;
  public boolean dry_run = false;
  public boolean local_server = false;
  public boolean ignore_times = false;
  public boolean delete_mode = false;
  public boolean delete_excluded = false;
  public boolean one_file_system = false;
  public boolean sparse_files = false;
  public boolean do_compression = false;
  public boolean am_root = false;
  public boolean relative_paths = false;
  public boolean numeric_ids = false;
  public boolean force_delete = false;
  public boolean read_only = false;
  public boolean am_server = false;
  public boolean am_sender = false;
  public boolean recurse = false;
  public boolean do_stats = false;
  public boolean do_progress = false;
  public boolean keep_partial = false;
  public boolean safe_symlinks = false;
  public boolean copy_unsafe_links = false;
  public boolean size_only = false;
  public boolean delete_after = false;
  public boolean only_existing = false;
  public boolean opt_ignore_existing = false;
  public boolean ignore_errors = false;
  public boolean blocking_io = true;
  public boolean make_backups = false;
  public short orig_umask;
  public int verbose = 0;
  public int quiet = 0;
  public int io_timeout = 0;
  public int max_delete = 0;
  public int bwlimit = 0;
  public int block_size = Constants.BLOCK_LENGTH;
  public int modify_window = 2;
  public String backup_suffix = "~";
  public String tmpdir = null;
  public String compare_dest = null;
  public String shell_cmd = "/usr/bin/ssh";
  public String log_format = null;
  public String password_file = null;
  public String rsync_path = "/usr/bin/rsync";
  public String backup_dir = null;
  public String exclude = "";
  public String exclude_from = "";
  public String include = "";
  public String include_from = "";

  public boolean use_ssl = false;
  public String keystore = System.getProperty("user.home") +
    System.getProperty("file.separator") + ".keystore";

  // Constructor.
  // -----------------------------------------------------------------------

  public Options() { }

  // Class methods.
  // -----------------------------------------------------------------------

  public int parseArguments(String progname, String[] argv, PrintStream err)
    throws IllegalArgumentException
  {
    Getopt g = new Getopt(progname, argv, OPTSTRING, LONGOPTS);
    int c;
    g.setOpterr(false);

    while ((c = g.getopt()) != -1)
      {
        switch (c)
          {
          case OPT_VERSION:
            if (err != null)
              version(progname, err);
            return -1;

          case OPT_DELETE_EXCLUDED:
            delete_excluded = true;
            delete_mode = true;
            break;

          case OPT_EXCLUDE:
            exclude += " " + g.getOptarg();
            break;

          case OPT_INCLUDE:
            include += " " + g.getOptarg();
            break;

          case OPT_EXCLUDE_FROM:
            exclude_from += " " + g.getOptarg();
            break;

          case OPT_INCLUDE_FROM:
            include_from += " " + g.getOptarg();
            break;

          case 'W':
          case OPT_WHOLE_FILE:
            whole_file = true;
            no_whole_file = false;
            break;

          case OPT_NO_WHOLE_FILE:
            whole_file = false;
            no_whole_file = true;
            break;

          case OPT_NO_BLOCKING_IO:
            blocking_io = false;
            break;

          case 'h':
            if (err != null)
              help(progname, err);
            return -1;

          case 'H':
            throw new IllegalArgumentException("hard links not supported");

          case 'v':
            verbose++;
            break;

          case 'q':
            quiet++;
            break;

          case 'a':
            recurse = true;
            preserve_perms = true;
            preserve_times = true;
            preserve_uid = true;
            preserve_gid = true;
            preserve_devices = true;
            break;

          case OPT_SENDER:
            if (!am_server)
              {
                throw new IllegalArgumentException("not server");
              }
            am_sender = true;
            break;

          case 'P':
            do_progress = true;
            keep_partial = true;
            break;

          case OPT_SUFFIX:
            backup_suffix = g.getOptarg();
            break;

          case OPT_RSYNC_PATH:
            rsync_path = g.getOptarg();
            break;

          case OPT_PASSWORD_FILE:
            password_file = g.getOptarg();
            break;

          case 'I':
            ignore_times = true;
            break;

          case OPT_SIZE_ONLY:
            size_only = true;
            break;

          case OPT_MODIFY_WINDOW:
            modify_window = Integer.parseInt(g.getOptarg());
            break;

          case 'x':
            one_file_system = true;
            break;

          case OPT_DELETE:
            delete_mode = true;
            break;

          case OPT_EXISTING:
            only_existing = true;
            break;

          case OPT_IGNORE_EXISTING:
            opt_ignore_existing = true;
            break;

          case OPT_DELETE_AFTER:
            delete_after = true;
            break;

          case OPT_FORCE:
            force_delete = true;
            break;

          case OPT_NUMERIC_IDS:
            numeric_ids = true;
            break;

          case OPT_SAFE_LINKS:
            safe_symlinks = true;
            break;

          case 'b':
            make_backups = true;
            break;

          case 'n':
            dry_run = true;
            break;

          case 'S':
            sparse_files = true;
            break;

          case 'C':
            cvs_exclude = true;
            break;

          case 'u':
            update_only = true;
            break;

          case 'l':
            preserve_links = true;
            break;

          case 'L':
            copy_links = true;
            break;

          case OPT_COPY_UNSAFE_LINKS:
            copy_unsafe_links = true;
            break;

          case 'p':
            preserve_perms = true;
            break;

          case 'o':
            preserve_uid = true;
            break;

          case 'g':
            preserve_gid = true;
            break;

          case 'D':
            preserve_devices = true;
            break;

          case 't':
            preserve_times = true;
            break;

          case 'c':
            always_checksum = true;
            break;

          case OPT_SERVER:
            am_server = true;
            break;

          case 'r':
            recurse = true;
            break;

          case 'R':
            relative_paths = true;
            break;

          case 'e':
            shell_cmd = g.getOptarg();
            break;

          case 'B':
            block_size = Integer.parseInt(g.getOptarg());
            break;

          case OPT_MAX_DELETE:
            max_delete = Integer.parseInt(g.getOptarg());
            break;

          case OPT_TIMEOUT:
            io_timeout = Integer.parseInt(g.getOptarg());
            break;

          case 'T':
            tmpdir = g.getOptarg();
            break;

          case OPT_COMPARE_DEST:
            compare_dest = g.getOptarg();
            break;

          case 'z':
            do_compression = true;
            break;

          case OPT_STATS:
            do_stats = true;
            break;

          case OPT_PROGRESS:
            do_progress = true;
            break;

          case OPT_PARTIAL:
            keep_partial = true;
            break;

          case OPT_IGNORE_ERRORS:
            ignore_errors = true;
            break;

          case OPT_BLOCKING_IO:
            blocking_io = true;
            break;

          case OPT_BWLIMIT:
            bwlimit = Integer.parseInt(g.getOptarg());
            break;

          case OPT_BACKUP_DIR:
            backup_dir = g.getOptarg();
            break;

          case OPT_USE_SSL:
            try
              {
                Class.forName("javax.net.ssl.SSLSocket");
              }
            catch (ClassNotFoundException cnfe)
              {
                throw new IllegalArgumentException("SSL not available");
              }
            use_ssl = true;
            break;

          case '?':
            throw new IllegalArgumentException("unknown option `" +
                                               argv[g.getOptind()-1] + "'");
          }
      }

    return g.getOptind();
  }

  public static void help(String progname, PrintStream out) {
    out.println(progname + "  (Jarsync " + org.metastatic.rsync.version.VERSION + ")  protocol version " + Constants.PROTOCOL_VERSION);
    out.println("Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>");
    out.println("<http://jarsync.sourceforge.net/>");
    out.println();
    out.println(progname + " comes with ABSOLUTELY NO WARRANTY.  This is free software, and you");
    out.println("are welcome to redistribute it under certain conditions.  See the GNU");
    out.println("General Public Licence for details.");
    out.println();
    out.println(progname + " is a file transfer program capable of efficient remote update");
    out.println("via a fast differencing algorithm.");
    out.println();
    out.println("Usage: "+progname+" [OPTION]... SRC [SRC]... [USER@]HOST:DEST");
    out.println("  or   "+progname+" [OPTION]... [USER@]HOST:SRC DEST");
    out.println("  or   "+progname+" [OPTION]... SRC [SRC]... DEST");
    out.println("  or   "+progname+" [OPTION]... [USER@]HOST::SRC [DEST]");
    out.println("  or   "+progname+" [OPTION]... SRC [SRC]... [USER@]HOST::DEST");
    out.println("  or   "+progname+" [OPTION]... rsync://[USER@]HOST[:PORT]/SRC [DEST]");
    out.println("SRC on single-colon remote HOST will be expanded by remote shell");
    out.println("SRC on server remote HOST may contain shell wildcards or multiple");
    out.println("  sources separated by space as long as they have same top-level");
    out.println();
    out.println("Options");
    out.println(" -v, --verbose               increase verbosity");
    out.println(" -q, --quiet                 decrease verbosity");
    out.println(" -c, --checksum              always checksum");
    out.println(" -a, --archive               archive mode");
    out.println(" -r, --recursive             recurse into directories");
    out.println(" -R, --relative              use relative path names");
    out.println(" -b, --backup                make backups (default ~ suffix)");
    out.println("     --backup-dir            make backups into this directory");
    out.println("     --suffix=SUFFIX         override backup suffix");
    out.println(" -u, --update                update only (don't overwrite newer files)");
    out.println(" -l, --links                 copy symlinks as symlinks");
    out.println(" -L, --copy-links            copy the referent of symlinks");
    out.println("     --copy-unsafe-links     copy links outside the source tree");
    out.println("     --safe-links            ignore links outside the destination tree");
    out.println(" -H, --hard-links            preserve hard links");
    out.println(" -p, --perms                 preserve permissions");
    out.println(" -o, --owner                 preserve owner (root only)");
    out.println(" -g, --group                 preserve group");
    out.println(" -D, --devices               preserve devices (root only)");
    out.println(" -t, --times                 preserve times");
    out.println(" -S, --sparse                handle sparse files efficiently");
    out.println(" -n, --dry-run               show what would have been transferred");
    out.println(" -W, --whole-file            copy whole files, no incremental checks");
    out.println("     --no-whole-file         turn off --whole-file");
    out.println(" -x, --one-file-system       don't cross filesystem boundaries");
    out.println(" -B, --block-size=SIZE       checksum blocking size (default 700)");
    out.println(" -e, --rsh=COMMAND           specify rsh replacement");
    out.println("     --rsync-path=PATH       specify path to rsync on the remote machine");
    out.println(" -C, --cvs-exclude           auto ignore files in the same way CVS does");
    out.println("     --existing              only update files that already exist");
    out.println("     --ignore-existing       ignore files that already exist on the receiving side");
    out.println("     --delete                delete files that don't exist on the sending side");
    out.println("     --delete-excluded       also delete excluded files on the receiving side");
    out.println("     --delete-after          delete after transferring, not before");
    out.println("     --ignore-errors         delete even if there are IO errors");
    out.println("     --max-delete=NUM        don't delete more than NUM files");
    out.println("     --partial               keep partially transferred files");
    out.println("     --force                 force deletion of directories even if not empty");
    out.println("     --numeric-ids           don't map uid/gid values by user/group name");
    out.println("     --timeout=TIME          set IO timeout in seconds");
    out.println(" -I, --ignore-times          don't exclude files that match length and time");
    out.println("     --size-only             only use file size when determining if a file should be transferred");
    out.println("     --modify-window=NUM     Timestamp window (seconds) for file match (default=0)");
    out.println(" -T  --temp-dir=DIR          create temporary files in directory DIR");
    out.println("     --compare-dest=DIR      also compare destination files relative to DIR");
    out.println(" -P                          equivalent to --partial --progress");
    out.println(" -z, --compress              compress file data");
    out.println("     --exclude=PATTERN       exclude files matching PATTERN");
    out.println("     --exclude-from=FILE     exclude patterns listed in FILE");
    out.println("     --include=PATTERN       don't exclude files matching PATTERN");
    out.println("     --include-from=FILE     don't exclude patterns listed in FILE");
    out.println("     --version               print version number");
    out.println("     --blocking-io           use blocking IO for the remote shell");
    out.println("     --no-blocking-io        turn off --blocking-io");
    out.println("     --stats                 give some file transfer stats");
    out.println("     --progress              show progress during transfer");
    out.println("     --password-file=FILE    get password from FILE");
    out.println("     --bwlimit=KBPS          limit I/O bandwidth, KBytes per second");
    out.println("     --ssl                   make socket connections over SSL (if available)");
    out.println("     -Joption                options to pass directly to java interpreter");
    out.println(" -h, --help                  show this help screen");
    out.println();
  }

  public static void version(String progname, PrintStream out) {
    out.print(progname + " (Jarsync " + org.metastatic.rsync.version.VERSION + ")  ");
    out.print("Java version " + System.getProperty("java.version") + " (");
    try
      {
        Class.forName("java.nio.channels.SocketChannel");
        out.print("nio, ");
      }
    catch (Exception x) { }
    try
      {
        Class.forName("javax.net.ssl.SSLSocket");
        out.print("ssl");
      }
    catch (Exception x) { }
    out.println(")");
    out.println("Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>");
    out.println();
    out.println("Jarsync comes with NO WARRANTY, to the extent permitted by law.");
    out.println("You may redistribute copies of Jarsync under the terms of the GNU");
    out.println("General Public License.  See the file `COPYING' for details.");
  }
}
