/* vim:set softtabstop=3 tabstop=3 shiftwidth=3 expandtab tw=72:
   $Id$
  
   Protocol: the base protocol operations.
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

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;

import java.net.InetAddress;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.Checksum32;
import org.metastatic.rsync.BrokenMD4;
import org.metastatic.rsync.Util;

public class Protocol implements Constants {

   // Constants and fields.
   // -----------------------------------------------------------------------

   private final Logger logger;

   public static final String SPACES = "               ";

   protected int state;
   protected int inState;
   protected int outState;

   /** The unique session ID. */
   protected String sid;

   /** The message-of-the-day. */
   protected String motd;

   /** The module list. */
   protected Map modules;
   protected InetAddress client;

   protected ByteBuffer inBuffer;
   protected ByteBuffer outBuffer;
   protected DuplexByteBuffer duplex;

   protected CharsetEncoder encoder;

   protected int remoteVersion;
   protected boolean connected;

   protected Module module;
   protected String error;
   protected String challenge;
   protected LinkedList argv;
   protected Options options;
   protected Configuration config;
   protected Statistics stats;
   protected List fileList;
   protected Map uids, gids;

   protected boolean start_glob = false;
   protected String request;

   protected BufferTool tool;

   // Constructors.
   // -----------------------------------------------------------------------

   public Protocol(String motd, Map modules, InetAddress client) {
      sid = "org.metastatic.rsync.v2."
         + Long.toHexString(System.currentTimeMillis());
      logger = Logger.getLogger(sid);
      this.motd = motd;
      this.modules = modules;
      this.client = client;
      state = STATE_SETUP_PROTOCOL;
      inState = SETUP_READ_GREETING;
      outState = SETUP_WRITE_GREETING;
      inBuffer  = ByteBuffer.allocate(CHUNK_SIZE);
      outBuffer = ByteBuffer.allocate(CHUNK_SIZE);
      duplex = new DuplexByteBuffer(outBuffer);
      inBuffer.order(ByteOrder.LITTLE_ENDIAN);
      outBuffer.order(ByteOrder.LITTLE_ENDIAN);
      inBuffer.flip();
      encoder = Charset.forName("ISO-8859-1").newEncoder();
      connected = true;
      argv = new LinkedList();
      stats = new Statistics();
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   /**
    * Get the byte buffer for input coming over the socket.
    *
    * @return The input buffer.
    */
   public ByteBuffer getInputBuffer() {
      return inBuffer;
   }

   /**
    * Get the byte buffer for output going over the socket.
    *
    * @return The output buffer.
    */
   public ByteBuffer getOutputBuffer() {
      return outBuffer;
   }

   public Statistics getStatistics() {
      return stats;
   }

   /**
    * Return whether or not this protocol's connection is finished.
    *
    * @return The connection state.
    */
   public boolean connectionFinished() {
      return !connected;
   }

   /**
    * Signals that there is more input to be consumed.
    */
   public void updateInput() {
      if (state == STATE_SETUP_PROTOCOL) {
         setupInput();
         return;
      }
      if ((state & INPUT_MASK) == 0)
         return;
      switch (state & INPUT_MASK) {
      case STATE_RECEIVE_EXCLUDE:
         try {
            String l = null;
            while ((l = BufferUtil.getString(inBuffer, MAXPATHLEN)) != null)
            {
               if (l.length() == 0) break;
               l = l.replace('/', File.separatorChar);
               if (options.exclude.length() == 0)
                  options.exclude = l;
               else
                  options.exclude += " " + l;
            }
         } catch (BufferUnderflowException bue) {
            break;
         }
         if (options.verbose > 2)
            logger.info("server sender process starting");
         tool = new BufferFileList(options, module.path, argv,
            remoteVersion, logger, FLIST_SEND_FILES);
         tool.setBuffers(duplex, inBuffer);
         state = STATE_SEND_FLIST;
         break;

      case STATE_SENDER_INPUT:
         try {
            if (!tool.updateInput()) {
               if (remoteVersion >= 24)
                  inBuffer.getInt();
               connected = false;
               module.connections--;
               state = (state & OUTPUT_MASK) | STATE_INPUT_DONE;
            }
         } catch (ProtocolException x) {
            state = STATE_INPUT_DONE;
            logger.error(x.getMessage());
            connected = false;
            module.connections--;
         } catch (BufferUnderflowException bue) {
         } catch (Exception x) {
            logger.warn("exception in send files: " + x);
            connected = false;
            module.connections--;
            state = STATE_DONE;
         }
         break;

      case STATE_INPUT_DONE:
         inBuffer.position(inBuffer.position() + inBuffer.remaining());
         break;
         
      default:
         logger.debug("unknown input state " + Integer.toHexString(state));
      }
   }

   /**
    * Signals that there is space for output, and that more output bytes
    * need to be placed in the output buffer.
    */
   public void updateOutput() {
      if (state == STATE_SETUP_PROTOCOL) {
         setupOutput();
         return;
      }
      if ((state & OUTPUT_MASK) == 0)
         return;
      switch (state & OUTPUT_MASK) {
      case STATE_SEND_FLIST:
         try {
            if (!tool.updateOutput()) {
               fileList = ((BufferFileList) tool).getFileList();
               uids = ((BufferFileList) tool).getUidList();
               gids = ((BufferFileList) tool).getGidList();
               tool = new BufferSender(options, config, fileList,
                  module.path, logger, remoteVersion);
               tool.setBuffers(duplex, inBuffer);
               ((BufferSender) tool).setStatistics(stats);
               state = STATE_SENDER;
               break;
            }
         } catch (Exception x) {
            logger.warn("exception in send files: " + x);
            connected = false;
            module.connections--;
            state = STATE_DONE;
         }
         break;

      case STATE_SENDER_OUTPUT:
         try {
            if (!tool.updateOutput()) {
               if (options.verbose > 0 || remoteVersion >= 20) {
                  duplex.putLong(stats.total_read);
                  duplex.putLong(stats.total_written);
                  duplex.putLong(stats.total_size);
               }
               state = (state & INPUT_MASK) | STATE_OUTPUT_DONE;
            }
         } catch (Exception x) {
            x.printStackTrace();
            logger.warn("error in server sender " + x);
            connected = false;
            module.connections--;
            state = STATE_DONE;
         }
         break;

      case STATE_OUTPUT_DONE:
         break;

      default:
         logger.debug("unknown output state " + Integer.toHexString(state));
      }
      duplex.flush();
   }

   // Own methods.
   // -----------------------------------------------------------------------

   /**
    * Continue setting up this session (version exchange, module,
    * authentication).
    */
   protected void setupInput() {
      String line;
      switch (inState) {
      case SETUP_READ_GREETING:
         line = readLine();
         if (line == null)
            break;
         if (!line.startsWith(RSYNCD_GREETING)) {
            error = "client did not send greeting";
            outState = SETUP_WRITE_ERROR;
            inState = SETUP_READ_DONE;
            break;
         }
         try {
            remoteVersion = Integer.parseInt(
               line.substring(RSYNCD_GREETING.length()));
            if (remoteVersion < MIN_PROTOCOL_VERSION ||
                remoteVersion > MAX_PROTOCOL_VERSION) {
               error = "unsupported protocol version";
               outState = SETUP_WRITE_ERROR;
               inState = SETUP_READ_DONE;
               break;
            }
         } catch (NumberFormatException nfe) {
            error = "poorly formed greeting string";
            outState = SETUP_WRITE_ERROR;
            inState = SETUP_READ_DONE;
            break;
         }
         inState = SETUP_READ_MODULE;
         if (!inBuffer.hasRemaining())
            break;

      case SETUP_READ_MODULE:
         line = readLine();
         if (line == null)
            break;
         if (line.length() == 0 || line.equals("#list")) {
            outState = SETUP_WRITE_MODULES;
            break;
         }
         if (line.startsWith("#")) {
            error = "Unknown command";
            outState = SETUP_WRITE_ERROR;
            inState = SETUP_READ_DONE;
            break;
         }
         module = (Module) modules.get(line);
         if (module == null) {
            error = "Unknown module";
            outState = SETUP_WRITE_ERROR;
            inState = SETUP_READ_DONE;
            break;
         }

         if (module.maxConnections > 0 &&
             module.connections == module.maxConnections) {
            error = "max connections (" + module.maxConnections +
               ") reached. Try again later\n";
            outState = SETUP_WRITE_ERROR;
            inState = SETUP_READ_DONE;
            break;
         }

         if (!module.hostAllowed(client)) {
            error = "client " + client + " not allowed to connect";
            outState = SETUP_WRITE_ERROR;
            inState = SETUP_READ_DONE;
            break;
         }

         if (module.users != null) {
            inState = SETUP_READ_AUTH;
            outState = SETUP_WRITE_CHALLENGE;
            break;
         } else {
            inState = SETUP_READ_OPTIONS;
            outState = SETUP_WRITE_OK;
         }
         module.connections++;
         break;

      case SETUP_READ_AUTH:
         line = readLine();
         if (line == null)
            break;
         if (line.indexOf(" ") < 0) {
            error = "bad response";
            outState = SETUP_WRITE_ERROR;
            inState = SETUP_READ_DONE;
         }
         if (!authenticate(line)) {
            outState = SETUP_WRITE_ERROR;
            inState = SETUP_READ_DONE;
         } else {
            outState = SETUP_WRITE_OK;
            inState = SETUP_READ_OPTIONS;
         }
         break;

      case SETUP_READ_OPTIONS:
         while ((line = readLine()) != null) {
            if (line.equals("")) break;
            if (!line.startsWith("-"))
               line = line.replace('/', File.separatorChar);
            if (line.equals(".")) {
               start_glob = true;
               continue;
            }
            if (start_glob) {
               line = globExpand(line);
            }
            argv.add(line);
         }
         if (line == null)
            break;
         try {
            options = new Options();
            String[] args = (String[]) argv.toArray(new String[argv.size()]);
            int optind = options.parseArguments("jarsyncd", args, null);
            argv.clear();
            for (int i = optind; i < args.length; i++) {
               argv.add(args[i]);
            }
         } catch (Exception e) {
            module.connections--;
            connected = false;
         }
         inState = SETUP_READ_DONE;
         outState = SETUP_WRITE_DONE;
         duplex.setDuplex(remoteVersion < 23 && (remoteVersion == 22 ||
            (remoteVersion > 17 && options.am_sender)));
         logger.addAppender(new RsyncBufferAppender(duplex));
         options.exclude += " " + module.exclude;
         options.include += " " + module.include;
         break;

      case SETUP_READ_DONE:
      }
   }

   /**
    * The connection setup output. Based on the current output state,
    * this method fills the output buffer with the approriate info for
    * the state.
    */
   protected void setupOutput() {
      switch (outState) {
      case SETUP_READ_GREETING:
         encoder.reset();
         encoder.encode(CharBuffer.wrap(RSYNCD_GREETING+PROTOCOL_VERSION+"\n"),
            outBuffer, true);
         encoder.flush(outBuffer);
         outState = SETUP_WRITE_WAIT;
         break;

      case SETUP_WRITE_MODULES:
         encoder.reset();
         encoder.encode(CharBuffer.wrap(motd + "\n"), outBuffer, false);
         for (Iterator i = modules.values().iterator(); i.hasNext(); ) {
            Module m = (Module) i.next();
            StringBuffer name = new StringBuffer();
            if (m.list) {
               name.append(SPACES);
               name.replace(0, Math.min(m.name.length(), SPACES.length()),
                  m.name);
               name.append('\t');
               name.append(m.comment);
               name.append('\n');
            }
            encoder.encode(CharBuffer.wrap(name), outBuffer,
               !i.hasNext() && remoteVersion < 25);
         }
         if (remoteVersion >= 25)
            encoder.encode(CharBuffer.wrap(RSYNCD_EXIT+"\n"), outBuffer, true);
         encoder.flush(outBuffer);
         outState = SETUP_WRITE_WAIT;
         inState = SETUP_READ_DONE;
         connected = false;
         break;

      case SETUP_WRITE_CHALLENGE:
         SecureRandom rand = null;
         try {
            rand = SecureRandom.getInstance("SHAPRNG");
         } catch (NoSuchAlgorithmException nsae) {
            rand = new SecureRandom();
         }
         rand.setSeed(System.currentTimeMillis());
         byte[] buf = new byte[16];
         rand.nextBytes(buf);
         challenge = Util.base64(buf);
         encoder.reset();
         encoder.encode(CharBuffer.wrap(RSYNCD_AUTHREQD + challenge + "\n"),
            outBuffer, true);
         encoder.flush(outBuffer);
         inState = SETUP_READ_AUTH;
         outState = SETUP_WRITE_WAIT;
         break;

      case SETUP_WRITE_OK:
         encoder.reset();
         encoder.encode(CharBuffer.wrap(RSYNCD_OK + "\n"), outBuffer, true);
         encoder.flush(outBuffer);
         outState = SETUP_WRITE_WAIT;
         break;

      case SETUP_WRITE_ERROR:
         encoder.reset();
         encoder.encode(CharBuffer.wrap(AT_ERROR + ": " + error + "\n"),
            outBuffer, remoteVersion < 25);
         if (remoteVersion >= 25)
            encoder.encode(CharBuffer.wrap(RSYNCD_EXIT+"\n"), outBuffer, true);
         encoder.flush(outBuffer);
         if (module != null)
            module.connections--;
         logger.error(error);
         connected = false;
         break;

      case SETUP_WRITE_DONE:
         config = new Configuration();
         try {
            config.strongSum = MessageDigest.getInstance("BrokenMD4");
         } catch (NoSuchAlgorithmException nsae) {
            duplex.putString(duplex.FERROR, "server configuration error\n");
            connected = false;
            module.connections--;
            return;
         }
         if (remoteVersion >= 14)
            config.strongSumLength = 2;
         else
            config.strongSumLength = SUM_LENGTH;
         config.weakSum = new Checksum32();
         if (remoteVersion >= 12) {
            int seed = (int) System.currentTimeMillis();
            config.checksumSeed = new byte[4];
            config.checksumSeed[0] = (byte) (seed & 0xFF);
            config.checksumSeed[1] = (byte) (seed >>>  8 & 0xFF);
            config.checksumSeed[2] = (byte) (seed >>> 16 & 0xFF);
            config.checksumSeed[3] = (byte) (seed >>> 24 & 0xFF);
            duplex.putInt((int) seed);
         }
         encoder = null;
         duplex.setDuplex(true);
         if (options.am_sender) {
            state = STATE_RECEIVE_EXCLUDE;
         } else {
            state = STATE_SEND_FLIST;
         }

      case SETUP_WRITE_WAIT:
      }
   }

   /**
    * Read a line of text from the input buffer and return it, or return
    * null if there is not yet a full line in the buffer.
    *
    * @return The next string, minus the trailing newline.
    */
   private String readLine() {
      if (!inBuffer.hasRemaining()) {
         return null;
      }
      String line = "";
      int pos = inBuffer.position();
      while (inBuffer.hasRemaining()) {
         char c = (char) inBuffer.get();
         if (c == '\n')
            break;
         line += c;
         if (!inBuffer.hasRemaining()) {
            inBuffer.position(pos);
            return null;
         }
      }
      return line;
   }

   /**
    * Authenticate a user based on his reply to our challenge, which is
    * of the form:
    *
    * <p><pre>[user] [base-64 encoded response]</pre>
    *
    * @param reply The reply read off the wire.
    * @return true if the user can be authenticated.
    */
   private boolean authenticate(String reply) {
      String user = reply.substring(0, reply.indexOf(" "));
      String resp = reply.substring(reply.indexOf(" ") + 1);
      if (!module.users.contains(user)) {
         error = "so such user " + user;
         return false;
      }
      try {
         LineNumberReader secrets = new LineNumberReader(
            new FileReader(module.secretsFile));
         String line;
         while ((line = secrets.readLine()) != null) {
            if (line.startsWith(user + ":")) {
               MessageDigest md4 = MessageDigest.getInstance("BrokenMD4");
               md4.update(new byte[4]);
               md4.update(
                  line.substring(line.indexOf(":")+1).getBytes("US-ASCII"));
               md4.update(challenge.getBytes("US-ASCII"));
               String hash = Util.base64(md4.digest());
               if (hash.equals(resp)) {
                  secrets.close();
                  return true;
               }
            }
         }
         secrets.close();
      } catch (Exception e) {
         logger.fatal(e.toString());
         error = "server configuration error";
         return false;
      }
      error = "authentication failure for module " + module.name;
      return false;
   }

   private String globExpand(String token) {
      String base = module.name;
      if (token.startsWith(base)) {
         token = token.substring(base.length());
      }
      token = sanitizePath(token);
      if (token.length() == 0) {
         token = ".";
      }
      return token;
   }

   private String sanitizePath(String path) {
      StringTokenizer tok = new StringTokenizer(path, File.separator);
      LinkedList p = new LinkedList();
      while (tok.hasMoreTokens()) {
         String s = tok.nextToken();
         if (s.equals("."))
            continue;
         if (s.equals("..")) {
            if (p.size() > 0)
               p.removeLast();
            continue;
         }
         p.addLast(s);
      }

      StringBuffer result = new StringBuffer();
      for (Iterator i = p.listIterator(); i.hasNext(); ) {
         result.append((String) i.next());
         if (i.hasNext() || path.endsWith(File.separator))
            result.append(File.separator);
      }
      return result.toString();
   }
}
