// vim:set tw=72 expandtab softtabstop=3 shiftwidth=3 tabstop=3:
// $Id$
//
// Rdiff - rdiff workalike program.
// Copyright (C) 2001,2002,2003  Casey Marshall <rsdio@metastatic.org>
//
// This file is a part of Jarsync.
//
// Jarsync is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2, or (at your option) any
// later version.
//
// Jarsync is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Jarsync; see the file COPYING.  If not, write to the
//
//    Free Software Foundation Inc.,
//    59 Temple Place - Suite 330,
//    Boston, MA 02111-1307
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
// --------------------------------------------------------------------

package org.metastatic.rsync;

import java.io.*;
import java.util.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

/**
 * A re-implementation of the <code>rdiff</code> utility from librsync.
 * For more info see <a href="http://rproxy.samba.org/">the rproxy
 * page</a>.
 *
 * @version $Revision$
 */
public class Rdiff implements RsyncConstants {

   // Constants and variables.
   // -----------------------------------------------------------------

   /** The short options. */
   protected static final String OPTSTRING = "b:I:i::S:sO:vVz::h";

   /** The long options. */
   protected static final LongOpt[] LONGOPTS = new LongOpt[] {
      new LongOpt("block-size",  LongOpt.REQUIRED_ARGUMENT, null, 'b'),
      new LongOpt("bzip2",       LongOpt.OPTIONAL_ARGUMENT, null, 'i'),
      new LongOpt("help",        LongOpt.NO_ARGUMENT, null, 'h'),
      new LongOpt("input-size",  LongOpt.REQUIRED_ARGUMENT, null, 'I'),
      new LongOpt("gzip",        LongOpt.OPTIONAL_ARGUMENT, null, 'z'),
      new LongOpt("output-size", LongOpt.REQUIRED_ARGUMENT, null, 'O'),
      new LongOpt("paranoia",    LongOpt.NO_ARGUMENT, null, 'p'),
      new LongOpt("statistics",  LongOpt.NO_ARGUMENT, null, 's'),
      new LongOpt("sum-size",    LongOpt.REQUIRED_ARGUMENT, null, 'S'),
      new LongOpt("verbose",     LongOpt.NO_ARGUMENT, null, 'v'),
      new LongOpt("version",     LongOpt.NO_ARGUMENT, null, 'V')
   };

   /** The `signature' command. */
   public static final String SIGNATURE = "signature";

   /** The `delta' command. */
   public static final String DELTA = "delta";

   /** The `patch' command. */
   public static final String PATCH = "patch";

   /** The program name printed to the console. */
   public static final String PROGNAME = "rdiff";

   /** Whether or not to trace to System.err. */
   protected static boolean verbose = false;

   /**
    * The length of blocks to checksum.
    */
   protected int blockLength;

   /**
    * The effective strong signature length.
    */
   protected int strongSumLength;

   // Constructors.
   // -----------------------------------------------------------------

   /**
    * Create an Rdiff object.
    */
   public Rdiff() {
      blockLength = RDIFF_BLOCK_LENGTH;
      strongSumLength = RDIFF_STRONG_LENGTH;
   }

   // Main entry point.
   // -----------------------------------------------------------------

   /**
    * Main entry point for console use.
    *
    * @param argv The argument vector.
    */
   public static void main(String[] argv) throws Throwable {
      Getopt g = new Getopt(PROGNAME, argv, OPTSTRING, LONGOPTS);
      int c;
      Rdiff rdiff = new Rdiff();
      boolean showStats = false;

      // parse the command line
      while ((c = g.getopt()) != -1) {
         switch (c) {
            case 'b':
               try {
                  rdiff.blockLength = Integer.parseInt(g.getOptarg());
                  if (rdiff.blockLength < 1) {
                     throw new NumberFormatException();
                  }
               } catch (NumberFormatException nfe) {
                  System.err.println(PROGNAME + ": bad block size.");
                  System.exit(1);
               }
               break;
            case 'h':
               usage(System.out);
               System.exit(0);
            case 'I': break;
            case 'i': break;
            case 'S':
               try {
                  rdiff.strongSumLength = Integer.parseInt(g.getOptarg());
                  if (rdiff.strongSumLength > SUM_LENGTH) {
                     throw new NumberFormatException();
                  }
               } catch (NumberFormatException nfe) {
                  System.err.println(PROGNAME +
                     ": bad sum length; must be > 0 and < " + SUM_LENGTH + ".");
                  System.exit(1);
               }
               break;
            case 's':
               showStats = true;
               break;
            case 'V':
               version(System.out);
               System.exit(0);
            case 'v':
               verbose = true;
               break;
            case 'z': break;
            case '?':
               System.err.println("Try `" + PROGNAME + " --help' for more info.");
               System.exit(1);
         }
      }

      // Parse the command.
      String command = null;
      if (g.getOptind() < argv.length) {
         command = argv[g.getOptind()];
      } else {
         System.err.println(PROGNAME + ": you must specify an action: "
            + "`signature', `delta', or `patch'.");
         System.err.println("Try `" + PROGNAME + " --help' for more info.");
         System.exit(1);
      }

      if (verbose) {
         System.err.println("bs=" + rdiff.blockLength + " sl="
            + rdiff.strongSumLength);
      }

      // The command is `signature'; generate signatures for the input
      // file (or System.in) and write them to the output file (or
      // System.out).
      if (SIGNATURE.startsWith(command)) {
         if (verbose) {
            System.err.println("Command is `signature'.");
         }
         OutputStream out = System.out;
         InputStream in = System.in;
         if (argv.length > g.getOptind()+1) {
            try {
               in = new FileInputStream(argv[g.getOptind()+1]);
               if (verbose) {
                  System.err.println("Reading basis from file " +
                     argv[g.getOptind()+1]);
               }
            } catch (FileNotFoundException fnfe) {
               System.err.println(PROGNAME + ": Error opening \""
                  + argv[g.getOptind()+1] +
                  "\" for reading: No such file or directory.");
               System.exit(1);
            }
         } else if (verbose) {
            System.err.println("Reading basis from standard input.");
         }
         Collection sums = rdiff.makeSignatures(in);
         if (in != System.in) {
            in.close();
         }

         if (argv.length > g.getOptind()+2) {
            try {
               out = new FileOutputStream(argv[g.getOptind()+2]);
               if (verbose) {
                  System.err.println("Writing signatures to file " +
                     argv[g.getOptind()+2]);
               }
            } catch (FileNotFoundException fnfe) {
               System.err.println(PROGNAME + ": Error opening \""
                  + argv[g.getOptind()+2] +
                  "\" for reading: No such file or directory.");
               System.exit(1);
            }
         } else if (verbose) {
            System.err.println("Writing signatures to standard output.");
         }
         rdiff.writeSignatures(sums, out);
         if (out != System.out) {
            out.close();
         }
         if (showStats) {
            System.err.println(PROGNAME + ": signature statistics: " +
               "signature[" + sums.size() + " blocks, " + rdiff.blockLength +
               " bytes per block]");
         }

      // Command is `delta'; read signatures from the given file,
      // generate deltas for the input file (or System.in) and write the
      // deltas to the output file (or System.out).
      } else if (DELTA.startsWith(command)) {
         if (verbose) {
            System.err.println("Command is `delta'.");
         }
         InputStream sigsIn = null;
         InputStream newIn = System.in;
         OutputStream out = System.out;
         if (argv.length > g.getOptind()+1) {
            try {
               sigsIn = new FileInputStream(argv[g.getOptind()+1]);
               if (verbose) {
                  System.err.println("Reading signatures from " +
                     argv[g.getOptind()+1]);
               }
            } catch (FileNotFoundException fnfe) {
               System.err.println(PROGNAME + ": Error opening \""
                  + argv[g.getOptind()+1] +
                  "\" for reading: No such file or directory.");
               System.exit(1);
            }
         } else {
            System.err.println("Usage for delta: " + PROGNAME +
               " [OPTIONS] delta SIGNATURE [NEWFILE [DELTA]]");
            System.err.println("Try `" + PROGNAME +
               " --help' for more information.");
            System.exit(1);
         }
         Collection sigs = rdiff.readSignatures(sigsIn);
         sigsIn.close();
         if (showStats) {
            System.err.println(PROGNAME + ": loadsig statistics: " +
               "signature[" + sigs.size() + " blocks, " + rdiff.blockLength +
               " bytes per block]");
         }

         if (argv.length > g.getOptind()+2) {
            try {
               newIn = new FileInputStream(argv[g.getOptind()+2]);
               if (verbose) {
                  System.err.println("Reading new file from " +
                     argv[g.getOptind()+2]);
               }
            } catch (FileNotFoundException fnfe) {
               System.err.println(PROGNAME + ": Error opening \""
                  + argv[g.getOptind()+2] +
                  "\" for reading: No such file or directory.");
               System.exit(1);
            }
         } else if (verbose) {
            System.err.println("Reading new file from standard input.");
         }
         Collection deltas = rdiff.makeDeltas(sigs, newIn);
         if (newIn != System.in) {
            newIn.close();
         }
         if (showStats) {
            int lit = 0;
            long litBytes = 0;
            int litCmdBytes = 0;
            int copy = 0;
            long copyBytes = 0;
            System.err.print(PROGNAME + ": delta statistics:");
            for (Iterator i = deltas.iterator(); i.hasNext(); ) {
               Object o = i.next();
               if (o instanceof Offsets) {
                  copy++;
                  copyBytes += ((Offsets) o).getBlockLength();
               } else {
                  lit++;
                  litBytes += ((DataBlock) o).getBlockLength();
                  litCmdBytes += 1 + integerLength(((DataBlock) o).getBlockLength());
               }
            }
            if (lit > 0) {
               System.err.print(" literal[" + lit + " cmds, " + litBytes
                  + " bytes, " + litCmdBytes + " cmdbytes]");
            }
            if (copy > 0) {
               System.err.print(" copy[" + copy + " cmds, " + copyBytes
                  + " bytes, 0 false, " + copy*9 + " cmdbytes]");
            }
            System.err.println();
         }

         if (argv.length > g.getOptind()+3) {
            try {
               out = new FileOutputStream(argv[g.getOptind()+3]);
               if (verbose) {
                  System.err.println("Writing deltas to file " +
                     argv[g.getOptind()+3]);
               }
            } catch (FileNotFoundException fnfe) {
               System.err.println(PROGNAME + ": Error opening \""
                  + argv[g.getOptind()+3] +
                  "\" for writing: No such file or directory.");
               System.exit(1);
            }
         } else if (verbose) {
            System.err.println("Writing deltas to standard output.");
         }
         rdiff.writeDeltas(deltas, out);
         if (out != System.out) {
            out.close();
         }

      // Command is `patch'; read the given basis file, read deltas from
      // the given file (or System.in), and reconstruct the file to the
      // given location (or System.out).
      } else if (PATCH.startsWith(command)) {
         if (verbose) {
            System.err.println("Command is `patch'.");
         }
         File basis = null;
         InputStream deltasIn = System.in;
         OutputStream newFile = System.out;
         if (argv.length > g.getOptind()+1) {
            try {
               basis = new File(argv[g.getOptind()+1]);
               if (!basis.exists()) {
                  throw new FileNotFoundException();
               }
               if (verbose) {
                  System.err.println("Reading basis file " +
                     argv[g.getOptind()+1]);
               }
            } catch (FileNotFoundException fnfe) {
               System.err.println(PROGNAME + ": Error opening \""
                  + argv[g.getOptind()+1] +
                  "\" for reading: No such file or directory.");
               System.exit(1);
            }
         } else {
            System.err.println("Usage for patch: " + PROGNAME +
               " [OPTIONS] patch BASIS [DELTA [NEW]]");
            System.err.println("Try `" + PROGNAME +
               " --help' for more information.");
            System.exit(1);
         }

         if (argv.length > g.getOptind()+2) {
            try {
               deltasIn = new FileInputStream(argv[g.getOptind()+2]);
               if (verbose) {
                  System.err.println("Reading deltas from file " +
                     argv[g.getOptind()+2]);
               }
            } catch (FileNotFoundException fnfe) {
               System.err.println(PROGNAME + ": Error opening \""
                  + argv[g.getOptind()+2] +
                  "\" for reading: No such file or directory.");
               System.exit(1);
            }
         } else if (verbose) {
            System.err.println("Reading deltas from standard input.");
         }
         Collection deltas = rdiff.readDeltas(deltasIn);
         if (deltasIn != System.in) {
            deltasIn.close();
         }
         if (showStats) {
            int lit = 0;
            long litBytes = 0;
            int litCmdBytes = 0;
            int copy = 0;
            long copyBytes = 0;
            System.err.print(PROGNAME + ": patch statistics:");
            for (Iterator i = deltas.iterator(); i.hasNext(); ) {
               Object o = i.next();
               if (o instanceof Offsets) {
                  copy++;
                  copyBytes += ((Offsets) o).getBlockLength();
               } else {
                  lit++;
                  litBytes += ((DataBlock) o).getBlockLength();
                  litCmdBytes += 1 + integerLength(((DataBlock) o).getBlockLength());
               }
            }
            if (lit > 0) {
               System.err.print(" literal[" + lit + " cmds, " + litBytes
                  + " bytes, " + litCmdBytes + " cmdbytes]");
            }
            if (copy > 0) {
               System.err.print(" copy[" + copy + " cmds, " + copyBytes
                  + " bytes, 0 false, " + copy*9 + " cmdbytes]");
            }
            System.err.println();
         }

         if (argv.length > g.getOptind()+3) {
            try {
               newFile = new FileOutputStream(argv[g.getOptind()+3]);
               if (verbose) {
                  System.err.println("Writing new file " +
                     argv[g.getOptind()+3]);
               }
            } catch (FileNotFoundException fnfe) {
               System.err.println(PROGNAME + ": Error opening \""
                  + argv[g.getOptind()+3] +
                  "\" for writing: No such file or directory.");
               System.exit(1);
            }
         } else if (verbose) {
            System.err.println("Writing new file to standard output.");
         }
         rdiff.rebuildFile(basis, deltas, newFile);
         if (newFile != System.out) {
            newFile.close();
         }
      } else {
         System.err.println(PROGNAME + ": you must specify an action: "
            + "`signature', `delta', or `patch'.");
         System.err.println("Try `" + PROGNAME + " --help' for more info.");
         System.exit(1);
      }

   }

   // Public instance methods.
   // -----------------------------------------------------------------

   /**
    * Write the signatures to the specified output stream.
    *
    * @param sigs The signatures to write.
    * @param out  The OutputStream to write to.
    * @throws java.io.IOException If writing fails.
    */
   public void
   writeSignatures(Collection sigs, OutputStream out) throws IOException {
      writeInt(SIG_MAGIC, out);
      writeInt(blockLength, out);
      writeInt(strongSumLength, out);
      for (Iterator i = sigs.iterator(); i.hasNext(); ) {
         ChecksumPair pair = (ChecksumPair) i.next();
         writeInt(pair.getWeak(), out);
         out.write(pair.getStrong(), 0, strongSumLength);
      }
   }

   /**
    * Make the signatures from data coming in through the input stream.
    *
    * @param in The input stream to generate signatures for.
    * @return A Collection of signatures.
    * @throws java.io.IOException If reading fails.
    */
   public Collection makeSignatures(InputStream in)
   throws IOException, NoSuchAlgorithmException {
      Configuration c = new Configuration();
      c.strongSum = MessageDigest.getInstance("MD4");
      c.weakSum = new Checksum32();
      c.blockLength = blockLength;
      c.strongSumLength = c.strongSumLength;
      return new Generator(c).generateSums(in);
   }

   /**
    * Read the signatures from the input stream.
    *
    * @param in The InputStream to read the signatures from.
    * @return A collection of {@link ChecksumPair}s read.
    * @throws java.io.IOException If the input stream is malformed.
    */
   public Collection readSignatures(InputStream in) throws IOException {
      Collection sigs = new LinkedList();
      int header = readInt(in);
      if (header != SIG_MAGIC) {
         throw new IOException("Bad signature header: 0x"
            + Integer.toHexString(header));
      }
      long off = 0;
      blockLength = readInt(in);
      strongSumLength = readInt(in); 

      int weak;
      byte[] strong = new byte[strongSumLength];
      do {
         try {
            weak = readInt(in);
            int len = in.read(strong);
            if (len < strongSumLength)
               break;
            sigs.add(new ChecksumPair(weak, strong, off));
            off += blockLength;
         } catch (EOFException eof) {
            break;
         }
      } while(true);
      return sigs;
   }

   /**
    * Write deltas to an output stream.
    *
    * @param deltas A collection of {@link Delta}s to write.
    * @param out    The OutputStream to write to.
    * @throws java.io.IOException If writing fails.
    */
   public void
   writeDeltas(Collection deltas, OutputStream out) throws IOException {
      writeInt(DELTA_MAGIC, out);
      for (Iterator i = deltas.iterator(); i.hasNext(); ) {
         Object o = i.next();
         if (o instanceof Offsets) {
            writeCopy((Offsets) o, out);
         } else if (o instanceof DataBlock) {
            writeLiteral((DataBlock) o, out);
         }
      }
      out.write(0);
   }

   /**
    * Make a collection of {@link Delta}s from the given sums and
    * InputStream.
    *
    * @param sums A collection of {@link ChecksumPair}s generated from
    *    the "old" file.
    * @param in   The InputStream for the "new" file.
    * @return A collection of {@link Delta}s that will patch the old
    *    file to the new.
    * @throws java.io.IOException If reading fails.
    */
   public Collection
   makeDeltas(Collection sums, InputStream in)
   throws IOException, NoSuchAlgorithmException {
      Configuration c = new Configuration();
      c.strongSum = MessageDigest.getInstance("MD4");
      c.weakSum = new Checksum32();
      c.blockLength = blockLength;
      c.strongSumLength = strongSumLength;
      return new Matcher(c).hashSearch(sums, in);
   }

   /**
    * Read a collection of {@link Delta}s from the InputStream.
    *
    * @param in The InputStream to read from.
    * @return A collection of {@link Delta}s read.
    * @throws java.io.IOException If the input stream is malformed.
    */
   public Collection readDeltas(InputStream in) throws IOException {
      Collection deltas = new LinkedList();
      int header = readInt(in);
      if (header != DELTA_MAGIC) {
         throw new IOException("Bad delta header: 0x" +
            Integer.toHexString(header));
      }
      int command;
      long offset = 0;
      byte[] buf;
      while ((command = in.read()) != -1) {
         switch (command) {
            case OP_END:
               return deltas;
            case OP_LITERAL_N1:
               buf = new byte[(int) readInt(1, in)];
               in.read(buf);
               deltas.add(new DataBlock(offset, buf));
               offset += buf.length;
               break;
            case OP_LITERAL_N2:
               buf = new byte[(int) readInt(2, in)];
               in.read(buf);
               deltas.add(new DataBlock(offset, buf));
               offset += buf.length;
               break;
            case OP_LITERAL_N4:
               buf = new byte[(int) readInt(4, in)];
               in.read(buf);
               deltas.add(new DataBlock(offset, buf));
               offset += buf.length;
               break;
            case OP_COPY_N4_N4:
               int oldOff = (int) readInt(4, in);
               int bs = (int) readInt(4, in);
               deltas.add(new Offsets(oldOff, offset, bs));
               offset += bs;
               break;
            default:
               throw new IOException("Bad delta command: 0x" +
                  Integer.toHexString(command));
         }
      }
      throw new IOException("Didn't recieve RS_OP_END.");
   }

   /**
    * Patch the file <code>basis</code> using <code>deltas</code>,
    * writing the patched file to <code>out</code>.
    *
    * @param basis  The basis file.
    * @param deltas The collection of {@link Delta}s to apply.
    * @param out    The OutputStream to write the patched file to.
    * @throws java.io.IOException If reading/writing fails.
    */
   public void
   rebuildFile(File basis, Collection deltas, OutputStream out)
   throws IOException {
      File temp = Rebuilder.rebuildFile(basis, deltas);
      temp.deleteOnExit();
      FileInputStream fin = new FileInputStream(temp);
      byte[] buf = new byte[1024*1024];
      int len = 0;
      while ((len = fin.read(buf)) != -1) {
         out.write(buf, 0, len);
      }
   }

   // Own methods.
   // -----------------------------------------------------------------

   /**
    * Print a console usage message to <code>out</code>.
    *
    * @param out The PrintStream to write to.
    */
   private static void usage(PrintStream out) {
      out.println("Usage: rdiff [OPTIONS] signature [BASIS [SIGNATURE]]");
      out.println("             [OPTIONS] delta SIGNATURE [NEWFILE [DELTA]]");
      out.println("             [OPTIONS] patch BASIS [DELTA [NEWFILE]]");
      out.println();
      out.println("Options: * == option currently unimplemented");
      out.println("  -v, --verbose             Trace internal processing");
      out.println("  -V, --version             Show program version");
      out.println("  -h, --help                Show this help message");
      out.println("  -s, --statistics          Show performance statistics");
      out.println("Delta-encoding options:");
      out.println("  -b, --block-size=BYTES    Signature block size");
      out.println("  -S, --sum-size=BYTES      Set signature strength");
      out.println("*     --paranoia            Verify all rolling checksums");
      out.println("IO options:");
      out.println("* -I, --input-size=BYTES    Input buffer size");
      out.println("* -O, --output-size=BYTES   Output buffer size");
      out.println("* -z, --gzip[=LEVEL]        gzip-compress deltas");
      out.println("* -i, --bzip2[=LEVEL]       bzip2-compress deltas");
   }

   /**
    * Print our version message to <code>out</code>.
    *
    * @param out The PrintStream to write to.
    */
   private static void version(PrintStream out) {
      out.println(PROGNAME + " (Jarsync " + version.VERSION + ")");
      out.println("Copyright (C) 2002 Casey Marshall.");
      out.println();
      out.println("Jarsync comes with NO WARRANTY, to the extent permitted by law.");
      out.println("You may redistribute copies of Jarsync under the terms of the GNU");
      out.println("General Public License.  See the file `COPYING' for details.");
   }

   /**
    * Write a "COPY" command to <code>out</code>.
    *
    * @param off The {@link Offsets} object to write as a COPY command.
    * @param out The OutputStream to write to.
    * @throws java.io.IOException if writing fails.
    */
   private static void
   writeCopy(Offsets off, OutputStream out) throws IOException {
      out.write(OP_COPY_N4_N4);
      writeInt(off.getOldOffset(), 4, out);
      writeInt(off.getBlockLength(), out);
   }

   /**
    * Write a "LITERAL" command to <code>out</code>.
    *
    * @param d   The {@link DataBlock} to write as a LITERAL command.
    * @param out The OutputStream to write to.
    * @throws java.io.IOException if writing fails.
    */
   private static void
   writeLiteral(DataBlock d, OutputStream out) throws IOException {
      byte cmd = 0;
      int param_len;

      switch (param_len = integerLength(d.getBlockLength())) {
         case 1:
            cmd = OP_LITERAL_N1;
            break;
         case 2:
            cmd = OP_LITERAL_N2;
            break;
         case 4:
            cmd = OP_LITERAL_N4;
            break;
      }

      out.write(cmd);
      writeInt(d.getBlockLength(), param_len, out);
      out.write(d.getData());
   }

   /**
    * Check if a long integer needs to be represented by 1, 2, 4 or 8
    * bytes.
    *
    * @param l The long to test.
    * @return The effective length, in bytes, of the argument.
    */
   private static int integerLength(long l) {
      if ((l & ~0xffL) == 0) {
         return 1;
      } else if ((l & ~0xffffL) == 0) {
         return 2;
      } else if ((l & ~0xffffffffL) == 0) {
         return 4;
      }
      return 8;
   }

   /**
    * Read a variable-length integer from the input stream. This method
    * reads <code>len</code> bytes from <code>in</code>, interpolating
    * them as composing a big-endian integer.
    *
    * @param len The number of bytes to read.
    * @param in  The InputStream to read from.
    * @return The integer.
    * @throws java.io.IOException if reading fails.
    */
   private static long readInt(int len, InputStream in) throws IOException {
      long i = 0;
      for (int j = len-1; j >= 0; j--) {
         int k = in.read();
         if (k == -1) throw new EOFException();
         i |= (k&0xff) << 8*j;
      }
      return i;
   }

   /**
    * Read a four-byte big-endian integer from the InputStream.
    *
    * @param in The InputStream to read from.
    * @return The integer read.
    * @throws java.io.IOException if reading fails.
    */
   private static int readInt(InputStream in) throws IOException {
      int i = 0;
      for (int j = 3; j >= 0; j--) {
         int k = in.read();
         if (k == -1) throw new EOFException();
         i |= (k&0xff) << 8*j;
      }
      return i;
   }

   /**
    * Write the lowest <code>len</code> bytes of <code>l</code> to
    * <code>out</code> in big-endian byte order.
    *
    * @param l   The integer to write.
    * @param len The number of bytes to write.
    * @param out The OutputStream to write to.
    * @throws java.io.IOException If writing fails.
    */
   private static void
   writeInt(long l, int len, OutputStream out) throws IOException {
      for (int i = len-1; i >= 0; i--) {
         out.write((int) (l >>> i*8) & 0xff);
      }
   }

   /**
    * Write a four-byte integer in big-endian byte order to
    * <code>out</code>.
    *
    * @param i   The integer to write.
    * @param out The OutputStream to write to.
    * @throws java.io.IOException If writing fails.
    */
   private static void writeInt(int i, OutputStream out) throws IOException {
      out.write((byte) ((i >>> 24) & 0xff));
      out.write((byte) ((i >>> 16) & 0xff));
      out.write((byte) ((i >>>  8) & 0xff));
      out.write((byte) ( i & 0xff));
   }
}
