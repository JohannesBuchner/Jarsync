// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// Delta: An update to a file.
// Copyright (C) 2001,2002  Casey Marshall <rsdio@metastatic.org>
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
// --------------------------------------------------------------------

package org.metastatic.rsync;

/**
 * A Delta is, in the Rsync algorithm, one of two things: (1) a block
 * of bytes and an offset, or (2) a pair of offsets, one old and one
 * new.
 *
 * @version $Revision$
 */
public interface Delta {

   /**
    * The size of the block of data this class represents.
    *
    * @since 1.1
    * @return The size of the block of data this class represents.
    */
   public abstract int getBlockLength();
}
