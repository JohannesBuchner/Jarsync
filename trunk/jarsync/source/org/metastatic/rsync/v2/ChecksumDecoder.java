/* ChecksumDecoder -- rsync protocol checksum decoder.
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

import java.io.InputStream;
import java.io.IOException;

import java.util.List;

import org.metastatic.rsync.ChecksumPair;
import org.metastatic.rsync.Configuration;

public class ChecksumDecoder extends org.metastatic.rsync.ChecksumDecoder
{

  // Fields.
  // -------------------------------------------------------------------------

  private long offset;

  // Constructor.
  // -------------------------------------------------------------------------

  public ChecksumDecoder(Configuration config, InputStream in)
  {
    super(config, in);
    offset = 0;
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public int read(List sums)
  {
    throw new UnsupportedOperationException("rsync protocol does not have end-of-checksums marker");
  }

  public ChecksumPair read() throws IOException
  {
    int weak = (in.read() & 0xFF)       | (in.read() & 0xFF) <<  8
             | (in.read() & 0xFF) << 16 | (in.read() & 0xFF) << 24;
    byte[] strong = new byte[config.strongSumLength];
    in.read(strong);
    ChecksumPair pair = new ChecksumPair(weak, strong, offset);
    offset += config.blockLength;
    return pair;
  }
}
