/* DefaultHandler.java -- non-interactive default callback handler.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This program is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.callbacks;

import java.util.Locale;

import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;

/**
 * This trivial implementation of {@link CallbackHandler} sets its
 * {@link Callback} arguments to default values, with no user interaction.
 */
public class DefaultCallbackHandler extends AbstractCallbackHandler
{

  // Constructor.
  // -------------------------------------------------------------------------

  public DefaultCallbackHandler()
  {
    super();
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  protected void handleChoice(ChoiceCallback c)
  {
    c.setSelectedIndex(c.getDefaultChoice());
  }

  protected void handleConfirmation(ConfirmationCallback c)
  {
    if (c.getOptionType() == ConfirmationCallback.YES_NO_OPTION)
      c.setSelectedIndex(ConfirmationCallback.NO);
    else if (c.getOptionType() == ConfirmationCallback.YES_NO_CANCEL_OPTION)
      c.setSelectedIndex(ConfirmationCallback.NO);
    else if (c.getOptionType() == ConfirmationCallback.OK_CANCEL_OPTION)
      c.setSelectedIndex(ConfirmationCallback.OK);
    else
      c.setSelectedIndex(c.getDefaultOption());
  }

  protected void handleLanguage(LanguageCallback c)
  {
    c.setLocale(Locale.getDefault());
  }

  protected void handleName(NameCallback c)
  {
    c.setName(System.getProperty("user.name"));
  }

  protected void handlePassword(PasswordCallback c)
  {
    c.setPassword(new char[0]);
  }

  protected void handleTextInput(TextInputCallback c)
  {
    c.setText("");
  }

  protected void handleTextOutput(TextOutputCallback c)
  {
  }
}
