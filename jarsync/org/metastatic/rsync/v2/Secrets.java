/* Secrets -- a collection of passwords.
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

package org.metastatic.rsync.v2;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class Secrets
{

  // Fields.
  // -------------------------------------------------------------------------

  private static final Logger logger = Logger.getLogger(Secrets.class.getName());

  private HashMap secrets;

  // Constructor.
  // -------------------------------------------------------------------------

  public Secrets(String filename) throws IOException
  {
    secrets = new HashMap();
    LineNumberReader in = new LineNumberReader(new FileReader(filename));
    String line = null;
    while ((line = in.readLine()) != null)
      {
        int i = 0;
        if ((i = line.indexOf(':')) < 0)
          {
            logger.error(filename + ": bad secret on line " + in.getLineNumber());
            continue;
          }
        String alias = line.substring(0, i);
        char[] pass = line.substring(i+1).toCharArray();
        secrets.put(alias, pass);
      }
    in.close();
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public char[] getPassword(String alias)
  {
    return (char[]) secrets.get(alias);
  }

  public void destroy()
  {
    for (Iterator i = secrets.values().iterator(); i.hasNext(); )
      {
        char[] c = (char[]) i.next();
        Arrays.fill(c, (char) 0);
      }
  }
}
