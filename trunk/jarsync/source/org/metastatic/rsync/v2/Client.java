/* Client -- main client process.
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


package org.metastatic.rsync.v2;

import java.awt.GridLayout;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.Socket;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import java.text.DecimalFormat;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import org.metastatic.rsync.Checksum32;
import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.JarsyncProvider;
import org.metastatic.rsync.Util;

public class Client implements Constants
{

  // Fields.
  // -------------------------------------------------------------------------

  public static final String PROGNAME = "jarsync";

  private static final Logger logger =
    Logger.getLogger("org.metastatic.rsync.v2");

  private static long starttime;
  private static boolean listOnly;
  private static boolean amSender;
  private static boolean local;
  private static boolean useSocket;
  private static Socket socket;
  private static Process remoteShell;

  private static List excludeList = new LinkedList();
  private static List server_argv;
  private static String remoteHost;
  private static String remoteUser;
  private static int remotePort = 873;
  private static int remoteVersion;

  private static Statistics stats;
  private static Configuration config;
  private static Options options;
  private static MultiplexedInputStream in;
  private static MultiplexedOutputStream out;

  // Constructors.
  // -------------------------------------------------------------------------

  private Client() { }

  // Class methods.
  // -------------------------------------------------------------------------

  public static int startServer(String[] argv, int optind)
  {
    return 0;
  }

  public static int startClient(String[] argv, int optind)
  {
    logger.addAppender(new ConsoleAppender(new PatternLayout(PROGNAME+": %m%n")));
    if (options.verbose == 0)
      logger.setLevel(Level.WARN);
    else if (options.verbose == 1)
      logger.setLevel(Level.INFO);
    else if (options.verbose >= 2)
      logger.setLevel(Level.DEBUG);
    String path = null;
    int i = 0;

    listOnly = (optind == argv.length - 1) && !options.am_server;
    if (argv[optind].toLowerCase().startsWith(URL_PREFIX))
      {
        remoteHost = argv[optind].substring(URL_PREFIX.length());
        i = remoteHost.indexOf('/');
        if (i > 0)
          {
            path = remoteHost.substring(i+1);
            remoteHost = remoteHost.substring(0, i);
          }
        else
          {
            path = "";
          }
        i = remoteHost.indexOf('@');
        if (i > 0)
          {
            remoteUser = remoteHost.substring(0, i);
            remoteHost = remoteHost.substring(i+1);
          }
        i = remoteHost.indexOf(':');
        if (i > 0)
          {
            try
              {
                remotePort = Integer.parseInt(remoteHost.substring(i+1));
              }
            catch (Exception x)
              {
                logger.error("bad port number.");
                System.exit(1);
              }
            remoteHost = remoteHost.substring(0, i);
          }
        return startSocketClient(path, argv, optind+1);
      }

    i = argv[optind].indexOf(':');
    if (i > 0)
      {
        if (i+1 < argv[optind].length() && argv[optind].charAt(i+1) == ':')
          {
            path = argv[optind].substring(i+2);
            remoteHost = argv[optind].substring(0, i);
            i = remoteHost.indexOf('@');
            if (i > 0)
              {
                remoteUser = remoteHost.substring(0, i);
                remoteHost = remoteHost.substring(i+1);
              }
            return startSocketClient(path, argv, optind+1);
          }

        if (optind == argv.length)
          {
            logger.error("too few arguments.");
            System.exit(1);
          }
        path = argv[optind].substring(i+1);
        remoteHost = argv[optind].substring(0, i);
        i = remoteHost.indexOf('@');
        if (i > 0)
          {
            remoteUser = remoteHost.substring(0, i);
            remoteHost = remoteHost.substring(i+1);
          }
        return startShellClient(path, argv, optind+1);
      }
    else
      {
        String[] args = null;
        options.am_sender = true;
        if (argv[argv.length-1].toLowerCase().startsWith(URL_PREFIX))
          {
            remoteHost = argv[argv.length-1].substring(URL_PREFIX.length());
            i = remoteHost.indexOf('/');
            if (i > 0)
              {
                path = remoteHost.substring(i+1);
                remoteHost = remoteHost.substring(0, i);
              }
            else
              {
                path = "";
              }
            i = remoteHost.indexOf('@');
            if (i > 0)
              {
                remoteUser = remoteHost.substring(0, i);
                remoteHost = remoteHost.substring(i+1);
              }
            i = remoteHost.indexOf(':');
            if (i > 0)
              {
                try
                  {
                    remotePort = Integer.parseInt(remoteHost.substring(i+1));
                  }
                catch (Exception x)
                  {
                    logger.error("bad port number.");
                    System.exit(1);
                  }
                remoteHost = remoteHost.substring(0, i);
              }
            args = new String[argv.length - optind - 1];
            System.arraycopy(argv, optind, args, 0, args.length);
            return startSocketClient(path, args, 0);
          }

        i = argv[argv.length-1].indexOf(':');
        if (i > 0)
          {
            if (i+1 < argv[argv.length-1].length() &&
                argv[argv.length-1].charAt(i+1) == ':')
              {
                path = argv[argv.length-1].substring(i+2);
                remoteHost = argv[argv.length-1].substring(0, i);
                i = remoteHost.indexOf('@');
                if (i > 0)
                  {
                    remoteUser = remoteHost.substring(0, i);
                    remoteHost = remoteHost.substring(i+1);
                  }
                return startSocketClient(path, argv, optind+1);
              }

            if (optind == argv.length)
              {
                logger.error("too few arguments.");
                System.exit(1);
              }
            path = argv[argv.length-1].substring(i+1);
            remoteHost = argv[argv.length-1].substring(0, i);
            i = remoteHost.indexOf('@');
            if (i > 0)
              {
                remoteUser = remoteHost.substring(0, i);
                remoteHost = remoteHost.substring(i+1);
              }
            args = new String[argv.length - optind - 1];
            System.arraycopy(argv, optind, args, 0, args.length);
            return startShellClient(path, args, 0);
          }
        else
          return localClient(argv, optind);
      }
  }

  /**
   * Starts the client process over a socket connected to port 873.
   */
  public static int startSocketClient(String path, String[] argv, int optind)
  {
    logger.debug("starting socket client to " + remoteHost + ":" + remotePort);
    if (path.startsWith("/"))
      {
        logger.error("remote path must start with a module name.");
        System.exit(1);
      }
    try
      {
        if (options.use_ssl)
          {
            try
              {
                // So we can compile on JSSE-less platforms...
                Class clazz = Class.forName("org.metastatic.rsync.v2.SSLUtil");
                Method m = clazz.getMethod("getSSLSocket",
                                           new Class[] { String.class,
                                                         Integer.TYPE });
                socket = (Socket) m.invoke(null, new Object[] { remoteHost,
                                                                new Integer(remotePort) });
              }
            catch (InvocationTargetException ite)
              {
                logger.error("can't create SSL socket.");
                logger.error(ite.getCause().toString());
                return 1;
              }
            catch (Exception ex)
              {
                logger.error("can't create SSL socket.");
                logger.error(ex.toString());
                return 1;
              }
          }
        else
          socket = new Socket(remoteHost, remotePort);
        if (options.io_timeout > 0)
          socket.setSoTimeout(options.io_timeout);
        socket.setKeepAlive(true);
        logger.debug("socket=" + socket);
        in = new MultiplexedInputStream(new BufferedInputStream(socket.getInputStream()), false);
        out = new MultiplexedOutputStream(new BufferedOutputStream(socket.getOutputStream()), false);
        in.setStats(stats);
        out.setStats(stats);
      }
    catch (IOException ioe)
      {
        logger.error("cannot connect to " + remoteHost +
                     ":" + remotePort + ": " + ioe.getMessage());
        return 1;
      }
    try
      {
        setupSocket(path, argv, optind);
      }
    catch (IOException ioe)
      {
        logger.error(ioe.getMessage());
        return 1;
      }
    return clientRun(argv, optind);
  }

  public static int startShellClient(String path, String[] argv, int optind)
  {
    logger.debug("starting shell client to " + remoteHost);
    serverArgs();
    server_argv.add(path);
    for (int i = optind; i < argv.length-1; i++)
      server_argv.add(argv[i]);
    if (remoteUser == null)
      remoteUser = System.getProperty("user.name");
    if (options.shell_cmd.equals("internal-ssh"))
      {
        logger.fatal("not implemented");
        return 1;
      }
    else
      {
        String[] sargs = new String[server_argv.size()+5];
        sargs[0] = options.shell_cmd;
        sargs[1] = "-l";
        sargs[2] = remoteUser;
        sargs[3] = remoteHost;
        sargs[4] = options.rsync_path;
        for (int i = 0; i < server_argv.size(); i++)
          sargs[i+5] = (String) server_argv.get(i);
        if (options.verbose > 2)
          {
            StringBuffer cmd = new StringBuffer();
            for (int i = 0; i < sargs.length; i++)
              cmd.append(sargs[i] + " ");
            logger.debug("executing " + cmd.toString());
          }
        Process p = null;
        try
          {
            p = Runtime.getRuntime().exec(sargs);
            in = new MultiplexedInputStream(new BufferedInputStream(p.getInputStream()), false);
            out = new MultiplexedOutputStream(new BufferedOutputStream(p.getOutputStream()), false);
            final InputStream err = p.getErrorStream();
            new Thread(new Runnable()
              {
                public void run()
                {
                  try
                    {
                      byte[] buf = new byte[4092];
                      int len;
                      while ((len = err.read(buf)) >= 0)
                        {
                          logger.error(new String(buf, 0, len));
                        }
                    }
                  catch (IOException ioe)
                    {
                    }
                }
              }).start();
            in.setStats(stats);
            out.setStats(stats);
            out.writeInt(PROTOCOL_VERSION);
            out.flush();
            remoteVersion = in.readInt();
            if (remoteVersion < MIN_PROTOCOL_VERSION ||
                remoteVersion > MAX_PROTOCOL_VERSION)
              {
                logger.fatal("protocol version mismatch (is your shell clean?)");
                p.destroy();
                return 1;
              }
          }
        catch (IOException ioe)
          {
            logger.fatal("could not execute " + options.shell_cmd);
            logger.fatal(ioe.getMessage());
            return 1;
          }
        int ret = clientRun(argv, optind);
        try { p.waitFor(); } catch (InterruptedException ie) { }
        return ret;
      }
  }

  public static int localClient(String[] argv, int optind)
  {
    logger.debug("starting local client");
    return 0;
  }

  public static int clientRun(String[] argv, int optind)
  {
    try
      {
        config.checksumSeed = new byte[4];
        in.read(config.checksumSeed);
        logger.debug("checksum seed=" + Util.toHexString(config.checksumSeed));
        if (remoteVersion >= 23)
          in.setMultiplex(true);
        stats.total_read = 0;
        stats.total_written = 0;
        if (listOnly && !options.recurse)
          excludeList.add("/*/*");
        if (!options.am_sender || (options.delete_mode && !options.delete_excluded))
          {
            for (Iterator i = excludeList.iterator(); i.hasNext(); )
              {
                String pattern = (String) i.next();
                if (pattern.startsWith("+ ") && remoteVersion < 19)
                  throw new IOException("remote rsync does not support include syntax");
                out.writeInt(pattern.length());
                out.writeString(pattern);
                out.flush();
              }
            out.writeInt(0);
            out.flush();
          }
        FileList flist = new FileList(in, out, remoteVersion, false, options);
        flist.setStatistics(stats);

        if (options.am_sender)
          {
            final List files = flist.createFileList(argv, optind,
                                                    argv.length-optind);
            logger.debug("sending files=" + files);
            long l = stats.total_written;
            flist.sendFileList(files);
            stats.flist_size = (int) (stats.total_written - l);
            // Give the other side a chance to set up.
            //try { Thread.sleep(3000); } catch (InterruptedException ie) { }
            Sender sender = new Sender(in, out, config, remoteVersion, false);
            sender.setStatistics(stats);
            sender.sendFiles(files);
            if (remoteVersion >= 24)
              in.readInt(); // final goodbye
            return readStats();
          }
        else
          {
            final List files = flist.receiveFileList();
            stats.flist_size = (int) stats.total_read;
            if (listOnly)
              {
                Collections.sort(files,
                  new Comparator() {
                    public int compare(Object a, Object b)
                      {
                        return ((FileInfo) a).filename().compareTo(
                               ((FileInfo) b).filename());
                      }
                  });
                for (Iterator i = files.iterator(); i.hasNext(); )
                  System.out.println(i.next());
                out.writeInt(-1); // End generator phase 0.
                out.flush();
                in.readInt();     // End receiver phase 0.
                out.writeInt(-1); // End generator phase 1.
                out.flush();
                in.readInt();     // End receiver phase 1.
                if (remoteVersion >= 24)
                  {
                    out.writeInt(-1); // Final goodbye.
                    out.flush();
                  }
                return readStats();
              }
            flist.toLocalList(files, argv[argv.length-1]);
            final Receiver recv = new Receiver(in, out, config, remoteVersion, false);
            recv.setStatistics(stats);
            Thread generator = new Thread(
              new Runnable()
              {
                public void run()
                {
                  try
                    {
                      recv.generateFiles(files);
                    }
                  catch (IOException ioe)
                    {
                      ioe.printStackTrace(new LoggerPrintStream(logger,
                                                                Level.ERROR));
                    }
                }
              }, "generator");
            generator.start();
            recv.receiveFiles(files);
            //Thread receiver = new Thread(
            //  new Runnable()
            //  {
            //    public void run()
            //    {
            //      try
            //        {
            //          recv.receiveFiles(files);
            //        }
            //      catch (IOException ioe)
            //        {
            //          ioe.printStackTrace(new LoggerPrintStream(logger,
            //                                                    Level.ERROR));
            //        }
            //    }
            //  }, "receiver");
            //generator.start();
            //receiver.start();
            //try
            //  {
            //    receiver.join();
            //  }
            //catch (Exception e)
            //  {
            //    logger.error(e.toString());
            //  }
            if (remoteVersion >= 24)
              out.write(-1);
            return readStats();
          }
      }
    catch (IOException ioe)
      {
        logger.error(ioe.getMessage());
        return 1;
      }
  }

  public static int readStats()
  {
    long t = System.currentTimeMillis();
    boolean send_stats = options.verbose > 0 || remoteVersion >= 20;
    if (send_stats && !options.am_sender)
      {
        try
          {
            stats.total_written = in.readLong();
            long l = in.readLong();
            stats.total_size = in.readLong();
            stats.total_read = l;
          }
        catch (IOException ioe)
          {
            logger.error("error reading stats: "+ioe.getMessage());
            return 1;
          }
      }
    if (options.do_stats)
      {
        if (!options.am_sender && !send_stats)
          {
            logger.error("Cannot show stats as receiver because remote protocol version is less than 20.");
            logger.error("Use --stats -v to show stats.");
            return 1;
          }
        logger.warn("Number of files: " + stats.num_files);
        logger.warn("Number of files transferred: "
                    + stats.num_transferred_files);
        logger.warn("Total file size: " + stats.total_size + " bytes");
        logger.warn("Total transferred file size: "
                    + stats.total_transferred_size + " bytes");
        logger.warn("Literal data: " + stats.literal_data + " bytes");
        logger.warn("Matched data: " + stats.matched_data + " bytes");
        logger.warn("File list size: " + stats.flist_size + " bytes");
        logger.warn("Total bytes written: " + stats.total_written + " bytes");
        logger.warn("Total bytes read: " + stats.total_read + " bytes");
        logger.warn("");
      }
    if (options.verbose > 0 || options.do_stats)
      {
        DecimalFormat df = new DecimalFormat("###0.00");
        logger.warn("wrote " + stats.total_written + " bytes  read " +
                    stats.total_read + " bytes  " +
                    df.format(((double)(stats.total_written+stats.total_read) /
                               (0.5 + (double)((t-starttime) / 1000L))))
                    + " bytes/sec");
        logger.warn("total size is " + stats.total_size + " bytes  speedup is " +
                    df.format((1.0*stats.total_size) / (double)(stats.total_written+stats.total_read)));
      }
    return 0;
  }

  // Main entry point.
  // -------------------------------------------------------------------------

  public static void main(String[] argv)
  {
    starttime = System.currentTimeMillis();
    Security.addProvider(new JarsyncProvider());
    int optind = 0;
    options = new Options();
    try
      {
        optind = options.parseArguments(PROGNAME, argv, System.err);
        if (optind == -1)
          System.exit(0);
      }
    catch (IllegalArgumentException iae)
      {
        System.err.println(PROGNAME + ": " + iae.getMessage());
        System.err.println("Try `" + PROGNAME + " --help' for more info.");
        System.exit(1);
      }

    if (optind == argv.length)
      {
        System.err.println(PROGNAME+": too few arguments.");
        System.err.println("Try `"+PROGNAME+" --help' for more info.");
        System.exit(1);
      }

    stats = new Statistics();
    config = new Configuration();
    config.weakSum = new Checksum32();
    try
      {
        config.strongSum = MessageDigest.getInstance("BrokenMD4");
      }
    catch (NoSuchAlgorithmException nsae)
      {
        System.err.println(PROGNAME+": could not create MD4 instance.");
        System.exit(1);
      }
    config.strongSumLength = 2;
    config.blockLength = options.block_size;

    int ret = 0;
    if (options.am_server)
      {
      }
    else
      {
        ret = startClient(argv, optind);
      }

    System.exit(ret);
  }

  // Own methods.
  // -------------------------------------------------------------------------

  /**
   * Sets up a socket connection.
   */
  private static void setupSocket(String path, String[] argv, int optind)
    throws IOException
  {
    Util.writeASCII(out, RSYNCD_GREETING + PROTOCOL_VERSION + '\n');
    out.flush();
    String greeting = Util.readLine(in);
    logger.debug("got greeting " + greeting);
    if (!greeting.startsWith(RSYNCD_GREETING))
      {
        throw new IOException("did not receive greeting");
      }
    try
      {
        remoteVersion = Integer.parseInt(greeting.substring(RSYNCD_GREETING.length()));
      }
    catch (NumberFormatException nfe)
      {
        throw new IOException("improper protocol version");
      }
    if (remoteVersion < MIN_PROTOCOL_VERSION ||
        remoteVersion > MAX_PROTOCOL_VERSION)
      throw new IOException("protocol version mismatch");
    if (remoteUser == null)
      remoteUser = System.getProperty("user.name");

    serverArgs();
    if (path.length() > 0)
      server_argv.add(path);

    String module = path;
    if (module.indexOf('/') > 0)
      module = module.substring(0, module.indexOf('/'));
    logger.debug("requesting module '" + module + "'");
    Util.writeASCII(out, module + '\n');
    out.flush();

    boolean kludge_around_eof = listOnly && (remoteVersion < 25);
    String line = null;
    while (true)
      {
        if ((line = Util.readLine(in)) == null)
          {
            if (kludge_around_eof)
              System.exit(0);
            throw new EOFException("did not receive server startup line.");
          }

        if (line.startsWith(RSYNCD_AUTHREQD))
          {
            userAuth(line.substring(18));
            continue;
          }
        if (line.equals(RSYNCD_OK))
          break;
        if (line.equals(RSYNCD_EXIT))
          System.exit(0);
        if (line.startsWith(AT_ERROR))
          throw new IOException(line);

        System.out.println(line);
      }

    for (Iterator i = server_argv.iterator(); i.hasNext(); )
      Util.writeASCII(out, (String) i.next() + '\n');
    Util.writeASCII(out, "\n");
    out.flush();

    if (remoteVersion < 23)
      {
        if (remoteVersion == 22 || (remoteVersion > 17 && !options.am_sender))
          in.setMultiplex(true);
      }
  }

  private static void userAuth(String challenge) throws IOException
  {
    try
      {
        MessageDigest md = MessageDigest.getInstance("BrokenMD4");
        String passwd = null;
        if (System.getProperty("jarsync.password.gui") != null)
          {
            JPasswordField pass = new JPasswordField();
            JPanel panel = new JPanel(new GridLayout(2, 1));
            panel.add(new JLabel("Password:"));
            panel.add(pass);
            JOptionPane.showMessageDialog(null, panel,
                                          remoteUser+'@'+remoteHost+"'s Password",
                                          JOptionPane.QUESTION_MESSAGE);
            passwd = new String(pass.getPassword());
          }
        else
          {
            System.out.print(remoteUser + '@' + remoteHost + "'s password: ");
            passwd = Util.readLine(System.in);
            System.out.println();
          }
        md.update(new byte[4]); // dummy checksum seed
        md.update(passwd.getBytes("US-ASCII"));
        md.update(challenge.getBytes("US-ASCII"));
        byte[] response = md.digest();
        Util.writeASCII(out, remoteUser + " " + Util.base64(response) + '\n');
        out.flush();
      }
    catch (NoSuchAlgorithmException nsae)
      {
        throw new IOException("could not create message digest.");
      }
  }

  private static void serverArgs()
  {
    server_argv = new LinkedList();
    server_argv.add("--server");
    if (!options.am_sender)
      server_argv.add("--sender");

    StringBuffer buf = new StringBuffer("-");
    for (int i = 0; i < options.verbose; i++)
      buf.append('v');

    if (options.make_backups)
      buf.append('b');
    if (options.update_only)
      buf.append('u');
    if (options.dry_run)
      buf.append('n');
    if (options.preserve_links)
      buf.append('l');
    if (options.copy_links)
      buf.append('L');
    if (options.whole_file)
      buf.append('W');
    if (options.preserve_hard_links)
      buf.append('H');
    if (options.preserve_uid)
      buf.append('o');
    if (options.preserve_gid)
      buf.append('g');
    if (options.preserve_devices)
      buf.append('D');
    if (options.preserve_times)
      buf.append('t');
    if (options.preserve_perms)
      buf.append('p');
    if (options.recurse)
      buf.append('r');
    if (options.always_checksum)
      buf.append('c');
    if (options.cvs_exclude)
      buf.append('C');
    if (options.ignore_times)
      buf.append('I');
    if (options.relative_paths)
      buf.append('R');
    if (options.one_file_system)
      buf.append('x');
    if (options.sparse_files)
      buf.append('S');
    if (options.do_compression)
      buf.append('z');

    if (listOnly && !options.recurse)
      buf.append('r');

    if (buf.length() > 1)
      server_argv.add(buf.toString());

    if (options.block_size != BLOCK_LENGTH)
      server_argv.add("-B" + options.block_size);

    if (options.io_timeout > 0)
      server_argv.add("--timeout=" + options.io_timeout);

    if (options.bwlimit > 0)
      server_argv.add("--bwlimit=" + options.bwlimit);

    if (!options.backup_suffix.equals("~"))
      {
        server_argv.add("--suffix");
        server_argv.add(options.backup_suffix);
      }

    if (options.delete_mode && !options.delete_excluded)
      server_argv.add("--delete");

    if (options.delete_excluded)
      server_argv.add("--delete-excluded");

    if (options.size_only)
      server_argv.add("--size-only");

    if (options.modify_window != 2)
      {
        server_argv.add("--modify-window");
        server_argv.add(String.valueOf(options.modify_window));
      }

    if (options.size_only)
      server_argv.add("--size-only");

    server_argv.add(".");
    logger.debug("server_argv=" + server_argv);
  }
}
