/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
  $Id$
  
  Glob: implements a unix-like glob(3).
  Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

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

import java.io.File;
import java.util.StringTokenizer;

public class Glob implements java.io.FilenameFilter {

   // Constants and fields.
   // -----------------------------------------------------------------------

   private final String patterns;

   private final boolean result;

   private final String baseDir;

   // Constructor.
   // -----------------------------------------------------------------------

   public Glob(String patterns, boolean result, String baseDir) {
      this.patterns = patterns;
      this.result = result;
      if (baseDir.endsWith(File.separator + "."))
         this.baseDir = baseDir.substring(0, baseDir.length()-2);
      else if (baseDir.endsWith(File.separator))
         this.baseDir = baseDir.substring(0, baseDir.length()-1);
      else
         this.baseDir = baseDir;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public boolean accept(File dir, String name) {
      StringTokenizer tok = new StringTokenizer(patterns, " \t,");
      String path = dir.getPath();
      if (!path.endsWith(File.separator))
         path += File.separator;
      path += name;
      if (path.startsWith(baseDir)) {
         path = path.substring(baseDir.length());
      }
      while (tok.hasMoreTokens()) {
         String pattern = tok.nextToken();
         if (FNMatch.fnmatch(pattern, path,
             FNMatch.FNM_PERIOD|FNMatch.FNM_FILE_NAME|FNMatch.FNM_LEADING_DIR))
         {
            return result;
         }
      }
      return !result;
   }
}
