/* :vim:set tw=78 expandtab tabstop=3 softtabstop=3 shiftwidth=3:
   $Id$

   Module: an rsyncd module.
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

import java.net.InetAddress;
import java.util.StringTokenizer;

public class Module {

   // Constants and fields.
   // -----------------------------------------------------------------------

   public String name;
   public String path;
   public String comment;
   public boolean readOnly = true;
   public boolean list = true;
   public java.util.Set users;
   public String secretsFile;
   public String uid;
   public String gid;
   public String exclude = "";
   public String excludeFrom;
   public String include = "";
   public String includeFrom;
   public boolean strictModes = false;
   public String hostsAllow;
   public String hostsDeny;
   public boolean ignoreErrors = false;
   public boolean ignoreNonReadable = false;
   public String transferLogging;
   public String logFormat = "%o %h [%a] %m (%u) %f %l";
   public int timeout;
   public String refuseOptions = "";
   public String dontCompress;
   public int maxConnections = 0;
   public int connections = 0;

   // Contstructor.
   // -----------------------------------------------------------------------

   public Module(String name) {
      this.name = name;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   /**
    * Test if a given host is allowed to connect to this module.
    *
    * @param addr The address to test.
    * @return true if the host is allowed.
    */
   public boolean hostAllowed(InetAddress addr) {
      // If neither list exists allow anything.
      if ((hostsAllow == null || hostsAllow.length() == 0) &&
          (hostsDeny == null  || hostsDeny.length() == 0))
         return true;

      // If there is no deny list, only allow hosts in the allow list.
      if (hostsDeny == null || hostsDeny.length() == 0)
         return accessMatch(hostsAllow, addr);

      // If there is no allow list, only deny hosts in the deny list.
      if (hostsAllow == null || hostsAllow.length() == 0)
         return !accessMatch(hostsDeny, addr);

      // If both exist allow hosts in the allow list...
      if (accessMatch(hostsAllow, addr))
         return true;

      // deny hosts in the deny list...
      if (accessMatch(hostsDeny, addr))
         return false;

      // and allow hosts in neither list.
      return true;
   }

   // Own methods.
   // -----------------------------------------------------------------------

   /**
    * Match an address (a hostname or a numeric address) to a list of
    * patterns.
    *
    * @param list The list of patterns, separated by commas or
    *        whitespace.
    * @param addr The address to match.
    * @return true if the address matches a pattern in the list.
    */
   private boolean accessMatch(String list, InetAddress addr) {
      StringTokenizer tok = new StringTokenizer(list.toLowerCase(), " \t,");
      while (tok.hasMoreTokens()) {
         String pattern = tok.nextToken();
         if (FNMatch.fnmatch(pattern, addr.getHostName(), 0))
            return true;
         if (matchAddress(addr, pattern))
            return true;
      }
      return false;
   }

   /**
    * Match a numeric inet address (in either IPv4 or IPv6 form) to any
    * of the following:
    *
    * <ul>
    * <li>A full, dotted (or colon'd) numeric IP address, e.g.
    * 127.0.0.1.</li>
    * <li>A numeric IP address, a slash (/), followed by a bit mask,
    * e.g. 192.168.0.0/255.255.0.0.</li>
    * <li>A numeric IP address, a slash (/), followed by the number of
    * bits in the mask (from the left), e.g. 192.168.0.0/16.</li>
    * </ul>
    *
    * @param addr The address to match.
    * @param tok  The pattern to match.
    * @return true if the address matches the pattern.
    */
   private boolean matchAddress(InetAddress addr, String tok) {
      if (tok.indexOf("/") < 0) {
         // Just an IP address, no mask.
         try {
            return addr.equals(InetAddress.getByName(tok));
         } catch (Exception e) {
            return false;
         }
      } else {
         try {
            byte[] addr1 = addr.getAddress();
            byte[] addr2 = InetAddress.getByName(tok.substring(0,
               tok.indexOf("/"))).getAddress();
            if (addr1.length != addr2.length)
               return false;
            byte[] mask = null;
            String maskStr = tok.substring(tok.indexOf("/")+1);
            if (maskStr.indexOf(".") > 0 || maskStr.indexOf(":") > 0)
               mask = InetAddress.getByName(maskStr).getAddress();
            else {
               mask = new byte[addr2.length];
               int bits = Integer.parseInt(maskStr);
               for (int i = 0, j = 0; --bits >= 0 && i < mask.length; ) {
                  mask[i] |= (byte) (0x80 >>> j);
                  if (j == 7) {
                     i++;
                     j = 0;
                  } else j++;
               }
            }
            for (int i = 0; i < addr1.length; i++)
               if ((addr1[i] & mask[i]) != (addr2[i] & mask[i]))
                  return false;
            return true;
         } catch (Exception e) {
            return false;
         }
      }
   }
}
