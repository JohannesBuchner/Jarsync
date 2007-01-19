/* Daemon -- the rsync daemon.
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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketPermission;

import java.security.Security;

import java.util.Iterator;
import java.util.Map;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import org.metastatic.rsync.*;

public class Daemon implements Constants, Runnable
{

  // Constants and fields.
  // -----------------------------------------------------------------------

  private static final boolean CAN_NONBLOCK;
  private static final boolean HAVE_SSL;

  static
  {
    boolean ok = false;
    try
      {
        Class.forName("java.nio.channels.ServerSocketChannel");
        ok = true; // probably
      }
    catch (Throwable x) { }
    CAN_NONBLOCK = ok;
    ok = false;
    try
      {
        Class.forName("javax.net.ssl.SSLContext");
        ok = true;
      }
    catch (Throwable x) { }
    HAVE_SSL = ok;
    java.security.Security.addProvider(new JarsyncProvider());
  }

  protected Logger logger;

  public static final String PROGNAME = "jarsyncd";

  public static final String OPTSTRING = "a:bBc:dhp:sv";

  public static final int KEYSTORE = 1000;
  public static final int SECRETS  = 1001;

  public static final LongOpt[] LONGOPTS = {
    new LongOpt("address", LongOpt.REQUIRED_ARGUMENT, null, 'a'),
    new LongOpt("config", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
    new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
    new LongOpt("blocking-io", LongOpt.NO_ARGUMENT, null, 'b'),
    new LongOpt("non-blocking-io", LongOpt.NO_ARGUMENT, null, 'B'),
    new LongOpt("debug", LongOpt.NO_ARGUMENT, null, 'd'),
    new LongOpt("ssl", LongOpt.NO_ARGUMENT, null, 's'),
    new LongOpt("ssl-keystore", LongOpt.REQUIRED_ARGUMENT, null, KEYSTORE),
    new LongOpt("ssl-secrets", LongOpt.REQUIRED_ARGUMENT, null, SECRETS),
    new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
    new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v')
  };

  protected String address;
  protected int port;
  protected int debug = 0;
  protected String motdFile;
  protected String pidFile;
  protected String logFile;
  protected Map modules;

  protected boolean ssl;
  protected String ssl_keystore;
  protected Secrets ssl_secrets;

  // Class methods.
  // -------------------------------------------------------------------------

  public static void main(String[] argv) throws Throwable
  {
    Getopt g = new Getopt(PROGNAME, argv, OPTSTRING, LONGOPTS);
    String address = null;
    String conf = "/etc/jarsyncd.conf";
    int port = RSYNCD_PORT;
    boolean nonblock = false;
    boolean detach = true;
    int c;
    int debug = 0;
    boolean ssl = false;
    String ssl_keystore = null;
    Secrets ssl_secrets = null;

    while ((c = g.getopt()) != -1)
      switch (c)
        {
        case 'a':
          address = g.getOptarg();
          break;
        case 'c':
          conf = g.getOptarg();
          break;
        case 'd':
          debug++;
          break;
        case 'p':
          try
            {
              port = Integer.parseInt(g.getOptarg());
              if (port < 1)
                throw new NumberFormatException("port must be positive");
            }
          catch (NumberFormatException nfe)
            {
              System.err.println(PROGNAME + ": bad port: " + nfe.getMessage());
              System.exit(1);
            }
          break;
        case 'b':
          nonblock = false;
          break;
        case 'B':
          if (!CAN_NONBLOCK)
            {
              System.err.println(PROGNAME + ": this platform does not have nonblocking I/O capabilities");
              System.exit(1);
            }
          nonblock = true;
          break;
        case 's':
          if (!HAVE_SSL)
            {
              System.err.println(PROGNAME + ": this platform does not support SSL");
              System.exit(1);
            }
          ssl = true;
          break;
        case KEYSTORE:
          ssl_keystore = g.getOptarg();
          break;
        case SECRETS:
          try
            {
              ssl_secrets = new Secrets(g.getOptarg());
            }
          catch (Exception x)
            {
              System.err.println(PROGNAME + ": error loading secrets file " +
                                 g.getOptarg() + ": " + x.getMessage());
              System.exit(1);
            }
          break;
        case 'h':
          help();
          System.exit(0);
        case 'v':
          version();
          System.exit(0);
        case '?':
          System.err.println("Use `" + PROGNAME + " --help' for more info.");
          System.exit(1);
        }

    Daemon d = null;
    if (nonblock)
      {
        System.err.println("starting nonblocking server");
        d = (Daemon) Class.forName(
          "org.metastatic.rsync.v2.NonblockingDaemon").newInstance();
      }
    else
      {
        System.err.println("starting blocking server");
        d = new Daemon();
      }
    d.address = address;
    d.port = port;
    d.debug = debug;
    d.ssl = ssl;
    d.ssl_keystore = ssl_keystore;
    d.ssl_secrets = ssl_secrets;
    try
      {
        d.configure(conf);
      }
    catch (IOException ioe)
      {
        System.err.println(PROGNAME + ": error reading config file: " +
                           ioe.getMessage());
        System.exit(1);
      }
    d.run();
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public void run()
  {
    ServerSocket socket = null;
    logger.info("binding to " + address + ":" + port);
    try
      {
        if (address == null)
          socket = new ServerSocket(port);
        else
          socket = new ServerSocket(port, 0, InetAddress.getByName(address));
      }
    catch (Exception e)
      {
        logger.fatal("Could not bind socket: " + e);
        System.err.println("Could not bind socket: " + e);
        return;
      }

    while (true)
      {
        try
          {
            BlockingDaemon d = new BlockingDaemon(socket.accept(),
                                                  modules, motdFile);
            Thread t = new Thread(d);
            t.setDaemon(true);
            t.start();
          }
        catch (IOException ioe)
          {
            logger.warn("accept: " + ioe);
          }
      }
  }

  // Own methods.
  // -------------------------------------------------------------------------

  protected void configure(String conffile) throws IOException
  {
    RsyncdConf config = new RsyncdConf();
    Parameters p = new Parameters(config);
    p.begin(conffile);
    p.parse();
    motdFile = config.getMOTDFile();
    pidFile = config.getPIDFile();
    logFile = config.getLogFile();
    modules = config.getModules();

    for (Iterator i = modules.values().iterator(); i.hasNext(); )
      {
        Module m = (Module) i.next();
        if (m.excludeFrom != null) {
          try
            {
              LineNumberReader in = new LineNumberReader(
                                      new FileReader(m.excludeFrom));
              String line;
              while ((line = in.readLine()) != null)
                {
                  if (m.exclude.length() == 0)
                    m.exclude = line;
                  else
                    m.exclude += ", " + line;
                }
            }
          catch (IOException ioe)
            {
              continue;
            }
        }
        if (m.includeFrom != null)
          {
            try
              {
                LineNumberReader in = new LineNumberReader(
                                        new FileReader(m.includeFrom));
                String line;
                while ((line = in.readLine()) != null)
                  {
                    if (m.include.length() == 0)
                      m.include = line;
                    else
                      m.include += ", " + line;
                  }
              }
            catch (IOException ioe)
              {
                continue;
              }
          }
      }
    logger = Logger.getLogger("org.metastatic.rsync.v2");
    if (logFile != null && debug == 0)
      {
        logger.addAppender(new FileAppender(new PatternLayout("%d: %m%n"),
                                            logFile));
      }
    if (debug > 0)
      {
        logger.addAppender(new ConsoleAppender(new PatternLayout("%d: %m%n")));
      }
    if (debug == 0)
      logger.setLevel(Level.WARN);
    if (debug == 1)
      logger.setLevel(Level.INFO);
    if (debug >= 2)
      logger.setLevel(Level.DEBUG);
  }

  private static void help()
  {
    System.out.println("usage: " + PROGNAME + " [options]");
    System.out.println();
    System.out.println("  -a, --address=ADDR         Bind to address ADDR.");
    System.out.println("  -c, --config=PATH          Read config file from PATH.");
    System.out.println("  -p, --port=PORT            Specify the port to listen to (default 873).");
    System.out.println("  -b, --blocking-io          Start blocking I/O server.");
    System.out.println("  -B, --non-blocking-io      Start nonblocking I/O server.");
    System.out.println("  -s, --ssl                  Use SSL (if available).");
    System.out.println("      --ssl-keystore=STORE   Specify path to SSL certificates.");
    System.out.println("                             (format is PATH[;TYPE])");
    System.out.println("      --ssl-secrets=PATH     Specify path to certificate password file.");
    System.out.println("  -h, --help                 Show this help and exit.");
    System.out.println("  -v, --version              Show version information and exit.");
  }

  private static void version()
  {
    System.out.println(PROGNAME + " (Jarsync " +
                       org.metastatic.rsync.version.VERSION + ")");
    System.out.println("Copyright (C) 2003 Casey Marshall.");
    System.out.println();
    System.out.println("Jarsync comes with NO WARRANTY, to the extent permitted by law.");
    System.out.println("You may redistribute copies of Jarsync under the terms of the GNU");
    System.out.println("General Public License.  See the file `COPYING' for details.");
  }
}
