/* CertificateCallback.java -- callback for questionable certificates.
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

import java.security.cert.Certificate;
import javax.security.auth.callback.Callback;

/**
 * <p>A callback for determining whether or not a certificate chain, the
 * trust of which could not be established, should be accepted by an
 * application.</p>
 *
 * <p>The questionable certificate chain is included in this callback,
 * and handlers that accept this callback should provide some mechanism
 * for displaying these certificates when asking for confirmation. When
 * the user allows the certificates, the callback handler should set the
 * selected index to {@link #ACCEPT}. Otherwise, {@link #REJECT}.</p>
 */
public class CertificateCallback implements Callback
{

  // Fields.
  // -------------------------------------------------------------------------

  /**
   * <p>Index for when the certificates in question should be accepted.</p>
   */
  public static final int ACCEPT = 0;

  /**
   * <p>Index for when the certificates in question should be rejected.</p>
   */
  public static final int REJECT = 1;

  private final String prompt;
  private final Certificate[] certs;
  private int index;
  private final int defaultIndex;

  // Constructors.
  // -------------------------------------------------------------------------

  /**
   * <p>Create a new callback with the given prompt and certificates. The
   * default index will be set to {@link #REJECT}.</p>
   *
   * @param prompt The prompt.
   * @param certs The certificates.
   * @throws IllegalArgumentException If either parameter is null or empty.
   */
  public CertificateCallback(String prompt, Certificate[] certs)
  {
    this(prompt, certs, REJECT);
  }

  /**
   * <p>Create a new callback with the given prompt, certificates, and default
   * index.</p>
   *
   * @param prompt The prompt.
   * @param certs The certificates.
   * @param defaultIndex The default index.
   * @throws IllegalArgumentException If the prompt or certificates parameter
   *   is null or empty, or if the default index is not {@link #ACCEPT} nor
   *   {@link #REJECT}.
   */
  public CertificateCallback(String prompt, Certificate[] certs, int defaultIndex)
  {
    if (prompt == null || prompt.length() == 0)
      {
        throw new IllegalArgumentException("no prompt");
      }
    if (certs == null || certs.length == 0)
      {
        throw new IllegalArgumentException("no certificates");
      }
    if (defaultIndex != ACCEPT && defaultIndex != REJECT)
      {
        throw new IllegalArgumentException("invalid default index");
      }
    this.prompt = prompt;
    this.certs = (Certificate[]) certs.clone();
    this.defaultIndex = defaultIndex;
    index = -1;
  }

  // Instance methods.
  // -------------------------------------------------------------------------

  /**
   * <p>Returns the default index.</p>
   *
   * @return The default index.
   */
  public int getDefaultIndex()
  {
    return defaultIndex;
  }

  /**
   * <p>Returns the prompt.</p>
   *
   * @return The prompt.
   */
  public String getPrompt()
  {
    return prompt;
  }

  /**
   * <p>Returns the certificate chain as an array of certificates. The
   * certificate array is cloned to prevent susequent modification.</p>
   *
   * @return The certificate chain.
   */
  public Certificate[] getCertificates()
  {
    return (Certificate[]) certs.clone();
  }

  /**
   * <p>Returns the selected index, or -1 if this value has not been set.</p>
   *
   * @return The selected index.
   */
  public int getSelectedIndex()
  {
    return index;
  }

  /**
   * <p>Sets the selected index.</p>
   *
   * @param index The selected index.
   * @throws IllegalArgumentException If the selected index is neither
   *   {@link #ACCEPT} nor {@link #REJECT}.
   */
  public void setSelectedIndex(int index)
  {
    if (index != ACCEPT && index != REJECT)
      {
        throw new IllegalArgumentException("invalid index");
      }
    this.index = index;
  }
}
