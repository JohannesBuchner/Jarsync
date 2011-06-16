// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
// 
// UnixTerminalInfo -- A (pseudo) terminal on a UNIX machine. 
// Copyright 2002  Casey Marshall <rsdio@metastatic.org>
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// --------------------------------------------------------------------------

package org.metastatic.util;

import java.awt.Dimension;

/**
 * This class represents information about a character display terminal
 * on a UNIX-like system, and thus has the <code>termios(3)</code>
 * functions that we can call natively.
 *
 * @version $Revision$
 */
public class UnixTerminalInfo extends TerminalInfo {
   Dimension chars;
   Dimension pixels;

   public UnixTerminalInfo(String name) {
      super(name);
      chars = new Dimension();
      pixels = new Dimension();
      init();
   }

   public Dimension getSizeChars() {
      doGetSizeChars(chars);
      return chars;
   }

   public Dimension getSizePixels() {
      doGetSizePixels(pixels);
      return pixels;
   }

   public boolean dimensionChanged() {
      return doDimensionChanged();
   }

   private native void init();
   private native boolean doDimensionChanged();
   private native void doGetSizeChars(Dimension chars);
   private native void doGetSizePixels(Dimension pixels);
}
