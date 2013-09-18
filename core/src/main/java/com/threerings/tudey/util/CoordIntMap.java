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

package com.threerings.tudey.util;

import java.io.IOException;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.samskivert.util.IntListUtil;

import com.threerings.export.Exportable;
import com.threerings.export.Importer;
import com.threerings.util.DeepObject;

/**
 * Maps pairs of integer coordinates to integer values.
 */
public class CoordIntMap extends AbstractMap<Coord, Integer>
    implements Exportable
{
    /**
     * An entry in the map.
     */
    public static class CoordIntEntry
        implements Entry<Coord, Integer>
    {
        /**
         * Sets the value of the entry as an integer.
         *
         * @return the original value of the entry.
         */
        public int setIntValue (int value)
        {
            int ovalue = _values[_idx];
            _values[_idx] = value;
            return ovalue;
        }

        /**
         * Returns the value of the entry as an integer.
         */
        public int getIntValue ()
        {
            return _values[_idx];
        }

        // documentation inherited from interface Entry
        public Coord getKey ()
        {
            return _key;
        }

        // documentation inherited from interface Entry
        public Integer getValue ()
        {
            return getIntValue();
        }

        // documentation inherited from interface Entry
        public Integer setValue (Integer value)
        {
            return setIntValue(value);
        }

        @Override
        public String toString ()
        {
            return getKey().toString() + ": " + getValue().toString();
        }

        @Override
        public boolean equals (Object other)
        {
            if (!(other instanceof CoordIntEntry)) {
                return false;
            }
            CoordIntEntry oentry = (CoordIntEntry)other;
            return _key.equals(oentry._key) && getIntValue() == oentry.getIntValue();
        }

        @Override
        public int hashCode ()
        {
            return _key.hashCode() ^ getIntValue();
        }

        /** The coordinate key. */
        protected Coord _key = new Coord();

        /** The array in which the value is located. */
        protected int[] _values;

        /** The index of the value within the array. */
        protected int _idx;
    }

    /**
     * Creates a new coord int map with a top-level cell size of 8x8 and with the value -1
     * representing the absence of an entry.
     */
    public CoordIntMap ()
    {
        this(3);
    }

    /**
     * Creates a new coord int map with the value -1 representing the absence of an entry.
     *
     * @param granularity the size of the top-level cells, expressed as a power of two (e.g.,
     * 3 for cell size 8).
     */
    public CoordIntMap (int granularity)
    {
        this(granularity, -1);
    }

    /**
     * Creates a new coord int map.
     *
     * @param granularity the size of the top-level cells, expressed as a power of two (e.g.,
     * 3 for a cell size of 8 by 8).
     * @param empty the value indicating the absence of an entry.
     */
    public CoordIntMap (int granularity, int empty)
    {
        _granularity = granularity;
        _empty = empty;
        initTransientFields();
    }

    /**
     * Retrieves the value at the specified coordinates.
     */
    public int get (int x, int y)
    {
        Cell cell = getCell(x, y);
        return (cell == null) ? _empty : cell.get(x & _mask, y & _mask);
    }

    /**
     * Sets the value at the specified coordinates.
     *
     * @return the previously stored value.
     */
    public int put (int x, int y, int value)
    {
        if (value == _empty) {
            // putting the empty value is equivalent to removing
            return remove(x, y);
        }
        _coord.set(x >> _granularity, y >> _granularity);
        Cell cell = _cells.get(_coord);
        if (cell == null) {
            _cells.put(_coord.clone(), cell = new Cell());
        }
        int ovalue = cell.put(x & _mask, y & _mask, value);
        if (ovalue == _empty) {
            _size++;
        }
        return ovalue;
    }

    /**
     * Stores the bitwise OR of the previous value and the specified bits at the specified
     * coordinates.
     *
     * @return the previously stored value.
     */
    public int setBits (int x, int y, int bits)
    {
        if (bits == 0) {
            return get(x, y);
        }
        _coord.set(x >> _granularity, y >> _granularity);
        Cell cell = _cells.get(_coord);
        if (cell == null) {
            _cells.put(_coord.clone(), cell = new Cell());
        }
        int ovalue = cell.setBits(x & _mask, y & _mask, bits);
        if (ovalue == _empty) {
            _size++;
        }
        return ovalue;
    }

    /**
     * Removes the value at the specified coordinates.
     *
     * @return the previously stored value.
     */
    public int remove (int x, int y)
    {
        _coord.set(x >> _granularity, y >> _granularity);
        Cell cell = _cells.get(_coord);
        if (cell == null) {
            return _empty;
        }
        int ovalue = cell.remove(x & _mask, y & _mask);
        if (ovalue != _empty) {
            _size--;
            if (cell.size() == 0) {
                _cells.remove(_coord);
            }
        }
        return ovalue;
    }

    /**
     * Determines whether this map contains an entry for the specified coordinates.
     */
    public boolean containsKey (int x, int y)
    {
        Cell cell = getCell(x, y);
        return cell != null && cell.get(x & _mask, y & _mask) != _empty;
    }

    /**
     * Determines whether this map contains the specified value.
     */
    public boolean containsValue (int value)
    {
        for (Cell cell : _cells.values()) {
            if (cell.containsValue(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Custom field read method.
     */
    public void readFields (Importer in)
        throws IOException
    {
        in.defaultReadFields();
        initTransientFields();

        // compute the size
        for (Cell cell : _cells.values()) {
            _size += cell.size();
        }
    }

    /**
     * Returns a set view of the map entries.
     */
    public Set<CoordIntEntry> coordIntEntrySet ()
    {
        return new AbstractSet<CoordIntEntry>() {
            public Iterator<CoordIntEntry> iterator () {
                return new Iterator<CoordIntEntry>() {
                    public boolean hasNext () {
                        checkConcurrentModification();
                        return _count < _size;
                    }
                    public CoordIntEntry next () {
                        checkConcurrentModification();
                        if (_centry == null) {
                            _centry = _cit.next();
                        }
                        while (true) {
                            int[] values = _centry.getValue().getValues();
                            for (; _idx < values.length; _idx++) {
                                int value = values[_idx];
                                if (value != _empty) {
                                    Coord coord = _centry.getKey();
                                    _dummy.getKey().set(
                                        (coord.x << _granularity) | (_idx & _mask),
                                        (coord.y << _granularity) | (_idx >> _granularity));
                                    _dummy._values = values;
                                    _dummy._idx = _idx;
                                    _idx++;
                                    _count++;
                                    return _dummy;
                                }
                            }
                            _centry = _cit.next();
                            _idx = 0;
                        }
                    }
                    public void remove () {
                        checkConcurrentModification();
                        Cell cell = _centry.getValue();
                        cell.remove(_idx);
                        if (cell.size() == 0) {
                            _cit.remove();
                            _centry = null;
                            _idx = 0;
                        }
                        _size--;
                        _count--;
                        _omodcount = _modcount;
                    }
                    protected void checkConcurrentModification () {
                        if (_modcount != _omodcount) {
                            throw new ConcurrentModificationException();
                        }
                    }
                    protected Iterator<Entry<Coord, Cell>> _cit = _cells.entrySet().iterator();
                    protected Entry<Coord, Cell> _centry;
                    protected int _idx;
                    protected int _count;
                    protected int _omodcount = _modcount;
                    protected CoordIntEntry _dummy = new CoordIntEntry();
                };
            }
            public int size () {
                return _size;
            }
        };
    }

    @Override
    public Set<Entry<Coord, Integer>> entrySet ()
    {
        Set<?> cset = coordIntEntrySet();
        @SuppressWarnings("unchecked") Set<Entry<Coord, Integer>> set =
            (Set<Entry<Coord, Integer>>)cset;
        return set;
    }

    @Override
    public boolean containsKey (Object key)
    {
        if (!(key instanceof Coord)) {
            return false;
        }
        Coord coord = (Coord)key;
        return containsKey(coord.x, coord.y);
    }

    @Override
    public boolean containsValue (Object value)
    {
        return value instanceof Integer && containsValue(((Integer)value).intValue());
    }

    @Override
    public Integer get (Object key)
    {
        if (!(key instanceof Coord)) {
            return null;
        }
        Coord coord = (Coord)key;
        int value = get(coord.x, coord.y);
        return (value == _empty) ? null : value;
    }

    @Override
    public Integer put (Coord key, Integer value)
    {
        int ovalue = put(key.x, key.y, value);
        return (ovalue == _empty) ? null : ovalue;
    }

    @Override
    public Integer remove (Object key)
    {
        if (!(key instanceof Coord)) {
            return null;
        }
        Coord coord = (Coord)key;
        int ovalue = remove(coord.x, coord.y);
        return (ovalue == _empty) ? null : ovalue;
    }

    @Override
    public void clear ()
    {
        _cells.clear();
        _size = 0;
        _modcount++;
    }

    @Override
    public int size ()
    {
        return _size;
    }

    @Override
    public boolean isEmpty ()
    {
        return _size == 0;
    }

    /**
     * Initializes the transient fields.
     */
    protected void initTransientFields ()
    {
        _mask = (1 << _granularity) - 1;
    }

    /**
     * Returns the cell corresponding to the specified coordinates.
     */
    protected Cell getCell (int x, int y)
    {
        _coord.set(x >> _granularity, y >> _granularity);
        return _cells.get(_coord);
    }

    /**
     * Represents a single top-level cell.
     */
    protected class Cell extends DeepObject
        implements Exportable
    {
        /**
         * Creates a new cell.
         */
        public Cell ()
        {
            _values = new int[1 << _granularity << _granularity];
            Arrays.fill(_values, _empty);
        }

        /**
         * Returns a reference to the cell's array of values.
         */
        public int[] getValues ()
        {
            return _values;
        }

        /**
         * Determines whether this cell contains the specified value.
         */
        public boolean containsValue (int value)
        {
            return IntListUtil.contains(_values, value);
        }

        /**
         * Retrieves the value at the specified coordinates within the cell.
         */
        public int get (int x, int y)
        {
            return _values[(y << _granularity) | x];
        }

        /**
         * Sets the value at the specified coordinates within the cell.
         *
         * @return the previously stored value.
         */
        public int put (int x, int y, int nvalue)
        {
            int idx = (y << _granularity) | x;
            int ovalue = _values[idx];
            _values[idx] = nvalue;
            if (ovalue == _empty) {
                _size++;
            }
            _modcount++;
            return ovalue;
        }

        /**
         * Sets the value at the specified coordinates to the bitwise OR of the previous
         * value and the new value.
         *
         * @return the previously stored value.
         */
        public int setBits (int x, int y, int bits)
        {
            int idx = (y << _granularity) | x;
            int ovalue = _values[idx];
            _values[idx] |= bits;
            if (ovalue == _empty) {
                _size++;
            }
            _modcount++;
            return ovalue;
        }

        /**
         * Removes the value at the specified coordinates within the cell.
         *
         * @return the previously stored value.
         */
        public int remove (int x, int y)
        {
            int idx = (y << _granularity) | x;
            int ovalue = _values[idx];
            if (ovalue != _empty) {
                _values[idx] = _empty;
                _size--;
                _modcount++;
            }
            return ovalue;
        }

        /**
         * Removes the value at the specified index.
         */
        public void remove (int idx)
        {
            _values[idx] = _empty;
            _size--;
            _modcount++;
        }

        /**
         * Returns the number of entries in the cell.
         */
        public int size ()
        {
            return _size;
        }

        /**
         * Custom field read method.
         */
        public void readFields (Importer in)
            throws IOException
        {
            in.defaultReadFields();

            // compute the size
            for (int value : _values) {
                if (value != _empty) {
                    _size++;
                }
            }
        }

        /** The values in the cell. */
        protected int[] _values;

        /** The number of entries in the cell. */
        protected transient int _size;
    }

    /** The size of the top-level cells as a power of two. */
    protected int _granularity;

    /** The value indicating an empty mapping. */
    protected int _empty;

    /** The top-level cells. */
    protected HashMap<Coord, Cell> _cells = new HashMap<Coord, Cell>();

    /** The mask value derived from the granularity. */
    protected transient int _mask;

    /** The number of entries in the map. */
    protected transient int _size;

    /** The modification count (used to detect concurrent modifications). */
    protected transient int _modcount;

    /** A coord to reuse for queries. */
    protected transient Coord _coord = new Coord();
}
