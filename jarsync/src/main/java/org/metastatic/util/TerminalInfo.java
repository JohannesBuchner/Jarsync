// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
// 
// TerminalInfo -- Abstract superclass for terminal info.
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

import java.util.BitSet;
import java.util.HashMap;

/**
 * A terminal is an informational-only collection of relevant values for
 * a character display terminal. It contains information such as control
 * characters, I/O options, and on-screen dimensions. It is up to
 * subclasses to construct this information, as well as how the various
 * options are interpreted.
 *
 * @version $Revision$
 */
public abstract class TerminalInfo {

   /* control characters */
   public static final int _VINTR    =  0;
   public static final int _VQUIT    =  1;
   public static final int _VERASE   =  2;
   public static final int _VKILL    =  3;
   public static final int _VEOF     =  4;
   public static final int _VTIME    =  5;
   public static final int _VMIN     =  6; 
   public static final int _VSWTCH   =  7;
   public static final int _VSTART   =  8;
   public static final int _VSTOP    =  9;
   public static final int _VSUSP    = 10;
   public static final int _VEOL     = 11;
   public static final int _VREPRINT = 12;
   public static final int _VDISCARD = 13;
   public static final int _VWERASE  = 14;
   public static final int _VLNEXT   = 15;
   public static final int _VEOL2    = 16;
   public static final int _VDSUSP   = 17;

   /* input flags */
   public static final int _IGNBRK  =  0;
   public static final int _BRKINT  =  1;
   public static final int _IGNPAR  =  2;
   public static final int _PARMRK  =  3;
   public static final int _INPCK   =  4;
   public static final int _ISTRIP  =  5;
   public static final int _INLCR   =  6;
   public static final int _IGNCR   =  7;
   public static final int _ICRNL   =  8;
   public static final int _IUCLC   =  9;
   public static final int _IXON    = 10;
   public static final int _IXANY   = 11;
   public static final int _IXOFF   = 12;
   public static final int _IMAXBEL = 13;

   /* output flags */
   public static final int _OPOST  = 14;
   public static final int _OLCUC  = 15;
   public static final int _ONLCR  = 16;
   public static final int _OCRNL  = 17;
   public static final int _ONOCR  = 18;
   public static final int _ONLRET = 19;
   public static final int _OFILL  = 20;
   public static final int _OFDEL  = 21;
   public static final int _NLDLY  = 22;
   public static final int _NL0    = 23;
   public static final int _NL1    = 24;
   public static final int _CRDLY  = 25;
   public static final int _CR0    = 26;
   public static final int _CR1    = 27;
   public static final int _CR2    = 28;
   public static final int _CR3    = 29;
   public static final int _TABDLY = 30;
   public static final int _TAB0   = 31;
   public static final int _TAB1   = 32;
   public static final int _TAB2   = 33;
   public static final int _TAB3   = 34;
   public static final int _BSDLY  = 35;
   public static final int _BS0    = 36;
   public static final int _BS1    = 37;
   public static final int _FFDLY  = 38;
   public static final int _FF0    = 39;
   public static final int _FF1    = 40;

   /* control flags */
   public static final int _CSIZE   = 41;
   public static final int _CS5     = 42;
   public static final int _CS6     = 43;
   public static final int _CS7     = 44;
   public static final int _CS8     = 45;
   public static final int _CSTOPB  = 46;
   public static final int _CREAD   = 47;
   public static final int _PARENB  = 48;
   public static final int _PARODD  = 49;
   public static final int _HUPCL   = 50;
   public static final int _CLOCAL  = 51;
   public static final int _CIBAUD  = 52;
   public static final int _CRTSCTS = 53;

   /* local flags */
   public static final int _ISIG    = 54;
   public static final int _ICANON  = 55;
   public static final int _XCASE   = 56;
   public static final int _ECHO    = 57;
   public static final int _ECHOE   = 58;
   public static final int _ECHOK   = 59;
   public static final int _ECHONL  = 60;
   public static final int _ECHOCTL = 61;
   public static final int _ECHOPRT = 62;
   public static final int _ECHOKE  = 63;
   public static final int _FLUSHO  = 64;
   public static final int _NOFLSH  = 65;
   public static final int _TOSTOP  = 66;
   public static final int _PENDIN  = 67;
   public static final int _IEXTEN  = 68;

   /** The name of the underlying terminal. */
   protected String name;

   /** The input baud rate. */
   protected int in_baud;

   /** The output baud rate. */
   protected int out_baud;

   /** The set of input modes. */
   protected BitSet modes;

   /** The control characters. */
   protected HashMap control_chars;

   // Constructors.
   // -----------------------------------------------------------------------

   /**
    * @param name The name of this terminal.
    */
   protected TerminalInfo(String name) {
      this.name = name;
      modes = new BitSet(69);
      control_chars = new HashMap(18);
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   /**
    * Return this terminal's name.
    *
    * @return This terminal's name.
    */
   public String getName() {
      return name;
   }

   /**
    * Return the input baud rate.
    *
    * @param The input baud rate.
    */
   public int getBaudIn() {
      return in_baud;
   }

   /**
    * Return the output baud rate.
    *
    * @param The output baud rate.
    */
   public int getBaudOut() {
      return out_baud;
   }

   /**
    * Return whether or not a flag is set.
    *
    * @param flag The flag to check.
    * @return true If the flag is set.
    */
   public boolean isSet(int flag) {
      return modes.get(flag);
   }

   /**
    * Set the given flag (make it true).
    *
    * @param flag The flat to set.
    */
   public void set(int flag) {
      modes.set(flag);
   }

   /**
    * Clear the given flag (make it false).
    *
    * @param flag The flag to clear.
    */
   public void clear(int flag) {
      modes.clear(flag);
   }

   /**
    * Get the control character for the given type.
    *
    * @param type The type of control character to get.
    * @return The control character.
    */
   public Character controlChar(int type) {
      return (Character) control_chars.get(new Integer(type));
   }

   /**
    * Set a control character.
    *
    * @param type The type of control character to set.
    * @param c    The character.
    */
   public void setControlChar(int type, char c) {
      control_chars.put(new Integer(type), new Character(c));
   }

   /**
    * Check if this terminal's size has changed.
    *
    * @return true If the terminal's on-screen size has changed.
    */
   public boolean dimensionChanged() {
      return false;
   }

   /**
    * Get the size of this terminal, in characters.
    *
    * @return The size, in characters.
    */
   public java.awt.Dimension getSizeChars() {
      return new java.awt.Dimension(0, 0);
   }

   /**
    * Get the size of this terminal, in pixels.
    *
    * @return The size, in pixels.
    */
   public java.awt.Dimension getSizePixels() {
      return new java.awt.Dimension(0, 0);
   }

   public String toString() {
      return "name=" + name + " baud(in=" + in_baud + ", out=" + out_baud +
         ")\n" + modes.toString() + "\n"
         + control_chars.toString();
   }
}
