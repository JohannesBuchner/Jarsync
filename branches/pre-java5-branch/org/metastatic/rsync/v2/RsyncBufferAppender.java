/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$
  
   RsyncBufferAppender: Multiplexes log messages for NIO.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>
  
   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.
  
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
  
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the
  
      Free Software Foundation, Inc.,
      59 Temple Place, Suite 330,
      Boston, MA  02111-1307
      USA  */
  
package org.metastatic.rsync.v2;

import java.io.IOException;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Concrete Appender implementation that sends logging information over
 * the wire on nonblocking multiplexed data stream. This is only used if
 * we are the "server" in the transaction.
 *
 * @version $Revision$
 */
final class RsyncBufferAppender extends AppenderSkeleton {

   // Fields.
   // -----------------------------------------------------------------------

   /**
    * The multiplexed stream we write messages to.
    */
   private final DuplexByteBuffer out;

   private final Layout errLayout;

   private final Layout infoLayout;

   // Constructors.
   // -----------------------------------------------------------------------

   /**
    * Create a new RsyncBufferAppender. Messages that are logged to this
    * appender will be written to the {@link
    * DuplexByteBuffer#putString(int,java.lang.String)} method
    * of <i>out</i>.
    *
    * @param out The output stream to log messages to.
    */
   RsyncBufferAppender(DuplexByteBuffer out) {
      this.out = out;
      errLayout = new PatternLayout("%m (at %F:%L)\n");
      infoLayout = new PatternLayout("%m\n");
   }

   // Instance methods defining AppenderSkeleton.
   // -----------------------------------------------------------------------

   public void close() {
      closed = true;
   }

   public boolean requiresLayout() {
      return false;
   }

   protected void append(LoggingEvent e) {
      if (e.getLevel().isGreaterOrEqual(Level.WARN)) {
         out.putString(out.FERROR, errLayout.format(e));
      } else if (e.getLevel().equals(Level.INFO)) {
         out.putString(out.FINFO, infoLayout.format(e));
      } // else don't log this message here
   }
}
