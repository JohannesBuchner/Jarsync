/* DeltaDecoder -- decodes Delta objects from external representations.
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
 * The superclass of all classes that decode delta objects from an
 * external, binary format.
 *
 * <p>Subclasses MAY define themselves to be accessable through the
 * {@link #getInstance(java.lang.String,java.io.InputStream)} method
 * by providing a one-argument constructor that accepts an {@link
 * InputStream} and defining the system property
 * "jarsync.deltaDecoder.<i>encoding-name</i>".
 */
public abstract class DeltaDecoder {

  // Constants and fields.
  // -------------------------------------------------------------------------

  public static final String PROPERTY = "jarsync.deltaDecoder.";

  /**
   * The configuration.
   */
  protected final Configuration config;

  /**
   * The underlying input stream.
   */
  protected final InputStream in;

  // Constructors.
  // -------------------------------------------------------------------------

  public DeltaDecoder(Configuration config, InputStream in) {
    this.config = (Configuration) config.clone();
    this.in = in;
  }

  // Class methods.
  // -------------------------------------------------------------------------

  /**
   * Returns a new instance of the specified decoder.
   *
   * @param encoding The name of the decoder to get.
   * @param config   The configuration to use.
   * @param in       The source of binary data.
   * @return The new decoder.
   * @throws NullPointerException If any parameter is null.
   * @throws IllegalArgumentException If there is no appropriate
   *   decoder available.
   */
  public static final DeltaDecoder getInstance(String encoding,
                                               Configuration config,
                                               InputStream in)
  {
    if (encoding == null || config == null || in == null)
      throw new NullPointerException();
    if (encoding.length() == 0)
      throw new IllegalArgumentException();
    try {
      Class clazz = Class.forName(System.getProperty(PROPERTY + encoding));
      if (!DeltaDecoder.class.isAssignableFrom(clazz))
        throw new IllegalArgumentException(clazz.getName() +
                                           ": not a subclass of " +
                                           DeltaDecoder.class.getName());
      Constructor c = clazz.getConstructor(new Class[] { Configuration.class,
                                                         InputStream.class });
      return (DeltaDecoder) c.newInstance(new Object[] { in } );
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
   * Read (decode) a list of deltas from the input stream.
   *
   * @param deltas The list of deltas to write.
   * @throws IOException If an I/O error occurs.
   * @throws IllegalArgumentException If any element of the list is not
   *   a {@link Delta}.
   * @throws NullPointerException If any element is null.
   */
  public int read(List deltas) throws IOException {
    int count = 0;
    Delta d = null;
    while ((d = read()) != null)
      {
        deltas.add(d);
        ++count;
      }
    return count;
  }

  // Abstract methods.
  // -------------------------------------------------------------------------

  /**
   * Read (decode) a single delta from the input stream.
   *
   * <p>If this encoding provides an end-of-deltas marker, then this method
   * is required to return <code>null</code> upon receiving this marker.
   *
   * @return The delta read, or <code>null</code>
   * @throws IOException If an I/O error occurs.
   */
  public abstract Delta read() throws IOException;
}
