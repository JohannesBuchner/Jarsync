// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id $
//
// Parameters: parses Samba-like config files.
// Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of Jarsync.
//
// Jarsync is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// Jarsync is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Jarsync; if not, write to the
//
//    Free Software Foundation, Inc.,
//    59 Temple Place, Suite 330,
//    Boston, MA  02111-1307
//    USA
//
// Linking this library statically or dynamically with other modules is
// making a combined work based on this library.  Thus, the terms and
// conditions of the GNU General Public License cover the whole
// combination.
//
// As a special exception, the copyright holders of this library give
// you permission to link this library with independent modules to
// produce an executable, regardless of the license terms of these
// independent modules, and to copy and distribute the resulting
// executable under terms of your choice, provided that you also meet,
// for each linked independent module, the terms and conditions of the
// license of that module.  An independent module is a module which is
// not derived from or based on this library.  If you modify this
// library, you may extend this exception to your version of the
// library, but you are not obligated to do so.  If you do not wish to
// do so, delete this exception statement from your version.
//
// --------------------------------------------------------------------------

package org.metastatic.rsync;

import java.io.IOException;
import java.io.FileReader;
import java.io.LineNumberReader;

/**
 * <p>Simple parser for Samba-style config files. The parameters are
 * passed back to the caller via a simple event listener callback
 * interface, {@link ParameterListener}.</p>
 *
 * <p>A sample file looks like:</p>
 *
 * <pre>
 * [section one]
 * parameter one = value string
 * parameter two = another value
 * [section two]
 * new parameter = some value or t'other
 * </pre>
 *
 * <p>The syntax is roughly:</p>
 *
 * <pre>
 * file      ::= parameter* section* EOF
 * section   ::= header parameter*
 * header    ::= '[' NAME ']'
 * parameter ::= NAME '=' VALUE EOL
 * </pre>
 *
 * <p>Blank lines, and lines that begin with either '#' or ';' are
 * ignored. long lines may be continued by preceding the end-of-line
 * character(s) with a backslash ('\').</p>
 *
 * @version $Revision $
 */
public final class Parameters {

   // Constants and fields.
   // -----------------------------------------------------------------------

   /** The callback object. */
   private final ParameterListener listener;

   /** The current file being read. */
   private LineNumberReader in;

   // Constructor.
   // -----------------------------------------------------------------------

   /**
    * Create a new parameter file parser. The argument is a concrete
    * imlpmentation of {@link ParameterListener} which will take the
    * parsed arguments.
    *
    * @param listener The parameter listener.
    */
   public Parameters(ParameterListener listener) {
      this.listener = listener;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   /**
    * Begin parsing file <i>filename</i>.
    *
    * @param filename The name of the file to parse.
    * @throws IOException If an I/O error occurs.
    */
   public void begin(String filename) throws IOException {
      in = new LineNumberReader(new FileReader(filename));
   }

   /**
    * Parse, or continue parsing the file if a parsing error occured
    * in a previous call to this method. A call to {@link
    * #begin(java.lang.String)} must have succeeded before this method
    * is called.
    *
    * @throws ParameterException If a parsing error occurs. Parsing can
    *         continue if this exception is thrown by calling this
    *         method again.
    * @throws IOException If an I/O error occurs.
    */
   public void parse() throws IOException {
      if (in == null) {
         throw new IOException("nothing to parse");
      }
      String line;

      while ((line = in.readLine()) != null) {
         if (isIgnorable(line)) continue;

         // Concatenate continuation lines.
         while (line.endsWith("\\")) {
            String line2 = in.readLine();
            if (line2 == null) break;
            if (isIgnorable(line2)) continue;
            line = line.substring(0, line.length()-1);

            // Make sure we aren't fooled by '\' followed by a space.
            if (line2.endsWith("\\")) {
               line += line2.trim();
            } else if (line2.trim().endsWith("\\")) {
               line2 = line2.trim() + " ";
               line += line2;
            } else {
               line += line2.trim();
            }
         }

         line = line.trim();
         int i;
         if (line.startsWith("[") && line.endsWith("]")) {
            listener.beginSection(line.substring(1, line.length()-1));
         } else if ((i = line.indexOf('=')) > 1) {
            listener.setParameter(line.substring(0, i).trim(),
               line.substring(i+1).trim());
         } else {
            throw new ParameterException("malformed line at "
               + in.getLineNumber());
         }
      }

      in.close();
      in = null;
   }

   // Own methods.
   // -----------------------------------------------------------------------

   /**
    * Test if a line is a comment or empty.
    *
    * @param s The string to test.
    * @return true if this string is a comment or empty.
    */
   private static boolean isIgnorable(String s) {
      s = s.trim();
      if (s.length() == 0) return true;
      if (s.charAt(0) == '#' || s.charAt(0) == ';') return true;
      return false;
   }
}
