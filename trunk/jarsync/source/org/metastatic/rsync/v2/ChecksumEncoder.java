/* ChecksumEncoder -- rsync protocol checksum encoder.
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

import java.io.IOException;
import java.io.OutputStream;

import org.metastatic.rsync.ChecksumPair;
import org.metastatic.rsync.Configuration;

public class ChecksumEncoder extends org.metastatic.rsync.ChecksumEncoder
{

  // Constructor.
  // -------------------------------------------------------------------------

  public ChecksumEncoder(Configuration config, OutputStream out)
  {
    super(config, out);
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public void write(ChecksumPair pair) throws IOException
  {
    int weak = pair.getWeak();
    out.write(weak & 0xFF);
    out.write((weak >>>  8) & 0xFF);
    out.write((weak >>> 16) & 0xFF);
    out.write((weak >>> 24));
    out.write(pair.getStrong());
  }

  public void doFinal()
  {
    // Empty.
  }

  public boolean requiresOrder()
  {
    return true;
  }
}
