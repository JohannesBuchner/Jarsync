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

import org.apache.log4j.Logger;

import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.DataBlock;
import org.metastatic.rsync.Delta;
import org.metastatic.rsync.DeltaEncoder;
import org.metastatic.rsync.Offsets;
import org.metastatic.rsync.Util;

public class PlainDeltaEncoder extends DeltaEncoder
{

  private static final Logger logger = Logger.getLogger(PlainDeltaEncoder.class.getName());

  private final byte[] intBuf;

  // Constructor.
  // -------------------------------------------------------------------------

  public PlainDeltaEncoder(Configuration config, OutputStream out)
  {
    super(config, out);
    intBuf = new byte[4];
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public void write(Delta delta) throws IOException
  {
    if (delta instanceof Offsets)
      {
        int token = (int) (((Offsets) delta).getOldOffset() / config.blockLength);
        token = -(token + 1);
        logger.debug("writing token=" + token);
        writeInt(token);
      }
    else if (delta instanceof DataBlock)
      {
        int len = delta.getBlockLength();
        writeInt(len);
        out.write(((DataBlock) delta).getData());
      }
    else
      throw new IllegalArgumentException(delta.getClass().getName());
  }

  public void doFinal() throws IOException
  {
    writeInt(0);
  }

  public boolean requiresOrder()
  {
    return true;
  }

  private void writeInt(int i) throws IOException
  {
    intBuf[0] = (byte) i;
    intBuf[1] = (byte) (i >>>  8);
    intBuf[2] = (byte) (i >>> 16);
    intBuf[3] = (byte) (i >>> 24);
    out.write(intBuf);
  }
}
