// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// RsyncAppender -- Multiplexes log messages.
// Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the
//
//    Free Software Foundation, Inc.,
//    59 Temple Place, Suite 330,
//    Boston, MA  02111-1307
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

import org.apache.log4j.*;

/**
 * Concrete Appender implementation that sends logging information over
 * the wire on the multiplexed data stream. This is only used if we are
 * the "server" in the transaction.
 *
 * @version $Revision$
 */
final class RsyncAppender extends AppenderSkeleton {

   // Fields.
   // -----------------------------------------------------------------------

   /**
    * The multiplexed stream we write messages to.
    */
   private MultiplexedOutputStream out;

   // Constructors.
   // -----------------------------------------------------------------------

   /**
    * Create a new RsyncAppender. Messages that are logged to this
    * appender will be written to the {@link
    * MultiplexedOutputStream#writeMessage(int,java.lang.String)} method
    * of <i>out</i>.
    *
    * @param out The output stream to log messages to.
    */
   RsyncAppender(MultiplexedOutputStream out) {
      this.out = out;
   }

   // Instance method defining AppenderSkeleton.
   // -----------------------------------------------------------------------

   protected void append(LoggingEvent e) {
      try {
         if (e.getLevel().isGreaterOrEqual(Level.WARN)) {
            out.writeMessage(out.FERROR, e.getMessage().toString());
         } else {
            out.writeMessage(out.FINFO, e.getMessage().toString());
         }
      } catch (IOException ioe) {
      }
   }
}
