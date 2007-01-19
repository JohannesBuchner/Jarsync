/* Exec -- Execute a command on a remote machine.
   Copyright (C) 2002, 2003  Casey Marshall <rsdio@metastatic.org>

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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.net.Socket;

import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;

import java.util.StringTokenizer;

import org.metastatic.net.RemoteProcess;
import org.metastatic.util.TerminalInfo;

/**
 * This is an implementation of the "exec" command for SSH 2 sessions.
 * It implements both the {@link ChannelListener} interface for input
 * and uses an implementation of {@link java.lang.Thread} to handle
 * output.
 *
 * @version $Revision$
 */
public class Exec extends Thread implements ChannelListener
{

  // Constants and variables.
  // -------------------------------------------------------------------------

  private static final String VERSION = "HUSH_0.2";

  private static final String VERSION_PROPERTY =
    "org.metastatic.net.ssh2.Exec.version";

  private static final String KNOWN_HOSTS_PROPERTY =
    "org.metastatic.net.ssh2.Exec.known.hosts";

  private static final String DEFAULT_KNOWN_HOSTS =
    System.getProperty("user.home") + System.getProperty("file.separator") +
    ".ssh" + System.getProperty("file.separator") + "known_hosts2";

  private static final int BUFFER_SIZE = 1024;

  // Streams to be passed to the caller.
  protected PipedOutputStream cmdIn;
  protected PipedInputStream cmdOut;
  protected PipedInputStream cmdErr;

  // Our streams.
  protected PipedInputStream in;
  protected PipedOutputStream out;
  protected PipedOutputStream err;

  protected String cmd;
  protected boolean req_pty;
  protected TerminalInfo terminal;
  protected String[] envp;

  protected boolean running;
  protected byte[] buffer;
  protected int len;

  protected Object lock;
  protected Channel c;
  protected int exit_value;
  protected boolean we_sent_close;

  // Constructors.
  // -------------------------------------------------------------------------

  private Exec()
  {
    this(null, null, null, false);
  }

  public Exec(TerminalInfo terminal, String cmd, String[]envp, boolean req_pty)
  {
    this.cmd = cmd;
    this.envp = envp;
    this.req_pty = req_pty;
    this.terminal = terminal;
    running = false;
    buffer = new byte[BUFFER_SIZE];
    in = new PipedInputStream();
    cmdOut = new PipedInputStream();
    cmdErr = new PipedInputStream();
    try
      {
        cmdIn = new PipedOutputStream(in);
        out = new PipedOutputStream(cmdOut);
        err = new PipedOutputStream(cmdErr);
      }
    catch (IOException shouldNotHappen)
      {
        throw new Error(shouldNotHappen.toString());
      }
    lock = new Object();
    we_sent_close = false;
  }

  // Class methods.
  // -------------------------------------------------------------------------

  /**
   * Execute a remote process.
   *
   * @see #exec(java.lang.String,int,java.lang.String[],java.lang.String[],org.metastatic.util.TerminalInfo,org.metastatic.net.ssh2.Authentication.Parameters[])
   * @param host The host to connect to.
   * @param argv The command vector to pass.
   * @param user The user to log in as.
   * @param password The user's password.
   * @return A new {@link Exec.Process} for controlling the
   *    newly-created remote process.
   */
  //public static RemoteProcess exec(String host, String[] argv, String user, char[] password)
  //  throws IOException
  //{
  //  return exec(host, argv, null, null, user, password);
  //}

  /**
   * Execute a remote process.
   *
   * @see #exec(java.lang.String,int,java.lang.String[],java.lang.String[],org.metastatic.util.TerminalInfo,org.metastatic.net.ssh2.Authentication.Parameters[])
   * @param host The remote host to connect to.
   * @param argv The command vector to pass.
   * @param envp A list of environment variables to pass to the remote
   *    host, each in the form "NAME=VALUE".
   * @param term A {@link org.metastatic.util.TerminalInfo} to use for
   *    allocating a pseudo-terminal for the remote process.
   * @param auth_attempts A list of authentication methods to try, in
   *    order.
   * @return A new {@link Exec.Process} for controlling the
   *    newly-created remote process.
   */
   public static RemoteProcess exec(String host, String[] argv, String[] envp, TerminalInfo term,
                                    Authentication.Parameters[] auth_attempts,
                                    KnownHosts known_hosts)
     throws IOException
  {
    return exec(host, 22, argv, envp, term, auth_attempts, known_hosts);
  }

  /**
   * Execute a remote process.
   *
   * @see #exec(java.lang.String,int,java.lang.String[],java.lang.String[],org.metastatic.util.TerminalInfo,org.metastatic.net.ssh2.Authentication.Parameters[])
   * @param host The remote host to connect to.
   * @param argv The command vector to pass.
   * @param envp A list of environment variables to pass to the remote
   *    host, each in the form "NAME=VALUE".
   * @param term A {@link org.metastatic.util.TerminalInfo} to use for
   *    allocating a pseudo-terminal for the remote process.
   * @param user The user to log in as.
   * @param password The user's password.
   * @return A new {@link Exec.Process} for controlling the
   *    newly-created remote process.
   */
  public static RemoteProcess exec(String host, String[] argv, String[] envp,
                                  TerminalInfo term, String user, char[] password)
    throws IOException
  {
    PasswordParameters pp = new PasswordParameters(user, password,
                                                   "ssh-connection");
    KnownHosts kh = getKnownHosts();
    return exec(host, 22, argv, envp, term,
                new Authentication.Parameters[] { pp }, kh);
  }

  /**
   * Execute a remote process.
   *
   * @param host The remote host to connect to.
   * @param port The port to connect to.
   * @param argv The command vector to pass.
   * @param envp A list of environment variables to pass to the remote
   *    host, each in the form "NAME=VALUE".
   * @param term A {@link org.metastatic.util.TerminalInfo} to use for
   *    allocating a pseudo-terminal for the remote process.
   * @param auth_attempts A list of authentication methods to try, in
   *    order.
   * @param known_hosts The list of known host keys.
   * @return A new {@link Exec.Process} for controlling the
   *    newly-created remote process.
   */
  public static RemoteProcess exec(String host, int port, String[] argv,
                                   String[] envp, TerminalInfo term,
                                   Authentication.Parameters[] auth_attempts,
                                   KnownHosts known_hosts)
    throws IOException
  {
    if (host == null || argv == null || argv.length == 0
        || auth_attempts == null || known_hosts == null)
      throw new IllegalArgumentException();
    Socket sock = new Socket(host, port);
    java.io.InputStream in = sock.getInputStream();
    java.io.OutputStream out = sock.getOutputStream();

    String our_version = "SSH-2.0-" + System.getProperty(VERSION_PROPERTY, VERSION);
    out.write(our_version.getBytes());
    out.write('\r');  out.write('\n');

    StringBuffer server_version = new StringBuffer();
    int ch;
    while ((ch = in.read()) != '\n')
      {
        if (ch == -1)
          {
            throw new java.io.EOFException();
          }
        else if (ch != '\r')
          {
            server_version.append((char) ch);
          }
      }
    String remote = server_version.toString();
    int remote_major = 0;
    int remote_minor = 0;
    try
      {
        String version = remote.substring(4, remote.indexOf('-', 5));
        remote_major = Integer.parseInt(
          version.substring(0, version.indexOf('.')));
        remote_minor = Integer.parseInt(
          version.substring(version.indexOf('.')+1));
      }
    catch (NumberFormatException nfe)
      {
        throw new SSH2Exception("malformed remote version \""
                                + remote + "\"");
      }
    catch (IndexOutOfBoundsException ioob)
      {
        throw new SSH2Exception("malformed remote version \""
                                + remote + "\"");
      }
    if (!(remote_major == 2 || (remote_major == 1 && remote_minor == 99)))
      {
        sock.close();
        throw new SSH2Exception("protocol version mismatch");
      }
    Connection conn = new Connection(in, out, remote, our_version);
    conn.beginConnection();

    if (!conn.getSignatureResult())
      {
        throw new SSH2Exception("server signature not validated");
      }
    String host_alias = host + "," + sock.getInetAddress().getHostAddress();
    PublicKey host_key = conn.getHostKey();
    String host_alg = null;
    if (host_key instanceof DSAPublicKey)
      host_alg = "ssh-dss";
    else
      host_alg = "ssh-rsa";
    System.out.println("checking host alias " + host_alias + " alg=" + host_alg);
    if (!known_hosts.containsAlias(host_alg, host_alias)
        || !known_hosts.containsKey(host_alg, host_alias, host_key))
      {
        throw new SSH2Exception("server's key is not trusted");
      }

    boolean auth = false;
    for (int i = 0; i < auth_attempts.length; i++)
      {
        String cc = conn.getCanContinue();
        if (cc != null)
          {
            StringTokenizer tok = new StringTokenizer(cc, ",");
            boolean can_try = false;
            while (tok.hasMoreTokens())
              {
                if (tok.nextToken().equals(auth_attempts[i].getType().getName()))
                  {
                    can_try = true;
                    break;
                  }
              }
            if (!can_try)
              continue;
          }
        System.err.println("trying " + auth_attempts[i]);
        auth = conn.userAuthentication(auth_attempts[i]);
        if (auth) break;
      }
    if (!auth)
      {
        if (conn.connected())
          {
            conn.disconnect(
              SSH2Constants.SSH_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE);
          }
        throw new SSH2Exception("all authentication attempts failed");
      }
    conn.beginChannelMode();
    StringBuffer cmd = new StringBuffer();
    for (int i = 0; i < argv.length; i++)
      {
        cmd.append(argv[i]);
        if (i+1 < argv.length) cmd.append(" ");
      }

    Exec ex = new Exec(term, cmd.toString(), envp, term != null);
    conn.newChannel(new SessionSpec(), ex);
    return ex.new Process(host, argv, envp, ex);
  }

  private static KnownHosts getKnownHosts()
  {
    KnownHosts hosts = new KnownHosts();
    String file = System.getProperty(KNOWN_HOSTS_PROPERTY, DEFAULT_KNOWN_HOSTS);
    try
      {
        hosts.load(new FileInputStream(file));
      }
    catch (IOException ioe)
      {
      }
    return hosts;
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  /**
   * Get the {@link java.io.OutputStream} for output to the remote
   * process.
   *
   * @return An {@link java.io.OutputStream} for output to the remote
   *    process.
   */
  public OutputStream getOutputStream()
  {
    return cmdIn;
  }

  /**
   * Get the {@link java.io.InputStream} for receiving the process's
   * standard output data.
   *
   * @return An {@link java.io.InputStream} for receiving standard
   *    output data.
   */
  public InputStream getInputStream()
  {
    return cmdOut;
  }

  /**
   * Get the {@link java.io.InputStream} for receiving the process's
   * standard error data.
   *
   * @return An {@link java.io.InputStream} for receiving standard
   *    error data.
   */
  public InputStream getErrorStream()
  {
    return cmdErr;
  }

  /**
   * Returns true if the process is still running.
   *
   * @return true If the process is still running.
   */
  public boolean isRunning()
  {
    return running;
  }

  /**
   * Return the remote process's exit value.
   *
   * @return The exit value.
   */
  public int exitValue()
  {
    return exit_value;
  }

  /**
   * Get a lock that, when the {@link java.lang.Object#wait()} method
   * is called, will block until the remote process finishes.
   *
   * @return The lock.
   */
  public Object getLock()
  {
    return lock;
  }

  /**
   * Send a SIGKILL to the remote process.
   */
  public void kill()
  {
    running = false;
    we_sent_close = true;
    c.signal(SSH2Constants.SIG_KILL);
    c.writeEOF();
    c.close();
  }

  /**
   * End execution. This sends an EOF to the remote process, then
   * closes the associated channel.
   */
  void end()
  {
    running = false;
    we_sent_close = true;
    c.writeEOF();
    c.close();
  }

  // Instance methods overriding Thread.

  public void run()
  {
    while (running)
      {
        try
          {
            if (in.available() == 0)
              {
                yield();
                continue;
              }
            len = Math.min(in.available(), buffer.length);
            len = in.read(buffer, 0, len);
            if (len == -1)
              {
                c.writeEOF();
                running = false;
                return;
              }
            /*for (int i = 0; i < len; i++)
              {
                if (buffer[i] == 4)
                  {
                    c.writeData(buffer, 0, i);
                    end();
                    running = false;
                    return;
                  }
              }
              */
            c.writeData(buffer, 0, len);
          }
        catch (IOException ioe)
          {
            Debug.warning(ioe.getMessage());
          }
      }
  }

  // Instance methods implementing ChannelListener.
  // -----------------------------------------------------------------------

  public void startInput(Channel c)
  {
    this.c = c;
    if (envp != null)
      {
        for (int i = 0; i < envp.length; i++)
          {
            int ind = envp[i].indexOf('=');
            if (ind != -1)
              {
                String name = envp[i].substring(0, ind);
                String value = envp[i].substring(ind+1);
                c.env(name, value);
              }
          }
      }
    if (req_pty)
      {
        c.allocatePTY(terminal);
      }
    c.exec(cmd);
    running = true;
    start();
  }

  public void openFailed(SSH2Exception e)
  {
    try
      {
        err.write(e.getMessage().getBytes());
      }
    catch (IOException ioe)
      {
      }
    exit_value = 1;
  }

  public void input(ChannelEvent e)
  {
    try
      {
        out.write(e.getData());
      }
    catch (IOException ioe)
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
        catch (IOException ioe)
          {
          }
      }
  }

  public void requestSuccess(ChannelEvent e) { }
  public void requestFailure(ChannelEvent e) { }

  public void eof()
  {
    running = false;
    try
      {
        in.close();
        err.close();
        out.close();
        cmdIn.close();
        cmdOut.close();
        cmdErr.close();
      }
    catch (IOException ioe)
      {
        ioe.printStackTrace();
      }
  }

  public void exit(int value)
  {
    exit_value = value;
  }

  public void closeChannel(int channel)
  {
    if (!we_sent_close)
      c.close();
    else
      c.disconnect(SSH2Constants.SSH_DISCONNECT_BY_APPLICATION);
    synchronized (lock)
      {
        lock.notifyAll();
      }
  }

// Inner classes.
  // -------------------------------------------------------------------------

  /**
   * Implementation of org.metastatic.net.RemoteProcess for SSH 2
   * connections.
   */
  class Process extends RemoteProcess
  {

    // Constants and variables.
    // -----------------------------------------------------------------------

    /**
     * The underlying {@link Exec} object.
     */
    Exec ex;

    // Constructors.
    // -----------------------------------------------------------------------

    /**
     * Creates a new remote process.
     *
     * @param host The host on which the process is running.
     * @param argv The remote process's argument vector.
     * @param envp The environment variables passed to the remote
     *    process.
     * @param ex   The underlying {@link Exec} object.
     */
    Process(String host, String[] argv, String[] envp, Exec ex)
    {
      super(host, argv, envp);
      this.ex = ex;
    }

    // Instance methods implementing java.lang.Process.
    // -----------------------------------------------------------------------

    public InputStream getInputStream()
    {
      return ex.getInputStream();
    }

    public InputStream getErrorStream()
    {
      return ex.getErrorStream();
    }

    public OutputStream getOutputStream()
    {
      return ex.getOutputStream();
    }

    public void destroy()
    {
      ex.kill();
    }

    public int exitValue()
    {
      if (ex.isRunning())
        throw new IllegalThreadStateException("remote process still running");
      return ex.exitValue();
    }

    public int waitFor() throws InterruptedException
    {
      Object lock = ex.getLock();
      synchronized (lock)
        {
          lock.wait();
        }
      return ex.exitValue();
    }
  }
}
