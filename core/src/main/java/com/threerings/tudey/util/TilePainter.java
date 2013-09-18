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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.samskivert.util.IntTuple;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.config.GroundConfig;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.config.WallConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Paint;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;

/**
 * Encapsulates the logic used to "paint" ground and wall tiles.
 */
public class TilePainter
{
    /**
     * Creates a new tile painter.
     */
    public TilePainter (ConfigManager cfgmgr, TudeySceneModel scene, EntryManipulator manipulator)
    {
        _cfgmgr = cfgmgr;
        _scene = scene;
        _manipulator = manipulator;
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
            updateGroundEdges(border, coords, ground, elevation, revise);
            return;
        }

        // find the coordinates that need to be painted
        if (original.extendEdge) {
            coords.addAll(coords.getCardinalBorder());
        }

        // remove any coordinates painted with a higher priority
        for (Iterator<Coord> it = coords.iterator(); it.hasNext(); ) {
            Coord coord = it.next();
            Paint paint = _scene.getPaint(coord.x, coord.y);
            if (paint == null || paint.type == Paint.Type.WALL) {
                continue;
            }
            GroundConfig oconfig = paint.getConfig(_cfgmgr, GroundConfig.class);
            GroundConfig.Original ooriginal =
                (oconfig == null) ? null : oconfig.getOriginal(_cfgmgr);
            if (ooriginal != null && ooriginal.priority > original.priority) {
                it.remove();
            }
        }
        CoordSet inner = new CoordSet(coords);
        CoordSet border = inner.getBorder();
        coords.removeAll(border);

        // if not revising, remove anything that's already a floor tile
        if (!revise) {
            for (Iterator<Coord> it = coords.iterator(); it.hasNext(); ) {
                Coord coord = it.next();
                if (original.isFloor(_scene, ground, coord.x, coord.y, elevation)) {
                    it.remove();
                }
            }
        }

        // paint the floor
        paintFloor(coords, ground, elevation);

        // find the border tiles that need to be updated
        updateGroundEdges(inner.getBorder(), inner, ground, elevation, revise);
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
        Paint paint = new Paint(Paint.Type.WALL, wall, elevation);
    OUTER:
        for (Map.Entry<IntTuple, CoordSet> entry : sets.entrySet()) {
            IntTuple tuple = entry.getKey();
            CoordSet set = entry.getValue();
            while (!set.isEmpty()) {
                set.getLargestRegion(region);
                TileEntry tentry = original.createWall(
                    _cfgmgr, tuple, region.width, region.height, elevation);
                if (tentry == null) {
                    continue OUTER; // no appropriate tiles
                }
                Coord coord = tentry.getLocation();
                set.pickRandom(region.width, region.height, coord);
                TileConfig.Original tconfig = tentry.getConfig(_cfgmgr);
                int twidth = tentry.getWidth(tconfig);
                int theight = tentry.getHeight(tconfig);
                addEntry(tentry, region.set(coord.x, coord.y, twidth, theight), paint);
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
        Paint paint = new Paint(Paint.Type.FLOOR, ground, elevation);
        while (!coords.isEmpty()) {
            coords.getLargestRegion(region);
            TileEntry entry = original.createFloor(
                _cfgmgr, region.width, region.height, elevation);
            if (entry == null) {
                break; // no appropriate tiles
            }
            Coord coord = entry.getLocation();
            coords.pickRandom(region.width, region.height, coord);
            TileConfig.Original tconfig = entry.getConfig(_cfgmgr);
            int twidth = entry.getWidth(tconfig);
            int theight = entry.getHeight(tconfig);
            addEntry(entry, region.set(coord.x, coord.y, twidth, theight), paint);
            coords.removeAll(region);
        }
    }

    /**
     * Updates the edge tiles in the specified coordinate set.
     *
     * @param inner the inner region to exclude from the update.
     */
    protected void updateGroundEdges (
        CoordSet coords, CoordSet inner, ConfigReference<GroundConfig> ground,
        int elevation, boolean revise)
    {
        GroundConfig config = _cfgmgr.getConfig(GroundConfig.class, ground);
        GroundConfig.Original original = (config == null) ? null : config.getOriginal(_cfgmgr);

        // if no config, just erase the coordinates
        if (original == null) {
            removeEntries(coords);
            return;
        }

        // divide the coordinates up by case/rotation pairs
        HashMap<IntTuple, CoordSet> sets = new HashMap<IntTuple, CoordSet>();
        for (Coord coord : coords) {
            TileEntry entry = _scene.getTileEntry(coord.x, coord.y);
            if (entry != null && !original.isEdge(entry, elevation)) {
                Paint paint = _scene.getPaint(coord.x, coord.y);
                if (paint == null || paint.type == Paint.Type.WALL) {
                    continue;
                }
                GroundConfig oconfig = paint.getConfig(_cfgmgr, GroundConfig.class);
                GroundConfig.Original ooriginal = (oconfig == null) ?
                    null : oconfig.getOriginal(_cfgmgr);
                if (ooriginal == null || ooriginal.priority >= original.priority) {
                    continue;
                }
            }
            IntTuple tuple = original.getEdgeCaseRotations(
                _scene, ground, coord.x, coord.y, elevation);
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
        Paint paint = new Paint(Paint.Type.EDGE, ground, elevation);
    OUTER:
        for (Map.Entry<IntTuple, CoordSet> entry : sets.entrySet()) {
            IntTuple tuple = entry.getKey();
            CoordSet set = entry.getValue();
            while (!set.isEmpty()) {
                set.getLargestRegion(region);
                TileEntry tentry = original.createEdge(
                    _cfgmgr, tuple, region.width, region.height, elevation);
                if (tentry == null) {
                    continue OUTER; // no appropriate tiles
                }
                Coord coord = tentry.getLocation();
                set.pickRandom(region.width, region.height, coord);
                TileConfig.Original tconfig = tentry.getConfig(_cfgmgr);
                int twidth = tentry.getWidth(tconfig);
                int theight = tentry.getHeight(tconfig);
                addEntry(tentry, region.set(coord.x, coord.y, twidth, theight), paint);
                set.removeAll(region);
            }
        }

        // if there's a base ground config, expand the edges and update again
        if (original.base != null) {
            CoordSet combined = new CoordSet(inner);
            combined.addAll(coords);
            updateGroundEdges(combined.getBorder(), combined, original.base, elevation, revise);
        }
    }

    /**
     * Adds the specified entry, removing any entries underneath it.
     */
    protected void addEntry (TileEntry entry, Rectangle region, Paint paint)
    {
        removeEntries(region);
        _manipulator.addEntries(entry);
        _manipulator.setPaint(region, paint);
    }

    /**
     * Removes all entries within the specified region.
     */
    protected void removeEntries (Rectangle region)
    {
        ArrayList<TileEntry> results = new ArrayList<TileEntry>();
        _scene.getTileEntries(region, results);
        _manipulator.removeEntries(results);
    }

    /**
     * Removes all entries intersecting the given coordinates.
     */
    protected void removeEntries (CoordSet coords)
    {
        ArrayList<TileEntry> list = new ArrayList<TileEntry>();
        for (Coord coord : coords) {
            TileEntry entry = _scene.getTileEntry(coord.x, coord.y);
            if (entry != null) {
                list.add(entry);
            }
        }
        _manipulator.removeEntries(list);
    }

    /** The config manager used to resolve configuration references. */
    protected ConfigManager _cfgmgr;

    /** The scene into which we paint. */
    protected TudeySceneModel _scene;

    /** The object used to modify the entries. */
    protected EntryManipulator _manipulator;
}
