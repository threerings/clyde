//
// $Id$

package com.threerings.tudey.util;

import java.awt.Point;

import java.io.IOException;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
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
 * Maps pairs of coordinates (in the range [-32767, +32767]) to integer values using a 2D hash
 * table that employs linear probing to resolve collisions.  Internally, the key/value pairs are
 * packed into <code>long</code> values within a single array, which provides low overhead and
 * good locality of reference for caching.  Using linear probing can lead to
 * <a href="http://en.wikipedia.org/wiki/Linear_probing">clustering</a> issues, which is
 * something to keep in mind when using this class.  The worst case would be a sparse map with
 * features roughly spaced at power-of-two frequencies.
 *
 * <p> Another way to perform this mapping would be to use an {@link com.samskivert.util.IntIntMap}
 * (or equivalent) with keys formed by interleaving the bits of the x and y coordinates.  This
 * stores values in a <a href="http://en.wikipedia.org/wiki/Z-order_%28curve%29">Z-order curve</a>,
 * which provides even better spatial locality, although it requires more computation to generate
 * the interleaved keys
 * (<a href="http://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN">code here</a>).
 */
public class CoordIntMap
    implements Streamable, Exportable
{
    /**
     * Provides access to an entry in the map.
     */
    public interface CoordIntEntry extends Map.Entry<Point, Integer>
    {
        /**
         * Returns the x component of the key.
         */
        public int getKeyX ();

        /**
         * Returns the y component of the key.
         */
        public int getKeyY ();

        /**
         * Returns the integer value of the entry.
         */
        public int getIntValue ();
    }

    /**
     * Creates a new map with the default capacity and load factor.
     */
    public CoordIntMap ()
    {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new map with the default load factor and the specified capacity.
     *
     * @param capacity the number of entries in each dimension, expressed as a power of two.
     */
    public CoordIntMap (int capacity)
    {
        this(capacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new map with the specified capacity and load factor.
     *
     * @param capacity the number of entries in each dimension, expressed as a power of two.
     */
    public CoordIntMap (int capacity, float loadFactor)
    {
        setCapacity(capacity);
        _loadFactor = loadFactor;
    }

    /**
     * Sets the value to return from {@link #get}, {@link #put}, and {@link #remove} to indicate
     * that no mapping exists (or existed).  The default is -1, which works for most situations.
     */
    public void setNoneValue (int none)
    {
        _none = none;
    }

    /**
     * Returns the value returned when no mapping exists.
     */
    public int getNoneValue ()
    {
        return _none;
    }

    /**
     * Determines whether the map contains the specified key.
     */
    public boolean containsKey (int x, int y)
    {
        // hash the coordinates to find the starting index, then scan the entries for
        // a matching key until we reach an empty entry
        int key = CoordUtil.getCoord(x, y);
        for (int ii = getHashIndex(x, y);; ii = (ii + 1) & _imask) {
            long entry = _entries[ii];
            int ekey = (int)(entry >> 32);
            if (ekey == CoordUtil.UNUSED) {
                return false;
            } else if (ekey == key) {
                return true;
            }
        }
    }

    /**
     * Returns the value at the specified coordinates, or the configured none value if none.
     */
    public int get (int x, int y)
    {
        // hash the coordinates to find the starting index, then scan the entries for
        // a matching key until we reach an empty entry
        int key = CoordUtil.getCoord(x, y);
        for (int ii = getHashIndex(x, y);; ii = (ii + 1) & _imask) {
            long entry = _entries[ii];
            int ekey = (int)(entry >> 32);
            if (ekey == CoordUtil.UNUSED) {
                return _none;
            } else if (ekey == key) {
                return (int)entry;
            }
        }
    }

    /**
     * Maps the given value.
     *
     * @return the previous mapping, or the configured none value if none.
     */
    public int put (int x, int y, int value)
    {
        // make sure we have space for the new value
        ensureCapacity(_size + 1);

        // hash the coordinates to find the starting index, then scan the entries for an empty
        // or matching one
        int key = CoordUtil.getCoord(x, y);
        long nentry = ((long)key << 32) | ((long)value & 0xFFFFFFFFL);
        for (int ii = getHashIndex(x, y);; ii = (ii + 1) & _imask) {
            long entry = _entries[ii];
            int ekey = (int)(entry >> 32);
            if (ekey == CoordUtil.UNUSED) {
                _entries[ii] = nentry;
                _size++;
                _modcount++;
                return _none;
            } else if (ekey == key) {
                _entries[ii] = nentry;
                _modcount++;
                return (int)entry;
            }
        }
    }

    /**
     * Removes an element from the map.
     *
     * @return the previous mapping, or the configured none value if none.
     */
    public int remove (int x, int y)
    {
        // hash the coordinates to find the starting index, then scan the entries for
        // a matching key until we reach an empty entry
        int key = CoordUtil.getCoord(x, y);
        for (int ii = getHashIndex(x, y);; ii = (ii + 1) & _imask) {
            long entry = _entries[ii];
            int ekey = (int)(entry >> 32);
            if (ekey == CoordUtil.UNUSED) {
                return _none;
            } else if (ekey == key) {
                delete(ii);
                return (int)entry;
            }
        }
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
        Arrays.fill(_entries, EMPTY);
        _size = 0;
        _modcount++;
    }

    /**
     * Returns the set of entries in the map.
     */
    public Set<CoordIntEntry> entrySet ()
    {
        return new AbstractSet<CoordIntEntry>() {
            public int size () {
                return _size;
            }
            public Iterator<CoordIntEntry> iterator () {
                return new Iterator<CoordIntEntry>() {
                    public boolean hasNext () {
                        checkConcurrentModification();
                        return _count < _size;
                    }
                    public CoordIntEntry next () {
                        checkConcurrentModification();
                        for (_idx++; _entries[_idx] == EMPTY; _idx++);
                        _count++;
                        return _entry.set(_entries[_idx]);
                    }
                    public void remove () {
                        checkConcurrentModification();
                        delete(_idx--);
                        _count--;
                        _omodcount = _modcount;
                    }
                    protected void checkConcurrentModification () {
                        if (_modcount != _omodcount) {
                            throw new ConcurrentModificationException();
                        }
                    }
                    protected int _idx = -1;
                    protected int _count;
                    protected int _omodcount = _modcount;
                    protected DummyEntry _entry = new DummyEntry();
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
        for (long entry : _entries) {
            if (entry != EMPTY) {
                out.writeLong(entry);
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
            long entry = in.readLong();
            int key = (int)(entry >> 32);
            put(CoordUtil.getX(key), CoordUtil.getY(key), (int)entry);
        }
    }

    /**
     * Writes out the fields of this object.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        long[] entries = new long[_size];
        int idx = 0;
        for (long entry : _entries) {
            if (entry != EMPTY) {
                entries[idx++] = entry;
            }
        }
        out.write("entries", entries, (long[])null);
    }

    /**
     * Reads in the fields of this object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        long[] entries = in.read("entries", (long[])null);
        ensureCapacity(entries.length);
        for (long entry : entries) {
            int key = (int)(entry >> 32);
            put(CoordUtil.getX(key), CoordUtil.getY(key), (int)entry);
        }
    }

    /**
     * Prints a simple set of statistics for the map: size, capacity, average chain
     * length, etc.
     */
    public void printStatistics ()
    {
        // find the maximum offset and offset total
        int max = 0, total = 0;
        for (int ii = 0; ii < _entries.length; ii++) {
            long entry = _entries[ii];
            if (entry == EMPTY) {
                continue;
            }
            int key = (int)(entry >> 32);
            int offset = (ii - getHashIndex(key)) & _imask;
            max = Math.max(max, offset);
            total += offset;
        }
        System.out.println(
            "Size: " + _size +
            ", Capacity: " + _entries.length +
            ", Load: " + (float)_size / _entries.length +
            ", Max. Offset: " + max +
            ", Avg. Offset: " + (float)total / _size);
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
        long[] oentries = _entries;
        setCapacity(capacity);

        // remap the elements
        for (long entry : oentries) {
            int key = (int)(entry >> 32);
            if (key != CoordUtil.UNUSED) {
                put(CoordUtil.getX(key), CoordUtil.getY(key), (int)entry);
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
        _imask = (1 << _capacity << _capacity) - 1;
        _entries = new long[1 << _capacity << _capacity];
        Arrays.fill(_entries, EMPTY);
        _size = 0;
    }

    /**
     * Removes the element at the specified index, moving elements over as necessary.
     */
    protected void delete (int idx)
    {
        // first we remove the element at the index, then we rehash all elements up to the
        // next empty location
        _entries[idx] = EMPTY;
        for (int ii = (idx + 1) & _imask;; ii = (ii + 1) & _imask) {
            long entry = _entries[ii];
            if (entry == EMPTY) {
                break;
            }
            _entries[ii] = EMPTY;
            int key = (int)(entry >> 32);
            for (int jj = getHashIndex(key);; jj = (jj + 1) & _imask) {
                if (_entries[jj] == EMPTY) {
                    _entries[jj] = entry;
                    break;
                }
            }
        }
        _size--;
        _modcount++;
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
     * A reusable dummy entry to repopulate when iterating.
     */
    protected static class DummyEntry
        implements CoordIntEntry
    {
        /**
         * Sets the values of the entry.
         *
         * @return a reference to the entry, for chaining.
         */
        public DummyEntry set (long entry)
        {
            _key = (int)(entry >> 32);
            _value = (int)entry;
            return this;
        }

        // documentation inherited from interface Map.Entry
        public Point getKey ()
        {
            return new Point(getKeyX(), getKeyY());
        }

        // documentation inherited from interface CoordIntEntry
        public int getKeyX ()
        {
            return CoordUtil.getX(_key);
        }

        // documentation inherited from interface CoordIntEntry
        public int getKeyY ()
        {
            return CoordUtil.getY(_key);
        }

        // documentation inherited from interface Map.Entry
        public Integer getValue ()
        {
            return _value;
        }

        // documentation inherited from interface Map.Entry
        public Integer setValue (Integer value)
        {
            Integer ovalue = _value;
            _value = value;
            return ovalue;
        }

        // documentation inherited from interface CoordIntEntry
        public int getIntValue ()
        {
            return _value;
        }

        /** The coordinate key. */
        protected int _key;

        /** The mapped value. */
        protected int _value;
    }

    /** The current capacity (size in each dimension as a power of two). */
    protected int _capacity;

    /** Bitmasks derived from the capacity. */
    protected int _cmask, _imask;

    /** The entries containing packed keys and values. */
    protected long[] _entries;

    /** The number of values in the map. */
    protected int _size;

    /** The load factor. */
    protected float _loadFactor;

    /** The value to return to signify a nonexistent mapping. */
    protected int _none = -1;

    /** The modification count (used to detect concurrent modifications). */
    protected int _modcount;

    /** The default capacity. */
    protected static final int DEFAULT_CAPACITY = 2;

    /** The default load factor. */
    protected static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /** Used to represent empty locations. */
    protected static final long EMPTY = (long)CoordUtil.UNUSED << 32;
}
