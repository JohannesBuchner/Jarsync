/* LoggerPrintStream -- append print statements to loggers.
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with Jarsync; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.rsync.v2;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class LoggerPrintStream extends PrintStream
{

  // Fields.
  // -------------------------------------------------------------------------

  private boolean open;

  private final StringBuffer line;

  private final Logger logger;

  private final Level logLevel;

  // Constructor.
  // -------------------------------------------------------------------------

  public LoggerPrintStream(Logger logger, Level logLevel)
  {
    super(new ByteArrayOutputStream(1));
    this.logger = logger;
    this.logLevel = logLevel;
    line = new StringBuffer();
    open = true;
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public boolean checkError()
  {
    return false;
  }

  public void flush()
  {
    println();
  }

  public void close()
  {
    open = false;
  }

  public void write(int b)
  {
    if (open)
      line.append((char) b);
  }

  public void write(byte[] b)
  {
    if (open)
      line.append(new String(b));
  }

  public void write(byte[] b, int off, int len)
  {
    if (open)
      line.append(new String(b, off, len));
  }

  public void print(boolean b)
  {
    if (open)
      line.append(String.valueOf(b));
  }

  public void print(char c)
  {
    if (open)
      line.append(String.valueOf(c));
  }

  public void print(int i)
  {
    if (open)
      line.append(String.valueOf(i));
  }

  public void print(long l)
  {
    if (open)
      line.append(String.valueOf(l));
  }

  public void print(float f)
  {
    if (open)
      line.append(String.valueOf(f));
  }

  public void print(double d)
  {
    if (open)
      line.append(String.valueOf(d));
  }

  public void print(char[] c)
  {
    if (open)
      line.append(c);
  }

  public void print(String s)
  {
    if (open)
      line.append(s);
  }

  public void print(Object o)
  {
    if (open)
      line.append(String.valueOf(o));
  }

  public void println()
  {
    if (open)
      {
        logger.log(logLevel, line.toString());
        line.setLength(0);
      }
  }

  public void println(boolean b)
  {
    if (open)
      {
        line.append(String.valueOf(b));
        println();
      }
  }

  public void println(char c)
  {
    if (open)
      {
        line.append(String.valueOf(c));
        println();
      }
  }

  public void println(int i)
  {
    if (open)
      {
        line.append(String.valueOf(i));
        println();
      }
  }

  public void println(long l)
  {
    if (open)
      {
        line.append(String.valueOf(l));
        println();
      }
  }

  public void println(float f)
  {
    if (open)
      {
        line.append(String.valueOf(f));
        println();
      }
  }

  public void println(double d)
  {
    if (open)
      {
        line.append(String.valueOf(d));
        println();
      }
  }

  public void println(char[] c)
  {
    if (open)
      {
        line.append(c);
        println();
      }
  }

  public void println(String s)
  {
    if (open)
      {
        line.append(s);
        println();
      }
  }

  public void println(Object o)
  {
    if (open)
      {
        line.append(String.valueOf(o));
        println();
      }
  }
}
