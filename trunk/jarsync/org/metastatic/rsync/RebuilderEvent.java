/* RebuilderEvent -- file rebuilding event.
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

/**
 * a rebuilder event. Rebuilder events are emitted by a {@link
 * RebuilderStream} each time a new {@link Delta} is applied. The stream
 * will send this event to each of its {@link RebuilderListener}s.
 *
 * @see RebuilderStream
 * @see RebuilderListener
 */
public class RebuilderEvent extends java.util.EventObject
{

  // Fields.
  // -------------------------------------------------------------------------

  /** The destination offset. */
  protected transient long offset;

  // Constructors.
  // -------------------------------------------------------------------------

  /**
   * Create a new rebuilder event.
   *
   * @param data   The source of this event, the data block.
   * @param offset The destination offset.
   */
  public RebuilderEvent(byte[] data, long offset)
  {
    this(data, 0, data.length, offset);
  }

  public RebuilderEvent(byte[] data, int off, int len, long offset)
  {
    super(new byte[len]);
    System.arraycopy(data, off, source, 0, len);
    this.offset = offset;
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  /**
   * Get the data. This method is equivalent to {@link
   * java.util.EventObject#getSource()} but the source is already cast
   * for convenience.
   *
   * @return The data array.
   */
  public byte[] getData()
  {
    return (byte[]) source;
  }

  /**
   * Get the offset at which the data should be written.
   *
   * @return The offset.
   */
  public long getOffset()
  {
    return offset;
  }
}
