/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   NonblockingDaemon: nonblocking daemon process.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync.

   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2 of the License, or (at
   your option) any later version.

   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jarsync; if not, write to the

      Free Software Foundation, Inc.,
      59 Temple Place, Suite 330,
      Boston, MA  02111-1307
      USA  */

package org.metastatic.rsync.v2;

import java.io.FileReader;
import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Iterator;
import java.util.Set;

import org.metastatic.rsync.version;

public class NonblockingDaemon extends Daemon
{

   // Constants and fields.
   // -----------------------------------------------------------------------

   // Constructors.
   // -----------------------------------------------------------------------

   NonblockingDaemon() {
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void run() {
      Selector selector = null;
      ServerSocketChannel server = null;
      SelectionKey acceptKey = null;
      try {
         selector = Selector.open();
         server = ServerSocketChannel.open();
         if (address != null)
            server.socket().bind(new InetSocketAddress(address, port));
         else
            server.socket().bind(new InetSocketAddress(port));
         server.configureBlocking(false);
         acceptKey = server.register(selector, SelectionKey.OP_ACCEPT);
      } catch (IOException ioe) {
         logger.fatal("could not bind server: " + ioe);
         return;
      }
      logger.info("jarsyncd version " + version.VERSION +
         " listening on port " + port);

      String motd = "";
      try {
         StringBuffer buf = new StringBuffer();
         char[] c = new char[512];
         int len;
         FileReader motdIn = new FileReader(motdFile);
         while ((len = motdIn.read(c)) != -1) {
            buf.append(c, 0, len);
         }
         motd = buf.toString();
      } catch (IOException ioe) {
         logger.warn("error reading MOTD file: " + ioe.getMessage());
      }

      // Main server loop.
      for (;;) {
         Protocol prot;
         SocketChannel client;
         try {
            selector.select();
         } catch (IOException ioe) {
            logger.warn(ioe.toString());
            continue;
         }
         Set keys = selector.selectedKeys();

         for (Iterator i = keys.iterator(); i.hasNext(); ) {
            SelectionKey key = (SelectionKey) i.next();
            i.remove();

            if (key == acceptKey) {
               try {
                  if (key.isAcceptable()) {
                     SocketChannel c = server.accept();
                     c.configureBlocking(false);
                     InetAddress host = c.socket().getInetAddress();
                     SelectionKey newKey = c.register(selector,
                        SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                        new Protocol(motd, modules, host));
                     logger.info("connection made by " +
                        host.getHostName() + " (" + host.getHostAddress() +
                        ") on port " + c.socket().getPort());
                  }
               } catch (IOException ioe) {
                  logger.warn("error accepting connection: "+ioe.getMessage());
               }
            } else {
               prot = (Protocol) key.attachment();
               client = (SocketChannel) key.channel();
               Statistics stats = prot.getStatistics();
               try {
                  ByteBuffer out = prot.getOutputBuffer();
                  ByteBuffer in  = prot.getInputBuffer();
                  if (key.isWritable()) {
                     prot.updateOutput();
                     out.flip();
                     if (out.hasRemaining())
                        stats.total_written += client.write(out);
                     out.compact();
                  }
                  int len = 0;
                  if (key.isReadable()) {
                     in.compact();
                     len = client.read(in);
                     in.flip();
                  }
                  if (len > 0) {
                     stats.total_read += len;
                  }
                  if (in.hasRemaining()) {
                     prot.updateInput();
                  }
                  if (len == -1) {
                     if (in.hasRemaining() || out.position() > 0)
                        logger.warn("connection unexpectedly closed ("
                           + in.position() + " bytes in buffer).");
                     key.cancel();
                     client.close();
                     continue;
                  }
                  if (prot.connectionFinished() &&
                     !(in.hasRemaining() || out.position() > 0))
                  {
                     logger.info("connection to " +
                        client.socket().getInetAddress() + " finished");
                     key.cancel();
                     client.close();
                  }
               } catch (IOException ioe) {
                  logger.warn(client.socket().getInetAddress() + ": " + ioe);
                  key.cancel();
                  try {
                     client.close();
                  } catch (IOException ioe2) {
                     logger.warn("error closing connection: " + ioe2);
                  }
               } catch (BufferOverflowException boe) {
                  logger.warn("buffer overflow on connection to "
                     + client.socket().getInetAddress());
                  key.cancel();
                  try {
                     client.close();
                  } catch (IOException ioe) {
                     logger.warn("error closing connection: " + ioe);
                  }
               } catch (BufferUnderflowException bue) {
                  logger.warn("buffer underflow on connection to "
                     + client.socket().getInetAddress());
                  key.cancel();
                  try {
                     client.close();
                  } catch (IOException ioe) {
                     logger.warn("error closing connection: " + ioe);
                  }
               } catch (Exception x) {
                  logger.warn("uncaught exception: " + x +
                     " on the connection to " +
                     client.socket().getInetAddress());
                  x.printStackTrace();
                  key.cancel();
                  try {
                     client.close();
                  } catch (IOException ioe) {
                     logger.warn("error closing connection: " + ioe);
                  }
               }
            }
         }
      }
   }
}
