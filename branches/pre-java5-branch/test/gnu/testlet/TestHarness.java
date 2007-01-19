// Copyright (c) 1998, 1999, 2001  Cygnus Solutions
// Written by Tom Tromey <tromey@cygnus.com>

// This file is part of Mauve.

// Mauve is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2, or (at your option)
// any later version.

// Mauve is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Mauve; see the file COPYING.  If not, write to
// the Free Software Foundation, 59 Temple Place - Suite 330,
// Boston, MA 02111-1307, USA.

package gnu.testlet;

import java.io.Reader;
import java.io.InputStream;

public abstract class TestHarness
{
  // These methods are used to determine whether a test has passed.
  public abstract void check (boolean result);
  public void check (Object result, Object expected)
    {
      boolean ok = (result == null
		    ? expected == null
		    : result.equals(expected));
      check (ok);
      // This debug message may be misleading, depending on whether
      // string conversion produces same results for unequal objects.
      if (! ok)
	debug ("got " + result + " but expected " + expected);
    }
  public void check (double result, double expected)
    {
      // This triple check overcomes the fact that == does not
      // compare NaNs, and cannot tell between 0.0 and -0.0;
      // and all without relying on java.lang.Double (which may
      // itself be buggy - else why would we be testing it? ;)
      // For 0, we switch to infinities, and for NaN, we rely
      // on the identity in JLS 15.21.1 that NaN != NaN is true.
      boolean ok = (result == expected
		    ? (result != 0) || (1/result == 1/expected)
		    : (result != result) && (expected != expected));
      check (ok);
      if (! ok)
	// If Double.toString() is buggy, this debug statement may
	// accidentally show the same string for two different doubles!
	debug ("got " + result + " but expected " + expected);
    }
  public void check (long result, long expected)
    {
      boolean ok = (result == expected);
      check (ok);
      if (! ok)
	debug ("got " + result + " but expected " + expected);
    }
  public void check (int result, int expected)
    {
      boolean ok = (result == expected);
      check (ok);
      if (! ok)
	debug ("got " + result + " but expected " + expected);
    }

  // These methods are like the above, but checkpoint first.
  public void check (boolean result, String name)
    {
      checkPoint (name);
      check (result);
    }
  public void check (Object result, Object expected, String name)
    {
      checkPoint (name);
      check (result, expected);
    }
  public void check (int result, int expected, String name)
    {
      checkPoint (name);
      check (result, expected);
    }
  public void check (long result, long expected, String name)
    {
      checkPoint (name);
      check (result, expected);
    }
  public void check (double result, double expected, String name)
    {
      checkPoint (name);
      check (result, expected);
    }
  public void fail (String name)
    {
      checkPoint (name);
      check (false);
    }

  // Given a resource name, return a Reader on it.  Resource names are
  // just like file names.  They are relative to the top level Mauve
  // directory, but '#' characters are used in place of directory
  // separators.
  public abstract Reader getResourceReader (String name) 
    throws ResourceNotFoundException;

  public abstract InputStream getResourceStream (String name) 
    throws ResourceNotFoundException;

  // Provide a directory name for writing temporary files.
  public abstract String getTempDirectory ();

  // This can be used to mark a known place in a testlet.  It is
  // useful if you have a large number of tests -- it makes it easier
  // to find a failing test in the source code.
  public abstract void checkPoint (String name);

  // This will print a message when in verbose mode.
  public abstract void verbose (String message);

  // These will print a message when in debug mode.  In the Throwable
  // case, what is printed is the stack trace.
  public abstract void debug (String message);
  public abstract void debug (String message, boolean newline);
  public abstract void debug (Throwable ex);
  public abstract void debug (Object[] o, String desc);
}
