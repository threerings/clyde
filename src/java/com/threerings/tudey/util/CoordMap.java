//
// $Id$

package com.threerings.tudey.util;

import java.awt.Point;

import java.io.IOException;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;

/**
 * Maps pairs of coordinates (in the range [-32767, +32767]) to objects using a 2D hash table that
 * employs chaining to resolve collisions.
 *
 * <p> As with {@link CoordIntMap}, one could also use a {@link com.samskivert.util.HashIntMap}
 * with keys formed by interleaving the bits of the x and y coordinates.
 */
public class CoordMap<T>
    implements Streamable, Exportable
{
    /**
     * Provides access to an entry in the map.
     */
    public interface CoordEntry<V> extends Map.Entry<Point, V>
    {
        /**
         * Returns the x component of the key.
         */
        public int getKeyX ();

        /**
         * Returns the y component of the key.
         */
        public int getKeyY ();
    }

    /**
     * Creates a new map with the default capacity and load factor.
     */
    public CoordMap ()
    {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new map with the default load factor and the specified capacity.
     *
     * @param capacity the number of entries in each dimension, expressed as a power of two.
     */
    public CoordMap (int capacity)
    {
        this(capacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new map with the specified capacity and load factor.
     *
     * @param capacity the number of entries in each dimension, expressed as a power of two.
     */
    public CoordMap (int capacity, float loadFactor)
    {
        setCapacity(capacity);
        _loadFactor = loadFactor;
    }

    /**
     * Determines whether the map contains the specified key.
     */
    public boolean containsKey (int x, int y)
    {
        // search the chain at the hash index
        int key = CoordUtil.getCoord(x, y);
        for (Entry<T> entry = _entries[getHashIndex(x, y)]; entry != null; entry = entry.next) {
            if (entry.key == key) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value at the specified coordinates, or <code>null</code> if none.
     */
    public T get (int x, int y)
    {
        // search the chain at the hash index
        int key = CoordUtil.getCoord(x, y);
        for (Entry<T> entry = _entries[getHashIndex(x, y)]; entry != null; entry = entry.next) {
            if (entry.key == key) {
                return entry.value;
            }
        }
        return null;
    }

    /**
     * Maps the given value.
     *
     * @return the previous mapping, or null for none.
     */
    public T put (int x, int y, T value)
    {
        // make sure we have space for the new value
        ensureCapacity(_size + 1);

        // see if there's an existing chain
        int key = CoordUtil.getCoord(x, y);
        int idx = getHashIndex(x, y);
        Entry<T> entry = _entries[idx];
        if (entry == null) {
            _entries[idx] = new Entry<T>(key, value);
            _size++;
            _modcount++;
            return null;
        }

        // the chain has started, so update/append
        for (;; entry = entry.next) {
            if (entry.key == key) {
                T ovalue = entry.value;
                entry.value = value;
                _modcount++;
                return ovalue;
            } else if (entry.next == null) {
                entry.next = new Entry<T>(key, value);
                _size++;
                _modcount++;
                return null;
            }
        }
    }

    /**
     * Removes an element from the map.
     *
     * @return the previous mapping, or null for none.
     */
    public T remove (int x, int y)
    {
        // see if there's a chain
        int key = CoordUtil.getCoord(x, y);
        int idx = getHashIndex(x, y);
        Entry<T> entry = _entries[idx];
        if (entry == null) {
            return null;
        } else if (entry.key == key) {
            _entries[idx] = entry.next;
            _size--;
            _modcount++;
            return entry.value;
        }

        // follow the chain
        for (; entry.next != null; entry = entry.next) {
            if (entry.next.key == key) {
                T ovalue = entry.next.value;
                entry.next = entry.next.next;
                _size--;
                _modcount++;
                return ovalue;
            }
        }
        return null;
    }

    /**
     * Determines whether the map is empty.
     */
    public boolean isEmpty ()
    {
        return _size == 0;
    }

    /**
     * Returns the number of values in the map.
     */
    public int size ()
    {
        return _size;
    }

    /**
     * Removes all elements from the map.
     */
    public void clear ()
    {
        Arrays.fill(_entries, null);
        _size = 0;
        _modcount++;
    }

    /**
     * Returns a view of the values in the map.
     */
    public Collection<T> values ()
    {
        return new AbstractCollection<T>() {
            public int size () {
                return _size;
            }
            public Iterator<T> iterator () {
                return new Iterator<T>() {
                    public boolean hasNext () {
                        return _it.hasNext();
                    }
                    public T next () {
                        return _it.next().getValue();
                    }
                    public void remove () {
                        _it.remove();
                    }
                    protected Iterator<CoordEntry<T>> _it = entrySet().iterator();
                };
            }
        };
    }

    /**
     * Returns a view of the entries in the map.
     */
    public Set<CoordEntry<T>> entrySet ()
    {
        return new AbstractSet<CoordEntry<T>>() {
            public int size () {
                return _size;
            }
            public Iterator<CoordEntry<T>> iterator () {
                return new Iterator<CoordEntry<T>>() {
                    public boolean hasNext () {
                        checkConcurrentModification();
                        return _count < _size;
                    }
                    public CoordEntry<T> next () {
                        checkConcurrentModification();
                        // if there's another entry in the current chain, return that
                        if (_entry != null && _entry.next != null) {
                            _pentry = _entry;
                            _entry = _entry.next;

                        // otherwise, find the next chain
                        } else {
                            _pentry = null;
                            for (_idx++; (_entry = _entries[_idx]) == null; _idx++);
                        }
                        _count++;
                        return _entry;
                    }
                    public void remove () {
                        checkConcurrentModification();
                        if (_pentry == null) {
                            _entries[_idx] = _entry.next;
                        } else {
                            _pentry.next = _entry.next;
                        }
                        _size--;
                        _count--;
                        _omodcount = ++_modcount;
                    }
                    protected void checkConcurrentModification () {
                        if (_modcount != _omodcount) {
                            throw new ConcurrentModificationException();
                        }
                    }
                    protected int _idx = -1;
                    protected Entry<T> _entry, _pentry;
                    protected int _count;
                    protected int _omodcount = _modcount;
                };
            }
        };
    }

    /**
     * Custom serialization method.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.writeInt(_size);
        for (Entry entry : _entries) {
            for (; entry != null; entry = entry.next) {
                out.writeInt(entry.key);
                out.writeObject(entry.value);
            }
        }
    }

    /**
     * Custom serialization method.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        ensureCapacity(size);
        for (int ii = 0; ii < size; ii++) {
            int key = in.readInt();
            @SuppressWarnings("unchecked") T value = (T)in.readObject();
            put(CoordUtil.getX(key), CoordUtil.getY(key), value);
        }
    }

    /**
     * Writes out the fields of this object.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        int[] keys = new int[_size];
        Object[] values = new Object[_size];
        int idx = 0;
        for (Entry<T> entry : _entries) {
            for (; entry != null; entry = entry.next) {
                keys[idx] = entry.key;
                values[idx++] = entry.value;
            }
        }
        out.write("keys", keys, (int[])null);
        out.write("values", values, null, Object[].class);
    }

    /**
     * Reads in the fields of this object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        int[] keys = in.read("keys", (int[])null);
        Object[] values = in.read("values", null, Object[].class);
        ensureCapacity(keys.length);
        for (int ii = 0; ii < keys.length; ii++) {
            int key = keys[ii];
            @SuppressWarnings("unchecked") T value = (T)values[ii];
            put(CoordUtil.getX(key), CoordUtil.getY(key), value);
        }
    }

    /**
     * Prints a simple set of statistics for the map: size, capacity, average chain
     * length, etc.
     */
    public void printStatistics ()
    {
        // find the maximum chain length and number of chains
        int max = 0, count = 0;
        for (Entry<T> entry : _entries) {
            int length = 0;
            for (; entry != null; entry = entry.next) {
                length++;
            }
            max = Math.max(max, length);
            if (length > 0) {
                count++;
            }
        }
        System.out.println(
            "Size: " + _size +
            ", Capacity: " + _entries.length +
            ", Load: " + (float)_size / _entries.length +
            ", Max. Chain Length: " + max +
            ", Avg. Chain Length: " + (float)_size / count);
    }

    /**
     * Makes sure we can comfortably hold the specified number of elements.
     */
    protected void ensureCapacity (int size)
    {
        int capacity = _capacity;
        while (size > (int)((1 << capacity << capacity) * _loadFactor)) {
            capacity++;
        }
        if (_capacity != capacity) {
            rehash(capacity);
        }
    }

    /**
     * Resizes the entry array.
     */
    protected void rehash (int capacity)
    {
        // save the current entries and reset
        Entry<T>[] oentries = _entries;
        setCapacity(capacity);

        // remap the elements
        for (Entry<T> entry : oentries) {
            for (; entry != null; entry = entry.next) {
                put(CoordUtil.getX(entry.key), CoordUtil.getY(entry.key), entry.value);
            }
        }
    }

    /**
     * Sets the capacity and derived values and initializes the array of entries.
     */
    protected void setCapacity (int capacity)
    {
        _capacity = capacity;
        _cmask = (1 << _capacity) - 1;
        @SuppressWarnings("unchecked") Entry<T>[] nentries =
            (Entry<T>[])new Entry[1 << _capacity << _capacity];
        _entries = nentries;
        _size = 0;
    }

    /**
     * Returns the hashed index corresponding to the specified encoded coordinates.
     */
    protected final int getHashIndex (int coord)
    {
        return getHashIndex(CoordUtil.getX(coord), CoordUtil.getY(coord));
    }

    /**
     * Returns the hashed index corresponding to the specified coordinates.
     */
    protected final int getHashIndex (int x, int y)
    {
        return ((x & _cmask) << _capacity) | (y & _cmask);
    }

    /**
     * An entry in the map.
     */
    protected static final class Entry<T>
        implements CoordEntry<T>
    {
        /** The packed coordinate key. */
        public int key;

        /** The mapped value. */
        public T value;

        /** The next entry in the chain. */
        public Entry<T> next;

        public Entry (int key, T value)
        {
            this.key = key;
            this.value = value;
        }

        public Entry ()
        {
        }

        // documentation inherited from interface Map.Entry
        public Point getKey ()
        {
            return new Point(getKeyX(), getKeyY());
        }

        // documentation inherited from interface CoordEntry
        public int getKeyX ()
        {
            return CoordUtil.getX(key);
        }

        // documentation inherited from interface CoordEntry
        public int getKeyY ()
        {
            return CoordUtil.getY(key);
        }

        // documentation inherited from interface Map.Entry
        public T getValue ()
        {
            return value;
        }

        // documentation inherited from interface Map.Entry
        public T setValue (T value)
        {
            T ovalue = this.value;
            this.value = value;
            return ovalue;
        }
    }

    /** The current capacity (size in each dimension as a power of two). */
    protected int _capacity;

    /** Bitmask derived from the capacity. */
    protected int _cmask;

    /** The array containing the map entries. */
    protected Entry<T>[] _entries;

    /** The number of values in the map. */
    protected int _size;

    /** The load factor. */
    protected float _loadFactor;

    /** The modification count (used to detect concurrent modifications). */
    protected int _modcount;

    /** The default capacity. */
    protected static final int DEFAULT_CAPACITY = 2;

    /** The default load factor. */
    protected static final float DEFAULT_LOAD_FACTOR = 0.75f;
}
