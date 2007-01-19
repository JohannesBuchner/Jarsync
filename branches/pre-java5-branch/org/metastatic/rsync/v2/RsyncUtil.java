/* RsyncUtil -- basic rsync utilities.
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2, or (at your option) any
later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with Jarsync; see the file COPYING.  If not, write to the

   Free Software Foundation Inc.,
   59 Temple Place - Suite 330,
   Boston, MA 02111-1307
   USA  */

package org.metastatic.rsync.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.security.MessageDigest;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

class RsyncUtil implements Constants
{

  // Fields.
  // -------------------------------------------------------------------------

  // Constructor.
  // -------------------------------------------------------------------------

  private RsyncUtil() { }

  // Class methods.
  // -------------------------------------------------------------------------

  /**
   * Compute the MD4 checksum for a file, returning it in a new buffer.
   *
   * @param file The file to checksum.
   * @return The MD4 digest of the file.
   * @throws IOException If file cannot be read from.
   */
  static byte[] fileChecksum(File file) throws IOException
  {
    MessageDigest mdfour = null;
    try
      {
        mdfour = MessageDigest.getInstance("BrokenMD4");
      }
    catch (java.security.NoSuchAlgorithmException nse)
      {
        throw new Error(nse);
      }
    FileInputStream fin = new FileInputStream(file);
    byte[] buf = new byte[4096];
    int len;
    while ((len = fin.read(buf)) != -1)
      {
        mdfour.update(buf, 0, len);
      }
    return mdfour.digest();
  }

  /**
   * Compute the MD4 checksum for a file, returning it in a new buffer.
   *
   * @param fname The name of the file to checksum.
   * @return The MD4 digest of the file.
   * @throws IOException If the file cannot be read from.
   */
  static byte[] fileChecksum(String fname) throws IOException
  {
    MessageDigest mdfour = null;
    try
      {
        mdfour = MessageDigest.getInstance("BrokenMD4");
      }
    catch (java.security.NoSuchAlgorithmException nse)
      {
        throw new Error(nse);
      }
    FileInputStream fin = new FileInputStream(fname);
    byte[] buf = new byte[4096];
    int len;
    while ((len = fin.read(buf)) != -1)
      {
        mdfour.update(buf, 0, len);
      }
    return mdfour.digest();
  }

  /**
   * Remove all ".." and ".", returning the new path.
   *
   * @param path The path to sanitize.
   * @return The fixed path.
   */
  static String sanitizePath(String path)
  {
    StringTokenizer tok = new StringTokenizer(path, File.separator);
    LinkedList p = new LinkedList();
    while (tok.hasMoreTokens())
      {
        String s = tok.nextToken();
        if (s.equals("."))
          continue;
        if (s.equals(".."))
          {
            if (p.size() > 0)
              p.removeLast();
            continue;
          }
        p.addLast(s);
      }

    StringBuffer result = new StringBuffer();
    for (Iterator i = p.listIterator(); i.hasNext(); )
      {
        result.append((String) i.next());
        if (i.hasNext() || path.endsWith(File.separator))
          result.append(File.separator);
      }
    return result.toString();
  }

  static int adaptBlockSize(File file, int bsize)
  {
    if (bsize != BLOCK_LENGTH)
      return bsize;
    int ret = (int) (file.length() / 10000) & ~15;
    if (ret < bsize)
      return bsize;
    if (ret > CHUNK_SIZE/2)
      ret = CHUNK_SIZE/2;
    return ret;
  }
}
