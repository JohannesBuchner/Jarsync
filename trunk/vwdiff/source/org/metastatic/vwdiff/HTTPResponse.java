/* HTTPResponse.java -- simple HTTP response structure.
   vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HTTPResponse {

   // Fields.
   // -----------------------------------------------------------------------

   protected int status;
   protected String version;
   protected HashMap headers;
   protected ByteArrayOutputStream body;

   // Constructors.
   // -----------------------------------------------------------------------

   public HTTPResponse(int status, String version, Map headers, byte[] body) {
      this.status = status;
      this.version = version;
      this.headers = (headers != null) ? new HashMap(headers) : new HashMap();
      this.body = new ByteArrayOutputStream();
      if (body != null)
         this.body.write(body, 0, body.length);
   }

   public HTTPResponse(int status, String version, Map headers) {
      this(status, version, headers, null);
   }

   public HTTPResponse(int status, String version) {
      this(status, version, null, null);
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void setHeader(String name, String value) {
      if (name == null || value == null)
         throw new NullPointerException();
      headers.put(name, value);
   }

   public void writeBody(byte[] body) {
      if (body == null)
         throw new NullPointerException();
      this.body.write(body, 0, body.length);
   }

   public void writeBody(String str) {
      writeBody(str.getBytes());
   }

   public void writeBody(Throwable t) {
      t.printStackTrace(new PrintStream(body));
   }

   public void write(OutputStream out) throws IOException {
      String rp = reasonPhrase(status);
      if (rp == null)
         throw new HTTPException("Bad status code " + status);
      write(out, version + " " + status + " " + rp);
      for (Iterator i = headers.keySet().iterator(); i.hasNext(); ) {
         String name = (String) i.next();
         write(out, name + ": " + (String) headers.get(name));
      }
      write(out, null);
      body.writeTo(out);
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private static void write(OutputStream out, String value) throws IOException 
   {
      if (value != null)
         out.write(value.getBytes());
      out.write('\r');
      out.write('\n');
   }

   private static String reasonPhrase(int status) {
      switch (status) {
         case 100: return "Continue";
         case 101: return "Switching Protocols";
         case 200: return "OK";
         case 201: return "Created";
         case 202: return "Accepted";
         case 203: return "Non-Authoritative Information";
         case 204: return "No Content";
         case 205: return "Reset Content";
         case 206: return "Partial Content";
         case 300: return "Multiple Choices";
         case 301: return "Moved Permanently";
         case 302: return "Found";
         case 303: return "See Other";
         case 304: return "Not Modified";
         case 305: return "Use Proxy";
         case 307: return "Temporary Redirect";
         case 400: return "Bad Request";
         case 401: return "Unauthorized";
         case 402: return "Payment Required";
         case 403: return "Forbidden";
         case 404: return "Not Found";
         case 405: return "Method Not Allowed";
         case 406: return "Not Acceptable";
         case 407: return "Proxy Authentication Required";
         case 408: return "Request Time-out";
         case 409: return "Conflict";
         case 410: return "Gone";
         case 411: return "Length Required";
         case 412: return "Precondition Failed";
         case 413: return "Request Entity Too Large";
         case 414: return "Request-URI Too Large";
         case 415: return "Unsupported Media Type";
         case 416: return "Requested range not satisfiable";
         case 417: return "Expectation Failed";
         case 500: return "Internal Server Error";
         case 501: return "Not Implemented";
         case 502: return "Bad Gateway";
         case 503: return "Service Unavailable";
         case 504: return "Gateway Time-out";
         case 505: return "HTTP Version not supported";
         default: return null;
      }
   }
}
