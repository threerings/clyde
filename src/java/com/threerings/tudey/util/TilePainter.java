//
// $Id$

package com.threerings.tudey.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.samskivert.util.IntTuple;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.config.GroundConfig;
import com.threerings.tudey.config.PaintableConfig;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.config.WallConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;

/**
 * Encapsulates the logic used to "paint" ground and wall tiles.
 */
public class TilePainter
{
    /**
     * Creates a new tile painter.
     */
    public TilePainter (ConfigManager cfgmgr, TudeySceneModel scene)
    {
        _cfgmgr = cfgmgr;
        _scene = scene;
    }

    /**
     * Paints the specified coordinates with ground.
     */
    public void paintGround (CoordSet coords, ConfigReference<GroundConfig> ground, int elevation)
    {
        paintGround(coords, ground, elevation, false, false);
    }

    /**
     * Paints the specified coordinates with ground.
     *
     * @param erase if true, erase the ground within the coordinates.
     * @param revise if true, replace existing tiles with new variants.
     */
    public void paintGround (
        CoordSet coords, ConfigReference<GroundConfig> ground,
        int elevation, boolean erase, boolean revise)
    {
        GroundConfig config = _cfgmgr.getConfig(GroundConfig.class, ground);
        GroundConfig.Original original = (config == null) ? null : config.getOriginal(_cfgmgr);

        // if no config, just erase the coordinates
        if (original == null) {
            removeEntries(coords);
            return;
        }

        // if erasing, erase the outer region and update the edges
        if (erase) {
            removeEntries(coords);
            CoordSet border = coords.getBorder();
            removeEntries(border);
            updateGroundEdges(border, original, elevation, revise);
            return;
        }

        // find the coordinates that need to be painted
        if (original.extendEdge) {
            coords.addAll(coords.getCardinalBorder());
        }
        CoordSet border = coords.getBorder();
        if (!revise) {
            // remove anything that's already a floor tile
            for (Iterator<Coord> it = coords.iterator(); it.hasNext(); ) {
                Coord coord = it.next();
                if (original.isFloor(_scene.getTileEntry(coord.x, coord.y), elevation)) {
                    it.remove();
                }
            }
        }

        // paint the floor
        paintFloor(coords, ground, elevation);

        // find the border tiles that need to be updated
        updateGroundEdges(border, original, elevation, revise);
    }

    /**
     * Paints the specified coordinates with wall.
     */
    public void paintWall (CoordSet coords, ConfigReference<WallConfig> wall, int elevation)
    {
        paintWall(coords, wall, elevation, false, false);
    }

    /**
     * Paints the specified coordinates with wall.
     *
     * @param erase if true, erase the wall within the coordinates.
     * @param revise if true, replace existing tiles with new variants.
     */
    public void paintWall (
        CoordSet coords, ConfigReference<WallConfig> wall,
        int elevation, boolean erase, boolean revise)
    {
        WallConfig config = _cfgmgr.getConfig(WallConfig.class, wall);
        WallConfig.Original original = (config == null) ? null : config.getOriginal(_cfgmgr);

        // if no config, just erase the coordinates
        if (original == null) {
            removeEntries(coords);
            return;
        }

        // add the border
        CoordSet ocoords = new CoordSet(coords);
        coords.addAll(coords.getBorder());

        // initialize the set of will-be-wall coordinates
        CoordSet wcoords = new CoordSet();
        if (erase) {
            // paint over any non-wall tiles
            for (Iterator<Coord> it = ocoords.iterator(); it.hasNext(); ) {
                Coord coord = it.next();
                if (!original.isWall(_scene.getTileEntry(coord.x, coord.y), elevation)) {
                    it.remove();
                }
            }
            paintFloor(ocoords, original.ground, elevation);
        } else {
            wcoords.addAll(ocoords);
        }

        // divide the coordinates up by case/rotation pairs
        HashMap<IntTuple, CoordSet> sets = new HashMap<IntTuple, CoordSet>();
        for (Coord coord : coords) {
            TileEntry entry = _scene.getTileEntry(coord.x, coord.y);
            if (!(wcoords.contains(coord) || original.isWall(entry, elevation))) {
                continue; // only modify wall tiles
            }
            // classify the tile based on its surroundings
            int pattern = 0;
            for (Direction dir : Direction.CARDINAL_VALUES) {
                int x = coord.x + dir.getX(), y = coord.y + dir.getY();
                if (wcoords.contains(x, y) ||
                        original.isWall(_scene.getTileEntry(x, y), elevation)) {
                    pattern |= (1 << dir.ordinal());
                }
            }
            IntTuple tuple = original.getWallCaseRotations(pattern);
            if (tuple != null && (revise || !original.isWall(entry, tuple, elevation))) {
                CoordSet set = sets.get(tuple);
                if (set == null) {
                    sets.put(tuple, set = new CoordSet());
                }
                set.add(coord);
            }
        }

        // add wall tiles as appropriate
        Rectangle region = new Rectangle();
    OUTER:
        for (Map.Entry<IntTuple, CoordSet> entry : sets.entrySet()) {
            IntTuple tuple = entry.getKey();
            CoordSet set = entry.getValue();
            while (!set.isEmpty()) {
                set.getLargestRegion(region);
                TileEntry tentry = original.createWall(
                    _cfgmgr, tuple, region.width, region.height);
                if (tentry == null) {
                    continue OUTER; // no appropriate tiles
                }
                Coord coord = tentry.getLocation();
                set.pickRandom(region.width, region.height, coord);
                TileConfig.Original tconfig = tentry.getConfig(_cfgmgr);
                int twidth = tentry.getWidth(tconfig);
                int theight = tentry.getHeight(tconfig);
                tentry.elevation = elevation;
                addEntry(tentry, region.set(coord.x, coord.y, twidth, theight));
                set.removeAll(region);
            }
        }
    }

    /**
     * Paints floor tiles in the specified region.
     */
    protected void paintFloor (
        CoordSet coords, ConfigReference<GroundConfig> ground, int elevation)
    {
        GroundConfig config = _cfgmgr.getConfig(GroundConfig.class, ground);
        GroundConfig.Original original = (config == null) ? null : config.getOriginal(_cfgmgr);

        // if no config, just erase the coordinates
        if (original == null) {
            removeEntries(coords);
            return;
        }

        // cover floor tiles randomly until filled in
        Rectangle region = new Rectangle();
        while (!coords.isEmpty()) {
            coords.getLargestRegion(region);
            TileEntry entry = original.createFloor(_cfgmgr, region.width, region.height);
            if (entry == null) {
                break; // no appropriate tiles
            }
            Coord coord = entry.getLocation();
            coords.pickRandom(region.width, region.height, coord);
            TileConfig.Original tconfig = entry.getConfig(_cfgmgr);
            int twidth = entry.getWidth(tconfig);
            int theight = entry.getHeight(tconfig);
            entry.elevation = elevation;
            addEntry(entry, region.set(coord.x, coord.y, twidth, theight));
            coords.removeAll(region);
        }
    }

    /**
     * Updates the edge tiles in the specified coordinate set.
     */
    protected void updateGroundEdges (
        CoordSet coords, GroundConfig.Original original, int elevation, boolean revise)
    {
        // divide the coordinates up by case/rotation pairs
        HashMap<IntTuple, CoordSet> sets = new HashMap<IntTuple, CoordSet>();
        for (Coord coord : coords) {
            TileEntry entry = _scene.getTileEntry(coord.x, coord.y);
            if (original.isFloor(entry, elevation)) {
                continue; // if it's already floor, leave it alone
            }
            // classify the tile based on its surroundings
            int pattern = 0;
            for (Direction dir : Direction.values()) {
                int x = coord.x + dir.getX(), y = coord.y + dir.getY();
                if (original.isFloor(_scene.getTileEntry(x, y), elevation)) {
                    pattern |= (1 << dir.ordinal());
                }
            }
            IntTuple tuple = original.getEdgeCaseRotations(pattern);
            if (tuple != null && (revise || !original.isEdge(entry, tuple, elevation))) {
                CoordSet set = sets.get(tuple);
                if (set == null) {
                    sets.put(tuple, set = new CoordSet());
                }
                set.add(coord);
            }
        }

        // add edge tiles as appropriate
        Rectangle region = new Rectangle();
    OUTER:
        for (Map.Entry<IntTuple, CoordSet> entry : sets.entrySet()) {
            IntTuple tuple = entry.getKey();
            CoordSet set = entry.getValue();
            while (!set.isEmpty()) {
                set.getLargestRegion(region);
                TileEntry tentry = original.createEdge(
                    _cfgmgr, tuple, region.width, region.height);
                if (tentry == null) {
                    continue OUTER; // no appropriate tiles
                }
                Coord coord = tentry.getLocation();
                set.pickRandom(region.width, region.height, coord);
                TileConfig.Original tconfig = tentry.getConfig(_cfgmgr);
                int twidth = tentry.getWidth(tconfig);
                int theight = tentry.getHeight(tconfig);
                tentry.elevation = elevation;
                addEntry(tentry, region.set(coord.x, coord.y, twidth, theight));
                set.removeAll(region);
            }
        }
    }

    /**
     * Adds the specified entry, removing any entries underneath it.
     */
    protected void addEntry (TileEntry entry, Rectangle region)
    {
        removeEntries(region);
        _scene.addEntry(entry);
    }

    /**
     * Removes all entries within the specified region.
     */
    protected void removeEntries (Rectangle region)
    {
        ArrayList<TileEntry> results = new ArrayList<TileEntry>();
        _scene.getTileEntries(region, results);
        for (int ii = 0, nn = results.size(); ii < nn; ii++) {
            _scene.removeEntry(results.get(ii).getKey());
        }
    }

    /**
     * Removes all entries intersecting the given coordinates.
     */
    protected void removeEntries (CoordSet coords)
    {
        for (Coord coord : coords) {
            TileEntry entry = _scene.getTileEntry(coord.x, coord.y);
            if (entry != null) {
                _scene.removeEntry(entry.getKey());
            }
        }
    }

    /** The config manager used to resolve configuration references. */
    protected ConfigManager _cfgmgr;

    /** The scene into which we paint. */
    protected TudeySceneModel _scene;
}
