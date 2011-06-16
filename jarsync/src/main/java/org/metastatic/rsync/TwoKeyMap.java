/* TwoKeyMap.java -- Two-key Map implementation.
   Copyright (C) 2003, 2007  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with Jarsync; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

Linking Jarsync statically or dynamically with other modules is making
a combined work based on Jarsync.  Thus, the terms and conditions of
the GNU General Public License cover the whole combination.

As a special exception, the copyright holders of Jarsync give you
permission to link Jarsync with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on Jarsync.  If you modify Jarsync, you may extend this
exception to your version of it, but you are not obligated to do so.
If you do not wish to do so, delete this exception statement from your
version.

ALTERNATIVELY, Jarsync may be licensed under the Apache License,
Version 2.0 (the "License"); you may not use this file except in
compliance with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.  See the License for the specific language governing
permissions and limitations under the License.

If you modify Jarsync, you may extend this same choice of license for
your library, but you are not obligated to do so. If you do not offer
the same license terms, delete the license terms that your library is
NOT licensed under.  */


package org.metastatic.rsync;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * <p>This is a "double-keyed" mapping. The first key is a 16-bit integer,
 * and the second key is a variable-length byte array. With this, we can
 * compute if a given mapping is "probably" in the map using the first key,
 * and compute whether or not a mapping is definitely in the hashtable with
 * the second key. The rationale behind this is that the first key is
 * trivial to compute and that the second key is more difficult to compute
 * but more unique.</p>
 *
 * <p>Since the strong key can be a byte array of any length, then this
 * "strong" key can be shorter (and thus less unique) than the "weak"
 * key. For this class to work properly, the stronger key should be at
 * least four bytes in length, preferably longer.</p>
 *
 * <p>The weak-key/strong-key method is inspired by (and is was written
 * for) the "hashtable" in the rsync algorithm, and has three levels of
 * key search:</p>
 *
 * <ol>
 * <li>Test if the lower 2 bytes of the weak key (the positive part of a
 * 32-bit integer in Java) have been mapped to anything yet. This method
 * always takes O(1) time, and it is assumed that the weak key is
 * trivial to compute.</li>
 *
 * <li>Test if the entire weak key is mapped to anything. Since this
 * class uses a linked list to handle collisions where the lower 2 bytes
 * are the same, this method takes at most O(n) operations (also
 * assuming that the weak key is trivial to compute).</li>
 *
 * <li>Test if both the weak and strong keys map to an Object. In
 * addition to the linked-list search of the second step, this involves
 * a search of a red-black tree, meaning that the upper-bound time
 * complexity ranges from O(lg(n)) to O(n). This works under the
 * assumption that the strong key is some sort of {@link
 * MessageDigest}, and thus takes longer to compute.</li>
 * </ol>
 *
 * <p>With this method, we can determine if it is worth it to compute the
 * strong key if we have already computed the weak key.</p>
 *
 * <p><code>null</code> is not a valid key in this map.</p>
 *
 * @author Casey Marshall
 * @version $Revision$
 */
public class TwoKeyMap<V> implements java.io.Serializable, Map<ChecksumPair, V> {

  // Constants and variables.
  // -----------------------------------------------------------------

  /**
   * 
   */
  private static final long serialVersionUID = 9086291256860176241L;

  /**
   * The sub-tables whose keys are the larger, stronger keys. The
   * index of this array is the shorter, weaker key.
   *
   * @since 1.1
   */
  //protected final SubTable<V>[] tables;
  protected final HashMap<Integer, SubTable<V>> tables;

  // Inner classes.
  // -----------------------------------------------------------------

  static final class MapEntry<W> implements Map.Entry<ChecksumPair, W>
  {
    private final ChecksumPair key;
    private final W value;
    
    MapEntry(int weak, StrongKey strong, W value)
    {
      key = new ChecksumPair(weak, strong.key);
      this.value = value;
    }
    
    public ChecksumPair getKey()
    {
      return key;
    }
    
    public W getValue()
    {
      return value;
    }

    public W setValue(W value)
    {
      throw new UnsupportedOperationException();
    }
  }
  
  /**
   * A {@link java.util.Map.Entry} that contains another {@link
   * java.util.Map} that is keyed with stronger, larger keys, and links
   * to other SubTables whose {@link #key}'s lower four bytes are
   * equivalent.
   *
   * @since 1.1
   * @version 1.1
   * @author Casey Marshall
   */
  public static class SubTable<W>
    implements Map.Entry<Integer, Map<StrongKey, W>>, java.io.Serializable
  {
    private static final long serialVersionUID = 3275612303022001357L;

    /**
     * The sub-table, a {@link java.util.Map} that is an instance of
     * {@link java.util.TreeMap}.
     *
     * @since 1.1
     */
    protected Map<StrongKey, W> data;

    /**
     * The index in the array of sub-tables in which this entry is a
     * member.
     *
     * @since 1.1
     */
    protected Integer key;

    /**
     * The next sub-table, implements a linked list.
     *
     * @since 1.1
     */
    SubTable<W> next;

    /**
     * Create a new sub-table with a given index and a given Comparator.
     *
     * @since 1.1
     */
    SubTable(Integer key) {
      data = new TreeMap<StrongKey, W>();
      this.key = key;
      next = null;
    }

    SubTable(int key) {
      this(new Integer(key));
    }

    // Public instance methods.
    // --------------------------------------------------------------

    /**
     * Get the Object that is mapped by the strong key <tt>key</tt>.
     *
     * @since 1.1
     * @param key The key to look for in this sub-table.
     * @return The object mapped to by the given key, or null if there
     *    is no such mapping.
     */
    public W get(StrongKey key) {
      return data.get(key);
    }

    /**
     * Map the given key to the given value.
     *
     * @since 1.1
     * @param key The key.
     * @param value The value.
     */
    public void put(StrongKey key, W value) {
      data.put(key, value);
    }

    /**
     * Test if this sub-table contains the given key.
     *
     * @since 1.1
     * @param key The key to look for.
     * @return <tt>true</tt> if there is a mapping from the given key.
     */
    public boolean containsKey(StrongKey key) {
      return data.containsKey(key);
    }

    /**
     * Test if this sub-table contains the given value.
     *
     * @since 1.1
     * @param value The value to look for.
     * @return <tt>true</tt> if there is a mapping to the given value.
     */
    public boolean containsValue(Object value) {
      return data.containsValue(value);
    }

    // Public instance methods implementing java.util.Map.Entry

    /**
     * Test if another object equals this one.
     *
     * @since 1.1
     * @param o The object to test.
     * @return <tt>true</tt> If <tt>o</tt> is an instance of this class
     *    and its fields are equivalent.
     * @throws java.lang.ClassCastException If <tt>o</tt> is not an
     *    instance of this class.
     * @throws java.lang.NullPointerException If <tt>o</tt> is null.
     */
    public boolean equals(Object o) {
      return data.equals(((SubTable) o).data) &&
      key.equals(((SubTable) o).key);
    }

    /**
     * Get the key that maps to this entry.
     *
     * @return The key, a {@link java.lang.Integer} that maps to this
     *    entry.
     * @since 1.1
     */
    public Integer getKey() {
      return key;
    }

    /**
     * Get the value of this entry.
     *
     * @return A {@link java.util.Map} that represents the sub-table.
     * @since 1.1
     */
    public Map<StrongKey, W> getValue() {
      return data;
    }

    /**
     * Return the hash code for this entry.
     *
     * @return The hash code for the Map that implements this sub-table.
     * @since 1.1
     */
    public int hashCode() {
      return data.hashCode();
    }

    // Unsupported methods from java.util.Map.Entry ------------------

    public Map<StrongKey, W> setValue(Map<StrongKey, W> value) {
      throw new UnsupportedOperationException();
    }

    // Public instance method overriding Object. ---------------------

    /**
     * Return a string representation of this object.
     *
     * @return A string representation of this object.
     * @since 1.1
     */
    public String toString() {
      return Integer.toHexString(key.intValue()) + " => " + data.toString();
    }
  }

  /**
   * The stronger of the two keys in this {@link java.util.Map}. It is
   * basically a wrapper around an array of bytes, providing methods
   * important for using this class as the key in a Map, namely the
   * {@link #hashCode()} and {@link #compareTo(java.lang.Object)}
   * methods.
   *
   * @since 1.1
   * @version 1.0
   * @author Casey Marshall
   */
  public static class StrongKey implements java.io.Serializable,
    Comparable<StrongKey>
  {

    private static final long serialVersionUID = 5794183852715084621L;

    /**
     * The key itself. An array of some number of bytes.
     *
     * @since 1.0
     */
    protected byte[] key;

    // Constructors.
    // --------------------------------------------------------------

    /**
     * Create a new key with the specified bytes. <code>key</code> can be
     * <code>null</code>.
     *
     * @since 1.0
     * @param key The bytes that will make up this key.
     */
    StrongKey(byte[] key) {
      if (key != null)
        this.key = (byte[]) key.clone();
      else
        this.key = key;
    }

    // Instance methods.
    // --------------------------------------------------------------

    /**
     * Return the bytes that compose this Key.
     *
     * @since 1.0
     * @return {@link #key}, the bytes that compose this key.
     */
    public byte[] getBytes() {
      if (key != null)
        return (byte[]) key.clone();
      return null;
    }

    /**
     * Set the byte array that composes this key.
     *
     * @since 1.0
     * @param key The bytes that will compose this key.
     */
    public void setBytes(byte[] key) {
      if (key != null)
        this.key = (byte[]) key.clone();
      else
        this.key = key;
    }

    /**
     * The length, in bytes, of this key.
     *
     * @since 1.0
     * @return The length of this key in bytes.
     */
    public int length() {
      if (key != null)
        return key.length;
      return 0;
    }

    // Public instance methods overriding java.lang.Object -----------

    /**
     * Return a zero-padded hexadecimal string representing this key.
     *
     * @return A hexadecimal string of the bytes in {@link #key}.
     * @since 1.0
     */
    public String toString() {
      if (key == null || key.length == 0)
        return "nil";
      StringBuffer str = new StringBuffer(key.length * 2);
      for (int i = 0; i < key.length; i++)
        {
          if ((key[i] & 0xFF) < 0x10)
            str.append('0');
          str.append(Integer.toHexString(key[i] & 0xFF));
        }
      return str.toString();
    }

    /**
     * The hash code for this key. This is defined as the XOR of all
     * 32-bit blocks of the {@link #key} array.
     *
     * @return The hash code for this key.
     * @since 1.0
     */
    public int hashCode() {
      if (key == null)
        return 0;
      int code = 0;
      for (int i = key.length - 1; i >= 0; i--)
        code ^= ((int) key[i] & 0xff) << (((key.length - i - 1) * 8) % 32);
      return code;
    }

    /**
     * Test if this key equals another. Two keys are equal if the method
     * {@link java.util.Arrays#equals(byte[],byte[])} returns true for
     * thier key arrays.
     *
     * @since 1.0
     * @throws java.lang.ClassCastException If o is not a StrongKey.
     * @param o The object to compare to.
     * @return <tt>true</tt> If this key is equivalent to the argument.
     */
    public boolean equals(Object o) {
      return Arrays.equals(key, ((StrongKey) o).key);
    }

    // java.lang.Comparable interface implementation -----------------

    /**
     * Compare this object to another. This method returns an integer
     * value less than, equal to, or greater than zero if this key
     * is less than, equal to, or greater than the given key. This
     * method will return
     * <ul>
     * <li>0 if the {@link #key} fields are references to the same
     *    array.
     * <li>1 if {@link #key} in this class is null.
     * <li>-1 if {@link #key} in <tt>o</tt> is null (null is always less
     *    than everything).
     * <li>0 if the lengths of the {@link #key} arrays are the same and
     *    their contents are equivalent.
     * <li>The difference between the lengths of the keys if different.
     * <li>The difference between the first two different members of the
     *    arrays.
     * </ul>
     *
     * @since 1.0
     * @throws java.lang.ClassCastException If o is not a StrongKey.
     * @param o The key to compare to.
     * @return An integer derived from the differences of the two keys.
     */
    public int compareTo(StrongKey sk)
    {
      if (key == sk.key) return 0;
      if (key == null) return 1;
      if (sk.key == null) return -1;
      if (java.util.Arrays.equals(key, sk.getBytes())) return 0;
      if (key.length != sk.length()) return key.length - sk.length();
      byte[] arr = sk.getBytes();
      for (int i = 0; i < key.length; i++)
        if (key[i] != arr[i]) return (key[i] - arr[i]);
      return 0; // unreachable
    }
  }


 // Constructors.
  // -----------------------------------------------------------------

  private static final int DEFAULT_CAPACITY = 256;
  
  public TwoKeyMap()
  {
    this(DEFAULT_CAPACITY);
  }
  
  /**
   * Creates a new Map with 2^16 sub-tables.
   *
   * @since 1.1
   */
  public TwoKeyMap(int initialCapacity)
  {
    super();
    tables = new HashMap<Integer, SubTable<V>>(initialCapacity);
  }

  /**
   * Create a new Map with all the mappings of the given map.
   *
   * @since 1.1
   * @param m The initial mappings this Map should contain.
   * @throws java.lang.ClassCastException If one of the keys in
   *    <code>m</code> is not a ChecksumPair.
   * @throws java.lang.NullPointerException If one of the keys in
   *    <code>m</code> is null.
   */
  public TwoKeyMap(Map<ChecksumPair, V> m)
  {
    this();
    putAll(m);
  }

  // Instance methods.
  // -----------------------------------------------------------------

  /**
   * Test if the map contains the lower two bytes of the weak key. This
   * is the fastest, and least accurate <code>containsKey</code>
   * method.
   *
   * @since 1.1
   * @param key The key to check.
   * @return true If the index <code>key &amp; 0xffff</code> in {@link
   *    #tables} is non-null.
   */
  public boolean containsKey(int key)
  {
    return tables.containsKey(key & 0xffff);
  }

  /**
   * Put the given object at the location specified by a key pair.
   *
   * @since 1.1
   * @param key   The {@link ChecksumPair} to use as the key.
   * @param value The value to map to.
   * @return The old value mapped, if any.
   */
  public V put(ChecksumPair pair, V value)
  {
    V old = null;
    SubTable<V> entry = null;
    if (containsKey(pair))
      old = get(pair);
    if (!tables.containsKey(pair.weak & 0xffff))
      {
        entry = new SubTable<V>(pair.weak);
        tables.put(pair.weak & 0xffff, entry);
      }
    else
      {
        entry = tables.get(pair.weak & 0xffff);
        while (entry.getKey() != pair.weak)
          {
            SubTable<V> temp = entry;
            entry = entry.next;
            if (entry == null)
              {
                entry = new SubTable<V>(pair.weak);
                temp.next = entry;
              }
          }
      }
    entry.put(new StrongKey(pair.strong), value);
    return old;
  }

  /**
   * Test if this map contains either the supplied weak key (if the
   * argument is an Integer) or both the weak and strong keys (if the
   * argument is a {@link ChecksumPair}).
   *
   * @since 1.1
   * @param key The key to check.
   * @return true If the map contains the given weak key (if
   *    <code>key</code> is an Integer) or the given pair of keys (if
   *    <code>key</code> is a {@link ChecksumPair}).
   */
  public boolean containsKey(Object key)
  {
    SubTable<V> t;
    if (key instanceof Integer)
      {
        t = tables.get(((Integer) key) & 0xffff);
        while (t != null)
          {
            if (t.getKey().equals(key))
              {
                return true;
              }
            t = t.next;
          }
      }
     else if (key instanceof ChecksumPair)
       {
         ChecksumPair pair = (ChecksumPair) key;
         t = tables.get(pair.weak & 0xffff);
         while (t != null)
           {
             if (t.getKey().equals(key))
               {
                 return t.containsKey(new StrongKey(pair.strong));
               }
             t = t.next;
           }
       }
    return false;
  }

  /**
   * Get the object mapped to by the given key pair. The argument
   * SHOULD be a {@link ChecksumPair}.
   *
   * @since 1.1
   * @param key The key of the object to get.
   * @return The object keyed by <code>key</code>, or <code>null</code>
   *    if there is no such object.
   */
  public V get(Object key)
  {
    try
    {
      ChecksumPair pair = (ChecksumPair) key;
      SubTable<V> table = tables.get(pair.weak & 0xffff);
      while (table != null)
        {
          if (table.getKey() == pair.weak)
            {
              return table.get(new StrongKey(pair.strong));
            }
          table = table.next;
        }
    }
    catch (ClassCastException cce)
    {
      return null;
    }
    return null;
  }

  /**
   * Clear this Map.
   *
   * @since 1.1
   */
  public void clear()
  {
    tables.clear();
  }

  /**
   * Test if the given value is in one of the sub-tables.
   *
   * @since 1.1
   * @param value The value to search for.
   * @return <tt>true</tt> if <tt>value</tt> is in this Map.
   */
  public boolean containsValue(Object value)
  {
    for (Object table : entrySet())
      {
        if (((SubTable) table).containsValue(value))
          {
            return true;
          }
      }
    return false;
  }

  /**
   * Return an unmodifiable set of the SubTable objects in this class.
   *
   * @since 1.1
   * @return A set of all sub-tables from this class.
   */
  public Set<Map.Entry<ChecksumPair, V>> entrySet()
  {
    HashSet<Map.Entry<ChecksumPair, V>> s = new HashSet<Map.Entry<ChecksumPair, V>>();
    for (SubTable<V> entry : tables.values())
      {
        while (entry != null)
          {
            for (Map.Entry<StrongKey, V> e : entry.data.entrySet())
              s.add(new MapEntry<V>(entry.key, e.getKey(), e.getValue()));
            entry = entry.next;
          }
      }
    return s;
  }

  /**
   * Test if this object equals another.
   *
   * @since 1.1
   * @return <tt>true</tt> if <tt>o</tt> is an instance of this class
   *    and if it contains the same sub-tables.
   * @throws java.lang.ClassCastException if <tt>o</tt> is not an
   *    instance of this class.
   * @throws java.lang.NullPointerException if <tt>o</tt> is null.
   */
  public boolean equals(Object o)
  {
    if (!(o instanceof TwoKeyMap))
      return false;
    TwoKeyMap that = (TwoKeyMap) o;
    return entrySet().equals(that.entrySet());
  }

  /**
   * Return the hash code of this object.
   *
   * @since 1.1
   * @return The hash code of this object.
   */
  public int hashCode()
  {
    return tables.hashCode();
  }

  /**
   * Test if there are no mappings in this map.
   *
   * @return <tt>true</tt> if this map is empty.
   * @since 1.1
   */
  public boolean isEmpty()
  {
    return tables.isEmpty();
  }

  /**
   * Return an unmodifiable set of all the pairs of keys in this
   * mapping.
   *
   * @since 1.1
   * @return A set of all the {@link ChecksumPair}s in this mapping.
   */
  public Set<ChecksumPair> keySet()
  {
    HashSet<ChecksumPair> s = new HashSet<ChecksumPair>();
    for (SubTable<V> entry : tables.values())
      {
        while (entry != null)
          {
            for (StrongKey strong : entry.data.keySet())
              s.add(new ChecksumPair(entry.key, strong.key));
            entry = entry.next;
          }
      }
    return s;
  }

  /**
   * Put every entry in <code>m</code> in this map. This method will
   * only work if the keys of <code>m</code> are of type {@link
   * ChecksumPair}.
   *
   * @since 1.1
   * @param m The mappings to put.
   * @throws java.lang.ClassCastException If every key in <code>m</code>
   *   is not a ChecksumPair.
   * @throws java.lang.NullPointerException If a key in <code>m</code>
   *   is null.
   * @see #put(Object,Object)
   */
  public void putAll(Map<? extends ChecksumPair, ? extends V> m)
  {
    for (Map.Entry<? extends ChecksumPair, ? extends V> e : m.entrySet())
      put(e.getKey(), e.getValue());
  }

  /**
   * Removes a single mapping if the argument is a {@link ChecksumPair}, or
   * an entire {@link SubTable} if the argument is a {@link
   * java.lang.Integer}.
   *
   * @since 1.1
   * @param key The key of the object to be removed.
   * @return The removed object, if such a mapping existed.
   *    <code>null</code> otherwise.
   */
  public V remove(Object key)
  {
    if (key instanceof ChecksumPair)
      {
        ChecksumPair pair = (ChecksumPair) key;
        SubTable<V> prev = null;
        SubTable<V> entry = tables.get(pair.weak & 0xffff);
        while (entry != null)
          {
            if (entry.getKey() == pair.weak)
              {
                V value = entry.getValue().remove(new StrongKey(pair.strong));
                if (entry.getValue().isEmpty())
                  {
                    if (prev == null)
                      {
                        if (entry.next != null)
                          tables.put(pair.weak & 0xffff, entry.next);
                        else
                          tables.remove(pair.weak & 0xffff);
                      }
                    else
                      prev.next = entry.next;
                  }
                return value;
              }
            prev = entry;
            entry = entry.next;
          }
      }
    return null;
  }

  /**
   * Return the number of mappings.
   *
   * @since 1.1
   * @return The number of mappings.
   */
  public int size()
  {
    int size = 0;
    for (SubTable<V> entry : tables.values())
      {
        while (entry != null)
          {
            size += ((Map) entry.getValue()).size();
            entry = entry.next;
          }
      }
    return size;
  }

  /**
   * Return a Collection of all the values mapped by this object.
   *
   * @since 1.1
   * @return A Collection of all values mapped by this object.
   */
  public Collection<V> values()
  {
    Collection<V> c = new ArrayList<V>();
    for (SubTable<V> entry : tables.values())
      {
        while (entry != null)
          {
            c.addAll(entry.data.values());
            entry = entry.next;
          }
      }
    return c;
  }

  // Public instance methods overriding java.lang.Object. ------------

  /**
   * Create a printable version of this Map.
   *
   * @return A {@link java.lang.String} representing this object.
   * @since 1.1
   */
  public String toString()
  {
    StringBuffer str = new StringBuffer("{ ");
    for (Map.Entry<Integer, SubTable<V>> entry : tables.entrySet())
      {
        str.append((str.length() != 2) ? (", ") : (" "));
        str.append(Integer.toHexString(entry.getKey()) + " => { ");
        SubTable<V> t = entry.getValue();
        while (t != null)
          {
            str.append(t.toString());
            t = t.next;
            str.append((t == null) ? (" }") : (", "));
          }
      }
    str.append(" }");
    return str.toString();
  }
}
