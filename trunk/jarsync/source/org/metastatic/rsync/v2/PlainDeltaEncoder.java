/* PlainDeltaEncoder -- uncompressed delta encoder.
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

import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.DataBlock;
import org.metastatic.rsync.Delta;
import org.metastatic.rsync.DeltaEncoder;
import org.metastatic.rsync.Offsets;

public class PlainDeltaEncoder extends DeltaEncoder
{

  // Constructor.
  // -------------------------------------------------------------------------

  public PlainDeltaEncoder(Configuration config, OutputStream out)
  {
    super(config, out);
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public void write(Delta delta) throws IOException
  {
    if (delta instanceof Offsets)
      {
        int token = (int) (((Offsets) delta).getOldOffset() / config.blockLength);
        token = -(token + 1);
        out.write(token & 0xFF);
        out.write((token >>>  8) & 0xFF);
        out.write((token >>> 16) & 0xFF);
        out.write(token >>> 24);
      }
    else if (delta instanceof DataBlock)
      {
        int len = delta.getBlockLength();
        out.write(len & 0xFF);
        out.write((len >>>  8) & 0xFF);
        out.write((len >>> 16) & 0xFF);
        out.write(len >>> 24);
        out.write(((DataBlock) delta).getData());
      }
    else
      throw new IllegalArgumentException();
  }

  public void doFinal() throws IOException
  {
    out.write(0);
    out.write(0);
    out.write(0);
    out.write(0);
  }

  public boolean requiresOrder()
  {
    return true;
  }
}
