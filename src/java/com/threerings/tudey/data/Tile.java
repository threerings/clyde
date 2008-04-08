//
// $Id$

package com.threerings.tudey.data;

import java.awt.Rectangle;

import com.threerings.util.DeepUtil;

import com.threerings.tudey.util.CoordUtil;

/**
 * Represents a tile placed in the scene.
 */
public class Tile extends SceneElement
{
    /** The tile's unique identifier. */
    public short tileId;

    /** The location of the tile. */
    public short x, y;

    /** The orientation of the tile. */
    public byte orient;

    /** The elevation of the tile. */
    public byte elevation;

    /**
     * Decodes the encoded tile coordinates and data provided.
     *
     * @param result a tile object to populate with the result, or <code>null</code> to
     * create a new object.
     * @return a reference to the object populated.
     */
    public static Tile decode (int x, int y, int data, Tile result)
    {
        if (result == null) {
            result = new Tile();
        }
        result.tileId = (short)(data >> 16);
        result.x = (short)x;
        result.y = (short)y;
        result.orient = (byte)(data >> 8 & 0x03);
        result.elevation = (byte)(data & 0xFF);
        return result;
    }

    /**
     * Creates a new tile.
     */
    public Tile (int tileId, int x, int y, int orient, int elevation)
    {
        this.tileId = (short)tileId;
        this.x = (short)x;
        this.y = (short)y;
        this.orient = (byte)orient;
        this.elevation = (byte)elevation;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Tile ()
    {
    }

    /**
     * Returns a reference to the configuration of this tile.
     */
    public TileConfig getConfig ()
    {
        return TileConfig.getConfig(tileId);
    }

    /**
     * Determines whether the tile is passable at the specified (absolute) coordinates.
     */
    public boolean isPassable (int x, int y)
    {
        return getConfig().isPassable(orient, x - this.x, y - this.y);
    }

    /**
     * Checks whether the configuration of this tile is valid.
     */
    public boolean isValid ()
    {
        return getConfig() != null;
    }

    /**
     * Determines whether or not this tile is "interesting" (that is, whether it must be stored
     * as an object as opposed to a code).
     */
    public boolean isInteresting ()
    {
        return getClass() != Tile.class;
    }

    /**
     * Returns the packed coordinate of this piece.
     */
    public int getCoord ()
    {
        return CoordUtil.getCoord(x, y);
    }

    /**
     * Returns this tile's packed data.
     */
    public int getData ()
    {
        return (tileId << 16) | (orient << 8) | (elevation & 0xFF);
    }

    /**
     * Returns the bounds of the tile.
     */
    public Rectangle getBounds ()
    {
        if (_bounds == null) {
            _bounds = new Rectangle();
        }
        TileConfig config = getConfig();
        _bounds.setBounds(x, y, config.getWidth(orient), config.getHeight(orient));
        return _bounds;
    }

    @Override // documentation inherited
    public void getResources (java.util.Set<SceneResource> results)
    {
        getConfig().getResources(results);
    }

    /** The bounds of the tile. */
    protected transient Rectangle _bounds;
}
