/* Target.java -- a sigle web page being monitored.
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

import java.net.URL;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.metastatic.rsync.Configuration;

public class Target {

   // Fields.
   // -----------------------------------------------------------------------

   String name;
   URL url;
   int frequency;
   Configuration config;

   // Constructor.
   // -----------------------------------------------------------------------

   public Target(String name) {
      this.name = name;
      config = new Configuration();
      frequency = 1;
      try {
         config.strongSum = MessageDigest.getInstance("MD4");
      } catch (NoSuchAlgorithmException nsae) {
         throw new Error(nsae);
      }
      config.strongSumLength = config.strongSum.getDigestLength();
   }
}
