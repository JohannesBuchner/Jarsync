/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   BufferTool: interface to generic nonblocking rsync operations.
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

import java.nio.ByteBuffer;

public interface BufferTool {

   /**
    * Update this tool with new input.
    *
    * @return true if there is still more to receive.
    */
   boolean updateInput() throws Exception;

   /**
    * Update this tool with new output.
    *
    * @return true if there is still more to send.
    */
   boolean updateOutput() throws Exception;

   /**
    * Set the buffers.
    */
   void setBuffers(DuplexByteBuffer out, ByteBuffer in);
}
