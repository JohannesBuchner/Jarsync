/*  FNMatch.java -- filename expansion/pattern matching
	
	Copyright (C) 1998, 1999
	Kevin Raulerson, http://www.gjt.org/~kevinr/
	Copyright (C) 1991, 1992, 1993 
	Free Software Foundation, Inc.
	
	NOTE: The canonical source of this file is maintained with the
	GNU C Library.  Bugs can be reported to
	bug-glibc@prep.ai.mit.edu.  For the Java version, bugs can be
	reported to kevinr@gjt.org.
	
	This program is free software; you can redistribute it and/or
	modify it under the terms of the GNU General Public License as
	published by the Free Software Foundation; either version 2, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public
	License along with this program; if not, write to the Free
	Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
	MA 02111-1307, USA.
 */

package org.metastatic.rsync.v2;

import java.io.*;

/**
 * Handles wildcards for filename matching.
 *
 * <p>I haven't tested this very thoroughly.  I blindly replaced all the
 * '/' characters in the code with File.separatorChar.  I tried to
 * rework the logic for C-language tests for '\0' correctly.
 *
 * <p> I would be eager to receive any bug reports at <a
 * href="mailto:kevinr@gjt.org">kevinr@gjt.org</a>.
 *
 * <p> Ported to Java by Kevin Raulerson<br> JOS <a
 * href="http://jos.org/wiki/view/main/?topic=CommandLineGroup">CommandLineGroup</a>
 * 
 * @version 1.0
 * @since 1.0
 */
public class FNMatch {

   // public methods
	
   /**
    * Match STRING against the filename pattern PATTERN, returning
    * FNM_MATCH if it matches, FNM_NOMATCH if not.
    * 
    * @param pattern A string with a wildcard 
    *        (* . ? [ ], etc.;  \ is the escape).
    * @param string The string to check for wildcards.
    * @param flags Behavior modifiers.
    * @return Constant int value FNM_MATCH or FNM_NOMATCH.
    */
   public static boolean
   fnmatch(final String pattern, final String string, int flags)
   {
      char c;

      int len = pattern.length();
      int n = 0;
      for( int p = 0;  p < len;  p++ )
      {
         c = pattern.charAt(p);
         c = fold(c, flags);
         switch (c)
         {
            case '?':
               if( string.length() == n )
               {
                  return FNM_NOMATCH;
               }
               else if((flags & FNM_FILE_NAME) != 0  &&
                  string.charAt(n) == File.separatorChar )
               {
                  return FNM_NOMATCH;
               }
               else if((flags & FNM_PERIOD) != 0 &&
                  string.charAt(n) == '.' &&
                  (n == 0 || (flags & FNM_FILE_NAME) != 0 &&
                  string.charAt(n-1) == File.separatorChar))
               {
                  return FNM_NOMATCH;
               }
               break;
            
            case '\\':
               if((flags & FNM_NOESCAPE) != 0) {
                  c = fold(pattern.charAt(p++), flags);
               }
               if(fold(string.charAt(n), flags ) != c)
                  return FNM_NOMATCH;
               break;
            
            case '*':
               if((flags & FNM_PERIOD) != 0 && 
                  string.charAt(n) == '.' &&
                  (n == 0 || (flags & FNM_FILE_NAME) != 0  && 
                  string.charAt(n-1) == File.separatorChar) )
               {
                  return FNM_NOMATCH;
               }
               for(c = pattern.charAt(p++); c == '?' || c == '*';
                  c = pattern.charAt(p++), ++n)
               {
                  if(p == pattern.length())
                     return FNM_MATCH;
                  if(((flags & FNM_FILE_NAME) != 0 &&
                     string.charAt(n) == File.separatorChar) ||
                     (c == '?'  &&  string.length() == n))
                  {
                     return FNM_NOMATCH;
                  }
               }
               if(p == pattern.length())
                  return FNM_MATCH;
               
               char c1 = ((flags & FNM_NOESCAPE) == 0 && c == '\\') 
                  ? pattern.charAt(p)
                  : c;
               c1 = fold(c1, flags);
               for(--p; string.length() != n; ++n)
               {
                  if((c == '[' || fold(string.charAt(n), flags) == c1) &&
                        fnmatch(pattern.substring(p), string.substring(n),
                           flags & ~FNM_PERIOD))
                  {
                     return FNM_MATCH;
                  }
               }
               return FNM_NOMATCH;
               
            case '[':
               // Nonzero if the sense of the
               // character class is inverted.
               boolean not;
               
               if(string.length() == n)
                  return FNM_NOMATCH;
               
               if((flags & FNM_PERIOD) != 0 && 
                  string.charAt(n) == '.' &&
                  (n == 0  || (flags & FNM_FILE_NAME) != 0  &&
                  string.charAt(n-1) == File.separatorChar))
               {
                  return FNM_NOMATCH;
               }
               not = (pattern.charAt(p) == '!' || pattern.charAt(p) == '^');
               if( not )
                  ++p;
               // may throw an exception  ???
               // KR:
               c = pattern.charAt(++p);
               boolean matched = false;
               for (;;)
               {
                  char cstart = c, cend = c;
                  if((flags & FNM_NOESCAPE) == 0  &&  c == '\\' )
                     cstart = cend = pattern.charAt(p++);
                  cstart = cend = fold(cstart, flags);
                  if (p == pattern.length())
                  {
                     // [
                     // (unterminated)
                     // loses.
                     return FNM_NOMATCH;
                  }
                  c = fold(pattern.charAt(++p), flags);
                  
                  if ((flags & FNM_FILE_NAME) != 0 && c == File.separatorChar)
                  {
                     // [/] can never
                     // match.
                     return FNM_NOMATCH;
                  }
                  if (c == '-' && pattern.charAt(p) != ']')
                  {
                     cend = pattern.charAt(p++);
                     if ((flags & FNM_NOESCAPE) == 0  && cend == '\\' )
                     {
                        cend = pattern.charAt(p++);
                     }
                     if (p == pattern.length() )
                     {
                        return FNM_NOMATCH;
                     }
                     cend = fold(cend, flags);
                     c = pattern.charAt(p++);
                  }
                  // reuse c1:
                  c1 = fold(string.charAt(n), flags);
                  if (c1 >= cstart && c1 <= cend)
                  {
                     matched = true;
                     break;
                  }
                  if( c == ']' && !matched )
                     break;
               }
               if(!not && !matched)
                  return FNM_NOMATCH;
               if(!matched)
                  break;
               
               // we only get here if matched
               
               // Skip the rest of the [...] that already matched.
               while( c != ']' )
               {
                  if (p == pattern.length())
                  {
                     // [...
                     // (unterminated)
                     // loses.
                     return FNM_NOMATCH;
                  }
                  c = pattern.charAt(p++);
                  if ((flags & FNM_NOESCAPE) == 0 && c == '\\')
                  {
                     // XXX 1003.2d11 is unclear if this is right.
                     ++p;
                  }
               }
               if (not)
                  return FNM_NOMATCH;
               break;
               
            default:
               if (c != fold(string.charAt(n), flags))
                  return FNM_NOMATCH;
         }
      
         ++n;
      }

      if (string.length() == n)
         return FNM_MATCH;
      
      if ((flags & FNM_LEADING_DIR) != 0
            && string.charAt(n) == File.separatorChar)
      {
         // The FNM_LEADING_DIR flag says that "foo*"
         // matches "foobar/frobozz".
         return FNM_MATCH;
      }
      return FNM_NOMATCH;
   }

   // protected methods

   /**
    * If flags has its FNM_CASEFOLD bit set, then returns
    * the lowercase of c;  otherwise returns c.
    *  
    * @param c A character to fold.
    * @param flags Bits, set or not, to modify behavior.
    * @return A `folded' character.
    */
   protected static char fold(char c, int flags)
   {
      return (flags & FNM_CASEFOLD) != 0 ? Character.toLowerCase(c) : c;
   }
   
   // data and accessor methods
   
   /**
    * No wildcard can ever match `/'. A constant for bits set in the FLAGS
    * argument to fnmatch().
    */
   public static final int FNM_PATHNAME = 1 << 0;
   
   /**
    * Backslashes don't quote special chars. A constant for bits set in the
    * FLAGS argument to fnmatch().
    */
   public static final int FNM_NOESCAPE = 1 << 1;
   
   /**
    * Leading `.' is matched only explicitly. A constant for bits set in the
    * FLAGS argument to fnmatch().
    */
   public static final int FNM_PERIOD = 1 << 2;
   
   /**
    * Preferred GNU name. A constant for bits set in the FLAGS argument to
    * fnmatch().
    */
   public static final int FNM_FILE_NAME = FNM_PATHNAME;
   
   /**
    * Ignore `/...' after a match. A constant for bits set in the FLAGS
    * argument to fnmatch().
    */
   public static final int FNM_LEADING_DIR = 1 << 3;
   
   /**
    * Compare without regard to case. A constant for bits set in the FLAGS
    * argument to fnmatch().
    */
   public static final int FNM_CASEFOLD = 1 << 4;

   /**
    * Value returned by fnmatch() if STRING does not match PATTERN.
    */
   public static final boolean FNM_NOMATCH = false;
   
   /**
    * Value returned by fnmatch() if STRING matches PATTERN.
    */
   public static final boolean FNM_MATCH = true;
   
}

