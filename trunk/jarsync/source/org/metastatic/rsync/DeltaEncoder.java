/* DeltaEncoder -- encodes Delta objects to external representations.
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
import java.io.OutputStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.Iterator;
import java.util.List;

/**
 * The superclass of objects that encode sets of deltas to external
 * representations, such as the over-the-wire format of rsync or the
 * rdiff file format.
 *
 * <p>Subclasses MAY define themselves to be accessable through the
 * {@link #getInstance(java.lang.String,java.io.OutputStream)} method
 * by providing a one-argument constructor that accepts an {@link
 * OutputStream} and defining the system property
 * "jarsync.deltaEncoder.<i>encoding-name</i>".
 */
public abstract class DeltaEncoder {

  // Constants and fields.
  // -------------------------------------------------------------------------

  public static final String PROPERTY = "jarsync.deltaEncoder.";

  /**
   * The configuration.
   */
  protected Configuration config;

  /**
   * The output stream.
   */
  protected OutputStream out;

  // Constructors.
  // -------------------------------------------------------------------------

  /**
   * Creates a new delta encoder.
   *
   * @param config The configuration.
   * @param out The output stream to write the data to.
   */
  public DeltaEncoder(Configuration config, OutputStream out) {
    this.config = (Configuration) config.clone();
    this.out = out;
  }

  // Class methods.
  // -------------------------------------------------------------------------

  /**
   * Returns a new instance of the specified encoder.
   *
   * @throws IllegalArgumentException If there is no appropriate
   * encoder available.
   */
  public static final DeltaEncoder getInstance(String encoding,
                                               Configuration config,
                                               OutputStream out) {
    if (encoding == null || config == null || out == null)
      throw new NullPointerException();
    if (encoding.length() == 0)
      throw new IllegalArgumentException();
    try {
      Class clazz = Class.forName(System.getProperty(PROPERTY + encoding));
      if (!DeltaEncoder.class.isAssignableFrom(clazz))
        throw new IllegalArgumentException(clazz.getName() +
                                           ": not a subclass of " +
                                           DeltaEncoder.class.getName());
      Constructor c = clazz.getConstructor(new Class[] { Configuration.class,
                                                         OutputStream.class });
      return (DeltaEncoder) c.newInstance(new Object[] { config, out } );
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
   * Write (encode) a list of deltas to the output stream. This method does
   * <b>not</b> call {@link #doFinal()}.
   *
   * <p>This method checks every element of the supplied list to ensure that
   * all are either non-null or implement the {@link Delta} interface, before
   * writing any data.
   *
   * @param deltas The list of deltas to write.
   * @throws IOException If an I/O error occurs.
   * @throws IllegalArgumentException If any element of the list is not
   *   a {@link Delta}.
   * @throws NullPointerException If any element is null.
   */
  public void write(List deltas) throws IOException {
    for (Iterator i = deltas.iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (o == null)
        throw new NullPointerException();
      if (!(o instanceof Delta))
        throw new IllegalArgumentException();
    }
    for (Iterator i = deltas.iterator(); i.hasNext(); )
      write((Delta) i.next());
  }

  // Abstract methods.
  // -----------------------------------------------------------------------

  /**
   * Write (encode) a single delta to the output stream.
   *
   * @param d The delta to write.
   * @throws IOException If an I/O error occurs.
   */
  public abstract void write(Delta d) throws IOException;

  /**
   * Finish encoding the deltas (at least, this set of deltas) and write any
   * encoding-specific end-of-deltas entity.
   *
   * @throws IOException If an I/O error occurs.
   */
  public abstract void doFinal() throws IOException;

  /**
   * Returns whether or not this encoder requires the deltas it is
   * presented to be in <i>write offset</i> order, that is, the deltas
   * passed to the <code>write</code> methods <b>must</b> be presented
   * in increasing order of their {@link Delta#getWriteOffset()}
   * values.
   *
   * @return True if this encoder requires write order.
   */
  public abstract boolean requiresOrder();
}
