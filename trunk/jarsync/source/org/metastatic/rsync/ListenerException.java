/* ListenerException.java -- Exception thrown by listeners.
   $Id$
  
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>
  
   This file is a part of Jarsync
  
   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2 of the License, or (at
   your option) any later version.
  
   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
  
   You should have received a copy of the GNU General Public License
   along with Jarsync; if not, write to the
  
      Free Software Foundation, Inc.,
      59 Temple Place, Suite 330,
      Boston, MA  02111-1307
      USA
  
   Linking Jarsync statically or dynamically with other modules is
   making a combined work based on Jarsync.  Thus, the terms and
   conditions of the GNU General Public License cover the whole
   combination.
  
   As a special exception, the copyright holders of Jarsync give you
   permission to link Jarsync with independent modules to produce an
   executable, regardless of the license terms of these independent
   modules, and to copy and distribute the resulting executable under
   terms of your choice, provided that you also meet, for each linked
   independent module, the terms and conditions of the license of that
   module.  An independent module is a module which is not derived from
   or based on Jarsync.  If you modify Jarsync, you may extend this
   exception to your version of it, but you are not obligated to do so.
   If you do not wish to do so, delete this exception statement from
   your version.  */


package org.metastatic.rsync;

/**
 * Signals an exception raised by an @{link GeneratorListener}, @{link
 * MatcherListener}, or @{link RebuilderListener}.
 *
 * <p>Listener exceptions may contain other exceptions (the "cause") and
 * may be chained together if there are multiple failures accross
 * multiple listeners.
 */
public class ListenerException extends Exception {

   // Fields.
   // -----------------------------------------------------------------------

   protected ListenerException next;

   protected Throwable cause;

   // Constructors.
   // -----------------------------------------------------------------------

   public ListenerException(Throwable cause) {
      super();
      this.cause = cause;
   }

   public ListenerException(Throwable cause, String msg) {
      super(msg);
      this.cause = cause;
   }

   public ListenerException(String msg) {
      super(msg);
   }

   public ListenerException() {
      super();
   }

   // Instance methods.
   // -----------------------------------------------------------------------

   /**
    * Returns the next exception in this chain, or <code>null</code> if
    * there are no other exceptions.
    *
    * @return The next exception.
    */
   public ListenerException getNext() {
      return next;
   }

   /**
    * Sets the next exception in this chain.
    *
    * @param next The next exception.
    */
   public void setNext(ListenerException next) {
      this.next = next;
   }

   /**
    * Gets the cause of this exception, or <code>null</code> if the
    * cause is unknown.
    *
    * @return The cause.
    */
   public Throwable getCause() {
      return cause;
   }

   /**
    * Sets the cause of this exception.
    *
    * @param cause The cause of this exception.
    */
   public synchronized Throwable initCause(Throwable cause) {
      Throwable old = this.cause;
      this.cause = cause;
      return old;
   }
}
