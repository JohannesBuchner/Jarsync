/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   BlockingDaemon: blocking-I/O daemon process.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync.

   Jarsync is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   Jarsync is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

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

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.metastatic.rsync.*;

public class BlockingDaemon implements Runnable, Constants {

   // Constants and fields.
   // -----------------------------------------------------------------------

   private static final String SPACES = "                    ";

   private static final Logger logger =
      Logger.getLogger(BlockingDaemon.class.getName());

   private Map modules;

   private Socket socket;
   private InputStream in;
   private OutputStream out;

   private Options options;

   private Configuration config;

   private String motdFile;

   private int remoteVersion;

   // Constructors.
   // -----------------------------------------------------------------------

   public BlockingDaemon(Socket socket, Map modules, String motdFile)
   throws IOException
   {
      this.socket = socket;
      socket.setTcpNoDelay(true);
      socket.setKeepAlive(true);
      in = socket.getInputStream();
      out = socket.getOutputStream();
      this.modules = modules;
      this.motdFile = motdFile;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void run() {
      try {
         Util.writeASCII(out, RSYNCD_GREETING + PROTOCOL_VERSION + "\n");
         String client_greeting = Util.readLine(in);
         if (!client_greeting.startsWith(RSYNCD_GREETING)) {
            socket.close();
            logger.error("got badly formed greeting");
            return;
         }
         try {
            remoteVersion = Integer.parseInt(
               client_greeting.substring(RSYNCD_GREETING.length()));
         } catch (NumberFormatException nfe) {
            socket.close();
            logger.error("got badly formed greeting");
            return;
         }
         if (remoteVersion < MIN_PROTOCOL_VERSION) {
            socket.close();
            logger.error("remote version " + remoteVersion + " not supported");
            return;
         }
         InetAddress addr = socket.getInetAddress();
         addr.getHostName();
         logger.info("client " + addr + " connected on port "
            + socket.getPort());

         String mod = Util.readLine(in);
         logger.info("client asked for module '" + mod + "'");
         if (mod.equalsIgnoreCase("#list") || mod.length() == 0) {
            listModules();
            if (remoteVersion > 25) {
               Util.writeASCII(out, RSYNCD_EXIT + "\n");
            }
            socket.close();
            return;
         }

         Module module = (Module) modules.get(mod);
         if (module == null) {
            Util.writeASCII(out, AT_ERROR+": Unknown module '"+mod+"'\n");
            if (remoteVersion > 25)
               Util.writeASCII(out, RSYNCD_EXIT + "\n");
            socket.close();
            return;
         }

         if (!module.hostAllowed(addr)) {
            Util.writeASCII(out, AT_ERROR+": host " + addr +
               " not allowed to connect to " + mod + "\n");
            logger.warn("host " + addr + " not allowed to connect to " + mod);
            if (remoteVersion > 25)
               Util.writeASCII(out, RSYNCD_EXIT + "\n");
            socket.close();
            return;
         }

         synchronized (module) {
            if (module.maxConnections > 0 &&
                module.connections == module.maxConnections) {
               Util.writeASCII(out, AT_ERROR + ": max connections (" 
                  + module.maxConnections + ") reached. Try again later\n");
               if (remoteVersion > 25)
                  Util.writeASCII(out, RSYNCD_EXIT + "\n");
               socket.close();
               return;
            }
            module.connections++;
         }

         if (module.users != null) {
            if (!authenticate(module)) {
               synchronized (module) {
                  module.connections--;
               }
               return;
            }
         }
         Util.writeASCII(out, RSYNCD_OK + "\n");
         if (module.timeout > 0)
            socket.setSoTimeout(module.timeout * 1000);

         String arg;
         List clientArgs = new LinkedList();
         while (!(arg = Util.readLine(in)).equals("."))
            clientArgs.add(arg);
         logger.info("got args " + clientArgs);
         try {
            options = new Options();
            options.parseArguments("jarsyncd", 
               (String[]) clientArgs.toArray(new String[0]), null);
         } catch (Exception e) { }

         List files = new LinkedList();
         while (!(arg = Util.readLine(in)).equals(""))
            files.add(arg);
         logger.info("got file list " + files);

         MultiplexedInputStream min = new MultiplexedInputStream(in, false);
         MultiplexedOutputStream mout = new MultiplexedOutputStream(out,
            remoteVersion >= 17);

         if (options == null) {
            mout.writeMessage(mout.FERROR, "...");
         }

         synchronized (module) {
            module.connections--;
         }

         socket.close();
      } catch (Exception e) {
         try {
            socket.close();
         } catch (Exception x) { }
         logger.error(e);
      }
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private void listModules() {
      if (motdFile != null) {
         try {
            FileInputStream motd = new FileInputStream(motdFile);
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = motd.read(buf)) != -1)
               out.write(buf, 0, len);
            out.write('\n');
         } catch (IOException ioe) {
            logger.warn("error sending MOTD: " + ioe.getMessage());
         }
      }
      for (Iterator it = modules.values().iterator(); it.hasNext(); ) {
         Module m = (Module) it.next();
         if (m.list) {
            try {
               StringBuffer name = new StringBuffer(SPACES);
               name.replace(0, m.name.length(), m.name);
               Util.writeASCII(out, name + m.comment + "\n");
            } catch (IOException ioe) {
               logger.warn("error listing module " + m.name + ": "
                  + ioe.getMessage());
            }
         }
      }
   }

   private boolean authenticate(Module module) throws Exception {
      SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
      rand.setSeed(System.currentTimeMillis());
      byte[] challenge = new byte[16];
      rand.nextBytes(challenge);
      String b64 = Util.base64(challenge);
      Util.writeASCII(out, RSYNCD_AUTHREQD + b64 + "\n");

      String reply = Util.readLine(in);
      if (reply.indexOf(" ") < 0) {
         Util.writeASCII(out, AT_ERROR + ": bad response\n");
         if (remoteVersion < 25)
            Util.writeASCII(out, RSYNCD_EXIT + "\n");
         socket.close();
         throw new IOException("bad response");
      }
      String user = reply.substring(0, reply.indexOf(" "));
      String response = reply.substring(reply.indexOf(" ")+1);

      if (!module.users.contains(user)) {
         Util.writeASCII(out, AT_ERROR + ": user "+user+" not allowed\n");
         if (remoteVersion < 25)
            Util.writeASCII(out, RSYNCD_EXIT + "\n");
         socket.close();
         throw new IOException("user " + user + " not allowed");
      }

      LineNumberReader secrets = new LineNumberReader(
         new FileReader(module.secretsFile));
      MessageDigest md4 = MessageDigest.getInstance("BrokenMD4");
      String line;
      while ((line = secrets.readLine()) != null) {
         if (line.startsWith(user + ":")) {
            String passwd = line.substring(line.lastIndexOf(":")+1);
            md4.update(new byte[4]);
            md4.update(passwd.getBytes("US-ASCII"));
            md4.update(b64.getBytes("US-ASCII"));
            String hash = Util.base64(md4.digest());
            if (hash.equals(response)) {
               secrets.close();
               return true;
            } else {
               Util.writeASCII(out, AT_ERROR + ": auth failed on module "
                  + module.name + "\n");
               if (remoteVersion < 25)
                  Util.writeASCII(out, RSYNCD_EXIT + "\n");
               socket.close();
               secrets.close();
               logger.error("auth failed on module " + module.name);
               return false;
            }
         }
      }
      Util.writeASCII(out, AT_ERROR + ": auth failed on module "
         + module.name + "\n");
      if (remoteVersion < 25)
         Util.writeASCII(out, RSYNCD_EXIT + "\n");
      socket.close();
      secrets.close();
      logger.error("auth failed on module " + module.name);
      return false;
   }
}
