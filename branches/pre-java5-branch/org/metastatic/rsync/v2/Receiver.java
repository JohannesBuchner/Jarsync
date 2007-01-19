/* Receiver -- File receiving methods.
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

/*
 * Based on rsync-2.5.5.
 *
 * Rsync Copyright (C) 1992-2001 Andrew Tridgell
 *                     1996 Paul Mackerras
 *                     2001, 2002 Martin Pool
 *                     and others.
 */

package org.metastatic.rsync.v2;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.Delta;
import org.metastatic.rsync.DeltaDecoder;
import org.metastatic.rsync.GeneratorEvent;
import org.metastatic.rsync.GeneratorListener;
import org.metastatic.rsync.GeneratorStream;
import org.metastatic.rsync.ListenerException;
import org.metastatic.rsync.Offsets;
import org.metastatic.rsync.RebuilderEvent;
import org.metastatic.rsync.RebuilderListener;
import org.metastatic.rsync.RebuilderStream;
import org.metastatic.rsync.Util;

/**
 * The receiver process. The receiver is the one who generates the
 * checksums for the to-be-synched files, and receives the deltas. These
 * two processes (checksum generation and applying deltas) are usually
 * run in parallel -- one thread generates sums for one file while
 * another thread rebuilds another file.
 *
 * @version $Revision$
 */
public class Receiver implements Constants, GeneratorListener, RebuilderListener
{

  // Constants and fields.
  // -----------------------------------------------------------------------

  private static final Logger logger =
    Logger.getLogger(Receiver.class.getName());

  private final MultiplexedInputStream in;
  private final MultiplexedOutputStream out;

  private final Object recvLock = new Object();

  private Statistics stats;

  private final Configuration genConfig, recvConfig;

  private final int remoteVersion;

  private int[] retry;

  private int retryIndex;

  private int genPhase = 0, recvPhase = 0;
  private int saveBlockLength;

  private RandomAccessFile rebuildFile;

  private ChecksumEncoder checkOut;
  private DeltaDecoder deltasIn;

  // Constructors.
  // -----------------------------------------------------------------------

  /**
   * Create a new receiver object.
   *
   * @param in       The input stream from the Sender.
   * @param out      The output stream to the Sender.
   * @param config   The configuration to use.
   * @param amServer true if this is the server process.
   */
  public Receiver(MultiplexedInputStream in, MultiplexedOutputStream out,
                  Configuration config, int remoteVersion, boolean amServer)
  {
    this.in = in;
    this.out = out;
    if (amServer)
      logger.addAppender(new RsyncAppender(out));
    this.genConfig = (Configuration) config.clone();
    this.recvConfig = (Configuration) config.clone();
    this.remoteVersion = remoteVersion;
    stats = new Statistics();
  }

  // Instance methods.
  // -----------------------------------------------------------------------

  /**
   * Return the statistics for this session.
   *
   * @return The statistics.
   */
  public Statistics getStatistics()
  {
    return stats;
  }

  /**
   * Set the pre-initialized statistics object fer this session.
   *
   * @param stats The statistics object to use.
   */
  public void setStatistics(Statistics stats)
  {
    if (stats != null) this.stats = stats;
  }

  /**
   * Generate the checksums for a list of files and send them to the
   * other side.
   *
   * @param files The files to generate the checksums for.
   * @throws IOException If an I/O error occurs.
   */
  public void generateFiles(List files) throws IOException
  {
    logger.debug("generateFiles starting thread=" + Thread.currentThread());
    genPhase = 0;

    retryIndex = 0;
    retry = new int[files.size()];
    for (int i = 0; i < retry.length; i++)
      retry[i] = -1;

    for (int i = 0; i < files.size(); i++)
      {
        FileInfo f = (FileInfo) files.get(i);
        sendSums(new File(f.filename()), i);
      }

    genPhase++;
    out.writeInt(-1);
    out.flush();
    logger.debug("generateFiles phase=" + genPhase);

    if (remoteVersion >= 13)
      {
        while (recvPhase == 0)
          {
            try
              {
                synchronized (recvLock)
                  {
                    recvLock.wait();
                  }
              }
            catch (InterruptedException ignore) { }
          }
        genConfig.strongSumLength = SUM_LENGTH;
        // in newer versions of the protocol the files can cycle
        // through the system more than once to catch initial checksum
        // errors.
        //
        // Rsync uses a socket with two processes talking to one
        // another. Here, since we are running two threads with the
        // same object, we just use an array of integers.
        for (int i = 0; i < retryIndex && retry[i] != -1; i++)
          {
            FileInfo f = (FileInfo) files.get(retry[i]);
            sendSums(new File(f.filename()), retry[i]);
          }
        genPhase++;
        logger.debug("generateFiles phase=" + genPhase);
        out.writeInt(-1);
        out.flush();
      }
  }

  /**
   * Recieve the deltas from the other side, and rebuild the target
   * files.
   *
   * @param files The list of {@link java.io.File}s to be received.
   * @throws IOException If an I/O error occurs.
   */
  public void receiveFiles(List files) throws IOException
  {
    logger.debug("receiveFiles starting thread=" + Thread.currentThread());
    recvPhase = 0;

    while (true)
      {
        int i = in.readInt();
        if (i == -1)
          {
            if (recvPhase == 0 && remoteVersion >= 13)
              {
                recvPhase++;
                logger.debug("receiveFiles phase=" + recvPhase);
                synchronized (recvLock)
                  {
                    recvLock.notify();
                  }
                continue;
              }
            break;
          }

        if (i < 0 || i >= files.size())
          {
            String msg = "Invalid file index " + i + " in receiveFiles count="
               + files.size();
            logger.fatal(msg);
            throw new IOException(msg);
          }
        logger.debug("receiveFiles read index=" + i);

        FileInfo finfo = (FileInfo) files.get(i);
        File f = new File(finfo.filename());

        stats.num_transferred_files++;
        stats.total_transferred_size += f.length();

        if (!receiveData(f))
          {
            if (genConfig.strongSumLength == SUM_LENGTH)
              {
                logger.error("File corruption in " + f.getName()
                             + ". File changed during transfer?");
              }
            else
              {
                // We need to retry this file. See the generateFiles
                // method above for how these integers are used.
                logger.warn("redoing " + f.getName() + "(" + i + ")");
                retry[retryIndex++] = i;
              }
          }
      }

    logger.debug("receiveFiles finished");
  }

  public void update(GeneratorEvent e) throws ListenerException
  {
    logger.debug("emitting sum=" + e.getChecksumPair());
    try
      {
        checkOut.write(e.getChecksumPair());
      }
    catch (IOException ioe)
      {
        throw new ListenerException(ioe);
      }
  }

  public void update(RebuilderEvent e) throws ListenerException
  {
    try
      {
        logger.debug("inserting data at=" + e.getOffset());
        rebuildFile.seek(e.getOffset());
        rebuildFile.write(e.getData());
      }
    catch (IOException ioe)
      {
        throw new ListenerException(ioe);
      }
  }

// Own methods.
  // -------------------------------------------------------------------------

  /**
   * Generate and send the checksums for a file.
   *
   * @param f The file to generate the checksums for.
   * @param i The index of this file.
   * @throws IOException If an I/O error occurs.
   */
  private void sendSums(File f, int i) throws IOException
  {
    int blen = genConfig.blockLength;
    if (genConfig.blockLength == BLOCK_LENGTH)
      {
        int l = (int) (f.length() / 10000) & ~15;
        if (l < genConfig.blockLength)
          l = genConfig.blockLength;
        if (l > CHUNK_SIZE / 2)
          l = CHUNK_SIZE / 2;
        genConfig.blockLength = l;
      }

    out.writeInt(i);
    if (f.exists())
      {
        int count = (int) (f.length() / genConfig.blockLength);
        int rem = (int) (f.length() % genConfig.blockLength);
        if (rem > 0)
          count++;
        out.writeInt(count);
        out.writeInt(genConfig.blockLength);
        out.writeInt(rem);
        out.flush();
        logger.debug("writing sums i=" + i + " count=" + count +
                     " blockLen=" + genConfig.blockLength + " rem=" +
                     rem);

        FileInputStream fin = new FileInputStream(f);
        byte[] buf = new byte[CHUNK_SIZE];
        int len = 0;
        checkOut = new ChecksumEncoder(genConfig, out);
        GeneratorStream gen = new GeneratorStream(genConfig);
        gen.addListener(this);
        logger.debug("generating=" + fin);
        try
          {
            logger.debug("about to read; available=" + fin.available());
            while ((len = fin.read(buf)) > 0)
              {
                logger.debug("read " + len + " bytes from file");
                gen.update(buf, 0, len);
                out.flush();
              }
            gen.doFinal();
          }
        catch (ListenerException le)
          {
            throw (IOException) le.getCause();
          }
      }
    else
      {
        out.writeInt(0);
        out.writeInt(genConfig.blockLength);
        out.writeInt(0);
        out.flush();
      }
    genConfig.blockLength = blen;
  }

  /**
   * Receive the deltas for a file, and rebuild it.
   *
   * @param f The file to rebuild.
   * @throws IOException If an I/O error occurs.
   */
  private boolean receiveData(File f) throws IOException
  {
    int count = in.readInt();
    int n = in.readInt();
    int remainder = in.readInt();
    long offset = 0;
    byte[] file_sum1, file_sum2;
    byte[] data = new byte[CHUNK_SIZE];

    logger.debug("receiveData count=" + count + " n=" + n +
                 " remainder=" + remainder);

    recvConfig.blockLength = n;
    DeltaDecoder deltasIn = new PlainDeltaDecoder(recvConfig, in);
    RebuilderStream rebuilder = new RebuilderStream();
    rebuilder.addListener(this);
    try
      {
        rebuilder.setBasisFile(f);
      }
    catch (FileNotFoundException fnfe)
      {
        // Ok, we might be writing a new file.
      }
    Delta delta = null;
    File newf = null;
    if (f.getParentFile() != null)
      newf = File.createTempFile(".jarsync", ".tmp", f.getParentFile());
    else
      newf = File.createTempFile(".jarsync", ".tmp",
                                 new File(System.getProperty("user.dir")));
    rebuildFile = new RandomAccessFile(newf, "rw");
    try
      {
        int i = 0;
        while ((delta = deltasIn.read()) != null)
          {
            i++;
            if (i == count && (delta instanceof Offsets) && remainder > 0)
              {
                delta = new Offsets(((Offsets) delta).getOldOffset(),
                                    ((Offsets) delta).getNewOffset(),
                                    remainder);
              }
            if (delta instanceof Offsets)
              stats.matched_data += delta.getBlockLength();
            else
              stats.literal_data += delta.getBlockLength();
            rebuilder.update(delta);
          }
      }
    catch (ListenerException le)
      {
        throw (IOException) le.getCause();
      }
    rebuilder.doFinal();
    rebuildFile.close();
    if (!newf.renameTo(f))
      {
        throw new IOException("cannot rename " + newf + " to " + f);
      }

    if (remoteVersion >= 14)
      {
        MessageDigest md = null;
        try
          {
            md = MessageDigest.getInstance("BrokenMD4");
          }
        catch (NoSuchAlgorithmException nsae)
          {
            logger.fatal("could not create message digest");
            throw new Error(nsae);
          }
        FileInputStream fin = new FileInputStream(f);
        int i = 0;
        md.update(recvConfig.checksumSeed);
        while ((i = fin.read(data)) != -1)
          {
            md.update(data, 0, i);
          }
        file_sum1 = md.digest();
        file_sum2 = new byte[file_sum1.length];
        in.read(file_sum2);
        logger.debug("file_sum1=" + Util.toHexString(file_sum1));
        logger.debug("file_sum2=" + Util.toHexString(file_sum2));
        return Arrays.equals(file_sum1, file_sum2);
      }

    return true;
  }

   /** Partial byte count in {@link #receiveToken(byte[])}. */
   private int residue = 0;

   /**
    * Receive a single "token": either (1) an integer with the most
    * significant bit unset, then that many bytes (a "insert" command),
    * (2) an integer with the most significant bit set, which represents
    * that the current block should become block -(<i>i</i> + 1) (a
    * "copy" command), or (3) the integer 0, meaning the end of file.
    *
    * @param buf The byte buffer to store literal data in (if any).
    * @return The token, as described above.
    * @throws IOException If an I/O error occurs.
    */
   private int receiveToken(byte[] buf) throws IOException
   {
      // XXX compression.
      if (residue == 0) {
         int i = in.readInt();
         if (i <= 0) return i;
         residue = i;
      }

      int n = Math.min(CHUNK_SIZE, residue);
      n = in.read(buf, 0, n);
      residue -= n;
      return n;
   }
}
