// vim:set tabstop=3 expandtab tw=72:
// $Id$
//
// Rdiff - rdiff workalike program.
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

import java.io.*;
import java.util.*;

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

   private static final String OPTSTRING = "b:I:i::S:sO:vVz::h";

   private static final LongOpt[] LONGOPTS = new LongOpt[] {
      new LongOpt("block-size", LongOpt.REQUIRED_ARGUMENT, null, 'b'),
      new LongOpt("bzip2", LongOpt.OPTIONAL_ARGUMENT, null, 'i'),
      new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
      new LongOpt("input-size", LongOpt.REQUIRED_ARGUMENT, null, 'I'),
      new LongOpt("gzip", LongOpt.OPTIONAL_ARGUMENT, null, 'z'),
      new LongOpt("output-size", LongOpt.REQUIRED_ARGUMENT, null, 'O'),
      new LongOpt("paranoia", LongOpt.NO_ARGUMENT, null, 'p'),
      new LongOpt("statistics", LongOpt.NO_ARGUMENT, null, 's'),
      new LongOpt("sum-size", LongOpt.REQUIRED_ARGUMENT, null, 'S'),
      new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v'),
      new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'V')
   };

   private static final String SIGNATURE = "signature";
   private static final String DELTA = "delta";
   private static final String PATCH = "patch";
   private static final String PROGNAME = "rdiff";

   private static boolean verbose = false;

   protected int blockLength;
   protected int strongSumLength;

   // Constructors.
   // -----------------------------------------------------------------

   public Rdiff(int blockLength, int strongSumLength) {
      this.blockLength = blockLength;
      this.strongSumLength = strongSumLength;
   }

   // Main entry point.
   // -----------------------------------------------------------------

   public static void main(String[] argv) throws Throwable {
      Getopt g = new Getopt(PROGNAME, argv, OPTSTRING, LONGOPTS);
      int c;
      int blockLength = RDIFF_BLOCK_LENGTH, sumLength = RDIFF_STRONG_LENGTH;
      boolean showStats = false;
      while ((c = g.getopt()) != -1) {
         switch (c) {
            case 'b':
               blockLength = Integer.parseInt(g.getOptarg());
               break;
            case 'h':
               usage(System.out);
               System.exit(0);
            case 'I': break;
            case 'i': break;
            case 'S':
               sumLength = Integer.parseInt(g.getOptarg());
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
               System.err.println("try `" + PROGNAME + " --help' for more info.");
               break;
         }
      }

      String command = null;
      if (g.getOptind() < argv.length) {
         command = argv[g.getOptind()];
      } else {
         System.err.println(PROGNAME + ": you must specify an action: "
            + "`signature', `delta', or `patch'.");
         System.err.println("try `" + PROGNAME + " --help' for more info.");
         System.exit(1);
      }

      Rdiff rdiff = new Rdiff(blockLength, sumLength);
      if (verbose) {
         System.err.println("bs=" + blockLength + " sl=" + sumLength);
      }

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
               "signature[" + sums.size() + " blocks, " + blockLength +
               " bytes per block]");
         }

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
               "signature[" + sigs.size() + " blocks, " + blockLength +
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
         System.err.println("try `" + PROGNAME + " --help' for more info.");
         System.exit(1);
      }

   }

   // Public instance methods.
   // -----------------------------------------------------------------

   /**
    * Write the signatures to the specified output stream.
    */
   public void
   writeSignatures(Collection sigs, OutputStream out) throws IOException {
      writeInt(SIG_MAGIC, out);
      writeInt(blockLength, out);
      writeInt(strongSumLength, out);
      for (Iterator i = sigs.iterator(); i.hasNext(); ) {
         ChecksumPair pair = (ChecksumPair) i.next();
         writeInt(pair.getWeak().intValue(), out);
         out.write(pair.getStrong(), 0, strongSumLength);
      }
   }

   /**
    * Make the signatures from data coming in through the input stream.
    */
   public Collection makeSignatures(InputStream in) throws IOException {
      int len = 0;
      long off = 0;
      byte[] buf = new byte[blockLength*blockLength];
      Collection sums = null;
      Generator gen = new Generator(new Configuration(new MD4(), blockLength));
      while ((len = in.read(buf)) > 0) {
         if (sums == null) {
            sums = gen.generateSums(buf, 0, len);
         } else {
            sums.addAll(gen.generateSums(buf, 0, len));
         }
      }
      return sums;
   }

   /**
    * Read the signatures from the input stream.
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

   public Collection
   makeDeltas(Collection sums, InputStream in) throws IOException {
      Collection deltas = null;
      int len = 0;
      long offset = 0;
      byte[] buf = new byte[blockLength*blockLength];
      Configuration config = new Configuration(blockLength);
      config.setStrongSumLength(strongSumLength);
      Matcher m = new Matcher(config);
      while ((len = in.read(buf)) > 0) {
         if (deltas == null) {
            deltas = m.hashSearch(sums, buf, 0, len, offset);
         } else {
            deltas.addAll(m.hashSearch(sums, buf, 0, len, offset));
         }
         offset += len;
      }
      return deltas;
   }

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
               buf = new byte[readInt(1, in)];
               in.read(buf);
               deltas.add(new DataBlock(offset, buf));
               offset += buf.length;
               break;
            case OP_LITERAL_N2:
               buf = new byte[readInt(2, in)];
               in.read(buf);
               deltas.add(new DataBlock(offset, buf));
               offset += buf.length;
               break;
            case OP_LITERAL_N4:
               buf = new byte[readInt(4, in)];
               in.read(buf);
               deltas.add(new DataBlock(offset, buf));
               offset += buf.length;
               break;
            case OP_COPY_N4_N4:
               int newOff = readInt(4, in);
               int bs = readInt(4, in);
               deltas.add(new Offsets(offset, newOff, bs));
               offset += bs;
               break;
            default:
               throw new IOException("Bad delta command: 0x" +
                  Integer.toHexString(command));
         }
      }
      throw new IOException("Didn't recieve RS_OP_END.");
   }

   public void
   rebuildFile(File basis, Collection deltas, OutputStream out)
   throws IOException {
      File temp = Rebuilder.rebuildFile(basis, deltas);
      FileInputStream fin = new FileInputStream(temp);
      byte[] buf = new byte[1024*1024];
      int len = 0;
      while ((len = fin.read(buf)) != -1) {
         out.write(buf, 0, len);
      }
   }

   // Own methods.
   // -----------------------------------------------------------------

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

   private static void version(PrintStream out) {
      out.println(PROGNAME + " (Jarsync " + JARSYNC_VERSION + ")");
      out.println("Copyright (C) 2002 Casey Marshall.");
      out.println();
      out.println("Jarsync comes with NO WARRANTY, to the extent permitted by law.");
      out.println("You may redistribute copies of Jarsync under the terms of the GNU");
      out.println("General Public License.  See the file `COPYING' for details.");
   }

   private static void
   writeCopy(Offsets off, OutputStream out) throws IOException {
      out.write(OP_COPY_N4_N4);
      writeInt(off.getNewOffset(), 4, out);
      writeInt(off.getBlockLength(), out);
   }

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

   private static int integerLength(long l) {
      if ((l & ~0xffL) == 0) {
         return 1;
      } else if ((l & ~0xffffL) == 0) {
         return 2;
      } else if ((l & ~0xffffffffL) == 0) {
         return 4;
      } else if ((1 & ~0xffffffffffffffffL) == 0) {
         return 8;
      }
      return 4;
   }

   private static int readInt(int len, InputStream in) throws IOException {
      int i = 0;
      for (int j = len-1; j >= 0; j--) {
         int k = in.read();
         if (k == -1) throw new EOFException();
         i |= (k&0xff) << 8*j;
      }
      return i;
   }

   private static int readInt(InputStream in) throws IOException {
      int i = 0;
      for (int j = 3; j >= 0; j--) {
         int k = in.read();
         if (k == -1) throw new EOFException();
         i |= (k&0xff) << 8*j;
      }
      return i;
   }

   private static void
   writeInt(long l, int len, OutputStream out) throws IOException {
      for (int i = len-1; i >= 0; i--) {
         out.write((int) ((l >>> i*8) & 0xff));
      }
   }

   private static void writeInt(int i, OutputStream out) throws IOException {
      out.write((byte) ((i >>> 24) & 0xff));
      out.write((byte) ((i >>> 16) & 0xff));
      out.write((byte) ((i >>>  8) & 0xff));
      out.write((byte) ( i & 0xff));
   }
}
