/* Target.java -- a sigle web page being monitored.
   -*- mode: java; c-basic-offset: 3; -*-
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of vwdiff.

Vwdiff is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your
option) any later version.

Vwdiff is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License along
with vwdiff; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA  */


package org.metastatic.vwdiff;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.keypoint.PngEncoderB;

import org.apache.log4j.Logger;

import org.metastatic.rsync.Checksum32;
import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.Delta;
import org.metastatic.rsync.Generator;
import org.metastatic.rsync.Matcher;
import org.metastatic.rsync.Offsets;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;

public class Target implements java.io.Serializable {

   // Fields.
   // -----------------------------------------------------------------------

   private static final NVPair[] USER_AGENT = { new NVPair("User-Agent",
      "vwdiff/" + version.VERSION +
      " (Jarsync/" + org.metastatic.rsync.version.VERSION +
      "; Java/" + System.getProperty("java.version") +
      "; " + System.getProperty("java.vm.name") +
      "/"  + System.getProperty("java.vm.version") +
      "; " + System.getProperty("os.name") +
      "/"  + System.getProperty("os.version") + 
      "; http://jarsync.sourceforge.net/vwdiff/)") };

   private static final Logger logger = Logger.getLogger(Target.class);

   private static final Color CLEAR = new Color(0, 0, 0, 0);

   private static final short CHAR_OFFSET = 31;

   private static final long serialVersionUID = -4218732007574903639L;

   private final transient Object lock = new Object();

   private final String name;
   private String user;
   private char[] password;
   private URL url;
   private long frequency;
   private Configuration config;
   private Color color, newColor, movedColor;
   private int width;
   private byte[] basis;
   private byte[] image;
   private long lastUpdate, lastAccess;
   private long bytes, newBytes, movedBytes;
   private double threshold;

   // Constructor.
   // -----------------------------------------------------------------------

   public Target(String name) {
      this.name = name;
      config = new Configuration();
      frequency = 3600000;
      try {
         config.strongSum = MessageDigest.getInstance("MD4");
      } catch (NoSuchAlgorithmException nsae) {
         throw new Error(nsae);
      }
      config.strongSumLength = 2;
      config.weakSum = new Checksum32(CHAR_OFFSET);
      color = new Color(216, 216, 255);
      movedColor = new Color(196, 196, 255);
      newColor = new Color(255, 255, 160);
      width = 128;
      lastUpdate = 0;
      lastAccess = 0;
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   public void access() {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[512];
      try {
         HTTPConnection conn = new HTTPConnection(url);
         conn.setDefaultHeaders(USER_AGENT);
         HTTPResponse resp = conn.Get(url.getFile());
         if (resp.getStatusCode() >= 400) {
            throw new IOException(resp.getStatusCode() + resp.getReasonLine());
         }
         InputStream in = resp.getInputStream();
         int len = 0;
         while ((len = in.read(buf)) != -1)
            out.write(buf, 0, len);
      } catch (HTTPClient.ModuleException me) {
         logger.warn(me.toString());
         return;
      } catch (IOException ioe) {
         logger.warn(ioe.toString());
         return;
      }
      synchronized (lock) {
         basis = out.toByteArray();
         lastAccess = System.currentTimeMillis();
      }
      update(true, true);
   }

   public void update(boolean force, boolean basisOnly) {
      try {
         logger.info("updating [" + name + "] with URL " + url);
         if (!force && System.currentTimeMillis() - lastUpdate < frequency) {
            logger.info("[" + name + "] not ready for update");
            return;
         }
         Generator gen = new Generator(config);
         List sums = Collections.EMPTY_LIST;
         if (basis != null)
            sums = gen.generateSums(basis);
         InputStream in = null;
         Matcher match = new Matcher(config);
         if (!basisOnly) {
            HTTPConnection conn = new HTTPConnection(url);
            conn.setDefaultHeaders(USER_AGENT);
            HTTPResponse resp = conn.Get(url.getFile());
            if (resp.getStatusCode() >= 400) {
               throw new IOException(resp.getStatusCode() + resp.getReasonLine());
            }
            in = resp.getInputStream();
         } else {
            in = new ByteArrayInputStream(basis);
         }
         List deltas = match.hashSearch(sums, in);
         bytes = newBytes = movedBytes = 0;
         for (Iterator it = deltas.iterator(); it.hasNext(); ) {
            Delta d = (Delta) it.next();
            bytes += d.getBlockLength();
            if (d instanceof Offsets) {
               if (((Offsets) d).getOldOffset() != ((Offsets) d).getNewOffset())
                  movedBytes += d.getBlockLength();
            } else {
               newBytes += d.getBlockLength();
            }
         }
         synchronized (lock) {
            image = createImage(bytes, deltas);
            lastUpdate = System.currentTimeMillis();
            logger.info("created " + deltas.size() + " deltas, read "
                        + bytes + " bytes");
         }
      } catch (HTTPClient.ModuleException me) {
         logger.warn(me.toString());
      } catch (IOException ioe) {
         logger.error(ioe.getMessage());
      }
   }

   // Property accessor methods.
   // -----------------------------------------------------------------------

   public String getName() {
      return name;
   }

   public synchronized String getHTTPUser() {
      return user;
   }

   public synchronized void setHTTPUser(String user) {
      this.user = user;
   }

   public synchronized char[] getHTTPPassword() {
      return password;
   }

   public synchronized void setHTTPPassword(char[] password) {
      this.password = password;
   }

   public synchronized URL getURL() {
      return url;
   }

   public synchronized void setURL(URL url) {
      this.url = url;
   }

   public synchronized long getFrequency() {
      return frequency;
   }

   public synchronized void setFrequency(long frequency) {
      this.frequency = frequency;
   }

   public Configuration getConfig() {
      return config;
   }

   public void setColor(Color color) {
      this.color = color;
   }

   public void setNewColor(Color newColor) {
      this.newColor = newColor;
   }

   public void setMovedColor(Color movedColor) {
      this.movedColor = movedColor;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public byte[] getBasis() {
      return basis;
   }

   public void setBasis(byte[] basis) {
      this.basis = basis;
   }

   public byte[] getImage() {
      return image;
   }

   public void setImage(byte[] image) {
      this.image = image;
   }

   public long getLastUpdate() {
      return lastUpdate;
   }

   public void setLastUpdate(long lastUpdate) {
      this.lastUpdate = lastUpdate;
   }

   public long getLastAccess() {
      return lastAccess;
   }

   public void setLastAccess(long lastAccess) {
      this.lastAccess = lastAccess;
   }

   public long getLength() {
      return bytes;
   }

   public void setLength(long bytes) {
      this.bytes = bytes;
   }

   public long getNewLength() {
      return newBytes;
   }

   public void setNewLength(long newBytes) {
      this.newBytes = newBytes;
   }

   public long getMovedLength() {
      return movedBytes;
   }

   public void setMovedLength(long movedBytes) {
      this.movedBytes = movedBytes;
   }

   public double getThreshold() {
      return threshold;
   }

   public void setThreshold(double threshold) {
      this.threshold = threshold;
   }

   // Own methods.
   // -----------------------------------------------------------------------

   private byte[] createImage(long bytes, List deltas) {
      int h = (int) (bytes / width) + (bytes % width != 0 ? 1 : 0);
      int x = 0, y = 0;
      BufferedImage img = new BufferedImage(width, h,
                                            BufferedImage.TYPE_4BYTE_ABGR);
      Graphics2D g = img.createGraphics();
      g.setBackground(CLEAR);
      g.clearRect(0, 0, width, h);
      for (Iterator it = deltas.iterator(); it.hasNext(); ) {
         Delta d = (Delta) it.next();
         if (d instanceof Offsets) {
            if (((Offsets) d).getOldOffset() != ((Offsets) d).getNewOffset())
               g.setColor(movedColor);
            else
               g.setColor(color);
         } else {
            g.setColor(newColor);
         }
         for (long l = 0; l < d.getBlockLength(); l++) {
            g.fillRect(x, y, 1, 1);
            x++;
            if (x == width) {
               x = 0;
               y++;
            }
         }
      }
      PngEncoderB encoder = new PngEncoderB(img, true);
      encoder.setCompressionLevel(7);
      return encoder.pngEncode();
   }
}
