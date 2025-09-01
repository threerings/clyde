//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.config;

import java.io.IOException;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;

import com.samskivert.util.ObjectUtil;
import com.samskivert.util.SortableArrayList;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Copyable;
import com.threerings.util.DeepUtil;

/**
 * Stores arguments in a sorted entry list.
 */
public class ArgumentMap extends AbstractMap<String, Object>
  implements Copyable, Streamable
{
  /**
   * Creates an argument map with the supplied arguments.
   */
  public ArgumentMap (String firstKey, Object firstValue, Object... otherArgs)
  {
    put(firstKey, firstValue);
    for (int ii = 0; ii < otherArgs.length; ii += 2) {
      put((String)otherArgs[ii], otherArgs[ii + 1]);
    }
  }

  /**
   * Creates an empty map.
   */
  public ArgumentMap ()
  {
  }

  /**
   * Custom write method.
   */
  public void writeObject (ObjectOutputStream out)
    throws IOException
  {
    int size = _entries.size();
    out.writeInt(size);
    for (int ii = 0; ii < size; ii++) {
      Map.Entry<String, Object> entry = _entries.get(ii);
      out.writeIntern(entry.getKey());
      out.writeObject(entry.getValue());
    }
  }

  /**
   * Custom read method.
   */
  public void readObject (ObjectInputStream in)
    throws IOException, ClassNotFoundException
  {
    for (int ii = 0, nn = in.readInt(); ii < nn; ii++) {
      _entries.add(newEntry(in.readIntern(), in.readObject()));
    }
  }

  // documentation inherited from interface Copyable
  public Object copy (Object dest)
  {
    return copy(dest, null);
  }

  // documentation inherited from interface Copyable
  public Object copy (Object dest, Object outer)
  {
    ArgumentMap cmap;
    if (dest instanceof ArgumentMap) {
      cmap = (ArgumentMap)dest;
      cmap.clear();
    } else {
      cmap = new ArgumentMap();
    }
    for (int ii = 0, nn = _entries.size(); ii < nn; ii++) {
      Map.Entry<String, Object> entry = _entries.get(ii);
      cmap._entries.add(newEntry(entry.getKey(), DeepUtil.copy(entry.getValue())));
    }
    return cmap;
  }

  @Override
  public int size ()
  {
    return _entries.size();
  }

  @Override
  public boolean containsValue (Object value)
  {
    for (int ii = 0, nn = _entries.size(); ii < nn; ii++) {
      // equals() must be called on the value provided to this method, per Java spec
      if (Objects.equal(value, _entries.get(ii).getValue())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsKey (Object key)
  {
    return findKeyIndex(key) >= 0;
  }

  @Override
  public Object get (Object key)
  {
    int idx = findKeyIndex(key);
    return (idx >= 0) ? _entries.get(idx).getValue() : null;
  }

  /**
   * Retrieve a casted value for the specified key, or null.
   */
  public <T> T get (String key, Class<T> type)
  {
    int idx = _entries.binarySearch(_key.as(key));
    return (idx >= 0) ? ObjectUtil.as(_entries.get(idx).getValue(), type) : null;
  }

  @Override
  public Object put (String key, Object value)
  {
    int idx = _entries.binarySearch(_key.as(key));
    if (idx >= 0) {
      return _entries.get(idx).setValue(value);
    } else {
      _entries.add(-idx - 1, newEntry(key, value));
      return null;
    }
  }

  @Override
  public Object remove (Object key)
  {
    int idx = findKeyIndex(key);
    return (idx >= 0) ? _entries.remove(idx).getValue() : null;
  }

  @Override
  public void clear ()
  {
    _entries.clear();
  }

  @Override
  public Set<Map.Entry<String, Object>> entrySet ()
  {
    return new AbstractSet<Map.Entry<String, Object>>() {
      @Override public int size () {
        return _entries.size();
      }
      @Override public boolean contains (Object o) {
        return findEntryIndex(o) >= 0;
      }
      @Override public Iterator<Map.Entry<String, Object>> iterator () {
        return _entries.iterator();
      }
      @Override public boolean remove (Object o) {
        int idx = findEntryIndex(o);
        if (idx < 0) {
          return false;
        }
        _entries.remove(idx);
        return true;
      }
      @Override public void clear () {
        _entries.clear();
      }

      /**
       * Find the index of the Map.Entry that is equals() to the specified value, or
       * return a negative value.
       */
      protected int findEntryIndex (Object value)
      {
        if (value instanceof Map.Entry<?,?>) {
          Map.Entry<?,?> entry = (Map.Entry<?,?>)value;
          int idx = findKeyIndex(entry.getKey());
          return ((idx >= 0) &&
              Objects.equal(entry.getValue(), _entries.get(idx).getValue()))
            ? idx
            : -1;
          // Note that we have "externalized" the call to this entry's equals().
          // It is technically possible that the passed-in value implements
          // Map.Entry but overrides equals() to act like a fishing-key
          // for removal. That is fucking weird, and to properly support that
          // we'd have to fall-back to checking every entry, which is a terrible
          // thing to do with normal Map.Entry objects that are simply not present here.
        }
        if (value != null) {
          for (int ii = 0, nn = _entries.size(); ii < nn; ii++) {
            if (value.equals(_entries.get(ii))) {
              return ii;
            }
          }
        }
        return -1;
      }
    };
  }

  @Override
  public boolean equals (Object other)
  {
    if (other == this) {
      return true;
    }
    if (!(other instanceof ArgumentMap)) {
      return false;
    }
    ArgumentMap omap = (ArgumentMap)other;
    int size = size();
    if (size != omap.size()) {
      return false;
    }
    for (int ii = 0; ii < size; ii++) {
      Map.Entry<String, Object> entry = _entries.get(ii), oentry = omap._entries.get(ii);
      if (!entry.getKey().equals(oentry.getKey())) {
        return false;
      }
      _a1[0] = entry.getValue();
      _a2[0] = oentry.getValue();
      if (!Arrays.deepEquals(_a1, _a2)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode ()
  {
    int hash = 0;
    for (int ii = 0, nn = _entries.size(); ii < nn; ii++) {
      Map.Entry<String, Object> entry = _entries.get(ii);
      _a1[0] = entry.getValue();
      hash += entry.getKey().hashCode() ^ Arrays.deepHashCode(_a1);
    }
    return hash;
  }

  @Override
  public ArgumentMap clone ()
  {
    return (ArgumentMap) copy(null);
  }

  /**
   * Return the key index or a negative value to indicate not found.
   * If the provided key is a String (the very common case) the return value is the result
   * of a binary search for that key, otherwise we examine each key individually and return
   * an arbitrary negative value.
   * The reason we do this is that the Java Collection spec provides that an arbitrary
   * Object of a different class can be passed-in as a key. Note also that we do not check
   * the hashcode() of the key because we are not a hashing map.
   * While this feature is generally useful, it seems less so for this map because
   * the keys are Strings and that's a pretty lightweight object already...
   */
  protected int findKeyIndex (Object key)
  {
    if (key instanceof String) {
      return _entries.binarySearch(_key.as((String)key));
    }
    if (key != null) {
      for (int ii = 0, nn = _entries.size(); ii < nn; ii++) {
        if (key.equals(_entries.get(ii).getKey())) {
          return ii;
        }
      }
    }
    return -1;
  }

  /**
   * Helper: create an Entry for the specified key/value,
   */
  protected static Map.Entry<String, Object> newEntry (String k, Object v)
  {
    return new SimpleEntry<String, Object>(k, v);
  }

  protected static class Key
    implements Comparable<Map.Entry<String, Object>>
  {
    /**
     * Update the value of this key, and return <tt>this</tt>.
     */
    public Key as (String key)
    {
      _key = key;
      return this;
    }

    public int compareTo (Map.Entry<String, Object> entry)
    {
      return _key.compareTo(entry.getKey());
    }

    protected String _key;
  }

  /** The entries in the map. */
  protected transient SortableArrayList<Map.Entry<String, Object>> _entries =
    new SortableArrayList<Map.Entry<String, Object>>();

  /** Dummy key used for searching. */
  protected transient Key _key = new Key();

  /** Used for {@link Arrays#deepHashCode} and {@link Arrays#deepEquals}. */
  protected transient Object[] _a1 = new Object[1], _a2 = new Object[1];
}
