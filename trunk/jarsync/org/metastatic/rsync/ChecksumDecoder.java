/* ChecksumDecoder -- decodes checksums from external representations.
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
   USA

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
version.  */


package org.metastatic.rsync;

import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.List;

/**
 * The base class of objects that decode (internalize) checksum pairs
 * from byte streams.
 *
 * @version $Revision$
 */
public abstract class ChecksumDecoder {

  // Constants and fields.
  // -------------------------------------------------------------------------

  /**
   * Property prefix for checksum encoders.
   */
  public static final String PROPERTY = "jarsync.checksumDecoder.";

  /**
   * The configuration object.
   */
  protected Configuration config;

  /**
   * The input stream being read from.
   */
  protected InputStream in;

  // Constructor.
  // -------------------------------------------------------------------------

  public ChecksumDecoder(Configuration config, InputStream in) {
    this.config = (Configuration) config.clone();
    this.in = in;
  }

  // Class method.
  // -------------------------------------------------------------------------

  /**
   * Gets an instance of a checksum decoder for the specified
   * encoding.
   *
   * @param encoding The encoding name.
   * @param config The configuration object.
   * @param in The input stream.
   * @throws NullPointerException If any parameter is null.
   * @throws IllegalArgumentException If the specified encoding cannot
   *   be found, or if any of the arguments are inappropriate.
   */
  public static ChecksumEncoder getInstance(String encoding,
                                            Configuration config,
                                            InputStream in)
  {
    if (encoding == null || config == null || in == null)
      throw new NullPointerException();
    if (encoding.length() == 0)
      throw new IllegalArgumentException();
    try {
      Class clazz = Class.forName(System.getProperty(PROPERTY + encoding));
      if (!ChecksumEncoder.class.isAssignableFrom(clazz))
        throw new IllegalArgumentException(clazz.getName() +
                                           ": not a subclass of " +
                                           ChecksumEncoder.class.getName());
      Constructor c = clazz.getConstructor(new Class[] { Configuration.class,
                                                         InputStream.class });
      return (ChecksumEncoder) c.newInstance(new Object[] { config, in } );
    } catch (ClassNotFoundException cnfe) {
      throw new IllegalArgumentException("class not found: " +
                                         cnfe.getMessage());
    } catch (NoSuchMethodException nsme) {
      throw new IllegalArgumentException("subclass has no constructor");
    } catch (InvocationTargetException ite) {
      throw new IllegalArgumentException(ite.getMessage());
    } catch (InstantiationException ie) {
      throw new IllegalArgumentException(ie.getMessage());
    } catch (IllegalAccessException iae) {
      throw new IllegalArgumentException(iae.getMessage());
    }
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  /**
   * Decodes checksums from the stream, storing them into the
   * specified list, until the end of checksums is encountered.
   *
   * @param sums The list to store the sums into.
   * @throws IOException If an I/O error occurs.
   * @throws NullPointerException If any element of the list is null.
   * @return The number of checksums read.
   */
  public int read(List sums) throws IOException
  {
    if (sums == null)
      throw new NullPointerException();
    int count = 0;
    ChecksumPair pair;
    while ((pair = read()) != null)
      {
        sums.add(pair);
        ++count;
      }
    return count;
  }

  // Abstract methods.
  // -------------------------------------------------------------------------

  /**
   * Decodes a checksum pair from the input stream.
   *
   * @return The pair read, or null if the end of stream is encountered.
   * @throws IOException If an I/O error occurs.
   */
  public abstract ChecksumPair read() throws IOException;
}
