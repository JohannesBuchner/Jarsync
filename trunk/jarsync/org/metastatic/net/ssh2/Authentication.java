/* Authentication -- User authentication methods for SSH 2.
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

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class forms the base for various methods for authenticating
 * users to SSH servers. The relevant method is {@link
 * #authenticate(Parameters)}, and the concrete implementations of {@link
 * Parameters}.
 *
 * @version $Revision$
 */
public class Authentication implements SSH2Constants
{

  // Constants and variables.
  // -------------------------------------------------------------------------

  /** The connection over which data is sent. */
  protected Connection conn;

  /** The authentication methods that the server reports can continue. */
  protected String can_continue;

  /** Whether or not an authentication attempt partially succeeded. */
  protected boolean partial_success;

  /** The banner the server sends, if any. */
  protected String userauth_banner;

  /** Whether or not the server requests a password change. */
  protected boolean change_requested;

  /** The server's password change prompt. */
  protected String change_prompt;

  // Constructors.
  // -------------------------------------------------------------------------

  /**
   * Create a new authentication object.
   *
   * @param conn The {@link Connection} over which authentication
   *    attempts are sent.
   */
  protected Authentication(Connection conn)
  {
    this.conn = conn;
    change_requested = false;
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  /**
   * Authenticate a user to the server. Returns true if the
   * authentication succeeds. If this method returns false, the
   * authentication attempt MAY have been "partially successful".
   *
   * @param params The {@link Authentication.Parameters} object that
   *    represents the authentication attempt.
   * @return true If the server accepts the authentication.
   * @throws java.io.IOException If the transport fails.
   */
  boolean authenticate(Parameters params) throws IOException
  {
    change_requested = false;
    byte msg_type;
    PacketInputStream pin = conn.getPacketInputStream();
    PacketOutputStream pout = conn.getPacketOutputStream();
    OutputStream out = conn.getOutputStream();

    if (params.getType() == Type.PUBLIC_KEY)
      {
        params.writeRequestPacket(pout, params.SEND_PUBLICKEY);
        out.write(pout.toBinaryPacket());
        do
          {
            pin.startPacket();
            msg_type = (byte) pin.read();
            switch (msg_type)
              {
              case SSH_MSG_USERAUTH_SUCCESS:
                pin.endPacket();
                return true;
              case SSH_MSG_USERAUTH_PK_OK:
                pin.readString();
                pin.readString();
                params.writeRequestPacket(pout, params.SEND_SIGNATURE);
                out.write(pout.toBinaryPacket());
                pin.endPacket();
                break;
              case SSH_MSG_USERAUTH_FAILURE:
                can_continue = pin.readASCII();
                partial_success = pin.readBoolean();
                pin.endPacket();
                return false;
              case SSH_MSG_IGNORE:
                pin.readString();
                pin.endPacket();
                break;
              case SSH_MSG_DEBUG:
                pin.readBoolean();
                pin.readUTF8();
                pin.readASCII();
                pin.endPacket();
                break;
              case SSH_MSG_DISCONNECT:
                int code = pin.readUInt32();
                String desc = pin.readUTF8();
                pin.readASCII();
                throw new DisconnectException(code, desc);
              default:
                throw new SSH2Exception("Got bad message type " + msg_type);
              }
          }
        while (true);
      }
    else if (params.getType() == Type.PASSWORD)
      {
        params.writeRequestPacket(pout, params.SEND_PASSWORD);
        out.write(pout.toBinaryPacket());
        do
          {
            pin.startPacket();
            msg_type = (byte) pin.read();
            switch (msg_type)
              {
              case SSH_MSG_USERAUTH_SUCCESS:
                pin.endPacket();
                return true;
              case SSH_MSG_USERAUTH_FAILURE:
                can_continue = pin.readASCII();
                partial_success = pin.readBoolean();
                pin.endPacket();
                return false;
              case SSH_MSG_USERAUTH_PASSWD_CHANGEREQ:
                change_requested = true;
                change_prompt = pin.readUTF8();
                pin.readUTF8(); // XXX language tag
                pin.endPacket();
                return false;
              case SSH_MSG_IGNORE:
                pin.readString();
                pin.endPacket();
                break;
              case SSH_MSG_DEBUG:
                pin.readBoolean();
                pin.readUTF8();
                pin.readASCII();
                pin.endPacket();
                break;
              case SSH_MSG_USERAUTH_BANNER:
                userauth_banner = pin.readUTF8();
                pin.readASCII();
                pin.endPacket();
                break;
              case SSH_MSG_DISCONNECT:
                int code = (int) pin.readUInt32();
                String desc = pin.readUTF8();
                pin.readASCII();
                throw new DisconnectException(code, desc);
              default:
                throw new SSH2Exception("Got bad message type " + msg_type);
              }
          }
        while (true);
      }
    else if (params.getType() == Type.HOST_BASED)
      {
        // ...
      }
    else if (params.getType() == Type.NONE)
      {
        params.writeRequestPacket(pout, 0);
        out.write(pout.toBinaryPacket());
        do
          {
            pin.startPacket();
            msg_type = (byte) pin.read();
            switch (msg_type)
              {
              case SSH_MSG_USERAUTH_SUCCESS:
                pin.endPacket();
                return true;
              case SSH_MSG_USERAUTH_FAILURE:
                can_continue = pin.readASCII();
                partial_success = pin.readBoolean();
                pin.endPacket();
                return false;
              case SSH_MSG_IGNORE:
                pin.readString();
                pin.endPacket();
                break;
              case SSH_MSG_DEBUG:
                pin.readBoolean();
                pin.readUTF8();
                pin.readASCII();
                pin.endPacket();
                break;
              case SSH_MSG_USERAUTH_BANNER:
                userauth_banner = pin.readUTF8();
                pin.readASCII();
                pin.endPacket();
                break;
              case SSH_MSG_DISCONNECT:
                int code = (int) pin.readUInt32();
                String desc = pin.readUTF8();
                pin.readASCII();
                throw new DisconnectException(code, desc);
              default:
                throw new SSH2Exception("Got bad message type " + msg_type);
              }
          }
        while (true);
      }
    return false;
  }

  /**
   * Returns whether or not the authentication attempt was partially
   * successful.
   *
   * @return true If authentication was partially successful.
   */
  boolean partialSuccess()
  {
    return partial_success;
  }

  /**
   * Return a comma-separated list of authentications that the server
   * will still accept.
   *
   * @return The list of authentications that can continue.
   */
  String getCanContinue()
  {
    return can_continue;
  }

  /**
   * Returns whether or not the server has requested a change of
   * password.
   *
   * @return true If the server requested a password change.
   */
  boolean passwordChangeRequested()
  {
    return change_requested;
  }

  /**
   * Returns the password change prompt that the server sends when it
   * requests a change of password.
   *
   * @return The prompt.
   */
  String getPasswordChangePrompt()
  {
    return change_prompt;
  }

  // Inner classes.
  // -------------------------------------------------------------------------

  /**
   * Method-specific parameters for a type of authentication.
   */
  public static abstract class Parameters
  {

    // Constants and variables.
    // -----------------------------------------------------------------------

    /** The first stage of public key authentication. */
    public static final int SEND_PUBLICKEY = 0;

    /** The second (and last) stage of public key authentication. */
    public static final int SEND_SIGNATURE = 1;

    /** Password authentication. */
    public static final int SEND_PASSWORD  = 2;

    /** A new password. */
    public static final int SEND_NEWPASSWD = 3;

    /**
     * The username to be sent for the authentication.
     */
    protected String username;

    /**
     * The service that will be requested after authentication.
     */
    protected String service;

    // Constructor.
    // -----------------------------------------------------------------------

    /**
     * Create new parameters.
     *
     * @param username The username to be used for authentication.
     * @param service  The service being requested.
     */
    protected Parameters(String username, String service)
    {
      this.username = username;
      this.service = service;
    }

    // Instance methods.
    // -----------------------------------------------------------------------

    /**
     * Get the user name.
     *
     * @return The user name.
     */
    public String getUsername()
    {
      return username;
    }

    /**
     * Get the service name.
     *
     * @param The service name.
     */
    public String getService()
    {
      return service;
    }

    // Abstract methods to be implemented by concrete subclasses.
    // -----------------------------------------------------------------------

    /**
     * Get the {@link Type} of this authentication.
     */
    public abstract Type getType();

    /**
     * Write the method-specific packet to the given packet output
     * stream.
     */
    public abstract void writeRequestPacket(PacketOutputStream pout, int what)
      throws SSH2Exception;
  }

  /**
   * A type of authentication, one of public key, password, host-based,
   * or none. This is basically a type-safe enumeration of the possible
   * authentication methods.
   */
  static final class Type
  {
    private String name;
    private Type() { }
    private Type(String s) { name = s; }
    public static final Type PUBLIC_KEY = new Type("publickey");
    public static final Type PASSWORD   = new Type("password");
    public static final Type HOST_BASED = new Type("hostbased");
    public static final Type NONE       = new Type("none");
    public String getName() { return name; }
  }
}
