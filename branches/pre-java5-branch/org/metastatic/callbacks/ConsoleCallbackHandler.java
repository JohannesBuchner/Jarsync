/* ConsoleCallbackHandler.java -- console callback handler.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;

/**
 * An implementation of {@link CallbackHandler} that reads and writes
 * information to and from <code>System.in</code> and <code>System.out</code>.
 */
public class ConsoleCallbackHandler extends AbstractCallbackHandler
{

  // Fields.
  // -------------------------------------------------------------------------

  private static final int NATIVE_NOT_LOADED = 0;
  private static final int NATIVE_LOAD_FAILED = 1;
  private static final int NATIVE_LOADED = 2;
  private int native_state = NATIVE_NOT_LOADED;

  // Constructors.
  // -------------------------------------------------------------------------

  public ConsoleCallbackHandler()
  {
    super();
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  protected void handleChoice(ChoiceCallback c) throws IOException
  {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    System.out.println(c.getPrompt());
    System.out.print('(');
    String[] choices = c.getChoices();
    for (int i = 0; i < choices.length; i++)
      {
        System.out.print(choices[i]);
        if (i != choices.length - 1)
          System.out.print(", ");
      }
    System.out.print(") ");
    if (c.getDefaultChoice() >= 0 && c.getDefaultChoice() < choices.length)
      {
        System.out.print('[');
        System.out.print(choices[c.getDefaultChoice()]);
        System.out.print("] ");
      }
    String reply = in.readLine();
    if (reply == null || reply.length() == 0)
      {
        c.setSelectedIndex(c.getDefaultChoice());
        return;
      }
    if (!c.allowMultipleSelections())
      {
        for (int i = 0; i < choices.length; i++)
          {
            if (reply.trim().equals(choices[i]))
              {
                c.setSelectedIndex(i);
                return;
              }
          }
        c.setSelectedIndex(c.getDefaultChoice());
      }
    else
      {
        TreeSet indices = new TreeSet();
        StringTokenizer tok = new StringTokenizer(reply, ",");
        String[] replies = new String[tok.countTokens()];
        int idx = 0;
        while (tok.hasMoreTokens())
          {
            replies[idx++] = tok.nextToken().trim();
          }
        for (int i = 0; i < choices.length; i++)
          for (int j = 0; j < replies.length; i++)
            {
              if (choices[i].equals(replies[j]))
                {
                  indices.add(new Integer(i));
                }
            }
        if (indices.size() == 0)
          c.setSelectedIndex(c.getDefaultChoice());
        else
          {
            int[] ii = new int[indices.size()];
            int i = 0;
            for (Iterator it = indices.iterator(); it.hasNext(); )
              ii[i++] = ((Integer) it.next()).intValue();
            c.setSelectedIndexes(ii);
          }
      }
  }

  protected void handleConfirmation(ConfirmationCallback c) throws IOException
  {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    if (c.getPrompt() != null)
      System.out.println(c.getPrompt());
    String[] choices = null;
    int[] values = null;
    switch (c.getOptionType())
      {
      case ConfirmationCallback.OK_CANCEL_OPTION:
        System.out.print(messages.getString("callback.okCancel"));
        choices = new String[] {
          messages.getString("callback.ok"),
          messages.getString("callback.cancel"),
          messages.getString("callback.shortOk"),
          messages.getString("callback.shortCancel")
        };
        values = new int[] {
          ConfirmationCallback.OK, ConfirmationCallback.CANCEL,
          ConfirmationCallback.OK, ConfirmationCallback.CANCEL
        };
        break;
      case ConfirmationCallback.YES_NO_CANCEL_OPTION:
        System.out.print(messages.getString("callback.yesNoCancel"));
        choices = new String[] {
          messages.getString("callback.yes"),
          messages.getString("callback.no"),
          messages.getString("callback.cancel"),
          messages.getString("callback.shortYes"),
          messages.getString("callback.shortNo"),
          messages.getString("callback.shortCancel")
        };
        values = new int[] {
          ConfirmationCallback.YES, ConfirmationCallback.NO,
          ConfirmationCallback.CANCEL, ConfirmationCallback.YES,
          ConfirmationCallback.NO, ConfirmationCallback.CANCEL
        };
        break;
      case ConfirmationCallback.YES_NO_OPTION:
        System.out.print(messages.getString("callback.yesNo"));
        choices = new String[] {
          messages.getString("callback.yes"),
          messages.getString("callback.no"),
          messages.getString("callback.shortYes"),
          messages.getString("callback.shortNo")
        };
        values = new int[] {
          ConfirmationCallback.YES, ConfirmationCallback.NO,
          ConfirmationCallback.YES, ConfirmationCallback.NO
        };
        break;
      case ConfirmationCallback.UNSPECIFIED_OPTION:
        choices = c.getOptions();
        values = new int[choices.length];
        for (int i = 0; i < values.length; i++)
          values[i] = i;
        System.out.print('(');
        for (int i = 0; i < choices.length; i++)
          {
            System.out.print(choices[i]);
            if (i != choices.length - 1)
              System.out.print(", ");
          }
        System.out.print(") [");
        System.out.print(choices[c.getDefaultOption()]);
        System.out.print("] ");
        break;
      default:
        throw new IllegalArgumentException();
      }
    String reply = in.readLine();
    if (reply == null)
      {
        c.setSelectedIndex(c.getDefaultOption());
        return;
      }
    reply = reply.trim();
    for (int i = 0; i < choices.length; i++)
      if (reply.equalsIgnoreCase(choices[i]))
        {
          c.setSelectedIndex(values[i]);
          return;
        }
    c.setSelectedIndex(c.getDefaultOption());
  }

  protected void handleLanguage(LanguageCallback c) throws IOException
  {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    System.out.println(messages.getString("callback.language"));
    String reply = null;
    reply = in.readLine();
    if (reply == null)
      {
        c.setLocale(Locale.getDefault());
      }
    else
      {
        c.setLocale(new Locale(reply.trim()));
      }
  }

  protected void handleName(NameCallback c) throws IOException
  {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    System.out.print(c.getPrompt());
    String name = in.readLine();
    if (name != null)
      c.setName(name.trim());
  }

  protected void handlePassword(PasswordCallback c) throws IOException
  {
    System.out.print(c.getPrompt());
    BufferedReader in =
      new BufferedReader(new InputStreamReader(System.in));
    String pass = in.readLine();
    System.out.println();
    c.setPassword(pass.toCharArray());
  }

  protected void handleTextInput(TextInputCallback c) throws IOException
  {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    System.out.print(c.getPrompt());
    String text = in.readLine();
    if (text != null)
      c.setText(text);
  }

  protected void handleTextOutput(TextOutputCallback c)
  {
    System.out.print(c.getMessage());
  }

  //protected native String _getpass(String prompt);
}
