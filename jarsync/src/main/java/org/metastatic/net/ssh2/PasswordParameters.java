/* PasswordParameters -- Password-based SSH2 authentication.
   Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>

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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;

import org.metastatic.callbacks.DefaultCallbackHandler;

/**
 * This class represents password-based authentication in the SSH 2
 * protocol. The password is typically the user's login password for the
 * remote host.
 *
 * @version $Revision$
 */
public class PasswordParameters extends Authentication.Parameters
  implements SSH2Constants
{

  // Constants and variables.
  // -------------------------------------------------------------------------

  public static final Authentication.Type TYPE =
    Authentication.Type.PASSWORD;

  /** The password itself. */
  protected char[] password;

  // Constructor.
  // -------------------------------------------------------------------------

  public PasswordParameters(String username, char[] password, String service)
  {
    super(username, service);
    this.password = password;
  }

  public PasswordParameters(String username, String service)
  {
    this(username, null, service);
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  /**
   * Clear the password char array, filling it with NULL characters.
   */
  public void clearPassword()
  {
    if (password != null)
      {
        java.util.Arrays.fill(password, '\u0000');
      }
  }

  // Concrete implementations of Authentication.Parameters.
  // -------------------------------------------------------------------------

  public Authentication.Type getType()
  {
    return TYPE;
  }

  public void writeRequestPacket(PacketOutputStream pout, int type)
  {
    System.err.println("password=" + password);
    if (password == null)
      {
        password = askPassword(username);
        if (password == null)
          password = new char[0];
      }
    pout.reset();
    pout.write(SSH_MSG_USERAUTH_REQUEST);
    pout.writeUTF8(username);
    pout.writeUTF8(service);
    pout.writeASCII(TYPE.getName());
    pout.writeBoolean(false);
    pout.writeUTF8(new String(password));
  }

  // Own methods. ------------------------------------------------------------

  /**
   * Ask the user for a password.
   *
   * @param user The user name.
   * @return The password.
   */
  private char[] askPassword(String user)
  {
    CallbackHandler handler = new DefaultCallbackHandler();
    try
      {
        Class c = Class.forName(System.getProperty("org.metastatic.net.ssh2.password.handler"));
        handler = (CallbackHandler) c.newInstance();
      }
    catch (Exception x) { }
    PasswordCallback passwd = new PasswordCallback(user + "'s password: ", false);
    try
      {
        handler.handle(new Callback[] { passwd });
      }
    catch (Exception x)
      {
        x.printStackTrace();
      }
    return passwd.getPassword();
  }
}
