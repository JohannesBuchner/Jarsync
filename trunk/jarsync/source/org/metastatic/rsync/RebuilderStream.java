/* RebuilderStream: streaming file reconstructor.
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your
option) any later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License along
with Jarsync; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA

Linking Jarsync statically or dynamically with other modules is making a
combined work based on Jarsync.  Thus, the terms and conditions of the
GNU General Public License cover the whole combination.

As a special exception, the copyright holders of Jarsync give you
permission to link Jarsync with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under terms
of your choice, provided that you also meet, for each linked independent
module, the terms and conditions of the license of that module.  An
independent module is a module which is not derived from or based on
Jarsync.  If you modify Jarsync, you may extend this exception to your
version of it, but you are not obligated to do so.  If you do not wish
to do so, delete this exception statement from your version.  */


package org.metastatic.rsync;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A "streaming" alternative to {@link Rebuilder}. To use this class,
 * create an intsance with a file argument representing the file being
 * rebuilt. Then register one or more implementations of the {@link
 * RebuilderListener} interface, which will write the data to the new
 * file. Then call the {@link update(Delta)} method for each {@link
 * Delta} to be applied.
 *
 * <p>Note that unlike the {@link GeneratorStream} and {@link
 * MatcherStream} classes this class does not need a {@link
 * Configuration}, nor does it have any "doFinal" method -- it is
 * completely stateless (except for the file) and the operations are
 * finished when the last delta has been applied.
 *
 * <p>This class is optimal for situations where the deltas are coming
 * in a stream over a communications link, and when it would be
 * inefficient to wait until all deltas are received.
 * 
 */
public class RebuilderStream {

   // Fields.
   // -----------------------------------------------------------------------

   /** The basis file. */
   protected RandomAccessFile basisFile;

   /** The list of {@link RebuilderListener}s. */
   protected final LinkedList listeners;

   // Constructors.
   // -----------------------------------------------------------------------

   /**
    * Create a new rebuilder.
    */
   public RebuilderStream() {
      listeners = new LinkedList();
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   /**
    * Add a RebuilderListener listener to this rebuilder.
    *
    * @param listener The listener to add.
    * @throws IllegalArgumentException If <i>listener</i> is null.
    */
   public void addListener(RebuilderListener listener) {
      if (listener == null)
         throw new IllegalArgumentException();
      listeners.add(listener);
   }

   /**
    * Remove a listener from this rebuilder.
    *
    * @param listener The listener to remove.
    */
   public void removeListener(RebuilderListener listener) {
      listeners.remove(listener);
   }

   /**
    * Set the basis file.
    *
    * @param file The basis file.
    * @throws IOException If the file is not readable.
    */
   public void setBasisFile(File file) throws IOException {
      if (basisFile != null) {
         basisFile.close();
         basisFile = null;  
      }
      if (file != null)
         basisFile = new RandomAccessFile(file, "r");
   }

   /**
    * Set the basis file.
    *
    * @param file The basis file name.
    * @throws IOException If the file name is not the name of a readable file.
    */
   public void setBasisFile(String file) throws IOException {
      if (basisFile != null) {
         basisFile.close();
         basisFile = null;
      }
      if (file != null)
         basisFile = new RandomAccessFile(file, "r");
   }

   /**
    * Update this rebuilder with a delta.
    *
    * @param delta The delta to apply.
    * @throws IOException If there is an error reading from the basis
    *    file, or if no basis file has been specified.
    */
   public void update(Delta delta) throws IOException, ListenerException {
      ListenerException exception = null, current = null;
      RebuilderEvent e = null;
      if (delta instanceof DataBlock) {
         e = new RebuilderEvent(((DataBlock) delta).getData(),
             delta.getWriteOffset());
      } else {
         if (basisFile == null)
            throw new IOException("offsets found but no basis file specified");
         byte[] buf = new byte[delta.getBlockLength()];
         basisFile.seek(((Offsets) delta).getOldOffset());
         basisFile.readFully(buf);
         e = new RebuilderEvent(buf, delta.getWriteOffset());
      }
      for (Iterator i = listeners.iterator(); i.hasNext(); ) {
         try {
            ((RebuilderListener) i.next()).update(e);
         } catch (ListenerException le) {
            if (exception != null) {
               current.setNext(le);
               current = le;
            } else {
               exception = le;
               current = le;
            }
         }
      }
      if (exception != null)
         throw exception;
   }
}
