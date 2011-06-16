/* Debug -- Print debugging messages.
   Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>

This file is a part of HUSH, the Hopefully Uncomprehensible Shell.

HUSH is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your
option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.net.ssh2;

import java.io.PrintStream;

/**
 * Logger-like class, but with only a single PrintStream for output and
 * simpler levels. This is merely an alternative to the java.util.logging
 * package for Java versions < 1.4.
 *
 * @version $Revision$
 */
public class Debug
{

  // Constants and variables.
  // -------------------------------------------------------------------------

  /** Logs nothing. */
  public static final int NONE    = 6;

  /** Log fatal messages. */
  public static final int FATAL   = 5;

  /** Warning messages. */
  public static final int WARNING = 4;

  /** Informational messages. */
  public static final int INFO    = 3;

  /** First-level debug messages. */
  public static final int DEBUG   = 2;

  /** Second-level debug messages. */
  public static final int DEBUG2  = 1;

  /** Third-level debug messages. */
  public static final int DEBUG3  = 0;

  /** Textual prefixes for messages. */
  static final String[] prefixes = new String[] {
    "DEBUG3", "DEBUG2", "DEBUG", "INFO", "WARNING", "FATAL"
  };

  /** The sink for debugging messages. */
  static PrintStream out = System.err;

  /**
   * The level. Messages will be logged if and only if the message's
   * level is greater than or equal to this global level.
   */
  static int level = NONE;

  // Constructors.
  // -------------------------------------------------------------------------

  private Debug() { }

  // Class methods.
  // -------------------------------------------------------------------------

  /**
   * Set the PrintStream to which to print messages.
   *
   * @param out The new PrintStream.
   */
  public static void setPrintStream(PrintStream out)
  {
    Debug.out = out;
  }

  /**
   * Set the minimum level for loggable messages. The level will be
   * unchanged if the argument is less than {@link #DEBUG3} or greater
   * than {@link #NONE}.
   *
   * @param level The new level.
   */
  public static void setLevel(int level)
  {
    if (level >= DEBUG3 && level <= NONE)
      {
        Debug.level = level;
      }
  }

  /**
   * Return the minimum level for loggable messages.
   *
   * @return The current level.
   */
  public static int getLevel()
  {
    return level;
  }

  /**
   * Print a {@link #FATAL} message.
   *
   * @param msg The message.
   */
  public static void fatal(String msg)
  {
    printMessage(FATAL, msg);
  }

  /**
   * Print a {@link #WARNING} message.
   *
   * @param msg The message.
   */
  public static void warning(String msg)
  {
    printMessage(WARNING, msg);
  }

  /**
   * Print an {@link #INFO} message.
   *
   * @param msg The message.
   */
  public static void info(String msg)
  {
    printMessage(INFO, msg);
  }

  /**
   * Print a {@link #DEBUG} message.
   *
   * @param msg The message.
   */
  public static void debug(String msg)
  {
    printMessage(DEBUG, msg);
  }

  /**
   * Print a {@link #DEBUG2} message.
   *
   * @param msg The message.
   */
  public static void debug2(String msg)
  {
    printMessage(DEBUG2, msg);
  }

  /**
   * Print a {@link #DEBUG3} message.
   *
   * @param msg The message.
   */
  public static void debug3(String msg)
  {
    printMessage(DEBUG3, msg);
  }

  private static void printMessage(int level, String msg)
  {
    if (level < NONE && level >= Debug.level && out != null)
      {
        out.println(prefixes[level] + ": " + msg);
      }
  }
}
