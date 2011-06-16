/* Shell -- An implementation of an interactive shell.
   Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>

This file is a part of HUSH, the Hopefully Uncomprehensible Shell.

HUSH is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your
option) any later version.

HUSH is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with HUSH; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.net.ssh2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.metastatic.util.Reseeder;
import org.metastatic.util.TerminalInfo;

/**
 * Run an interactive shell. This class implements both the input and
 * the output for its channel, only forwarding input, output, and error
 * streams from the server to the streams specified in the constructor.
 * The session only ends when the server tells us it has ended, or if
 * the user enters the disconnect escape sequence.
 *
 * @version $Revision$
 */
public class Shell extends Thread implements ChannelListener
{

  // Constants and variables.
  // -------------------------------------------------------------------------

  /**
   * The channel we use to communicate with the server.
   */
  protected Channel c;

  /**
   * Bytes we read in from the terminal.
   */
  protected byte[] in_buf;

  /**
   * True if our input loop is running.
   */
  protected boolean running;

  /**
   * The terminal we are writing to (if any).
   */
  protected TerminalInfo terminal;

  /**
   * True if the client has requested a pseudo-terminal.
   */
  protected boolean pty;

  /**
   * Our input stream.
   */
  protected InputStream in;

  /**
   * Our output stream.
   */
  protected OutputStream out;

  /**
   * Our error stream.
   */
  protected OutputStream err;

  /**
   * We call the {@link java.lang.Object#notifyAll()} method of this
   * object when this session ends.
   */
  protected Object lock;

  /**
   * The escape character for interactive sessions.
   */
  protected char escape;

  /**
   * Whether or not to pay attention to escape sequences.
   */
  protected boolean escape_enabled;

  /**
   * We just saw a newline; an escape sequence may be next.
   */
  protected boolean start_of_line;

  /**
   * We've seen the escape character, the completing character (or
   * something else) is next.
   */
  protected boolean start_of_esc;

  /**
   * The thread that waits for terminal dimension changes.
   */
  protected TerminalSize term_size;

  /** The reseeder. */
  protected Reseeder reseeder;

  // Constructors.
  // -------------------------------------------------------------------------

  /**
   * Create a new shell.
   *
   * @param terminal The terminal connected to the output of this
   *    shell.
   * @param in The {@link java.io.InputStream} from which to read
   *    input.
   * @param out The {@link java.io.OutputStream} that will receive
   *    standard output data.
   * @param err The {@link java.io.OutputStream} that will receive
   *    standard error data.
   * @param pty true if a pseudo-terminal should be allocated (the
   *    parameter <code>terminal</code> must not be null if this is
   *    true).
   * @param escape The escape character to interpret. This may be
   *    <code>'\\u0000'</code> to disable escape characters.
   */
  public Shell(TerminalInfo terminal, InputStream in, OutputStream out,
               OutputStream err, boolean pty, char escape)
  {
    in_buf = new byte[4096];
    this.terminal = terminal;
    this.pty = pty;
    this.in = in;
    this.out = out;
    this.err = err;
    if ((this.escape = escape) != '\u0000')
      {
        escape_enabled = true;
      }
    else
      {
        escape_enabled = false;
      }
    start_of_line = true;
    running = true;
    lock = new Object();
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  public void setReseeder(Reseeder reseeder)
  {
    this.reseeder = reseeder;
  }

  /**
   * Returns whether or not this shell is still running.
   *
   * @return true If this shell is running.
   */
  public boolean isRunning()
  {
    return running;
  }

  /**
   * Get a lock that, when the {@link java.lang.Object#wait()} method
   * is invoked, blocks until this shell disconnects.
   *
   * @return The lock.
   */
  public Object getLock()
  {
    return lock;
  }

  public void run()
  {
    while (running)
      {
        try
          {
            int len = in.read(in_buf);
            if (len == 0)
              {
                yield();
                continue;
              }
            if (len == -1)
              {
                synchronized (c)
                  {
                    c.writeEOF();
                  }
                running = false;
              }
            else if (escape_enabled)
              {
                for (int i = 0; i < len; i++)
                  {
                    if (reseeder != null)
                      {
                        reseeder.tap(in_buf[i]);
                      }
                    if (escape_enabled && start_of_esc)
                      {
                        switch (in_buf[i])
                          {
                          case '.':
                            synchronized (c)
                              {
                                c.disconnect(
                                  SSH2Constants.SSH_DISCONNECT_BY_APPLICATION);
                              }
                            running = false;
                            synchronized (lock)
                              {
                                lock.notifyAll();
                              }
                            return;
                          case '?':
                            showEscapes();
                            break;
                          case 'R':
                            out.write("\nRe-exchanging keys.\n".getBytes());
                            synchronized (c)
                              {
                                c.rekey();
                              }
                            break;
                          case 'S':
                            showStats();
                            break;
                          case 'V':
                            showVersion();
                            break;
                          case '-':
                            escape_enabled = false;
                            break;
                          default:
                            if (in_buf[i] != escape)
                              {
                                synchronized (c)
                                  {
                                    c.writeData(new byte[] { (byte) escape },
                                                0, 1);
                                    c.writeData(in_buf, i, 1);
                                  }
                              }
                            else
                              {
                                synchronized (c)
                                  {
                                    c.writeData(in_buf, i, 1);
                                  }
                              }
                            start_of_line = false;
                          }
                        start_of_esc = false;
                        continue;
                      }
                    if (escape_enabled && start_of_line
                        && in_buf[i] == escape)
                      {
                        start_of_esc = true;
                        continue;
                      }
                    if (in_buf[i] == '\n')
                      {
                        start_of_line = true;
                      }
                    else
                      {
                        start_of_line = false;
                      }
                    synchronized (c)
                      {
                        c.writeData(in_buf, i, 1);
                      }
                  }
              }
            else
              {
                synchronized (c)
                  {
                    c.writeData(in_buf, 0, len);
                  }
              }
          }
        catch (java.io.IOException ioe)
          {
          }
      }
  }

  // Instance methods implementing ChannelListener.
  // -------------------------------------------------------------------------

  public void startInput(Channel c)
  {
    this.c = c;
    if (pty)
      {
        c.allocatePTY(terminal);
      }
    c.startShell();
    this.start();
    if (pty)
      {
        term_size = new TerminalSize(terminal, c);
        term_size.start();
      }
  }

  public void openFailed(SSH2Exception ex)
  {
    System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
  }

  public void input(ChannelEvent e)
  {
    try
      {
        out.write(e.getData());
      }
    catch (java.io.IOException ioe)
      {
      }
  }

  public void extended(ChannelEvent e, int type)
  {
    if (type == SSH2Constants.SSH_EXTENDED_DATA_STDERR)
      {
        try
          {
            err.write(e.getData());
          }
        catch (java.io.IOException ioe)
          {
          }
      }
   }

  public void requestSuccess(ChannelEvent e)
  {
    Debug.info("Request success");
  }

  public void requestFailure(ChannelEvent e)
  {
    Debug.info("Request failure");
  }

  public void eof()
  {
    term_size.setRunning(false);
    running = false;
  }

  public void exit(int status)
  {
    Debug.info("EXIT (" + status + ")");
    if (term_size != null)
      {
        term_size.setRunning(false);
      }
    running = false;
  }

  public void closeChannel(int channel)
  {
    Debug.info("Close channel.");
    synchronized (c)
      {
        c.close();
      }
    synchronized (lock)
      {
        lock.notifyAll();
      }
  }

  // Own methods. ------------------------------------------------------------

  /**
   * Write our escape sequences to the output stream.
   */
  private void showEscapes()
  {
    String escape_string = null;
    if (!Character.isISOControl(escape))
      {
        escape_string = String.valueOf(escape);
      }
    else
      {
        String s = Integer.toHexString((int) escape);
        for (int i = 0; i < 4 - s.length(); i++)
          {
            s = "0" + s;
          }
        escape_string = "0x" + s;
      }
    try
      {
        out.write('\n');
        out.write("Supported escape sequences:\n".getBytes());
        out.write((escape_string+".   Disconnect.\n").getBytes());
        out.write((escape_string+"?   Show these escapes.\n").getBytes());
        out.write((escape_string
                   +"R   Re-exchange keys (version 2 only).\n").getBytes());
        out.write((escape_string+
                   "S   Show connection statistics.\n").getBytes());
        out.write((escape_string+
                   "V   Show system version information.\n").getBytes());
        out.write((escape_string+
                   "-   Permanently disable escape character.\n").getBytes());
        out.write((escape_string+escape_string+
                   "   Send the escape character itself.\n").getBytes());
        out.write("(Escapes are only recognized after a newline.)\n".getBytes());
      }
    catch (IOException ioe)
      {
      }
  }

  /**
   * Show some simple statistics, including the number of bytes sent
   * and received, and how long the current keys have been in use.
   */
  private void showStats()
  {
    try
      {
        Connection conn = c.getConnection();
        out.write('\n');
        out.write("Total bytes sent with current keys = ".getBytes());
        out.write(String.valueOf(
          conn.getPacketOutputStream().getTotalBytesOut()).getBytes());
        out.write('\n');
        out.write("Total bytes received with current keys = ".getBytes());
        out.write(String.valueOf(
          conn.getPacketInputStream().getTotalBytesIn()).getBytes());
        out.write('\n');
        long mins = (System.currentTimeMillis() - conn.KEXTimestamp())
          / 60000;
        out.write(("Current keys have been in use for " + mins +
                   " minute" + ((mins==1)?"":"s") + "\n").getBytes());
      }
    catch (IOException ioe)
      {
      }
  }

  private void showVersion()
  {
    try
      {
        Connection conn = c.getConnection();
        Configuration cf = conn.getPacketInputStream().config;
        String s = "Decryption cipher: " + cf.cipher.name() + "\n";
        out.write('\n');
        out.write(s.getBytes());
        s = "Server MAC: " + cf.mac.name() + "\n";
        out.write(s.getBytes());
        s = (cf.flater != null) ? "Compression in effect.\n\n" :
          "No compression in effect.\n\n";
        out.write(s.getBytes());

        cf = conn.getPacketOutputStream().config;
        s = "Encryption cipher: " + cf.cipher.name() + "\n";
        out.write(s.getBytes());
        s = "Client MAC: " + cf.mac.name() + "\n";
        out.write(s.getBytes());
        s = (cf.flater != null) ? "Compression in effect.\n\n" :
          "No compression in effect.\n\n";
        out.write(s.getBytes());

        out.write("VM info:\n".getBytes());
        out.write(("\tversion=" + System.getProperty("java.vm.version")+"\n")
                  .getBytes());
        out.write(("\tvendor=" + System.getProperty("java.vm.vendor")+"\n")
                  .getBytes());
        out.write(("\tname=" + System.getProperty("java.vm.name")+"\n\n")
                  .getBytes());
        out.write(("OS: " + System.getProperty("os.name") + "-" +
                   System.getProperty("os.version") + " " +
                   System.getProperty("os.arch") + "\n").getBytes());
      }
    catch (IOException ioe)
      {
      }
  }

// Inner classes.
  // -------------------------------------------------------------------------

  /**
   * Low-priority thread to wait for the terminal size to change.
   */
  private static class TerminalSize extends Thread
  {

    // Constants and variables.
    // -----------------------------------------------------------------------

    /** The terminal to check. */
    protected TerminalInfo terminal;

    /** The channel this shell is running over. */
    protected Channel c;

    /** True if we are running. */
    protected boolean running;

    // Constructors.
    // -----------------------------------------------------------------------

    /**
     * @param terminal The terminal to periodically check for a
     *    dimension change.
     * @param c The channel over which this shell is running.
     */
    TerminalSize(TerminalInfo terminal, Channel c)
    {
      this.terminal = terminal;
      this.c = c;
    }

    // Instance methods.
    // -----------------------------------------------------------------------

    /**
     * Set the {@link #running} flag.
     *
     * @param running The new flag value.
     */
    void setRunning(boolean running)
    {
      this.running = running;
    }

    /**
     * Run this thread. It checks whether or not the terminal's
     * dimensions have changed every five seconds, and sends an
     * appropriate message to the server.
     */
    public void run()
    {
      setPriority(MIN_PRIORITY);
      running = true;
      java.awt.Dimension chars, pixels;
      chars = terminal.getSizeChars();
      pixels = terminal.getSizePixels();
      // We ask dimensionChanged because a native implementation of
      // TerminalInfo will take a long time to fetch the actual size.
      while (running)
        {
          if (terminal.dimensionChanged())
            {
              c.windowChange(terminal.getSizeChars(),
                             terminal.getSizePixels());
            }
          // We don't need to check that often. 5 seconds is probably
          // enough.
          try
            {
              sleep(5000);
            }
          catch (InterruptedException ie)
            {
            }
        }
    }
  }
}
