/* HTTPRequest.java -- simple HTTP request structure.
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

import java.io.InputStream;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;

/**
 * An HTTP request coming from a client. This class provides basic
 * methods for parsing incoming requests from an input stream and
 * presents easy access to the request's fields.
 */
public class HTTPRequest {

   // Constants and fields.
   // -----------------------------------------------------------------------

   private String method;
   private URI uri;
   private String version;
   private HashMap headers;

   // Constructors.
   // -----------------------------------------------------------------------

   private HTTPRequest() {
      headers = new HashMap();
   }

   // Class methods.
   // -----------------------------------------------------------------------

   public HTTPRequest parse(InputStream in) throws IOException {
      HTTPRequest req = new HTTPRequest();
      int c;
      StringBuffer buf = new StringBuffer();
      while ((c = in.read()) != ' ') {
         if (Character.isLetter((char) c) && Character.isUpperCase((char) c))
            buf.append((char) c);
         else
            throw new HTTPException("Malformed request line.");
      }
      req.method = buf.toString();
      buf.setLength(0);
      
      while ((c = in.read()) != ' ') {
         buf.append((char) c);
      }
      try {
         req.uri = new URI(buf.toString());
      } catch (URISyntaxException use) {
         throw new HTTPException("Malformed URI");
      }
      buf.setLength(0);
      
      while ((c = in.read()) != ' ') {
         if (Character.isLetterOrDigit((char) c) || c == '/' || c == '.')
            buf.append((char) c);
         else
            throw new HTTPException("Malformed HTTP version");
      }
      req.version = buf.toString();
      buf.setLength(0);

      do {
         while ((c = in.read()) != '\r')
            buf.append((char) c);
         if (in.read() != '\n')
            throw new HTTPException("Malformed header");
         if (buf.length() > 0) {
            if (buf.indexOf(":") < 0)
               throw new HTTPException("Malformed header");
            String name = buf.substring(0, buf.indexOf(":"));
            String value = buf.substring(buf.indexOf(":") + 1);
            req.headers.put(name, value);
         }
      } while (buf.length() > 0);
      return req;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public String getMethod() {
      return method;
   }

   public URI getRequestURI() {
      return uri;
   }

   public String getHTTPVersion() {
      return version;
   }

   public String getHeader(String name) {
      return (String) headers.get(name);
   }
}
