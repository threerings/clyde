//
// $Id$

package com.threerings.tudey.util;

import java.io.IOException;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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
        _coord.set(x >> _granularity, y >> _granularity);
        Cell cell = _cells.get(_coord);
        if (cell == null) {
            _cells.put((Coord)_coord.clone(), cell = new Cell());
        }
        int ovalue = cell.put(x & _mask, y & _mask, value);
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
     * Clears out the map contents.
     */
    public void clear ()
    {
        _cells.clear();
        _size = 0;
        _modcount++;
    }

    /**
     * Returns the number of entries in the map.
     */
    public int size ()
    {
        return _size;
    }

    /**
     * Determines whether the map is empty.
     */
    public boolean isEmpty ()
    {
        return _size == 0;
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

    @Override // documentation inherited
    public Set<Entry<Coord, Integer>> entrySet ()
    {
        return new AbstractSet<Entry<Coord, Integer>>() {
            public Iterator<Entry<Coord, Integer>> iterator () {
                return new Iterator<Entry<Coord, Integer>>() {
                    public boolean hasNext () {
                        checkConcurrentModification();
                        return _count < _size;
                    }
                    public Entry<Coord, Integer> next () {
                        checkConcurrentModification();

                        return null;
                    }
                    public void remove () {
                        checkConcurrentModification();

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

                    protected int _count;
                    protected int _omodcount = _modcount;
                };
            }
            public int size () {
                return _size;
            }
        };
    }

    @Override // documentation inherited
    public Integer put (Coord key, Integer value)
    {
        int ovalue = put(key.x, key.y, value);
        return (ovalue == _empty) ? null : ovalue;
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
    protected int _modcount;

    /** A coord to reuse for queries. */
    protected transient Coord _coord = new Coord();
}
