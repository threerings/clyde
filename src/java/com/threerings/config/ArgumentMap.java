//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.ObjectUtil;

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
        _entries.add(new Entry(firstKey, firstValue));
        for (int ii = 0; ii < otherArgs.length; ii += 2) {
            _entries.add(new Entry((String)otherArgs[ii], otherArgs[ii + 1]));
        }
        _entries.sort();
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
            Entry entry = _entries.get(ii);
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
            _entries.add(new Entry(in.readIntern(), in.readObject()));
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
            Entry entry = _entries.get(ii);
            cmap._entries.add(new Entry(entry.getKey(), DeepUtil.copy(entry.getValue())));
        }
        return cmap;
    }

    @Override // documentation inherited
    public int size ()
    {
        return _entries.size();
    }

    @Override // documentation inherited
    public boolean containsValue (Object value)
    {
        for (int ii = 0, nn = _entries.size(); ii < nn; ii++) {
            if (ObjectUtil.equals(_entries.get(ii).getValue(), value)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public boolean containsKey (Object key)
    {
        if (!(key instanceof String)) {
            return false;
        }
        _dummy.setKey((String)key);
        return _entries.binarySearch(_dummy) >= 0;
    }

    @Override // documentation inherited
    public Object get (Object key)
    {
        if (!(key instanceof String)) {
            return null;
        }
        _dummy.setKey((String)key);
        int idx = _entries.binarySearch(_dummy);
        return (idx >= 0) ? _entries.get(idx).getValue() : null;
    }

    @Override // documentation inherited
    public Object put (String key, Object value)
    {
        _dummy.setKey(key);
        int idx = _entries.binarySearch(_dummy);
        if (idx >= 0) {
            return _entries.get(idx).setValue(value);
        } else {
            _entries.add(-idx - 1, new Entry(key, value));
            return null;
        }
    }

    @Override // documentation inherited
    public Object remove (Object key)
    {
        if (!(key instanceof String)) {
            return null;
        }
        _dummy.setKey((String)key);
        int idx = _entries.binarySearch(_dummy);
        return (idx >= 0) ? _entries.remove(idx).getValue() : null;
    }

    @Override // documentation inherited
    public void clear ()
    {
        _entries.clear();
    }

    @Override // documentation inherited
    public Set<Map.Entry<String, Object>> entrySet ()
    {
        return new AbstractSet<Map.Entry<String, Object>>() {
            @Override public int size () {
                return _entries.size();
            }
            @Override public boolean contains (Object o) {
                if (!(o instanceof Entry)) {
                    return false;
                }
                return _entries.binarySearch((Entry)o) >= 0;
            }
            @Override public Iterator<Map.Entry<String, Object>> iterator () {
                Iterator<?> it = _entries.iterator();
                @SuppressWarnings("unchecked") Iterator<Map.Entry<String, Object>> cit =
                    (Iterator<Map.Entry<String, Object>>)it;
                return cit;
            }
            @Override public boolean remove (Object o) {
                if (!(o instanceof Entry)) {
                    return false;
                }
                int idx = _entries.binarySearch((Entry)o);
                if (idx >= 0) {
                    _entries.remove(idx);
                    return true;
                } else {
                    return false;
                }
            }
            @Override public void clear () {
                _entries.clear();
            }
        };
    }

    @Override // documentation inherited
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
            Entry entry = _entries.get(ii), oentry = omap._entries.get(ii);
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

    @Override // documentation inherited
    public int hashCode ()
    {
        int hash = 0;
        for (int ii = 0, nn = _entries.size(); ii < nn; ii++) {
            Entry entry = _entries.get(ii);
            _a1[0] = entry.getValue();
            hash += entry.getKey().hashCode() ^ Arrays.deepHashCode(_a1);
        }
        return hash;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        return copy(null);
    }

    /**
     * The map entry class.
     */
    protected static class Entry
        implements Map.Entry<String, Object>, Comparable<Entry>
    {
        /**
         * Creates a new entry.
         */
        public Entry (String key, Object value)
        {
            _key = key;
            _value = value;
        }

        /**
         * Sets the entry key.
         */
        public void setKey (String key)
        {
            _key = key;
        }

        // documentation inherited from interface Map.Entry
        public String getKey ()
        {
            return _key;
        }

        // documentation inherited from interface Map.Entry
        public Object setValue (Object value)
        {
            Object ovalue = _value;
            _value = value;
            return ovalue;
        }

        // documentation inherited from interface Map.Entry
        public Object getValue ()
        {
            return _value;
        }

        // documentation inherited from interface Comparable
        public int compareTo (Entry oentry)
        {
            return _key.compareTo(oentry._key);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return ((_key == null) ? 0 : _key.hashCode()) ^
                (_value == null ? 0 : _value.hashCode());
        }

        @Override // documentation inherited
        public boolean equals (Object object)
        {
            Entry oentry;
            return object instanceof Entry &&
                ObjectUtil.equals(_key, (oentry = (Entry)object)._key) &&
                ObjectUtil.equals(_value, oentry._value);
        }

        @Override // documentation inherited
        public String toString ()
        {
            return _key + "=" + _value;
        }

        /** The entry key. */
        protected String _key;

        /** The entry value. */
        protected Object _value;
    }

    /** The entries in the map. */
    protected transient ComparableArrayList<Entry> _entries = new ComparableArrayList<Entry>();

    /** Dummy entry used for searching. */
    protected transient Entry _dummy = new Entry(null, null);

    /** Used for {@link Arrays#deepHashCode} and {@link Arrays#deepEquals}. */
    protected transient Object[] _a1 = new Object[1], _a2 = new Object[1];
}
