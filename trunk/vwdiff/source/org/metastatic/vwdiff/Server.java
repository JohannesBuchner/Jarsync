/* Server.java -- The simple web server.
   -*- mode: java; c-basic-offset: 3; -*-
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of vwdiff.

Vwdiff is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your
option) any later version.

Vwdiff is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License along
with vwdiff; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.vwdiff;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

import java.security.MessageDigest;

import org.apache.log4j.Logger;

public class Server implements Runnable {

   // Fields.
   // -----------------------------------------------------------------------

   private static final Logger logger = Logger.getLogger(Server.class);

   public static final String SERVER = "vwdiff/" + version.VERSION
      + " Jarsync/" + org.metastatic.rsync.version.VERSION
      + " Java/" + System.getProperty("java.version")
      + " " + System.getProperty("java.vm.name")
      + "/" + System.getProperty("java.vm.version");

   public static final String HEAD =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n" +
      "  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n\n" +
      "<html><head><title>vwdiff-" + version.VERSION + "</title>\n" +
      "<link rel=\"stylesheet\" href=\"/styles.css\" type=\"text/css\"/>\n" +
      "</head>\n\n";

   private SimpleDateFormat fmt =
      new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm:ss z");
   private MessageDigest md5;
   private ServerSocket server;
   private Socket client;
   private Config conf;
   private HashMap nonces;
   private String footer;

   // Constructors.
   // -----------------------------------------------------------------------

   public Server(Config conf) throws IOException {
      this.conf = conf;
      if (conf.bindAddress != null) {
         logger.info("binding to port " + conf.port + " on "
                     + conf.bindAddress);
         server = new ServerSocket(conf.port, 0,
            InetAddress.getByName(conf.bindAddress));
      } else {
         logger.info("binding to port " + conf.port);
         server = new ServerSocket(conf.port, 0, InetAddress.getLocalHost());
      }
      logger.info("server socket=" + server);
      nonces = new HashMap();
      footer = "<hr/><p class=\"footer\"><a href=\"http://jarsync.sourceforge.net/vwdiff/\">vwdiff/" +
         version.VERSION + "</a> on " + server.getInetAddress() +
         " port " + conf.port + "</p>\n</body>\n</html>\n";
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void run() {
      logger.info("server thread running");
      while (true) {
         try {
            client = server.accept();
            InputStream in = client.getInputStream();
            HTTPRequest req = HTTPRequest.parse(new InputStreamReader(in));
            logger.info(client.getInetAddress().getHostAddress() +
                        " " + req.getRequestURI());
            if (conf.passwdFile != null) {
               if (!checkAuth(client.getOutputStream(), req)) {
                  client.close();
                  continue;
               }
            }
            if (!(req.getMethod().equals("GET") || req.getMethod().equals("HEAD"))) {
               badRequest(client.getOutputStream(),
                          "Method " + req.getMethod() + " not supported.");
               continue;
            }
            URI uri = req.getRequestURI();
            if (uri.getPath().equals("/")) {
               brief(client.getOutputStream(), req);
            } else if (uri.getPath().equalsIgnoreCase("/about")) {
               about(client.getOutputStream(), req);
            } else if (uri.getPath().equalsIgnoreCase("/styles.css")) {
               stylesheet(client.getOutputStream(), req);
            } else if (uri.getPath().equalsIgnoreCase("/image")) {
               image(client.getOutputStream(), req);
            } else if (uri.getPath().equalsIgnoreCase("/go")) {
               access(client.getOutputStream(), req);
            } else if (uri.getPath().equalsIgnoreCase("/detail")) {
               detail(client.getOutputStream(), req);
            } else if (uri.getPath().equalsIgnoreCase("/update")) {
               update(client.getOutputStream(), req);
            } else if (uri.getPath().equalsIgnoreCase("/refresh")) {
               refresh(client.getOutputStream(), req);
            } else if (uri.getPath().equalsIgnoreCase("/log")) {
               showLog(client.getOutputStream(), req);
            } else {
               notFound(client.getOutputStream(), req);
            }
            client.close();
         } catch (HTTPException he) {
            try {
               badRequest(client.getOutputStream(),
                  conf.debug ? (Object) he : (Object) he.getMessage());
            } catch (IOException ioe) {
               logger.warn(ioe.toString());
            }
         } catch (IOException ioe) {
            logger.warn(ioe.toString());
         }
      }
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private void badRequest(OutputStream out, Object why)
      throws IOException
   {
      HTTPResponse resp = new HTTPResponse(400, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Content-Type", "text/html; charset=utf-8");
      resp.setHeader("Connection", "close");
      resp.writeBody(
         "<html>\n" +
         "<body><h1>Bad Request</h1>\n" +
         "<p>\n"
      );
      if (why instanceof Throwable) {
         resp.writeBody("<pre>");
         resp.writeBody((Throwable) why);
         resp.writeBody("</pre>");
      } else {
         resp.writeBody(why.toString());
      }
      resp.writeBody(footer);
      resp.write(out);
   }

   private void brief(OutputStream out, HTTPRequest req)
      throws IOException
   {
      HTTPResponse resp = new HTTPResponse(200, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Content-Type", "text/html; charset=utf-8");
      resp.setHeader("Connection", "close");
      if (req.getMethod().equals("GET")) {
         resp.writeBody
         (
            HEAD + "<body><h1>VWDIFF @ " + server.getInetAddress() +
            "</h1><p><a href=\"/detail\">Detailed View (all pages)</a> | " +
            "<a href=\"/update\">Update All Pages</a> | " +
            "<a href=\"/refresh\">Refresh Configuration</a> | " +
            "<a href=\"/log\">View Log</a> | " +
            "<a href=\"/about\">About Vwdiff</a></p>\n" +
            "<table><tr class=\"header\">\n" +
            "<th></th><th colspan=2>URL</th><th>% new</th><th>% moved</th>" +
            "<th></th></tr>\n"
         );
         boolean ab = true;
         for (Iterator it = conf.targets.values().iterator(); it.hasNext(); ) {
            Target t = (Target) it.next();
            String encodedName = URLEncoder.encode(t.getName(), "UTF-8");
            resp.writeBody("<tr class=\"" + (ab?"a":"b") + "\">");
            ab =! ab;
            double perNew = (t.getNewLength() == 0) ? 0.0 :
               (double) t.getNewLength() / (double) t.getLength() * 100.0;
            double perMoved = (t.getMovedLength() == 0) ? 0.0 :
               (double) t.getMovedLength() / (double) t.getLength() * 100.0;
            if (perNew > t.getThreshold())
               resp.writeBody("<td class=\"new\">NEW</td>");
            else
               resp.writeBody("<td class=\"new\"> </td>");
            resp.writeBody
            (
               "<td class=\"url\"><a href=\"/go?" + encodedName +
               "\">[" + t.getName() + "]</a> </td><td><a href=\"/go?" +
               encodedName + "\">" + t.getURL() + "</a></td>\n" +
               "<td class=\"pernew\">" + (int) perNew + "</td>" +
               "<td class=\"permoved\">" + (int) perMoved +
               "</td><td class=\"detail\"><a href=\"/detail?" +
               encodedName + "\">detail</a></td></tr>\n"
            );
         }
         resp.writeBody("</table>");
         resp.writeBody(footer);
      }
      resp.write(out);
   }

   private void about(OutputStream out, HTTPRequest req)
      throws IOException
   {
      HTTPResponse resp = new HTTPResponse(200, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Content-Type", "text/html; charset=utf-8");
      resp.setHeader("Connection", "close");
      if (req.getMethod().equals("GET")) {
         resp.writeBody(HEAD);
         resp.writeBody(
            "<body><h1>VWDIFF - " + version.VERSION + "</h1>\n" +
            "<p>Vwdiff, the Visual Web Differencer, is a Java application " +
            "that periodically polls a list of web pages, finds the " +
            "differences between the current version and the version last " +
            "accessed, and presents these differences in a concise, visual " +
            "form. It includes a basic web server so the results can be " +
            "viewed from any web browser.</p>\n" +
            "Vwdiff is free software, and is available on the web at " +
            "<a href=\"http://jarsync.sourceforge.net/vwdiff/\">" +
            "http://jarsync.sourceforge.net/vwdiff/</a>; it was written " +
            "by <a href=\"mailto:rsdio@metastatic.org\">Casey Marshall</a>.</p>\n" +
            "<p>Vwdiff uses the following software packages:</p>\n" +
            "<ul><li><a href=\"http://jarsync.sourceforge.net/\">" +
            "Jarsync 0.2.1</a> - a Java delta compression library.</li>\n" +
            "<li>J. David Eisenberg's <a href=\"http://www.catcode.com/pngencoder/\">PNGEncoder</a></li>\n" +
            "<li>The Apache Foundation's <a href=\"http://jakarta.apache.org/log4j/docs/\">Log4j</a></li>\n" +
            "<li>Aaron M. Renn's <a href=\"http://www.urbanophile.com/arenn/hacking/download.html\">Java getopt port</a></li></ul>\n"
         );
         resp.writeBody(footer);
      }
      resp.write(out);
   }

   private void access(OutputStream out, HTTPRequest req)
      throws IOException
   {
      String name = URLDecoder.decode(req.getRequestURI().getQuery(), "UTF-8");
      Target t = (Target) conf.targets.get(name);
      if (t == null) {
         notFound(out, req);
         return;
      }
      HTTPResponse resp = new HTTPResponse(302, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Location", t.getURL().toString());
      resp.setHeader("Connection", "close");
      resp.write(out);
      t.access();
   }

   private void detail(OutputStream out, HTTPRequest req)
      throws IOException
   {
      LinkedList toShow = new LinkedList();
      if (req.getRequestURI().getQuery() == null) {
         toShow.addAll(conf.targets.keySet());
      } else {
         String name = URLDecoder.decode(req.getRequestURI().getQuery(), "UTF-8");
         if (!conf.targets.containsKey(name)) {
            notFound(out, req);
            return;
         }
         toShow.add(name);
      }
      HTTPResponse resp = new HTTPResponse(200, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Content-Type", "text/html; charset=utf-8");
      resp.setHeader("Connection", "close");
      if (req.getMethod().equals("GET")) {
         resp.writeBody(HEAD);
         resp.writeBody(
            "<body><h1>VWDIFF @ " + server.getInetAddress() + "</h1>\n");
         resp.writeBody("<p><a href=\"/\">Brief View</a> | " +
                        "<a href=\"/detail\">Detailed View (all pages)</a> | "+
                        "<a href=\"/update\">Update All Pages</a> | " +
                        "<a href=\"/refresh\">Refresh Configuration</a> | " +
                        "<a href=\"/log\">View Log</a> | " +
                        "<a href=\"/about\">About Vwdiff</a></p>\n");
         for (Iterator it = toShow.iterator(); it.hasNext(); ) {
            Target t = (Target) conf.targets.get(it.next());
            double perNew = (t.getNewLength() == 0) ? 0.0 :
               (double) t.getNewLength() / (double) t.getLength() * 100.0;
            double perMoved = (t.getMovedLength() == 0) ? 0.0 :
               (double) t.getMovedLength() / (double) t.getLength() * 100.0;
            resp.writeBody("<table><tr><th colspan=2 class=\"detail\">" +
                           t.getName() + "</th></tr>\n");
            resp.writeBody("<tr><td class=\"image\"><img src=\"/image?" +
                           URLEncoder.encode(t.getName(), "UTF-8") +
                           "\" alt=\"" + t.getName() + "\" /></td>\n");
            resp.writeBody("<td class=\"info\">Last update: " +
                           fmt.format(new Date(t.getLastUpdate())) + "<br/>\n"+
                           "Last accessed: " +
                           fmt.format(new Date(t.getLastAccess())) + "<br/>\n"+
                           "Length: " + t.getLength() + "<br/>\n" +
                           "Moved Bytes: " + t.getMovedLength() + "(" +
                           (int) perMoved + "%)<br/>\n" +
                           "New Bytes: " + t.getNewLength() + "(" +
                           (int) perNew + "%)<br/>\n" +
                           "URL: <a href=\"/go?" +
                           URLEncoder.encode(t.getName(), "UTF-8") +
                           "\">" + t.getURL() + "</a><br/>\n" +
                           "<a href=\"/update?" +
                           URLEncoder.encode(t.getName(), "UTF-8") +
                           "\">update now</a></td></tr>\n");
            resp.writeBody("</table>\n");
         }
         resp.writeBody(footer);
      }
      resp.write(out);
   }

   private void image(OutputStream out, HTTPRequest req)
      throws IOException
   {
      String name = URLDecoder.decode(req.getRequestURI().getQuery(), "UTF-8");
      Target t = (Target) conf.targets.get(name);
      if (t == null || t.getImage() == null) {
         notFound(out, req);
         return;
      }
      HTTPResponse resp = new HTTPResponse(200, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Content-Type", "image/png");
      resp.setHeader("Connection", "close");
      resp.writeBody(t.getImage());
      resp.write(out);
   }

   private void stylesheet(OutputStream out, HTTPRequest req)
      throws IOException
   {
      HTTPResponse resp = new HTTPResponse(200, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Content-Type", "text/css; charset=utf-8");
      resp.setHeader("Connection", "close");
      resp.writeBody(conf.stylesheet);
      resp.write(out);
   }
   private void refresh(OutputStream out, HTTPRequest req)
      throws IOException
   {
      HTTPResponse resp = new HTTPResponse(302, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Location", req.getHeader("Referer") != null ?
                     req.getHeader("Referer") : "/");
      resp.setHeader("Connection", "close");
      resp.write(out);
      try {
         Main.loadConfig(conf);
      } catch (IOException ioe) {
         logger.warn("could not refresh config: " + ioe.getMessage());
      }
   }

   private void update(OutputStream out, HTTPRequest req)
      throws IOException
   {
      LinkedList toUpdate = new LinkedList();
      if (req.getRequestURI().getQuery() == null) {
         toUpdate.addAll(conf.targets.keySet());
      } else {
         String name = URLDecoder.decode(req.getRequestURI().getQuery(), "UTF-8");
         if (!conf.targets.containsKey(name)) {
            notFound(out, req);
            return;
         }
         toUpdate.add(name);
      }
      HTTPResponse resp = new HTTPResponse(302, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Location", req.getHeader("Referer") != null ?
                     req.getHeader("Referer") : "/");
      resp.setHeader("Connection", "close");
      resp.write(out);
      for (Iterator it = toUpdate.iterator(); it.hasNext(); ) {
         ((Target) conf.targets.get(it.next())).update(true, false);
      }
      try {
         conf.store();
      } catch (IOException ioe) {
         logger.warn("could not store data: " + ioe.getMessage());
      }
   }

   private void showLog(OutputStream out, HTTPRequest req)
      throws IOException
   {
      MemoryAppender app = Main.memoryAppender;
      HTTPResponse resp = new HTTPResponse(200, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Content-Type", "text/html; charset=utf-8");
      resp.setHeader("Connection", "close");
      resp.writeBody(HEAD);
      resp.writeBody("<body>");
      resp.writeBody("<p><a href=\"/\">Brief View</a> | " +
                     "<a href=\"/detail\">Detailed View (all pages)</a> | "+
                     "<a href=\"/update\">Update All Pages</a> | " +
                     "<a href=\"/refresh\">Refresh Configuration</a> | " +
                     "<a href=\"/about\">About Vwdiff</a></p>\n");
      resp.writeBody("<pre>\n");
      for (Iterator it = app.getLog().iterator(); it.hasNext(); ) {
         resp.writeBody((String) it.next());
      }
      resp.writeBody("</pre>\n");
      resp.writeBody(footer);
      resp.write(out);
   }

   private void notFound(OutputStream out, HTTPRequest req)
      throws IOException
   {
      HTTPResponse resp = new HTTPResponse(404, "HTTP/1.1");
      resp.setHeader("Server", SERVER);
      resp.setHeader("Content-Type", "text/html; charset=utf-8");
      resp.setHeader("Connection", "close");
      resp.writeBody(HEAD);
      resp.writeBody("<body><h1>Not Found</h1>\n");
      resp.writeBody("<p>The requested URL " + req.getRequestURI() +
                     " was not found on this server.</p>\n");
      resp.writeBody(footer);
      resp.write(out);
   }

   private boolean checkAuth(OutputStream out, HTTPRequest req)
      throws IOException
   {
      /*Map auth = new HashMap();
      if (req.getHeader("Authenticate") != null) {
         auth = parseAuth(req.getHeader("Authenticate"));
         if (nonces.contains(auth.get("nonce")))
            return true;
      }*/
      return true;
   }

   private Map parseAuth(String auth) throws HTTPException {
      HashMap result = new HashMap();
      StringTokenizer tok = new StringTokenizer(auth, ",");
      while (tok.hasMoreTokens()) {
         String line = tok.nextToken().trim();
         if (line.indexOf('=') < 0)
            throw new HTTPException("malformed Authenticate header");
         String name = line.substring(0, line.indexOf('='));
         String value = line.substring(line.indexOf('=')+1);
         value = value.substring(1, value.length()-1);
         result.put(name, value);
      }
      return result;
   }
}
