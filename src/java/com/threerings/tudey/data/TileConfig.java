//
// $Id$

package com.threerings.tudey.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.PropertiesUtil;

import com.threerings.util.ResourceUtil;

/**
 * Contains information about one of the tiles in a tileset.
 */
public class TileConfig extends SceneElementConfig
    implements TudeyCodes
{
    /** The name of the tile model. */
    public String model;

    /** The variant of the tile model. */
    public String variant;

    /** The width of the tile (in tile units). */
    public int width;

    /** The height of the tile. */
    public int height;

    /** Whether or not players can pass through each square unit of the tile. */
    public boolean[] passable;

    /** Whether or not bullets can pass through each square unit of the tile. */
    public boolean[] penetrable;

    /**
     * Returns the configuration of the named tile.
     */
    public static TileConfig getConfig (int id)
    {
        return _configs.get(id);
    }

    /**
     * Returns the configurations of all tiles.
     */
    public static Collection<TileConfig> getConfigs ()
    {
        return _configs.values();
    }

    /**
     * Returns the piece's width when oriented in the specified way.
     */
    public int getWidth (int orient)
    {
        return (orient % 2 == 0) ? width : height;
    }

    /**
     * Returns the tile's height when oriented in the specified way.
     */
    public int getHeight (int orient)
    {
        return (orient % 2 == 0) ? height : width;
    }

    /**
     * Determines whether players can pass through the tile block at the specified coordinates,
     * with the given tile orientation.
     */
    public boolean isPassable (int orient, int x, int y)
    {
        // adjust for rotation
        int ox = x, oy = y;
        if (orient == WEST) {
            x = oy;
            y = height - ox - 1;
        } else if (orient == SOUTH) {
            x = width - ox - 1;
            y = height - oy - 1;
        } else if (orient == EAST) {
            x = width - oy - 1;
            y = ox;
        }
        return passable[y*width + x];
    }

    @Override // documentation inherited
    public void getResources (Set<SceneResource> results)
    {
        super.getResources(results);
        results.add(new SceneResource.Model(model, variant));
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        width = getProperty("width", 1);
        height = getProperty("height", 1);
        model = getProperty("model", "tilesets/" + name);
        variant = getProperty("variant");
        passable = getProperty("passable", new boolean[] { false });
        int area = width * height;
        if (passable.length != area) {
            boolean value = passable.length > 0 && passable[0];
            passable = new boolean[area];
            Arrays.fill(passable, value);
        }
        penetrable = getProperty("penetrable", new boolean[] { false });
        if (penetrable.length != area) {
            boolean value = penetrable.length > 0 && penetrable[0];
            penetrable = new boolean[area];
            Arrays.fill(penetrable, value);
        }
        // anything passable must also be penetrable
        for (int ii = 0; ii < area; ii++) {
            penetrable[ii] |= passable[ii];
        }
    }

    /**
     * Loads the properties of a single tileset using the supplied id mapping.
     */
    protected static void loadTilesetProperties (String tileset, Properties ids)
    {
        Properties props = ResourceUtil.loadProperties(tileset);
        String[] tiles = getProperty(props, "tiles", new String[0]);
        String prefix = getName(tileset, "world/tileset/".length()) + "/";
        for (String tile : tiles) {
            String name = prefix + tile;
            int id = getProperty(ids, name, 0);
            TileConfig config = new TileConfig();
            config.init(name, id, PropertiesUtil.getSubProperties(props, tile));
            _configs.put(id, config);
        }
    }

    /** Maps tile ids to their configurations. */
    protected static HashIntMap<TileConfig> _configs = new HashIntMap<TileConfig>();

    /** Load the ids and tileset names. */
    static {
        Properties ids = ResourceUtil.loadProperties("world/tileset/ids.properties");
        for (String tileset : ResourceUtil.loadStrings("world/tileset/list.txt")) {
            loadTilesetProperties(tileset, ids);
        }
    }
}
