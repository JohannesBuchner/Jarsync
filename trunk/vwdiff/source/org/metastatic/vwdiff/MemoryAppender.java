/* MemoryAppender.java -- fixed-size memory log buffer.
   -*- mode: java; c-basic-offset: 3; -*-
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of vwdiff.

Vwdiff is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your
option) any later version.

Vwdiff is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License along
with vwdiff; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.vwdiff;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class MemoryAppender extends AppenderSkeleton {

   // Fields.
   // -----------------------------------------------------------------------

   private final LinkedList messages;

   private final int logsize;

   // Constructors.
   // -----------------------------------------------------------------------

   public MemoryAppender(int logsize, Layout layout) {
      messages = new LinkedList();
      this.logsize = logsize;
      this.layout = layout;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public List getLog() {
      return Collections.unmodifiableList(messages);
   }

   public void append(LoggingEvent event) {
      if (messages.size() == logsize)
         messages.removeFirst();
      messages.addLast(layout.format(event));
   }

   public void close() {
   }

   public boolean requiresLayout() {
      return true;
   }
}
