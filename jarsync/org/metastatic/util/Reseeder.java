// vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
// $Id$
//
// Reseeder -- simple time-based entropy gatherer.
// Copyright (C) 2002  Casey Marshall <rsdio@metastatic.org>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the
//
//    Free Software Foundation, Inc.,
//    59 Temple Place, Suite 330,
//    Boston, MA  02111-1307
//    USA
//
// --------------------------------------------------------------------------

package org.metastatic.util;

import java.security.SecureRandom;

/**
 * Gather a few bits of entropy by letting one or more threads
 * periodically "touch" this class when they receive events (such as
 * e.g. input from the network or a key press). The least-significant
 * eight bits of the difference of the times between touches will be
 * used to fill in the seed byte.
 *
 * <p>The PRNG will be reseeded when either (1) {@link #touch()} is
 * called enough times to fill the seed array, or (2) when a long
 * enough time has elapsed since the last reseed (and then only if the
 * seed array has been filled; otherwise the reseed will take place as
 * soon as the seed array is filled).
 *
 * <p>This class is EXPERIMENTAL. It DOES NOT guarantee good random
 * sequences when used in practice. It SHOULD, however, provide
 * reasonably unknowable bits that can be introduced into a
 * pseudo-random sequence generator.
 *
 * <p>Possible future directions of this class include:
 *
 * <ul>
 * <li>Hashing the seed bits before sending them to the PRNG (such as
 * with SHA-1)</li>
 * <li>Mix the new seed with some system entropy (e.g. with /dev/random
 * on Linux systems) before the reseed.</li>
 * <li>Make the initial choices of policy, seed length, and timeout to
 * be random values.</li>
 * </ul>
 *
 * @version $Revision$
 */
public final class Reseeder {

   // Constants and fields.
   // -----------------------------------------------------------------------

   /** Reseed when the seed array is full. */
   public static final int POLICY_WHEN_FULL = 0;

   /** Reseed when a certain amount of time has elapsed. */
   public static final int POLICY_TIME_LIMIT = 1;

   /** The smallest the seed array may be (8 bytes). */
   public static final int MINIMUM_SEED_SIZE = 8;

   /** The smallest timeout, in milliseconds (5 minutes). */
   public static final long MINIMUM_TIMEOUT = 300000;

   /**
    * The next seed. Elements will be the result of type-converting the
    * difference between timestamps from longs to bytes.
    */
   private byte[] nextSeed;

   /**
    * The index in the {@link #nextSeed} array for the next byte.
    */
   private int index;

   /**
    * The timestamp. Updated on every call to {@link #touch()}.
    */
   private long timestamp;

   /**
    * The time in between reseeds.
    */
   private long timeout;

   /**
    * The last time the seed has been set.
    */
   private long lastReseed;

   /**
    * The reseed policy. One of {@link #POLICY_WHEN_FULL} or {@link
    * #POLICY_TIME_LIMIT}.
    */
   private int policy;

   /**
    * When the policy is {@link #POLICY_TIME_LIMIT} and this is true,
    * reseed immediately regardless of the current time.
    */
   private boolean reseedImmediately;

   /**
    * The {@link java.security.SecureRandom} we are reseeding.
    */
   private SecureRandom prng;

   // Constructors.
   // -----------------------------------------------------------------------

   /**
    * Creates a new Reseeder. The <code>timeout</code> argument is
    * ingored when <code>policy</code> is {@link #POLICY_WHEN_FULL}.
    *
    * @param policy   The reseeding policy. One of {@link
    *        #POLICY_WHEN_FULL} or {@link #POLICY_TIME_LIMIT}.
    * @param seedSize The size, in bytes, of the new seed to build.
    * @param timeout  The time, in milliseconds, to wait before
    *        reseeding when the policy is {@link #POLICY_TIME_LIMIT}.
    * @param prng     The {@link java.security.SecureRandom} we are
    *        reseeding.
    * @throws java.lang.IllegalArgumentException If the policy is not
    *         {@link #POLICY_WHEN_FULL} or {@link #POLICY_TIME_LIMIT},
    *         if <code>seedSize</code> is less than {@link
    *         #MINIMUM_SEED_SIZE}, or if the policy is {@link
    *         #POLICY_TIME_LIMIT} and the <code>timeout</code> is less
    *         than {@link #POLICY_TIME_LIMIT}.
    * @throws java.lang.NullPointerException If <code>prng</code> is
    *         <code>null</code>.
    */
   public
   Reseeder(int policy, int seedSize, long timeout, SecureRandom prng)
   throws IllegalArgumentException, NullPointerException {
      if (policy != POLICY_WHEN_FULL && policy != POLICY_TIME_LIMIT) {
         throw new IllegalArgumentException("unknown policy " + policy);
      }
      if (seedSize < MINIMUM_SEED_SIZE) {
         throw new IllegalArgumentException("seed size must be >= "
            + MINIMUM_SEED_SIZE);
      }
      if (policy == POLICY_TIME_LIMIT) {
         if (timeout < MINIMUM_TIMEOUT) {
            throw new IllegalArgumentException("timeout must be >= "
               + MINIMUM_TIMEOUT);
         }
         this.timeout = timeout;
      }
      if (prng == null) {
         throw new NullPointerException();
      }
      this.policy = policy;
      nextSeed = new byte[seedSize];
      this.prng = prng;
      index = 0;
      lastReseed = 0;
      timestamp = System.currentTimeMillis();
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   /**
    * Tap this reseeder with an input byte. The seed value is the delay
    * between taps or touches XORed with the byte.
    *
    * @param b The byte.
    */
   public synchronized void tap(byte b) {
      long oldTimestamp = timestamp;
      timestamp = System.currentTimeMillis();
      long l = timestamp - oldTimestamp;
      if (l > 0) {
         nextSeed[index++] = (byte) (timestamp ^ b);
         if (policy == POLICY_WHEN_FULL) {
            if (index == nextSeed.length) {
               reseed();
            }
         } else {
            if (reseedImmediately) {
               if (index == nextSeed.length) {
                  reseed();
                  reseedImmediately = false;
               }
            } else {
               if (timestamp - lastReseed > timeout) {
                  if (index < nextSeed.length) {
                     reseedImmediately = true;
                  } else {
                     reseed();
                  }
               } else if (index == nextSeed.length) {
                  index = 0;
               }
            }
         }
      }
   }

   /**
    * Touch this reseeder, updating the new seed array and reseeding if
    * it is the right time.
    */
   public synchronized void touch() {
      long oldTimestamp = timestamp;
      timestamp = System.currentTimeMillis();
      long l = timestamp - oldTimestamp;
      if (l > 0) {
         nextSeed[index++] = (byte) timestamp;
         if (policy == POLICY_WHEN_FULL) {
            if (index == nextSeed.length) {
               reseed();
            }
         } else {
            if (reseedImmediately) {
               if (index == nextSeed.length) {
                  reseed();
                  reseedImmediately = false;
               }
            } else {
               if (timestamp - lastReseed > timeout) {
                  if (index < nextSeed.length) {
                     reseedImmediately = true;
                  } else {
                     reseed();
                  }
               } else if (index == nextSeed.length) {
                  index = 0;
               }
            }
         }
      }
   }

   // Own methods.
   // -----------------------------------------------------------------------

   /**
    * Reseed the PRNG with the new seed. This also resets {@link #index}
    * and puts {@link java.lang.System#currentTimeMillis()} into {@link
    * #lastReseed}.
    */
   private void reseed() {
      prng.setSeed(nextSeed);
      index = 0;
      lastReseed = System.currentTimeMillis();
   }
}
