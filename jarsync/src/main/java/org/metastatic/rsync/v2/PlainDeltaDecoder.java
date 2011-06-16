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

import org.apache.log4j.Logger;

import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.DataBlock;
import org.metastatic.rsync.Delta;
import org.metastatic.rsync.DeltaDecoder;
import org.metastatic.rsync.Offsets;
import org.metastatic.rsync.Util;

public class PlainDeltaDecoder extends DeltaDecoder
{

  // Fields.
  // -------------------------------------------------------------------------

  private static final Logger logger =
    Logger.getLogger(PlainDeltaDecoder.class.getName());

  private long offset;

  private Statistics stats;

  // Constructor.
  // -------------------------------------------------------------------------

  public PlainDeltaDecoder(Configuration config, InputStream in)
  {
    super(config, in);
    offset = 0;
    stats = new Statistics();
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public void setStatistics(Statistics stats)
  {
    if (stats != null) this.stats = stats;
  }

  public Statistics getStatistics()
  {
    return stats;
  }

  public Delta read() throws IOException
  {
    int token = in.read() & 0xFF;
    token |= (in.read() & 0xFF) <<  8;
    token |= (in.read() & 0xFF) << 16;
    token |= (in.read() & 0xFF) << 24;
    logger.debug("read token=" + token);
    if (token < 0)
      {
        long readOffset = (long) (-token - 1) * (long) config.blockLength;
        Offsets o = new Offsets(readOffset, offset, config.blockLength);
        logger.debug("decoded offsets=" + o);
        offset += config.blockLength;
        return o;
      }
    else if (token > 0)
      {
        byte[] buf = new byte[token];
        in.read(buf);
        DataBlock d = new DataBlock(offset, buf);
        logger.debug("decoded data block=" + d);
        offset += token;
        return d;
      }
    else
      return null; // end-of-deltas.
  }
}
