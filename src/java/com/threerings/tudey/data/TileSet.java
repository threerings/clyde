//
// $Id$

package com.threerings.tudey.data;

import java.util.Iterator;

import com.samskivert.util.StringUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.export.Exportable;

import com.threerings.tudey.util.CoordIntMap;
import com.threerings.tudey.util.CoordIntMap.CoordIntEntry;
import com.threerings.tudey.util.CoordMap;

import static com.threerings.tudey.Log.*;

/**
 * Contains a set of tiles.
 */
public class TileSet extends SimpleStreamableObject
    implements Exportable, Iterable<Tile>
{
    /**
     * Creates an empty tile set.
     */
    public TileSet ()
    {
    }

    /**
     * Creates a set containing a single tile.
     */
    public TileSet (Tile tile)
    {
        add(tile);
    }

    /**
     * Copy constructor.
     */
    public TileSet (TileSet set)
    {
        addAll(set);
    }

    /**
     * Fetches the tile with the specified coordinates.  Note that this method returns
     * "uninteresting" tiles by (re)populating a dummy object maintained by the set.  For cases
     * where the tile data must be preserved across calls, the object should be copied.
     */
    public Tile get (int x, int y)
    {
        Tile itile = _itiles.get(x, y);
        if (itile != null) {
            return itile;
        }
        int data = _utiles.get(x, y);
        return (data == -1) ? null : (_dummy = Tile.decode(x, y, data, _dummy));
    }

    /**
     * Copies the tile with the specified coordinates into the provided set.
     */
    public void get (int x, int y, TileSet result)
    {
        Tile itile = _itiles.get(x, y);
        if (itile != null) {
            result._itiles.put(x, y, itile);
            return;
        }
        int data = _utiles.get(x, y);
        if (data != -1) {
            result._utiles.put(x, y, data);
        }
    }

    /**
     * Returns the number of pieces in the set.
     */
    public int size ()
    {
        return _itiles.size() + _utiles.size();
    }

    /**
     * Checks whether this set is empty.
     */
    public boolean isEmpty ()
    {
        return _itiles.isEmpty() && _utiles.isEmpty();
    }

    /**
     * Adds all of the specified pieces to the set.
     */
    public void addAll (Iterable<Tile> tiles)
    {
        for (Tile tile : tiles) {
            add(tile);
        }
    }

    /**
     * Adds a tile to the set.
     */
    public void add (Tile tile)
    {
        if (tile.isInteresting()) {
            _itiles.put(tile.x, tile.y, tile);
        } else {
            _utiles.put(tile.x, tile.y, tile.getData());
        }
    }

    /**
     * Removes all of the specified tiles from the set.
     */
    public void removeAll (Iterable<Tile> tiles)
    {
        for (Tile tile : tiles) {
            remove(tile);
        }
    }

    /**
     * Removes a tile from the set.
     *
     * @return true if the tile was in the set.
     */
    public boolean remove (Tile tile)
    {
        if (tile.isInteresting()) {
            if (tile.equals(_itiles.get(tile.x, tile.y))) {
                _itiles.remove(tile.x, tile.y);
                return true;
            }
        } else if (tile.getData() == _utiles.get(tile.x, tile.y)) {
            _utiles.remove(tile.x, tile.y);
            return true;
        }
        return false;
    }

    /**
     * Checks whether this set contains the specified tile.
     */
    public boolean contains (Tile tile)
    {
        return tile.isInteresting() ? tile.equals(_itiles.get(tile.x, tile.y)) :
            (tile.getData() == _utiles.get(tile.x, tile.y));
    }

    /**
     * Checks whether this set contains a tile with the specified coordinates.
     */
    public boolean contains (int x, int y)
    {
        return _itiles.containsKey(x, y) || _utiles.containsKey(x, y);
    }

    /**
     * Clears out the set.
     */
    public void clear ()
    {
        _itiles.clear();
        _utiles.clear();
    }

    /**
     * Returns an iterator over the tiles in the set.  Note that the iterator returns
     * "uninteresting" tiles by (re)populating a dummy object maintained by the iterator.
     * For cases where the tile data must be preserved across iterations, the tile
     * object should be copied.
     */
    public Iterator<Tile> iterator ()
    {
        return new Iterator<Tile>() {
            public boolean hasNext () {
                return _iit.hasNext() || _uit.hasNext();
            }
            public Tile next () {
                if (_iit.hasNext()) {
                    return _iit.next();
                } else {
                    CoordIntEntry entry = _uit.next();
                    _dummy = Tile.decode(
                        entry.getKeyX(), entry.getKeyY(), entry.getIntValue(), _dummy);
                    return _dummy;
                }
            }
            public void remove () {
                if (_dummy == null) {
                    _iit.remove();
                } else {
                    _uit.remove();
                }
            }
            protected Iterator<Tile> _iit = _itiles.values().iterator();
            protected Iterator<CoordIntEntry> _uit = _utiles.entrySet().iterator();
            protected Tile _dummy;
        };
    }

    /**
     * Populates an array with the contents of this tile set.
     *
     * @param array the array to populate, or <code>null</code> to create a new array.
     * @return the populated array.
     */
    public Tile[] toArray (Tile[] array)
    {
        int length = size();
        if (array == null || array.length != length) {
            array = new Tile[length];
        }
        int idx = 0;
        for (Tile tile : _itiles.values()) {
            array[idx++] = tile;
        }
        for (CoordIntEntry entry : _utiles.entrySet()) {
            array[idx++] = Tile.decode(
                entry.getKeyX(), entry.getKeyY(), entry.getIntValue(), null);
        }
        return array;
    }

    /**
     * Removes any invalid tiles, logging warnings for each.
     */
    public void validate ()
    {
        for (Iterator<Tile> it = iterator(); it.hasNext(); ) {
            Tile tile = it.next();
            if (!tile.isValid()) {
                log.warning("Invalid tile [tile=" + tile + "].");
                it.remove();
            }
        }
    }

    @Override // documentation inherited
    public String toString ()
    {
        return StringUtil.toString(iterator());
    }

    /** The "interesting" tiles in the set, mapped by their coordinates. */
    protected CoordMap<Tile> _itiles = new CoordMap<Tile>();

    /** The data for the "uninteresting" tiles in the set, mapped by their coordinates. */
    protected CoordIntMap _utiles = new CoordIntMap();

    /** A dummy object for returning uninteresting tiles. */
    protected transient Tile _dummy;
}
