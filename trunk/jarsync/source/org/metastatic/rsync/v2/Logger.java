// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// Logger -- Log rsync messages.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of Jarsync.
//
// Jarsync is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2, or (at your option) any
// later version.
//
// Jarsync is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Jarsync; see the file COPYING.  If not, write to the
//
//    Free Software Foundation Inc.,
//    59 Temple Place - Suite 330,
//    Boston, MA 02111-1307
//    USA
//
// Linking this library statically or dynamically with other modules is
// making a combined work based on this library.  Thus, the terms and
// conditions of the GNU General Public License cover the whole
// combination.
//
// As a special exception, the copyright holders of this library give
// you permission to link this library with independent modules to
// produce an executable, regardless of the license terms of these
// independent modules, and to copy and distribute the resulting
// executable under terms of your choice, provided that you also meet,
// for each linked independent module, the terms and conditions of the
// license of that module.  An independent module is a module which is
// not derived from or based on this library.  If you modify this
// library, you may extend this exception to your version of the
// library, but you are not obligated to do so.  If you do not wish to
// do so, delete this exception statement from your version.
//
// --------------------------------------------------------------------------

package org.metastatic.rsync.v2;

import java.io.OutputStream;

import java.util.HashSet;
import java.util.Iterator;

import org.metastatic.rsync.RsyncConstants;

/**
 * Static methods for sending informational and error messages to a
 * global list of output streams.
 *
 * @version $Revision$
 */
public final class Logger {

   // Constants and variables.
   // -----------------------------------------------------------------------

   /** FINFO output streams. */
   private static final HashSet infoStreams = new HashSet();

   /** FERROR output streams. */
   private static final HashSet errorStreams = new HashSet();

   // Constructors.
   // -----------------------------------------------------------------------

   /** Cannot be instantiated. */
   private Logger() { }

   // Class methods.
   // -----------------------------------------------------------------------

   /**
    * Write the specified bytes to either all info streams or all error
    * streams, depending on the value of <tt>logcode</tt>. Any I/O
    * exceptions are silently ignored.
    *
    * @param logcode One of {@link RsyncConstants#FINFO} or {@link
    *    RsyncConstants#FERROR}.
    * @param b The bytes to write.
    */
   public static synchronized void write(int logcode, byte[] b) {
      if (logcode == RsyncConstants.FINFO) {
         for (Iterator i = infoStreams.iterator(); i.hasNext(); ) {
            try {
               ((OutputStream) i.next()).write(b);
            } catch (Exception e) { /* ignored; failures are ok. */ }
         }
      } else if (logcode == RsyncConstants.FERROR) {
         for (Iterator i = errorStreams.iterator(); i.hasNext(); ) {
            try {
               ((OutputStream) i.next()).write(b);
            } catch (Exception e) { /* likewise. */ }
         }
      }
   }

   /**
    * Add an output stream to which to send {@link MultplexedIO#FINFO}
    * messages. This method returns true iff <tt>out</tt> was not
    * already added to the info streams.
    * 
    * @param out The output stream to add.
    * @return true If the stream was not previously added.
    */
   public static boolean addInfoStream(OutputStream out) {
      return infoStreams.add(out);
   }

   /**
    * Remove an info stream. Returns true if the output stream was one
    * of the info streams to begin with.
    *
    * @param out The output stream to remove.
    * @return true If the output stream was removed.
    */
   public static boolean removeInfoStream(OutputStream out) {
      return infoStreams.remove(out);
   }

   /**
    * Add an output stream to which to send {@link MultplexedIO#FERROR}
    * messages. This method returns true iff <tt>out</tt> was not
    * previously added to the error streams.
    * 
    * @param err The output stream to add.
    * @return true If the stream was not previously added.
    */
   public static boolean addErrorStream(OutputStream err) {
      return errorStreams.add(err);
   }

   /**
    * Remove an error stream. Returns true if the output stream was one
    * of the error streams to begin with.
    *
    * @param err The output stream to remove.
    * @return true If the output stream was removed.
    */
   public static boolean removeErrorStream(OutputStream err) {
      return errorStreams.remove(err);
   }
}
