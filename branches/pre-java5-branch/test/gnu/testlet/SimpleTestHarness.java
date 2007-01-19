// Copyright (c) 1998, 1999, 2001  Red Hat, Inc.
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

// KNOWN BUGS:
//   - should look for /*{ ... }*/ and treat contents as expected
//     output of test.  In this case we should redirect System.out
//     to a temp file we create.

package gnu.testlet;
import java.io.*;
import java.util.Vector;

public class SimpleTestHarness 
    extends TestHarness 
    implements gnu.testlet.config
{
  private int count = 0;
  private int failures = 0;
  private static Vector expected_xfails = new Vector ();
  private int xfailures = 0;
  private int xpasses = 0;
  private int total = 0;
  private boolean verbose = false;
  private boolean debug = false;
  private boolean results_only=false;
  private String description;
  private String last_check;

  private final String getDescription (String pf)
    {
      return (pf + ": " + description
	      + ((last_check == null) ? "" : (": " + last_check))
	      + " (number " + (count + 1) + ")");
    }

  public void check (boolean result)
    {
      if (! result)
	{
	  String desc;
	  if (!expected_xfails.contains (desc = getDescription ("FAIL")))
	    {
	      System.out.println (desc);
	      ++failures;
	    }
	  else if (verbose || results_only)
	    {
	      System.out.println ("X" + desc);
	      ++xfailures;
	    }
	}
      else if (verbose || results_only)
	{
	  if (expected_xfails.contains (getDescription ("FAIL")))
	    {
	      System.out.println (getDescription ("XPASS"));
	      ++xpasses;
	    }
	  else
	    {
	      System.out.println (getDescription ("PASS"));
	    }
	}
      ++count;
      ++total;
    }

  public String getSourceDirectory ()
    {
      return srcdir;
    }

  public String getTempDirectory ()
    {
      return tmpdir;
    }

  public String getPathSeparator ()
    {
      return pathSeparator;
    }

  public String getSeparator ()
    {
      return separator;
    }

  public Reader getResourceReader (String name) 
    throws ResourceNotFoundException
    {
      return (new BufferedReader (new InputStreamReader (
		getResourceStream (name))));
    }

  public InputStream getResourceStream (String name) 
    throws ResourceNotFoundException
    {
      // The following code assumes File.separator is a single character.
      if (File.separator.length () > 1)
	throw new Error ("File.separator length is greater than 1");
      String realName = name.replace ('#', File.separator.charAt (0));
      try 
	{
	  return 
	    new FileInputStream (getSourceDirectory () 
				+ File.separator
				+ realName);
	}
      catch (FileNotFoundException ex)
	{
	  throw new ResourceNotFoundException (ex.getLocalizedMessage ()
			  	+ ": " + getSourceDirectory ()
				+ File.separator + realName);
	}
    }

  public void checkPoint (String name)
    {
      last_check = name;
      count = 0;
    }

  public void verbose (String message)
    {
      if (verbose)
	System.out.println (message);
    }

  public void debug (String message)
    {
      debug(message, true);
    }

  public void debug (String message, boolean newline)
    {
      if (debug)
        {
          if (newline)
	    System.out.println (message);
          else
	    System.out.print (message);
        }
    }

  public void debug (Throwable ex)
    {
      if (debug)
	ex.printStackTrace(System.out);
    }

  public void debug(Object[] o, String desc)
  {
    debug("Dumping Object Array: " + desc);
    if (o == null)
      {
        debug("null");
        return;
      }

    for (int i = 0; i < o.length; i++)
      if (o[i] instanceof Object[])
        debug((Object[])o[i], desc + " element " + i);
      else
        debug("  Element " + i + ": " + o[i]);
  }

  protected void runtest (String name)
    {
      // Try to ensure we start off with a reasonably clean slate.
      System.gc();
      System.runFinalization();

      checkPoint (null);

      Testlet t = null;
      try
	{
	  Class k = Class.forName (name);

          Object o = k.newInstance();
          if (!(o instanceof Testlet))
            return;

	  t = (Testlet)o;
	}
      catch (Throwable ex)
	{
	  String d = "FAIL: uncaught exception loading " + name;
	  if (verbose)
	    d += ": " + ex.toString();
	  System.out.println (d);
	  debug (ex);
	  ++failures;
	  ++total;
	}

      if (t != null)
	{
	  description = name;
	  try
	    {
	      t.test (this);
	    }
	  catch (Throwable ex)
	    {
	      String d = ("FAIL: " + description
			  + ": uncaught exception at "
			  + ((last_check == null) ? "" : 
			     ("\"" + last_check + "\""))
			  +" number "
			  + (count + 1));
	      if (verbose)
		d += ": " + ex.toString();
	      System.out.println (d);
	      debug (ex);
	      ++failures;
	      ++total;
	    }
	}
    }

  protected int done ()
    {
      if (!results_only)
	{
          System.out.println(failures + " of " + total + " tests failed");
          if (xpasses > 0)
            System.out.println(xpasses + " of " + total
			    + " tests unexpectedly passed");
          if (xfailures > 0)
            System.out.println(xfailures + " of " + total
			    + " tests expectedly failed");
	}
      return failures > 0 ? 1 : 0;

    }

  protected SimpleTestHarness (boolean verbose, boolean debug)
    {
      this(verbose, debug, false);
    }

  protected SimpleTestHarness (boolean verbose, boolean debug,
		               boolean results_only)
    {
      this.verbose = verbose;
      this.debug = debug;
      this.results_only= results_only;

      try
        {
          BufferedReader xfile = new BufferedReader (new FileReader ("xfails"));
	  String str;
          while ((str = xfile.readLine ()) != null)
            expected_xfails.addElement (str);
        }
      catch (FileNotFoundException ex)
        {
          // Nothing.
        }
      catch (IOException ex)
        {
          // Nothing.
        }
    }

  public static void main (String[] args)
    {
      boolean verbose = false;
      boolean debug = false;
      boolean results_only = false;
      int i;

      for (i = 0; i < args.length; i++) 
	{
	  if (args[i].equals("-verbose")) 
	    verbose = true;
	  else if (args[i].equals("-debug")) 
	    debug = true;
	  else if (args[i].equals("-resultsonly")) 
	    {
	      results_only = true;
	      verbose = false;
	      debug = false;
	    }
	  else
	    break;
        }

      SimpleTestHarness harness
	= new SimpleTestHarness (verbose, debug, results_only);

      BufferedReader r
	= new BufferedReader (new InputStreamReader (System.in));
      while (true)
	{
	  String cname = null;
	  try
	    {
	      cname = r.readLine ();
	      if (cname == null)
		break;
	      if (verbose)
		System.out.println (cname);
	    }
	  catch (IOException x)
	    {
	      // Nothing.
	    }
	  if (verbose)
	    System.out.println ("----");
	  harness.runtest (cname);
	}

      System.exit(harness.done());
    }
}
