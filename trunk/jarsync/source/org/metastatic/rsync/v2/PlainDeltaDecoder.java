/* PlainDeltaDecoder -- uncompressed delta decoder.
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

import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.DataBlock;
import org.metastatic.rsync.Delta;
import org.metastatic.rsync.DeltaDecoder;
import org.metastatic.rsync.Offsets;

public class PlainDeltaDecoder extends DeltaDecoder
{

  // Fields.
  // -------------------------------------------------------------------------

  private long offset;

  // Constructor.
  // -------------------------------------------------------------------------

  public PlainDeltaDecoder(Configuration config, InputStream in)
  {
    super(config, in);
    offset = 0;
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public Delta read() throws IOException
  {
    int token = in.read() | (in.read() << 8) | (in.read() << 16) | (in.read() << 24);
    if (token < 0)
      {
        long readOffset = (long) (-token - 1) * (long) config.blockLength;
        Offsets o = new Offsets(readOffset, offset, config.blockLength);
        offset += config.blockLength;
        return o;
      }
    else if (token > 0)
      {
        byte[] buf = new byte[token];
        in.read(buf);
        DataBlock d = new DataBlock(offset, buf);
        offset += config.blockLength;
        return d;
      }
    else
      return null; // end-of-deltas.
  }
}
