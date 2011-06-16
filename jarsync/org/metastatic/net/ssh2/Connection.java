/* Connection -- Establishes a connection to a remote server.
   Copyright (C) 2002, 2003  Casey Marshall <rsdio@metastatic.org>

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

import java.awt.Dimension;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.math.BigInteger;

import java.security.*;

import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import java.util.*;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;

import org.metastatic.util.Reseeder;
import org.metastatic.util.TerminalInfo;
import org.metastatic.util.Util;

import gnu.crypto.Registry;
import gnu.crypto.hash.HashFactory;
import gnu.crypto.hash.IMessageDigest;
import gnu.crypto.key.IKeyAgreementParty;
import gnu.crypto.key.IncomingMessage;
import gnu.crypto.key.KeyAgreementException;
import gnu.crypto.key.KeyAgreementFactory;
import gnu.crypto.key.OutgoingMessage;
import gnu.crypto.key.dh.DiffieHellmanKeyAgreement;
import gnu.crypto.key.dh.GnuDHPrivateKey;
import gnu.crypto.key.dh.GnuDHPublicKey;
import gnu.crypto.key.dss.DSSPublicKey;
import gnu.crypto.key.rsa.GnuRSAPublicKey;
import gnu.crypto.mac.IMac;
import gnu.crypto.mac.MacFactory;
import gnu.crypto.mode.IMode;
import gnu.crypto.mode.ModeFactory;
import gnu.crypto.sig.ISignature;
import gnu.crypto.sig.SignatureFactory;

/**
 * <p>An SSH 2 connection is a top-level connection through which all user
 * authentication, remote command execution, and forwarding traverses. A
 * connection is created for an InputStream/OutputStream pair coming
 * (usually) from a Socket that has been connected to a remote SSH
 * server.</p>
 *
 * <p>Initially there is no encryption, compression, or message
 * authentication; the only data that has been transmitted is the SSH
 * version message, which looks like:</p>
 *
 * <blockquote>
 * <p><code>SSH-(protocolversion)-(localversion)</code></p>
 * </blockquote>
 *
 * <p>Here the protocol version is "2.0". The local version depends on
 * the version strings of the specific implementations on each end.</p>
 *
 * <p>When a connection is created it must be initialized with the
 * {@link #beginConnection()} method, which performs key exchange and
 * algorithm agreement. A user can then authenticate herself via the
 * {@link #userAuthentication(Authentication.Parameters)} method.</p>
 *
 * <p>Once a user is authenticated, the channel system is initiated with
 * the {@link #beginChannelMode()} method, after which one or more
 * channels can be created.</p>
 *
 * @version $Revision$
 */
public final class Connection implements SSH2Constants
{

  // Constants and variables.
  // -------------------------------------------------------------------------

  /** The source of raw bytes coming in. */
  private InputStream in;

  /** The destination of our encrypted packets. */
  private OutputStream out;

  /** The packet input stream that decodes the binary packets coming in. */
  private PacketInputStream pin;

  /**
   * The packet output stream that holds and formats data
   * destined to be sent out.
   */
  private PacketOutputStream pout;

  /**
   * The packet output stream used to build channel packets.
   */
  private PacketOutputStream channel_pout;

  /**
   * The configuration (cipher, compression, and mac) used to read in
   * the server's messages.
   */
  private Configuration server;

  /** The configuration used to write messages. */
  private Configuration client;

  /**
   * The key that is agreed upon for encryption and message
   * authentication.
   */
  private BigInteger shared_secret;

  /** The authentication mechanism. */
  private Authentication auth;

  /** The server's version string. */
  private String server_version;

  /** The version string we sent over the wire. */
  private String client_version;

  /**
   * The SSH 2 name of the cipher used to decrypt packets coming from
   * the server.
   */
  private String server_cipher;

  /**
   * The SSH 2 name of the cipher used to encrypt packets going to the
   * server.
   */
  private String client_cipher;

  /** The MAC we use to verify incoming packets. */
  private String server_mac;

  /** The MAC we send. */
  private String client_mac;

  /** The compression algorithm the server will use. */
  private String server_comp;

  /** The compression algorithm we use. */
  private String client_comp;

  /** A mapping between channel numbers and listener threads. */
  private HashMap channels;

  /** The queue of packets waiting to leave. */
  private LinkedList channel_queue;

  /** The thread to handle input. */
  private ClientInputLoop input_loop;

  /** The thread to handle output. */
  private ClientOutputLoop output_loop;

  /** The last channel number allocated. */
  private Integer last_channel;

  /** True if we are, as far as we know, connected. */
  private boolean connected;

  /** The server's public key. */
  private PublicKey server_pubkey = null;

  /** The result of checking the server's public key. */
  private boolean sig_result = false;

  // Lists of algorithms we want to use.
  private List c_to_s_ciphers, s_to_c_ciphers, c_to_s_macs,
    s_to_c_macs, c_to_s_comp, s_to_c_comp;

  /** Whether or not we initiated a KEXINIT. */
  private boolean i_sent_kexinit = false;

  // Lists of algorithms we send to the server.
  private String my_host_key_algs, my_ciphers_client, my_ciphers_server,
    my_macs_client, my_macs_server, my_comp_client, my_comp_server;

  // Lists of algorithms we receive from the server.
  private String kex_algs, host_key_algs, ciphers_client,
    ciphers_server, macs_client, macs_server, comp_client,
    comp_server, lang_client, lang_server;

  private byte[] my_kex_payload, server_kex_payload;
  private IKeyAgreementParty dh_kex;
  private BigInteger dh_y;
  private IMessageDigest hash;
  private byte[] digest, K_S;
  private long kex_timestamp;

  /**
   * This is our local PRNG. It is not used often.
   */
  private SecureRandom random;

  private Reseeder reseeder;

// Constructor.
  // -------------------------------------------------------------------------

  /**
   * Create a new connection. The input stream should be where raw bytes
   * come from the server, and the output stream should be where raw
   * bytes go to the server. The two version strings must be read prior
   * to creating a new connection, and should be the full version string
   * without line terminators.
   *
   * @param in The source of data from the server.
   * @param out The sink of data for the server.
   * @param server_version The server's version string.
   * @param client_version Our version string.
   */
  public Connection(InputStream in, OutputStream out,
                    String server_version, String client_version)
    throws IOException
  {
    this.in = in;
    this.out = out;
    server = new Configuration();
    client = new Configuration();
    client.random = new SecureRandom();

    // Try to use /dev/urandom if it's available.
    try
      {
        client.random = SecureRandom.getInstance("DevURandom");
      }
    catch (Exception x)
      {
        Debug.warning(String.valueOf(x));
      }

    // This is so an attacker will (hopefully) not know HOW we use
    // network events, even though she MAY be able to predict what
    // entropy bits we are using from network latency.
    //
    // This is probably still insecure, however. Be warned.
    random = new SecureRandom();
    random.setSeed(System.currentTimeMillis());
    try
      {
        random = SecureRandom.getInstance("DevURandom");
      }
    catch (Exception x)
      {
        Debug.warning(String.valueOf(x));
        reseeder = new Reseeder(random.nextBoolean()
          ? Reseeder.POLICY_WHEN_FULL : Reseeder.POLICY_TIME_LIMIT,
          Reseeder.MINIMUM_SEED_SIZE + Math.abs(random.nextInt(128)),
          Reseeder.MINIMUM_TIMEOUT + Math.abs(random.nextInt(600000)),
          client.random);
      }

    pin = new PacketInputStream(server, in);
    pout = new PacketOutputStream(client);
    this.server_version = server_version;
    this.client_version = client_version;
    connected = false;
    s_to_c_ciphers = new LinkedList(Configuration.CIPHER_ALGS);
    c_to_s_ciphers = new LinkedList(Configuration.CIPHER_ALGS);
    s_to_c_macs = new LinkedList(Configuration.MAC_ALGS);
    c_to_s_macs = new LinkedList(Configuration.MAC_ALGS);
    s_to_c_comp = new LinkedList(Configuration.COMPRESSION_ALGS);
    c_to_s_comp = new LinkedList(Configuration.COMPRESSION_ALGS);
  }

// Instance methods.
  // -------------------------------------------------------------------------

  // XXX HELP! I'm too big and bulky!!!

  /**
   * Set the preferred list of client (encryption) ciphers. The actual
   * list will be the intersection of this list with the system's
   * available ciphers and the ciphers the server supports.
   *
   * @param ciphers The list of preferred ciphers.
   */
  public void setPreferredClientCiphers(List ciphers)
  {
    c_to_s_ciphers = new LinkedList(ciphers);
    c_to_s_ciphers.retainAll(Configuration.CIPHER_ALGS);
  }

  /**
   * Set the preferred list of server (decryption) ciphers.
   *
   * @param ciphers The list of preferred ciphers.
   */
  public void setPreferredServerCiphers(List ciphers)
  {
    s_to_c_ciphers = new LinkedList(ciphers);
    s_to_c_ciphers.retainAll(Configuration.CIPHER_ALGS);
  }

  /**
   * Set the preferred list of client MACs.
   *
   * @param macs The list of preferred MACs.
   */
  public void setPreferredClientMACs(List macs)
  {
    c_to_s_macs = new LinkedList(macs);
    c_to_s_macs.retainAll(Configuration.MAC_ALGS);
  }

  /**
   * Set the preferred list of server MACs.
   *
   * @param macs The list of preferred MACs.
   */
  public void setPreferredServerMACs(List macs)
  {
    s_to_c_macs = new LinkedList(macs);
    s_to_c_macs.retainAll(Configuration.MAC_ALGS);
  }

  /**
   * Set the preferred list of compression algorithms.
   *
   * @param comp The list of preferred compression algorithms.
   */
  public void setPreferredCompression(List comp)
  {
    c_to_s_comp = new LinkedList(comp);
    c_to_s_comp.retainAll(Configuration.COMPRESSION_ALGS);
  }

  /**
   * Set the preferred list of decompression algorithms.
   *
   * @param comp The list of preferred compression algorithms.
   */
  public void setPreferredDecompression(List comp)
  {
    s_to_c_comp = new LinkedList(comp);
    s_to_c_comp.retainAll(Configuration.COMPRESSION_ALGS);
  }

  /**
   * Get the reseeder, if any.
   *
   * @return The reseeder being used.
   */
  public Reseeder getReseeder()
  {
    return reseeder;
  }

  /**
   * Perform the key exchange, server signature verification, and
   * choose the algorithms. The state of the connection is assumed to be
   * just after the version string exchange. Once this method returns
   * successfully the user authentication may proceed.
   *
   * @throws org.metastatic.net.ssh2.SSH2Exception If a protocol error
   *    occurs.
   * @throws java.io.IOException If a transport failure occurs.
   * @throws
   */
  public void beginConnection() throws IOException, IllegalStateException
  {
    // Get the server's KEXINIT packet
    readAwaiting(SSH_MSG_KEXINIT);
    receiveKeyExchange();

    System.out.println("host_key_algs=" + host_key_algs);
    my_host_key_algs = makeAlgString(host_key_algs,
      new LinkedList(client.PUB_KEY_ALGS));
    my_ciphers_client = makeAlgString(ciphers_client, c_to_s_ciphers);
    my_ciphers_server = makeAlgString(ciphers_server, s_to_c_ciphers);
    my_macs_client = makeAlgString(macs_client, c_to_s_macs);
    my_macs_server = makeAlgString(macs_server, s_to_c_macs);
    my_comp_client = makeAlgString(comp_client, c_to_s_comp);
    my_comp_server = makeAlgString(comp_server, s_to_c_comp);
    if (my_ciphers_client.length() == 0)
      {
        pout.reset();
        pout.write(SSH_MSG_DISCONNECT);
        pout.writeUInt32(SSH_DISCONNECT_PROTOCOL_ERROR);
        pout.writeUTF8("no appropriate ciphers available.");
        pout.writeASCII("");
        out.write(pout.toBinaryPacket());
        out.flush();
        throw new DisconnectException(SSH_DISCONNECT_PROTOCOL_ERROR,
          "no appropriate ciphers available.");
      }
    if (my_ciphers_server.length() == 0)
      {
        pout.reset();
        pout.write(SSH_MSG_DISCONNECT);
        pout.writeUInt32(SSH_DISCONNECT_PROTOCOL_ERROR);
        pout.writeUTF8("no appropriate ciphers available.");
        pout.writeASCII("");
        out.write(pout.toBinaryPacket());
        out.flush();
        throw new DisconnectException(SSH_DISCONNECT_PROTOCOL_ERROR,
          "no appropriate ciphers available.");
      }
    if (my_macs_client.length() == 0)
      {
        pout.reset();
        pout.write(SSH_MSG_DISCONNECT);
        pout.writeUInt32(SSH_DISCONNECT_PROTOCOL_ERROR);
        pout.writeUTF8("no appropriate MAC available.");
        pout.writeASCII("");
        out.write(pout.toBinaryPacket());
        out.flush();
        throw new DisconnectException(SSH_DISCONNECT_PROTOCOL_ERROR,
          "no appropriate MAC available.");
      }
    if (my_macs_server.length() == 0)
      {
        pout.reset();
        pout.write(SSH_MSG_DISCONNECT);
        pout.writeUInt32(SSH_DISCONNECT_PROTOCOL_ERROR);
        pout.writeUTF8("no appropriate MAC available.");
        pout.writeASCII("");
        out.write(pout.toBinaryPacket());
        out.flush();
        throw new DisconnectException(SSH_DISCONNECT_PROTOCOL_ERROR,
          "no appropriate MAC available.");
      }
    if (my_comp_client.length() == 0)
      {
        pout.reset();
        pout.write(SSH_MSG_DISCONNECT);
        pout.writeUInt32(SSH_DISCONNECT_PROTOCOL_ERROR);
        pout.writeUTF8("no appropriate compression algorithm available.");
        pout.writeASCII("");
        out.write(pout.toBinaryPacket());
        out.flush();
        throw new DisconnectException(SSH_DISCONNECT_PROTOCOL_ERROR,
          "no appropriate compression algorithm available.");
      }
    if (my_comp_server.length() == 0)
      {
        pout.reset();
        pout.write(SSH_MSG_DISCONNECT);
        pout.writeUInt32(SSH_DISCONNECT_PROTOCOL_ERROR);
        pout.writeUTF8("no appropriate compression algorithm available.");
        pout.writeASCII("");
        out.write(pout.toBinaryPacket());
        out.flush();
        throw new DisconnectException(SSH_DISCONNECT_PROTOCOL_ERROR,
          "no appropriate compression algorithm available.");
      }
    client_cipher = chooseAlg(my_ciphers_client);
    server_cipher = chooseAlg(my_ciphers_server);
    client_mac = chooseAlg(my_macs_client);
    server_mac = chooseAlg(my_macs_server);
    client_comp = chooseAlg(my_comp_client);
    server_comp = chooseAlg(my_comp_server);

    sendKeyExchange();
    sendKeyExchangeDH();
    readAwaiting(SSH_MSG_KEXDH_REPLY);
    receiveKeyExchangeDH();
    newKeys();
    readAwaiting(SSH_MSG_NEWKEYS);
    pin.endPacket();
    setupAlgorithms();
    connected = true;
  }

  /**
   * Returns the result of verifying the server's public key.
   *
   * @return true If the server's public key is the one that made the
   *    signature.
   */
  public boolean getSignatureResult()
  {
    return sig_result;
  }

  /**
   * Get the server's public key. The returned object is either a
   * {@link java.security.spec.DSAPublicKeySpec} or a {@link
   * java.security.spec.RSAPublicKeySpec}. This method should be used
   * to verify that the host is known to the client.
   *
   * @return The server's public key.
   */
  public PublicKey getHostKey()
  {
    return server_pubkey;
  }

  /**
   * Returns true if the connection is (as far as is known) active.
   */
  public boolean connected()
  {
    return connected;
  }

  /**
   * Disconnect.
   */
  public void setConnected(boolean connected)
  {
    this.connected = connected;
  }

  public void disconnect(int why) throws IOException
  {
    connected = false;
    disconnect(why, "");
  }

  public void disconnect(int why, String message) throws IOException
  {
    connected = false;
    synchronized (pout)
      {
        pout.reset();
        pout.write(SSH_MSG_DISCONNECT);
        pout.writeUInt32(why);
        pout.writeUTF8(message);
        pout.writeASCII("");
        out.write(pout.toBinaryPacket());
        out.flush();
      }
  }

  /**
   * <p>Authenticate a user to the server. This method must be called
   * after a call to {@link #beginConnection()} succeeds and before any
   * channels are opened.</p>
   *
   * <p>This method returns <code>true</code> if the authentication
   * succeeds, <code>false</code> if not, or throws an {@link
   * SSH2Exception} if an I/O error occurs or there are no more
   * authentication methods possible.</p>
   *
   * @param params The {@link Authentication.Parameters} to use.
   * @return true If authentication succeeds; false otherwise.
   */
  public boolean userAuthentication(Authentication.Parameters params)
    throws IOException
  {
    if (auth == null)
      {
        // Get ready for ssh-userauth
        pout.reset();
        pout.write(SSH_MSG_SERVICE_REQUEST);
        pout.writeASCII("ssh-userauth");
        out.write(pout.toBinaryPacket());
        out.flush();

        readAwaiting(SSH_MSG_SERVICE_ACCEPT);
        String msg = pin.readASCII();
        if (!msg.equals("ssh-userauth"))
          {
            pout.reset();
            pout.write(SSH_MSG_DISCONNECT);
            pout.writeUInt32(SSH_DISCONNECT_PROTOCOL_ERROR);
            pout.writeUTF8("expecting \"ssh-userauth\"");
            pout.writeASCII("en-US");
            out.write(pout.toBinaryPacket());
            out.flush();
            connected = false;
            throw new SSH2Exception("protocol error: " + msg);
         }
        pin.endPacket();
        auth = new Authentication(this);
      }
    Debug.debug2("authenticating type=" + params.getType());
    return auth.authenticate(params);
  }

  /**
   * If {@link #userAuthentication} fails (returns false), this method
   * can query what authentication methods can still be used.
   */
  public String getCanContinue()
  {
    if (auth != null)
      return auth.getCanContinue();
    return null;
  }

  /**
   * Start channel mode. This is done after user authentication
   * succeeds, and before any channels are created.
   */
  public void beginChannelMode()
  {
    Debug.debug2("starting channel mode");
    channels = new HashMap();
    input_loop = new ClientInputLoop(this, channels, reseeder);
    input_loop.start();
    channel_pout = new PacketOutputStream();
    channel_queue = new LinkedList();
    output_loop = new ClientOutputLoop(this, channel_queue);
    output_loop.start();
    Debug.debug2("channel mode now running");
    new KEXWatcher(this).start();
    Debug.debug2("key-exchange watcher running.");
  }

  /**
   * Open a new channel. The requesting ChannelListener will be passed
   * a {@link Channel} if the request succeeds.
   *
   * @param spec The {@link Channel.Spec} that specifies what type of
   *    channel will be created.
   * @param output The {@link ChannelListener} that will handle input
   *    for the new channel.
   */
  public void newChannel(Channel.Spec spec, ChannelListener output)
    throws IOException
  {
    Debug.debug2("opening channel");
    Integer n = allocateChannel();

    // This mapping is temporary. Once the server tells us this
    // channel has been opened, we replace `spec' with the actual
    // channel in this chain.
    channels.put(n, spec);
    channels.put(spec, output);

    // Open the channel.
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_OPEN);
            channel_pout.writeASCII(spec.getType().getName());
            channel_pout.writeUInt32(n.intValue());
            channel_pout.writeUInt32(spec.initWindowSize());
            channel_pout.writeUInt32(spec.maxPacketSize());
            spec.writeTypeSpecific(channel_pout);
            channel_queue.add(new ChannelPacket(Channel.CONTROL_CHANNEL,
                                                channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }

    Debug.debug2("queued up channel open request.");
  }

/*
   public void requestRemoteForwarding() {

   }

   public void requestLocalForwarding(LocalForwarding lf) {
      synchronized (channel_pout) {
         synchronized (channel_queue) {

         }
      }
   }
 */

  // Package-private instance methods.
  // -------------------------------------------------------------------------

  Integer allocateChannel() throws SSH2Exception
  {
    if (last_channel == null)
      {
        last_channel = new Integer(0);
      }
    else
      {
        last_channel = new Integer(last_channel.intValue()+1);
      }
    if (channels.containsKey(last_channel))
      {
        throw new SSH2Exception("too many open channels");
      }
    return last_channel;
  }

  /**
   * Freeze channel output. This is useful e.g. when re-exchanging
   * keys.
   */
  void freezeChannelOutput()
  {
    output_loop.freeze();
  }

  /**
   * Unfreeze channel output.
   */
  void unfreezeChannelOutput()
  {
    output_loop.unfreeze();
  }

  // Key exchange methods.
  // -------------------------------------------------------------------------

  /**
   * Receive the server's KEXINIT packet, minus the type byte.
   */
  void receiveKeyExchange() throws IOException
  {
    byte[] cookie = new byte[16];
    pin.read(cookie);
    kex_algs = pin.readASCII();
    host_key_algs = pin.readASCII();
    ciphers_client = pin.readASCII();
    ciphers_server = pin.readASCII();
    macs_client = pin.readASCII();
    macs_server = pin.readASCII();
    comp_client = pin.readASCII();
    comp_server = pin.readASCII();
    lang_client = pin.readASCII();
    lang_server = pin.readASCII();
    boolean kex_follows = pin.readBoolean(); // probably false
    pin.readUInt32();
    server_kex_payload = pin.getPayload();
    pin.endPacket();
    Debug.debug2("Got server's KEXINIT");
  }

  /**
   * Send our KEXINIT packet.
   */
  void sendKeyExchange() throws IOException, IllegalStateException
  {
    if (i_sent_kexinit) return;  // Already sent this.
    // Write out KEXINIT packet
    pout.reset();
    pout.write(SSH_MSG_KEXINIT);
    byte[] cookie = new byte[16];
    client.random.nextBytes(cookie);
    pout.write(cookie);
    pout.writeASCII(client.KEX_ALG); // only diffie-hellman-group1-sha1
    pout.writeASCII(my_host_key_algs);
    pout.writeASCII(my_ciphers_client);
    pout.writeASCII(my_ciphers_server);
    pout.writeASCII(my_macs_client);
    pout.writeASCII(my_macs_server);
    pout.writeASCII(my_comp_client);
    pout.writeASCII(my_comp_server);
    pout.writeASCII("");
    pout.writeASCII("");
    pout.writeBoolean(false); // First KEX packet follows
    pout.writeUInt32(0);
    my_kex_payload = pout.getPayload();
    out.write(pout.toBinaryPacket());
    out.flush();
    i_sent_kexinit = true;
    Debug.debug2("Sent my KEXINIT");
  }

  /**
   * Send our KEXDH_INIT.
   */
  void sendKeyExchangeDH() throws IOException
  {
    // Send our first KEXDH packet
    pout.reset();
    dh_kex = KeyAgreementFactory.getPartyAInstance(Registry.DH_KA);
    Map attr = new HashMap();
    attr.put(DiffieHellmanKeyAgreement.KA_DIFFIE_HELLMAN_OWNER_PRIVATE_KEY,
             new GnuDHPrivateKey(null, DH_P, DH_G, null));
    //attr.put(DiffieHellmanKeyAgreement.SOURCE_OF_RANDOMNESS, random);
    try
      {
        dh_kex.init(attr);
        OutgoingMessage outm = dh_kex.processMessage(null);
        IncomingMessage inm = new IncomingMessage(outm.toByteArray());
        dh_y = inm.readMPI();
        Debug.debug2("dh_y=" + dh_y.toString(16));
      }
    catch (KeyAgreementException kae)
      {
        pout.reset();
        pout.write(SSH_MSG_DISCONNECT);
        pout.writeUInt32(SSH_DISCONNECT_KEY_EXCHANGE_FAILED);
        pout.writeUTF8(kae.getMessage());
        pout.writeASCII("en-US");
        out.write(pout.toBinaryPacket());
        out.flush();
        throw new SSH2Exception("error generating shared key: "
                                + kae.getMessage());
      }

    pout.write(SSH_MSG_KEXDH_INIT);
    pout.writeMPint(dh_y);
    out.write(pout.toBinaryPacket());
    out.flush();
    Debug.debug2("Sent my KEXDH_INIT");
  }

  /**
   * Receive the server's reply to our KEXDH_INIT, minus the first
   * header byte. This also generates the shared secret and attempts to
   * verify the server's signature.
   */
  void receiveKeyExchangeDH() throws IOException
  {
    BigInteger dss_p = null, dss_q = null, dss_g = null, dss_y = null;
    BigInteger rsa_e = null, rsa_n = null;
    PacketOutputStream ks_data = new PacketOutputStream(client);

    // Receive a KEXDH_REPLY
    // readAwaiting(SSH_MSG_KEXDH_REPLY);
    int ks_len = (int) pin.readUInt32(); // total length of the certs.
    host_key_algs = pin.readASCII();
    ks_data.writeASCII(host_key_algs);
    if (host_key_algs.equals("ssh-dss"))
      {
        dss_p = pin.readMPint();
        dss_q = pin.readMPint();
        dss_g = pin.readMPint();
        dss_y = pin.readMPint();
        ks_data.writeMPint(dss_p);
        ks_data.writeMPint(dss_q);
        ks_data.writeMPint(dss_g);
        ks_data.writeMPint(dss_y);
      }
    else if (host_key_algs.equals("ssh-rsa"))
      {
        rsa_e = pin.readMPint();
        rsa_n = pin.readMPint();
        ks_data.writeMPint(rsa_e);
        ks_data.writeMPint(rsa_n);
      }
    else
      {
        throw new SSH2Exception("Expecting host key algorithm name but got "
                                + host_key_algs);
      }
    BigInteger host_pub_bi = pin.readMPint();
    byte[] host_sig = null;
    pin.readUInt32();
    host_key_algs = pin.readASCII();
    host_sig = pin.readString();
    K_S = ks_data.getPayload();
    pin.endPacket();
    Debug.debug2("got KEXDH_REPLY");

    // Generate the shared secret.
    try
      {
        OutgoingMessage outm = new OutgoingMessage();
        outm.writeMPI(host_pub_bi);
        IncomingMessage inm = new IncomingMessage(outm.toByteArray());
        dh_kex.processMessage(inm);
        shared_secret = new BigInteger(1, dh_kex.getSharedSecret());
      }
    catch (KeyAgreementException kae)
      {
        pout.reset();
        pout.write(SSH_MSG_DISCONNECT);
        pout.writeUInt32(SSH_DISCONNECT_KEY_EXCHANGE_FAILED);
        pout.writeUTF8(kae.getMessage());
        pout.writeASCII("en-US");
        out.write(pout.toBinaryPacket());
        out.flush();
        throw new SSH2Exception("error generating shared key: "
                                + kae.getMessage());
      }

    // Validate the server's signature.
    hash = HashFactory.getInstance(Registry.SHA160_HASH);
    PacketOutputStream hash_data = new PacketOutputStream(new Configuration());
    hash_data.writeASCII(client_version);
    hash_data.writeASCII(server_version);
    hash_data.writeString(my_kex_payload);
    hash_data.writeString(server_kex_payload);
    hash_data.writeString(K_S);
    hash_data.writeMPint(dh_y);
    hash_data.writeMPint(host_pub_bi);
    hash_data.writeMPint(shared_secret);

    byte[] buf = hash_data.getPayload();
    hash.update(buf, 0, buf.length);
    hash_data = null;
    digest = hash.digest();
    Debug.debug2("verify digest=" + Util.toString(digest));
    if (server.session_id == null)
      server.session_id = (byte[]) digest.clone();
    if (client.session_id == null)
      client.session_id = (byte[]) digest.clone();

    Object host_sig_value = null;
    ISignature sig = null;
    if (host_key_algs.intern() == "ssh-dss")
      {
        sig = SignatureFactory.getInstance(Registry.DSS_SIG);
        server_pubkey = new DSSPublicKey(dss_p, dss_q, dss_g, dss_y);
        host_sig_value = dssBlobToGNU(host_sig);
        Debug.debug2("dss-p=" + dss_p);
        Debug.debug2("dss-q=" + dss_q);
        Debug.debug2("dss-g=" + dss_g);
        Debug.debug2("dss-y=" + dss_y);
      }
    else
      {
        sig = SignatureFactory.getInstance(Registry.RSA_PKCS1_V1_5_SIG);
        server_pubkey = new GnuRSAPublicKey(rsa_n, rsa_e);
        host_sig_value = host_sig;
      }
    sig.setupVerify(Collections.singletonMap(ISignature.VERIFIER_KEY,
                                             server_pubkey));
    sig.update(digest, 0, digest.length);
    sig_result = sig.verify(host_sig_value);
    Debug.debug("Signature alg=" + host_key_algs);
    Debug.debug("       result=" + sig_result);
  }

  /**
   * Send our NEWKEYS packet.
   */
  void newKeys() throws IOException
  {
    pout.reset();
    pout.write(SSH_MSG_NEWKEYS);
    out.write(pout.toBinaryPacket());
    out.flush();
  }

  /**
   * Create the session keys and initialize the ciphers, MACs, and
   * compression algorithms. This should be called right after we send
   * and receive a NEWKEYS packet.
   */
  void setupAlgorithms() throws SSH2Exception, IllegalStateException
  {
    byte[] shared_mpint = new byte[shared_secret.toByteArray().length+4];
    shared_mpint[0] = (byte)((shared_secret.toByteArray().length>>>24)&0xff);
    shared_mpint[1] = (byte)((shared_secret.toByteArray().length>>>16)&0xff);
    shared_mpint[2] = (byte)((shared_secret.toByteArray().length>>>8)&0xff);
    shared_mpint[3] = (byte) (shared_secret.toByteArray().length&0xff);
    System.arraycopy(shared_secret.toByteArray(), 0, shared_mpint, 4,
                     shared_secret.toByteArray().length);

    // Transform the shared secret into symmetric encryption keys, MAC
    // keys, and the IVs.
    byte x = (byte) 'A';
    hash.reset();
    hash.update(shared_mpint, 0, shared_mpint.length);
    hash.update(digest, 0, digest.length);
    hash.update(x);
    hash.update(client.session_id, 0, client.session_id.length);
    byte[] iv_client = trimOrExpand(hash.digest(), client.CIPHER_IV_LENGTHS,
                                    client_cipher, hash, shared_mpint, x++, digest);

    hash.reset();
    hash.update(shared_mpint, 0, shared_mpint.length);
    hash.update(digest, 0, digest.length);
    hash.update(x);
    hash.update(client.session_id, 0, client.session_id.length);
    byte[] iv_server = trimOrExpand(hash.digest(), server.CIPHER_IV_LENGTHS,
                                    server_cipher, hash, shared_mpint, x++, digest);

    hash.reset();
    hash.update(shared_mpint, 0, shared_mpint.length);
    hash.update(digest, 0, digest.length);
    hash.update(x);
    hash.update(client.session_id, 0, client.session_id.length);
    byte[] ckey_client = trimOrExpand(hash.digest(),
                                      client.CIPHER_KEY_LENGTHS, server_cipher,
                                      hash, shared_mpint, x++, digest);

    hash.reset();
    hash.update(shared_mpint, 0, shared_mpint.length);
    hash.update(digest, 0, digest.length);
    hash.update(x);
    hash.update(client.session_id, 0, client.session_id.length);
    byte[] ckey_server = trimOrExpand(hash.digest(),
                                      server.CIPHER_KEY_LENGTHS, server_cipher,
                                      hash, shared_mpint, x++, digest);

    hash.reset();
    hash.update(shared_mpint, 0, shared_mpint.length);
    hash.update(digest, 0, digest.length);
    hash.update(x);
    hash.update(client.session_id, 0, client.session_id.length);
    byte[] mkey_client = trimOrExpand(hash.digest(), client.MAC_KEY_LENGTHS,
                                      client_mac, hash, shared_mpint, x++, digest);

    hash.reset();
    hash.update(shared_mpint, 0, shared_mpint.length);
    hash.update(digest, 0, digest.length);
    hash.update(x);
    hash.update(client.session_id, 0, client.session_id.length);
    byte[] mkey_server = trimOrExpand(hash.digest(), server.MAC_KEY_LENGTHS,
                                      server_mac, hash, shared_mpint, x++, digest);

    // Choose our algorithms.
    chooseCipher(client, iv_client, ckey_client, client_cipher,
                 IMode.ENCRYPTION);
    chooseCipher(server, iv_server, ckey_server, server_cipher,
                 IMode.DECRYPTION);
    chooseMac(client, mkey_client, client_mac);
    chooseMac(server, mkey_server, server_mac);
    if (client_comp.equals("zlib"))
      {
        client.flater = new ZStream();
        client.flater.deflateInit(JZlib.Z_DEFAULT_COMPRESSION);
      }
    if (server_comp.equals("zlib"))
      {
        server.flater = new ZStream();
        server.flater.inflateInit();
      }
    i_sent_kexinit = false;  // Reset this for future key exchanges.
    kex_timestamp = System.currentTimeMillis();
    pin.resetTotalBytesIn();
    pout.resetTotalBytesOut();
    Debug.debug2("KEX done.");
  }

// Package-private methods for asynchronous I/O.
  // -------------------------------------------------------------------------

  /*
   * The rationale behind these methods is to exert some real control
   * over how packets leave.
   */

  /**
   * Queue up a CHANNEL_DATA packet.
   *
   * @param c The channel associated with this packet.
   * @param data The data to send.
   * @param off The offset from whence to read.
   * @param len The number of bytes to send.
   */
  void requestDataWrite(Channel c, byte[] data, int off, int len)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_DATA);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_pout.writeUInt32(len);
            channel_pout.write(data, off, len);
            channel_queue.add(
              new ChannelPacket(c, channel_pout.getPayload(), len));
            channel_queue.notify();
          }
      }
  }

  /**
   * Queue up a CHANNEL_EXTENDED_DATA packet.
   *
   * @param c The channel associated with this packet.
   * @param data The data to send.
   * @param type The type of extended data.
   */
  void requestExtendedDataWrite(Channel c, byte[] data, int type)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_EXTENDED_DATA);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_pout.writeUInt32(type);
            channel_pout.writeString(data);
            channel_queue.add(new ChannelPacket(c, channel_pout.getPayload(),
              data.length));
            channel_queue.notify();
          }
      }
  }

  /**
   * Queue up a CHANNEL_REQUEST to set an environment variable.
   *
   * @param c The Channel associated with this request.
   * @param name The variable's name.
   * @param value The variable's value.
   */
  void requestEnv(Channel c, String name, String value)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_REQUEST);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_pout.writeASCII("env");
            channel_pout.writeBoolean(false);
            channel_pout.writeUTF8(name);
            channel_pout.writeUTF8(value);
            channel_queue.add(
              new ChannelPacket(c, channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }
  }

  /**
   * Queue up a CHANNEL_EOF packet.
   *
   * @param c The channel associated with this packet.
   */
  void requestChannelEOF(Channel c)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_EOF);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_queue.add(
              new ChannelPacket(c, channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }
  }

  /**
   * Queue up a CHANNEL_REQUEST for a signal.
   *
   * @param c The channel associated with this request.
   * @param signal The signal to send.
   */
  void requestSignal(Channel c, String signal)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_REQUEST);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_pout.writeASCII("signal");
            channel_pout.writeBoolean(false);
            channel_pout.writeASCII(signal);
            channel_queue.add(
              new ChannelPacket(c, channel_pout.getPayload(), 0));
            channel_queue.notify();
         }
      }
  }

  /**
   * Queue up a CHANNEL_CLOSE packet.
   *
   * @param c The channel associated with this packet.
   */
  void requestChannelClose(Channel c)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_CLOSE);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_queue.add(
              new ChannelPacket(c, channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }
  }

  void requestOpenSuccess(Channel c, int local_chan)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_OPEN_CONFIRMATION);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_pout.writeUInt32(local_chan);
            channel_pout.writeUInt32(c.getReceiveWindowSize());
            channel_pout.writeUInt32(c.getReceiveMaxPacketSize());
            channel_queue.add(new ChannelPacket(Channel.CONTROL_CHANNEL,
              channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }
  }

  void requestOpenFailure(int recip, int why, String msg, String lang)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_OPEN_FAILURE);
            channel_pout.writeUInt32(recip);
            channel_pout.writeUInt32(why);
            channel_pout.writeUTF8(msg);
            channel_pout.writeASCII(lang);
            channel_queue.add(new ChannelPacket(Channel.CONTROL_CHANNEL,
                                                channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }
  }

  /**
   * Tell the server that packet <code>sequence</code> is an
   * unimplemented packet type.
   */
  void requestUnimplemented(int sequence)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_UNIMPLEMENTED);
            channel_pout.writeUInt32(sequence);
            channel_queue.add(new ChannelPacket(Channel.CONTROL_CHANNEL,
              channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }
  }

  /**
   * Queue up a CHANNEL_REQUEST for an interactive shell.
   *
   * @param c The channel associated with this packet.\
   */
  void requestShell(Channel c)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_REQUEST);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_pout.writeASCII("shell");
            channel_pout.writeBoolean(true);
            channel_queue.add(
              new ChannelPacket(c, channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }
  }

  /**
   * Queue up a CHANNEL_REQUEST for a remote command.
   *
   * @param c The channel associated with this packet.
   * @param cmd The command to execute.
   */
  void requestExec(Channel c, String cmd)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_REQUEST);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_pout.writeASCII("exec");
            channel_pout.writeBoolean(true);
            channel_pout.writeASCII(cmd);
            channel_queue.add(
              new ChannelPacket(c, channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }
  }

  /**
   * Queue up a CHANNEL_REQUEST for a pseudo-terminal.
   *
   * @param c The channel associated with this packet.
   * @param term Information about the destination terminal.
   */
  void requestPTY(Channel c, TerminalInfo term)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            Dimension chars = term.getSizeChars();
            Dimension pixels = term.getSizePixels();
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_REQUEST);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_pout.writeASCII("pty-req");
            channel_pout.writeBoolean(true);
            channel_pout.writeASCII(term.getName());
            channel_pout.writeUInt32(chars.width);
            channel_pout.writeUInt32(chars.height);
            channel_pout.writeUInt32(pixels.width);
            channel_pout.writeUInt32(pixels.height);
            channel_pout.writeString(makeTerminalModes(term));
            channel_queue.add(
              new ChannelPacket(c, channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }
  }

  /**
   * Queue up a CHANNEL_REQUEST for a terminal dimension change.
   *
   * @param c The channel associated with this packet.
   * @param chars The new size, in characters.
   * @param pixels The new size, in pixels.
   */
  void requestWindowChange(Channel c, Dimension chars, Dimension pixels)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_REQUEST);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_pout.writeASCII("window-change");
            channel_pout.writeBoolean(false);
            channel_pout.writeUInt32(chars.width);
            channel_pout.writeUInt32(chars.height);
            channel_pout.writeUInt32(pixels.width);
            channel_pout.writeUInt32(pixels.height);
            channel_queue.add(
              new ChannelPacket(c, channel_pout.getPayload(), 0));
            channel_queue.notify();
          }
      }
  }

  /**
   * Queue up a CHANNEL_REQUEST for X11 forwarding.
   *
   * @param c The channel associated with this packet. This is
   *        typically a session channel that has been previously
   *        opened.
   * @param single true if only a single forwarded connection should be
   *        allowed.
   * @param prot The name of the authentication protocol.
   * @param cookie The authentication cookie, in hexadecimal.
   * @param screen The screen number.
   * /
   void requestX11(Channel c, boolean single, String prot,
                   String cookie, int screen)
   {
      synchronized (channel_pout) {
         synchronized (channel_queue) {
            byte[] fake_cookie = new byte[cookie.length() / 2];
            random.nextBytes(fake_cookie);
            input_loop.x11fwd = new X11Params();
            input_loop.x11fwd.cookie = cookie;
            input_loop.x11fwd.fake_cookie = Util.toString(fake_cookie);
            input_loop.x11fwd.single =
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_REQUEST);
            channel_pout.write(c.getRecipientChannel());
            channel_pout.writeASCII("x11-req");
            channel_pout.writeBoolean(true);
            channel_pout.writeBoolean(single);
            channel_pout.writeString(prot.getBytes());
            channel_pout.writeString(cookie);
            channel_pout.writeUInt32(screen);
            channel_queue.add(
               new ChannelPacket(c, channel_pout.getPayload(), 0));
            channel_queue.notify();
         }
      }
   } */

  /**
   * Queue up a window adjustment (for input on this channel).
   *
   * @param c The channel to request a window change for.
   */
  void requestWindowAdjust(Channel c)
  {
    synchronized (channel_pout)
      {
        synchronized (channel_queue)
          {
            channel_pout.reset();
            channel_pout.write(SSH_MSG_CHANNEL_WINDOW_ADJUST);
            channel_pout.writeUInt32(c.getRecipientChannel());
            channel_pout.writeUInt32(c.getWindowIncrement());
            channel_queue.add(
              new ChannelPacket(c, channel_pout.getPayload(), 0));
            channel_queue.notify();
            c.adjustReceiveWindowSize(c.getWindowIncrement());
          }
      }
  }

  /**
   * Get the input stream that sends data to the remote server.
   *
   * @return The input stream.
   */
  InputStream getInputStream()
  {
    return in;
  }

  /**
   * Get the packet input stream for this connection.
   *
   * @param The input stream.
   */
  PacketInputStream getPacketInputStream()
  {
    return pin;
  }

  /**
   * Get the raw output stream to the server.
   *
   * @return The output stream.
   */
  OutputStream getOutputStream()
  {
    return out;
  }

  /**
   * Get the {@link PacketOutputStream} for this connection.
   *
   * @return The output stream.
   */
  PacketOutputStream getPacketOutputStream()
  {
    return pout;
  }

  /**
   * The last time keys were exchanged.
   *
   * @return The timestamp.
   */
  long KEXTimestamp()
  {
    return kex_timestamp;
  }

// Own methods.
  // -------------------------------------------------------------------------

  /**
   * Create a comma-separated list of algorithms both we and the server
   * supports.
   *
   * @param server_algs The comma-separated list of algorithms the
   *    server supports.
   * @param our_algs The algorithms we support.
   * @return A comma-separated list of the intersection of the two
   *    arguments.
   */
  private String makeAlgString(String server_algs, List our_algs)
  {
    List server = new LinkedList();
    String str = new String();
    for (Enumeration e = new StringTokenizer(server_algs, ",");
         e.hasMoreElements(); )
      {
        server.add(e.nextElement());
      }
    ours:for (Iterator i = our_algs.iterator(); i.hasNext(); )
      {
        String s1 = (String) i.next();
        int ii = (s1.indexOf('@') == -1) ? s1.length() : s1.indexOf('@');
        for (Iterator j = server.iterator(); j.hasNext(); )
          {
            String s2 = (String) j.next();
            int jj = (s2.indexOf('@') == -1) ? s2.length() : s2.indexOf('@');
            if (s1.equals(s2) || s2.startsWith(s1.substring(0, ii))
                || s1.startsWith(s2.substring(0, jj))) {
              if (str.length() > 0) str += ',';
              str += (String) s2;
              continue ours;
            }
          }
      }
    return str;
  }

  /**
   * Take the blob as two 80-bit integers, and encode it as a "raw" GNU
   * Crypto DSA signature.
   *
   * @param blob The SSH 2 DSA public key blob.
   * @return The key.
   */
  private BigInteger[] dssBlobToGNU(byte[] blob)
  {
    BigInteger[] b = new BigInteger[2];
    byte[] buf = new byte[blob.length/2];
    System.arraycopy(blob, 0, buf, 0, buf.length);
    b[0] = new BigInteger(1, buf);
    System.arraycopy(blob, buf.length, buf, 0, buf.length);
    b[1] = new BigInteger(1, buf);
    Debug.debug2("dss blob  =" + Util.toString(blob));
    Debug.debug2("dss blob 1=" + b[0].toString(16));
    Debug.debug2("dss blob 2=" + b[1].toString(16));
    return b;
  }

  /**
   * Choose the first algorithm name in <code>list</code>, given that
   * <code>list</code> is a comma-separated list of strings.
   *
   * @param list The list of comma-separated algorithm names.
   * @return The first algorithm in the list.
   */
  private String chooseAlg(String list)
  {
    int i = list.indexOf(',');
    if (i > 0)
      return list.substring(0, i);
    return list;
  }

  /**
   * Inflate or trim a raw key or initialization vector to its required
   * size, as specified in the SSH 2 transport protocol:
   *
   * <pre>
   *    K1 = HASH(K || H || X || session_id)
   *    K2 = HASH(K || H || K1)
   *    K3 = HASH(K || H || K1 || K2)
   *    ...
   *    key = K1 || K2 || K3 || ...
   * </pre>
   *
   * @param b       The bytes of the already-computed 'K1' above.
   * @param lengths The map from cipher names to key lengths.
   * @param alg     The algorithm's name.
   * @param hash    The message digest to use.
   * @param k       The raw shared secret.
   * @param x       A..F, depending upon what key this is.
   * @param h       The exchange hash.
   * @return The inflated or trimmed key.
   */
  private byte[] trimOrExpand(byte[] b, Map lengths, String alg,
                              IMessageDigest hash, byte[] k, byte x, byte[] h)
  {
    Integer len = (Integer) lengths.get(alg);
    if (len == null) return b;
    int l = len.intValue();
    if (l == b.length)
      {
        return b;
      }
    else if (l < b.length)
      {
        byte[] buf = new byte[l];
        System.arraycopy(b, 0, buf, 0, l);
        return buf;
      }
    byte[][] bb = new byte[l/b.length+((l%b.length==0)?0:1)][];
    for (int i = 0; i < bb.length; i++)
      {
        hash.reset();
        hash.update(k, 0, k.length);
        hash.update(h, 0, h.length);
        if (i == 0)
          {
            hash.update(x);
            hash.update(client.session_id, 0, client.session_id.length);
          }
        else
          {
            for (int j = 0; j < i; j++)
              hash.update(bb[j], 0, bb[j].length);
          }
        bb[i] = hash.digest();
      }
    byte[] buf = new byte[bb.length*b.length];
    int off = 0;
    for (int i = 0; i < bb.length; i++)
      {
        System.arraycopy(bb[i], 0, buf, off, bb[i].length);
        off += bb[i].length;
      }
    if (buf.length > l)
      {
        byte[] buf2 = new byte[l];
        System.arraycopy(buf, 0, buf2, 0, l);
        return buf2;
      }
    return buf;
  }

  /**
   * Initialize the cipher in the given Configuration.
   *
   * @param config The configuration in which to store the
   *    newly-created cipher.
   * @param iv     The initialization vector to initialize the cipher
   *    with.
   * @param key    The key to initialize the cipher with.
   * @param name   The name of the cipher to initialize.
   * @param mode   The mode for the new cipher (encrypt, decrypt).
   * @throws SSH2Exception If the cipher cannot be initialized.
   */
  private void chooseCipher(Configuration config, byte[] iv, byte[] key,
                            String name, int mode)
    throws SSH2Exception
  {
    Map attr = new HashMap();
    attr.put(IMode.KEY_MATERIAL, key);
    attr.put(IMode.IV, iv);
    attr.put(IMode.STATE, new Integer(mode));
    if (name.intern() == "3des-cbc")
      config.cipher = ModeFactory.getInstance(Registry.CBC_MODE, Registry.TRIPLEDES_CIPHER, 8);
    else if (name.intern() == "blowfish-cbc")
      config.cipher = ModeFactory.getInstance(Registry.CBC_MODE, Registry.BLOWFISH_CIPHER, 8);
    else if (name.startsWith("twofish"))
      config.cipher = ModeFactory.getInstance(Registry.CBC_MODE, Registry.TWOFISH_CIPHER, 16);
    else if (name.startsWith("aes"))
      config.cipher = ModeFactory.getInstance(Registry.CBC_MODE, Registry.AES_CIPHER, 16);
    else if (name.startsWith("serpent"))
      config.cipher = ModeFactory.getInstance(Registry.CBC_MODE, Registry.SERPENT_CIPHER, 16);
    else if (name.intern() == "cast128-cbc")
      config.cipher = ModeFactory.getInstance(Registry.CBC_MODE, Registry.CAST5_CIPHER, 8);
    try
      {
        config.cipher.init(attr);
      }
    catch (InvalidKeyException ike)
      {
        throw new SSH2Exception("key is invalid: " + ike.getMessage());
      }
    catch (IllegalArgumentException iae)
      {
        throw new SSH2Exception("illegal argument: " + iae.getMessage());
      }
  }

  /**
   * Inititalize the MAC for the given Configuration.
   *
   * @param config The configuration in which to store the
   *    newly-created MAC.
   * @param key    The key to initialize the MAC with.
   * @param name   The name of the MAC to initialize.
   * @throws SSH2Exception If the specified MAC cannot be initialized.
   */
  private void chooseMac(Configuration config, byte[] key, String name)
    throws SSH2Exception
  {
    Map attr = new HashMap();
    attr.put(IMac.MAC_KEY_MATERIAL, key);
    if (name.startsWith("hmac-sha1"))
      {
        config.mac = MacFactory.getInstance(Registry.HMAC_NAME_PREFIX+Registry.SHA160_HASH);
      }
    else if (name.startsWith("hmac-md5"))
      {
        config.mac = MacFactory.getInstance(Registry.HMAC_NAME_PREFIX+Registry.MD5_HASH);
      }
    if (name.endsWith("-96"))
      {
        attr.put(IMac.TRUNCATED_SIZE, new Integer(12));
      }
    try
      {
        config.mac.init(attr);
      }
    catch (InvalidKeyException ike)
      {
        throw new SSH2Exception("key is invalid: " + ike.getMessage());
      }
    catch (IllegalArgumentException iae)
      {
        throw new SSH2Exception("illegal argument: " + iae.getMessage());
      }
  }

  /**
   * Read up to the supplied message code, ignoring SSH_MSG_IGNORE and
   * SSH_MSG_DEBUG, throwing an exception if a SSH_MSG_DISCONNECT is
   * recieved, or sending an SSH_MSG_DISCONNECT and throwing an
   * exception if anything else is read.
   *
   * @param message The message type to wait for.
   */
  private void readAwaiting(byte message) throws IOException
  {
    Debug.debug2("readAwaiting " + message);
    byte msg_type;
    do
      {
        pin.startPacket();
        msg_type = (byte) pin.read();
        switch (msg_type)
          {
          case SSH_MSG_DISCONNECT:
            Debug.debug2("read SSH_MSG_DISCONNECT");
            int code = (int) pin.readUInt32();
            String desc = pin.readUTF8();
            pin.readString();
            pin.endPacket();
            connected = false;
            throw new DisconnectException(code, desc);

          case SSH_MSG_IGNORE:
            Debug.debug2("read SSH_MSG_IGNORE");
            pin.readString();
            pin.endPacket();
            break;

          case SSH_MSG_DEBUG:
            Debug.debug2("read SSH_MSG_DEBUG");
            pin.readBoolean();
            pin.readString();
            pin.readString();
            pin.endPacket();
            break;

          default:
            if (msg_type == message) break;
            pout.reset();
            pout.write(SSH_MSG_DISCONNECT);
            pout.writeUInt32(SSH_DISCONNECT_PROTOCOL_ERROR);
            pout.writeUTF8("expecting (" + message + ") but got (" +
                           msg_type + ")");
            pout.writeASCII("en-US");
            out.write(pout.toBinaryPacket());
            out.flush();
            connected = false;
            throw new SSH2Exception("expecting (" + message
                                    + ") but got " + msg_type);
          }
      }
    while (msg_type != message);
    Debug.debug2("ok, got " + message);
  }

  /**
   * Encode the modes associated with a terminal in the binary form
   * that SSH expects.
   *
   * @param t The terminal to encode.
   * @return The encoded modes.
   */
  private byte[] makeTerminalModes(TerminalInfo t)
  {
    PacketOutputStream modes_out =
      new PacketOutputStream(new Configuration());

    setControlChar(VINTR,    t._VINTR, modes_out, t);
    setControlChar(VQUIT,    t._VQUIT, modes_out, t);
    setControlChar(VERASE,   t._VERASE, modes_out, t);
    setControlChar(VKILL,    t._VKILL, modes_out, t);
    setControlChar(VEOF,     t._VEOF, modes_out, t);
    setControlChar(VEOL,     t._VEOL, modes_out, t);
    setControlChar(VEOL2,    t._VEOL2, modes_out, t);
    setControlChar(VSTART,   t._VSTART, modes_out, t);
    setControlChar(VSTOP,    t._VSTOP, modes_out, t);
    setControlChar(VSUSP,    t._VSUSP, modes_out, t);
    setControlChar(VREPRINT, t._VREPRINT, modes_out, t);
    setControlChar(VWERASE,  t._VWERASE, modes_out, t);
    setControlChar(VLNEXT,   t._VLNEXT, modes_out, t);
    setControlChar(VDISCARD, t._VDISCARD, modes_out, t);
    setControlChar(VSWTCH,   t._VSWTCH, modes_out, t);
    setControlChar(VDSUSP,   t._VDSUSP, modes_out, t);

    setMode(IGNPAR, t._IGNPAR, modes_out, t);
    setMode(PARMRK, t._PARMRK, modes_out, t);
    setMode(INPCK, t._INPCK, modes_out, t);
    setMode(ISTRIP, t._ISTRIP, modes_out, t);
    setMode(INLCR, t._INLCR, modes_out, t);
    setMode(IGNCR, t._IGNCR, modes_out, t);
    setMode(ICRNL, t._ICRNL, modes_out, t);
    setMode(IUCLC, t._IUCLC, modes_out, t);
    setMode(IXON, t._IXON, modes_out, t);
    setMode(IXANY, t._IXANY, modes_out, t);
    setMode(IXOFF, t._IXOFF, modes_out, t);
    setMode(IMAXBEL, t._IMAXBEL, modes_out, t);

    setMode(ISIG, t._ISIG, modes_out, t);
    setMode(ICANON, t._ICANON, modes_out, t);
    setMode(XCASE, t._XCASE, modes_out, t);
    setMode(ECHO, t._ECHO, modes_out, t);
    setMode(ECHOE, t._ECHOE, modes_out, t);
    setMode(ECHOK, t._ECHOK, modes_out, t);
    setMode(ECHONL, t._ECHONL, modes_out, t);
    setMode(ECHOCTL, t._ECHOCTL, modes_out, t);
    setMode(ECHOKE, t._ECHOKE, modes_out, t);
    setMode(NOFLSH, t._NOFLSH, modes_out, t);
    setMode(TOSTOP, t._TOSTOP, modes_out, t);
    setMode(PENDIN, t._PENDIN, modes_out, t);
    setMode(IEXTEN, t._IEXTEN, modes_out, t);

    setMode(OPOST, t._OPOST, modes_out, t);
    setMode(OLCUC, t._OLCUC, modes_out, t);
    setMode(ONLCR, t._ONLCR, modes_out, t);
    setMode(OCRNL, t._OCRNL, modes_out, t);
    setMode(ONOCR, t._ONOCR, modes_out, t);
    setMode(ONLRET, t._ONLRET, modes_out, t);

    setMode(CS7, t._CS7, modes_out, t);
    setMode(CS8, t._CS8, modes_out, t);
    setMode(PARENB, t._PARENB, modes_out, t);
    setMode(PARODD, t._PARODD, modes_out, t);

    modes_out.write(TTY_OP_ISPEED);
    modes_out.writeUInt32(t.getBaudIn());
    modes_out.write(TTY_OP_OSPEED);
    modes_out.writeUInt32(t.getBaudOut());
    modes_out.write(TTY_OP_END);

    return modes_out.getPayload();
  }

  private void setControlChar(byte ctl, int cchar, PacketOutputStream out,
                              TerminalInfo t)
  {
    out.write(ctl);
    if (t.controlChar(cchar) != null)
      {
        out.writeUInt32(t.controlChar(cchar).charValue());
      }
    else
      {
        out.writeUInt32(0xff);
      }
  }

  private void setMode(byte mode, int mode2, PacketOutputStream out,
                       TerminalInfo t)
  {
    out.write(mode);
    if (t.isSet(mode2))
      {
        out.writeUInt32(1);
      }
    else
      {
        out.writeUInt32(0);
      }
  }

// Inner classes.
  // -------------------------------------------------------------------------

  /**
   * Wait until a set of keys has been used for one hour or to transmit
   * >1GB, then start key re-exchange.
   */
  private class KEXWatcher extends Thread
  {

    // Constants and variables.
    // -----------------------------------------------------------------------

    /** One gigabyte. */
    static final long ONE_GIG = 1073741824;

    /** One hour, in milliseconds. */
    static final long ONE_HOUR = 3600000;

    /** The connection using the keys. */
    protected Connection conn;

    // Constructors.
    // -----------------------------------------------------------------------

    /**
     * @param conn The connection we do re-keying for.
     */
    KEXWatcher(Connection conn)
    {
      this.conn = conn;
    }

    // Methods overriding java.lang.Thread.
    // -----------------------------------------------------------------------

    public void run()
    {
      PacketInputStream pin = conn.getPacketInputStream();
      PacketOutputStream pout = conn.getPacketOutputStream();
      while (conn.connected)
        {
          synchronized (pout)
            {
              if (conn.KEXTimestamp()+ONE_HOUR <= System.currentTimeMillis() ||
                  ONE_GIG < pin.getTotalBytesIn()+pout.getTotalBytesOut())
                {
                  conn.freezeChannelOutput();
                  try
                    {
                      conn.sendKeyExchange();
                    }
                  catch (IOException ioe)
                    {
                      conn.setConnected(false);
                    }
                }
            }
            try
              {
                sleep(60000);  // One minute.
              }
            catch (InterruptedException ie) { }
        }
    }
  }
} // Connection
