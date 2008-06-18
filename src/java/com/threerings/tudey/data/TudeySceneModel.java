//
// $Id$

package com.threerings.tudey.data;

import java.awt.Rectangle;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.whirled.data.SceneModel;

import com.threerings.config.ConfigManager;
import com.threerings.export.Exportable;
import com.threerings.export.Importer;

import com.threerings.tudey.util.CoordIntMap;
import com.threerings.tudey.util.CoordUtil;

/**
 * Contains a representation of a Tudey scene.
 */
public class TudeySceneModel extends SceneModel
    implements Exportable
{
    /**
     * Creates an empty scene.
     */
    public TudeySceneModel ()
    {
        initTransientFields();
    }

    /**
     * Initializes the model.
     */
    public void init (ConfigManager cfgmgr)
    {
        _cfgmgr.init("scene", cfgmgr);
    }

    /**
     * Returns a reference to the scene's configuration manager.
     */
    public ConfigManager getConfigManager ()
    {
        return _cfgmgr;
    }

    /**
     * Returns a reference to the scene's environment object.
     */
    public Environment getEnvironment ()
    {
        return _environment;
    }

    /**
     * Sets the scene's environment object.
     */
    public void setEnvironment (Environment environment)
    {
        _environment = environment;
    }

    /**
     * Returns a reference to the set of all tiles in the scene.
     */
    public TileSet getTiles ()
    {
        return _tiles;
    }

    /**
     * Retrieves all of the tiles in the scene that intersect the specified region.
     *
     * @return a new set containing the results.
     */
    public TileSet getTiles (Rectangle region)
    {
        return getTiles(region, new TileSet());
    }

    /**
     * Retrieves all of the tiles in the scene that intersect the specified region and places them
     * in the set provided.
     *
     * @return a reference to the result set.
     */
    public TileSet getTiles (Rectangle region, TileSet results)
    {
        results.clear();
        for (int xx = region.x, xxmax = xx + region.width; xx < xxmax; xx++) {
            for (int yy = region.y, yymax = yy + region.height; yy < yymax; yy++) {
                Tile tile = getTile(xx, yy);
                if (tile != null) {
                    results.add(tile);
                }
            }
        }
        return results;
    }

    /**
     * Returns the tile elevation at the specified coordinates.
     */
    public int getElevation (int x, int y)
    {
        Tile tile = getTile(x, y);
        return (tile == null) ? 0 : tile.elevation;
    }

    /**
     * Returns the tile elevation at the specified coordinates.
     */
    public int getElevation (float x, float y)
    {
        return getElevation((int)Math.floor(x), (int)Math.floor(y));
    }

    /**
     * Returns the tile intersecting the given coordinates, if any. Note that the tile returned
     * may be a "dummy" tile maintained by a tile set, and should be copied if its values must be
     * preserved.
     */
    public Tile getTile (int x, int y)
    {
        int tcoord = _tcoords.get(x, y);
        return (tcoord == CoordUtil.UNUSED) ?
            null : _tiles.get(CoordUtil.getX(tcoord), CoordUtil.getY(tcoord));
    }

    /**
     * Adds a group of tiles to the scene.
     */
    public void addTiles (TileSet tiles)
    {
        for (Tile tile : tiles) {
            _tiles.add(tile);
            createShadow(tile);
        }
    }

    /**
     * Removes a group of tiles from the scene.
     */
    public void removeTiles (TileSet tiles)
    {
        for (Tile tile : tiles) {
            if (_tiles.remove(tile)) {
                clearShadow(tile);
            }
        }
    }

    /**
     * Returns a reference to the set of all placeable objects in the scene.
     */
    public PlaceableSet getPlaceables ()
    {
        return _placeables;
    }

    /**
     * Adds a group of placeable objects to the scene.
     */
    public void addPlaceables (PlaceableSet placeables)
    {
        _placeables.addAll(placeables);
    }

    /**
     * Removes a group of placeable objects from the scene.
     */
    public void removePlaceables (PlaceableSet placeables)
    {
        for (Placeable placeable : placeables) {
            _placeables.remove(placeable);
        }
    }

    /**
     * Reads the fields of the object.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        initTransientFields();
    }

    /**
     * Reads the fields of the object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        in.defaultReadFields();
        initTransientFields();
    }

    /**
     * Initializes the scene's transient fields after construction or deserialization.
     */
    protected void initTransientFields ()
    {
        // validate the contents of the scene
        _tiles.validate();
        _placeables.validate();

        // create the tile shadows
        _tcoords.setNoneValue(CoordUtil.UNUSED);
        for (Tile tile : _tiles) {
            createShadow(tile);
        }
    }

    /**
     * Sets the tile's shadow in the transient arrays.
     */
    protected void createShadow (Tile tile)
    {
        Rectangle bounds = tile.getBounds();
        int tcoord = tile.getCoord();
        for (int xx = bounds.x, xxmax = xx + bounds.width; xx < xxmax; xx++) {
            for (int yy = bounds.y, yymax = yy + bounds.height; yy < yymax; yy++) {
                _tcoords.put(xx, yy, tcoord);
            }
        }
    }

    /**
     * Clears the tile's shadow from the transient arrays.
     */
    protected void clearShadow (Tile tile)
    {
        Rectangle bounds = tile.getBounds();
        int tcoord = tile.getCoord();
        for (int xx = bounds.x, xxmax = xx + bounds.width; xx < xxmax; xx++) {
            for (int yy = bounds.y, yymax = yy + bounds.height; yy < yymax; yy++) {
                _tcoords.remove(xx, yy);
            }
        }
    }

    /** The scene configuration manager. */
    protected ConfigManager _cfgmgr = new ConfigManager();

    /** The scene's global environment. */
    protected Environment _environment = new Environment();

    /** The set of tiles in the scene. */
    protected TileSet _tiles = new TileSet();

    /** The set of placeables in the scene. */
    protected PlaceableSet _placeables = new PlaceableSet();

    /** For each location occupied by a tile, the coordinates of that tile. */
    protected transient CoordIntMap _tcoords = new CoordIntMap();
}
