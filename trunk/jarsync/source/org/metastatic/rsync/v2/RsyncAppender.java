/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   RsyncAppender: Multiplexes log messages.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync.

   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2 of the License, or (at
   your option) any later version.

   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jarsync; if not, write to the

      Free Software Foundation, Inc.,
      59 Temple Place, Suite 330,
      Boston, MA  02111-1307
      USA  */

package org.metastatic.rsync.v2;

import java.io.IOException;
import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

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
   private final MultiplexedOutputStream out;

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

   // Instance methods defining AppenderSkeleton.
   // -----------------------------------------------------------------------

   public void close() {
      closed = true;
   }

   public boolean requiresLayout() {
      return true;
   }

   protected void append(LoggingEvent e) {
      try {
         if (e.getLevel().isGreaterOrEqual(Level.WARN)) {
            out.writeMessage(out.FERROR, layout.format(e));
         } else {
            out.writeMessage(out.FINFO, layout.format(e));
         }
      } catch (IOException ioe) {
      }
   }
}
