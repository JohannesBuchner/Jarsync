/* SWTCallbackHandler.java -- SWT callback handler.
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
   USA

In addition, as a special exception, the copyright holders of this
library give permission to link the code of this program with the
Standard Widget Toolkit (SWT) library of the Eclipse project
<http://www.eclipse.org/> (or with modified versions of the SWT
library that use the same license of the SWT library), and distribute
linked combinations of the two. You must obey the GNU General Public
License in all respects for all of the code used other than the SWT
library. If you modify this file, you may extend this exception to
your version of the file, but you are not obligated to do so. If you
do not wish to do so, delete this exception statement from your
version.  */


package org.metastatic.callbacks;

import java.util.Locale;

import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SWTCallbackHandler extends AbstractCallbackHandler
  implements SelectionListener
{

  // Fields.
  // -------------------------------------------------------------------------

  private Button ok, cancel, yes, no;
  private Text input;
  private List choices;
  private static final int COMMAND_NONE   = 0;
  private static final int COMMAND_OK     = 1;
  private static final int COMMAND_CANCEL = 2;
  private static final int COMMAND_YES    = 3;
  private static final int COMMAND_NO     = 4;
  private int command;

  // Constructor.
  // -------------------------------------------------------------------------

  public SWTCallbackHandler()
  {
    super();
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  protected void handleChoice(ChoiceCallback c)
  {
    Display display = Display.getCurrent();
    boolean newDis = false;
    if (display == null)
      {
        display = new Display();
        newDis = true;
      }
    Shell shell = new Shell(display, SWT.DIALOG_TRIM);
    GridLayout layout = new GridLayout(2, false);
    shell.setLayout(layout);
    Label label = new Label(shell, SWT.NONE);
    label.setText(c.getPrompt());
    GridData labelData = new GridData(GridData.FILL_HORIZONTAL);
    labelData.horizontalSpan = 2;
    label.setLayoutData(labelData);
    choices = new List(shell,
      (c.allowMultipleSelections() ? SWT.MULTI : SWT.SINGLE) | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    choices.setItems(c.getChoices());
    if (c.getDefaultChoice() >= 0 && c.getDefaultChoice() < choices.getItemCount())
      {
        choices.select(c.getDefaultChoice());
      }
    GridData choicesData = new GridData(GridData.FILL_BOTH);
    choicesData.horizontalSpan = 2;
    choices.setLayoutData(choicesData);
    choices.addSelectionListener(this);
    ok = new Button(shell, SWT.PUSH);
    ok.setText(messages.getString("callback.ok"));
    ok.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
    ok.addSelectionListener(this);
    cancel = new Button(shell, SWT.PUSH);
    cancel.setText(messages.getString("callback.cancel"));
    cancel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    cancel.addSelectionListener(this);
    shell.pack();
    shell.setSize(400, 400);
    shell.open();
    center(shell);
    command = COMMAND_NONE;
    while (!shell.isDisposed())
      {
        if (!display.readAndDispatch())
          display.sleep();
        if (command == COMMAND_OK)
          {
            if (!c.allowMultipleSelections())
              c.setSelectedIndex(choices.getSelectionIndex());
            else
              c.setSelectedIndexes(choices.getSelectionIndices());
            shell.dispose();
            break;
          }
        if (command == COMMAND_CANCEL)
          {
            shell.dispose();
            break;
          }
      }
    if (newDis)
      display.dispose();
  }

  protected void handleConfirmation(ConfirmationCallback c)
  {
    Display display = Display.getCurrent();
    boolean newDis = false;
    if (display == null)
      {
        display = new Display();
        newDis = true;
      }
    Shell shell = new Shell(display, SWT.DIALOG_TRIM);
    center(shell);
    if (c.getOptionType() != ConfirmationCallback.UNSPECIFIED_OPTION)
      {
        int mbStyle = 0;
        switch (c.getOptionType())
          {
          case ConfirmationCallback.OK_CANCEL_OPTION:
            mbStyle = SWT.OK | SWT.CANCEL;
            break;
          case ConfirmationCallback.YES_NO_OPTION:
            mbStyle = SWT.YES | SWT.NO;
            break;
          case ConfirmationCallback.YES_NO_CANCEL_OPTION:
            mbStyle = SWT.YES | SWT.NO | SWT.CANCEL;
            break;
          }
        switch (c.getMessageType())
          {
          case ConfirmationCallback.ERROR:
            mbStyle |= SWT.ICON_ERROR;
            break;
          case ConfirmationCallback.INFORMATION:
            mbStyle |= SWT.ICON_INFORMATION;
            break;
          case ConfirmationCallback.WARNING:
            mbStyle |= SWT.ICON_WARNING;
            break;
          }
        MessageBox mb = new MessageBox(shell, mbStyle);
        mb.setMessage(c.getPrompt());
        switch (mb.open())
          {
          case SWT.CANCEL:
            c.setSelectedIndex(ConfirmationCallback.CANCEL);
            break;
          case SWT.NO:
            c.setSelectedIndex(ConfirmationCallback.NO);
            break;
          case SWT.OK:
            c.setSelectedIndex(ConfirmationCallback.OK);
            break;
          case SWT.YES:
            c.setSelectedIndex(ConfirmationCallback.YES);
            break;
          }
      }
    if (newDis)
      display.dispose();
  }

  protected void handleLanguage(LanguageCallback c)
  {
    System.err.println(">>handleLanguage");
    Locale[] locales = Locale.getAvailableLocales();
    String[] languages = new String[locales.length];
    Locale def = Locale.getDefault();
    int defind = 0;
    for (int i = 0; i < locales.length; i++)
      {
        StringBuffer lang =
          new StringBuffer(locales[i].getDisplayLanguage(locales[i]));
        String country = locales[i].getDisplayCountry(locales[i]);
        String variant = locales[i].getDisplayVariant(locales[i]);
        if (country.length() > 0 && variant.length() > 0)
          {
            lang.append(" (");
            lang.append(country);
            lang.append(", ");
            lang.append(variant);
            lang.append(")");
          }
        else if (country.length() > 0)
          {
            lang.append(" (");
            lang.append(country);
            lang.append(")");
          }
        else if (variant.length() > 0)
          {
            lang.append(" (");
            lang.append(variant);
            lang.append(")");
          }
        languages[i] = lang.toString();
        if (locales[i].equals(def))
          defind = i;
      }
    ChoiceCallback c2 =
      new ChoiceCallback(messages.getString("callback.language"), languages,
                         defind, false);
    handleChoice(c2);
    c.setLocale(def);
    if (c2.getSelectedIndexes() != null && c2.getSelectedIndexes().length > 0)
      {
        int index = c2.getSelectedIndexes()[0];
        if (index >= 0 && index < locales.length)
          c.setLocale(locales[index]);
      }
  }

  protected void handleName(NameCallback c)
  {
    System.err.println(">>handleName");
    Display display = Display.getCurrent();
    boolean newDis = false;
    if (display == null)
      {
        display = new Display();
        newDis = true;
      }
    Shell shell = new Shell(display, SWT.DIALOG_TRIM);
    GridLayout layout = new GridLayout(2, false);
    shell.setLayout(layout);
    Label label = new Label(shell, SWT.NONE);
    label.setText(c.getPrompt());
    GridData labelData = new GridData(GridData.FILL_BOTH);
    labelData.horizontalSpan = 2;
    label.setLayoutData(labelData);
    input = new Text(shell, SWT.SINGLE | SWT.BORDER);
    input.addSelectionListener(this);
    if (c.getDefaultName() != null)
      {
        input.setText(c.getDefaultName());
      }
    GridData inputData = new GridData(GridData.FILL_HORIZONTAL);
    inputData.horizontalSpan = 2;
    input.setLayoutData(inputData);
    ok = new Button(shell, SWT.PUSH);
    ok.setText(messages.getString("callback.ok"));
    ok.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
    ok.addSelectionListener(this);
    cancel = new Button(shell, SWT.PUSH);
    cancel.setText(messages.getString("callback.cancel"));
    cancel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    cancel.addSelectionListener(this);
    shell.pack();
    shell.open();
    shell.setSize(300, shell.getSize().y);
    center(shell);
    command = COMMAND_NONE;
    while (!shell.isDisposed())
      {
        if (!display.readAndDispatch())
          display.sleep();
        if (command == COMMAND_OK)
          {
            c.setName(input.getText());
            shell.dispose();
            break;
          }
        if (command == COMMAND_CANCEL)
          {
            shell.dispose();
            break;
          }
      }
    if (newDis)
      display.dispose();
  }

  protected void handlePassword(PasswordCallback c)
  {
    System.err.println(">>handlePassword");
    Display display = Display.getCurrent();
    boolean newDis = false;
    if (display == null)
      {
        display = new Display();
        newDis = true;
      }
    Shell shell = new Shell(display, SWT.DIALOG_TRIM);
    GridLayout layout = new GridLayout(2, false);
    shell.setLayout(layout);
    Label label = new Label(shell, SWT.NONE);
    label.setText(c.getPrompt());
    GridData labelData = new GridData(GridData.FILL_BOTH);
    labelData.horizontalSpan = 2;
    label.setLayoutData(labelData);
    input = new Text(shell, SWT.SINGLE | SWT.BORDER);
    input.addSelectionListener(this);
    input.setEchoChar('*');
    GridData inputData = new GridData(GridData.FILL_HORIZONTAL);
    inputData.horizontalSpan = 2;
    input.setLayoutData(inputData);
    ok = new Button(shell, SWT.PUSH);
    ok.setText(messages.getString("callback.ok"));
    ok.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
    ok.addSelectionListener(this);
    cancel = new Button(shell, SWT.PUSH);
    cancel.setText(messages.getString("callback.cancel"));
    cancel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    cancel.addSelectionListener(this);
    shell.pack();
    shell.open();
    shell.setSize(300, shell.getSize().y);
    center(shell);
    command = COMMAND_NONE;
    while (!shell.isDisposed())
      {
        if (!display.readAndDispatch())
          display.sleep();
        if (command == COMMAND_OK)
          {
            c.setPassword(input.getText().toCharArray());
            shell.dispose();
            break;
          }
        if (command == COMMAND_CANCEL)
          {
            shell.dispose();
            break;
          }
      }
    if (newDis)
      display.dispose();
  }

  protected void handleTextInput(TextInputCallback c)
  {
    Display display = Display.getCurrent();
    boolean newDis = false;
    if (display == null)
      {
        display = new Display();
        newDis = true;
      }
    Shell shell = new Shell(display, SWT.DIALOG_TRIM);
    GridLayout layout = new GridLayout(2, false);
    shell.setLayout(layout);
    Label label = new Label(shell, SWT.NONE);
    label.setText(c.getPrompt());
    GridData labelData = new GridData(GridData.FILL_HORIZONTAL);
    labelData.horizontalSpan = 2;
    label.setLayoutData(labelData);
    Text input = new Text(shell, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    if (c.getDefaultText() != null)
      {
        input.setText(c.getDefaultText());
      }
    GridData inputData = new GridData(GridData.FILL_BOTH);
    inputData.horizontalSpan = 2;
    input.setLayoutData(inputData);
    ok = new Button(shell, SWT.PUSH);
    ok.setText(messages.getString("callback.ok"));
    ok.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
    ok.addSelectionListener(this);
    cancel = new Button(shell, SWT.PUSH);
    cancel.setText(messages.getString("callback.cancel"));
    cancel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    cancel.addSelectionListener(this);
    shell.pack();
    shell.open();
    shell.setSize(400, 272);
    center(shell);
    command = COMMAND_NONE;
    while (!shell.isDisposed())
      {
        if (!display.readAndDispatch())
          display.sleep();
        if (command == COMMAND_OK)
          {
            c.setText(input.getText());
            shell.dispose();
            break;
          }
        if (command == COMMAND_CANCEL)
          {
            shell.dispose();
            break;
          }
      }
    if (newDis)
      display.dispose();
  }

  protected void handleTextOutput(TextOutputCallback c)
  {
    Display display = Display.getCurrent();
    boolean newDis = false;
    if (display == null)
      {
        display = new Display();
        newDis = true;
      }
    Shell shell = new Shell(display, SWT.DIALOG_TRIM);
    GridLayout layout = new GridLayout(1, false);
    shell.setLayout(layout);
    Text text = new Text(shell, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    text.setText(c.getMessage());
    text.setEditable(false);
    text.setLayoutData(new GridData(GridData.FILL_BOTH));
    ok = new Button(shell, SWT.PUSH);
    ok.setText(messages.getString("callback.ok"));
    ok.addSelectionListener(this);
    ok.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
    shell.pack();
    shell.open();
    shell.setSize(400, 272);
    center(shell);
    command = COMMAND_NONE;
    while (!shell.isDisposed())
      {
        if (!display.readAndDispatch())
          display.sleep();
        if (command == COMMAND_OK)
          {
            shell.dispose();
          }
      }
    if (newDis)
      display.dispose();
  }

  private void center(Shell shell)
  {
    Rectangle bounds = shell.getDisplay().getBounds();
    Point newPos = new Point(bounds.width / 2, bounds.height / 2);
    Point size = shell.getSize();
    newPos.x -= size.x / 2;
    newPos.y -= size.y / 2;
    shell.setLocation(newPos);
  }

  // SelectionListener interface implementation.
  // -------------------------------------------------------------------------

  public void widgetSelected(SelectionEvent se)
  {
    System.err.println("<<widgetSelected - " + se + ">>");
    if (se.getSource() == ok)
      command = COMMAND_OK;
    else if (se.getSource() == cancel)
      command = COMMAND_CANCEL;
    else if (se.getSource() == yes)
      command = COMMAND_YES;
    else if (se.getSource() == no)
      command = COMMAND_NO;
    else
      command = COMMAND_NONE;
  }

  public void widgetDefaultSelected(SelectionEvent se)
  {
    System.err.println("<<widgetDefaultSelected - " + se + ">>");
    if (se.getSource() == input || se.getSource() == choices)
      command = COMMAND_OK;
    else
      command = COMMAND_NONE;
  }
}
