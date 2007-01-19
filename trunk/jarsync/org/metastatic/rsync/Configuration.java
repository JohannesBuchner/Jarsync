/* Configuration -- Wrapper around configuration data.
   Copyright (C) 2003, 2007  Casey Marshall <rsdio@metastatic.org>

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
along with Jarsync; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

Linking Jarsync statically or dynamically with other modules is making
a combined work based on Jarsync.  Thus, the terms and conditions of
the GNU General Public License cover the whole combination.

As a special exception, the copyright holders of Jarsync give you
permission to link Jarsync with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on Jarsync.  If you modify Jarsync, you may extend this
exception to your version of it, but you are not obligated to do so.
If you do not wish to do so, delete this exception statement from your
version.

ALTERNATIVELY, Jarsync may be licensed under the Apache License,
Version 2.0 (the "License"); you may not use this file except in
compliance with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.  See the License for the specific language governing
permissions and limitations under the License.

If you modify Jarsync, you may extend this same choice of license for
your library, but you are not obligated to do so. If you do not offer
the same license terms, delete the license terms that your library is
NOT licensed under.  */


package org.metastatic.rsync;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A Configuration is a mere collection of objects and values that
 * compose a particular configuration for the algorithm, for example the
 * message digest that computes the strong checksum.
 *
 * <p>Usage of a Configuration involves setting the member fields of
 * this object to thier appropriate values; thus, it is up to the
 * programmer to specify the {@link #strongSum}, {@link #weakSum},
 * {@link #blockLength} and {@link #strongSumLength} to be used. The
 * other fields are optional.</p>
 *
 * @author Casey Marshall
 * @version $Revision$
 */
public class Configuration implements Cloneable, Serializable
{

  private static final long serialVersionUID = 8464824128888686020L;

  /**
   * The default block size.
   */
  public static final int BLOCK_LENGTH = 700;

  /**
   * The default chunk size.
   */
  public static final int CHUNK_SIZE = 32768;

  /**
   * The message digest that computes the stronger checksum.
   */
  public transient MessageDigest strongSum;

  /**
   * The rolling checksum.
   */
  public transient RollingChecksum weakSum;

  /**
   * The length of blocks to checksum.
   */
  public int blockLength;

  /**
   * The effective length of the strong sum.
   */
  public int strongSumLength;

  /**
   * Whether or not to do run-length encoding when making Deltas.
   */
  public boolean doRunLength;

  /**
   * The seed for the checksum, to perturb the strong checksum and help
   * avoid collisions in plain rsync (or in similar applicaitons).
   */
  public byte[] checksumSeed;

  /**
   * The maximum size of byte arrays to create, when they are needed.
   * This vale defaults to 32 kilobytes.
   */
  public int chunkSize;

  // Constructors.
  // ------------------------------------------------------------------------

  public Configuration() {
    blockLength = BLOCK_LENGTH;
    chunkSize = CHUNK_SIZE;
  }

  /**
   * Private copying constructor.
   */
  private Configuration(Configuration that)
  {
    try
    {
      this.strongSum = (MessageDigest) (that.strongSum != null
          ? that.strongSum.clone()
          : null);
    } catch (CloneNotSupportedException cnse) {
      try {
        this.strongSum =
          MessageDigest.getInstance(that.strongSum.getAlgorithm());
      } catch (NoSuchAlgorithmException nsae) {
        // Fucked up situation. We die now.
        throw new Error(nsae);
      }
    }
    this.weakSum = (RollingChecksum) (that.weakSum != null
        ? that.weakSum.clone()
        : null);
    this.blockLength = that.blockLength;
    this.doRunLength = that.doRunLength;
    this.strongSumLength = that.strongSumLength;
    this.checksumSeed = (byte[]) (that.checksumSeed != null
        ? that.checksumSeed.clone()
        : null);
    this.chunkSize = that.chunkSize;
  }

  // Instance methods.
  // -----------------------------------------------------------------------

  public Object clone()
  {
    return new Configuration(this);
  }

  // Serialization methods.
  // -----------------------------------------------------------------------

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeUTF(strongSum != null ? strongSum.getAlgorithm() : "NONE");
  }

  private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
  {
    in.defaultReadObject();
    String s = in.readUTF();
    if (!s.equals("NONE")) {
      try {
        strongSum = MessageDigest.getInstance(s);
      } catch (NoSuchAlgorithmException nsae) {
        InvalidObjectException ioe = new InvalidObjectException(s);
        ioe.initCause(nsae);
        throw ioe;
      }
    }
  }
}
