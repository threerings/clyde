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

package com.threerings.tudey.config;

import java.util.ArrayList;

import com.google.common.base.Objects;

import com.samskivert.util.IntTuple;
import com.samskivert.util.Randoms;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.Strippable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.tudey.data.TudeySceneModel.TileEntry;

/**
 * Base class for {@link GroundConfig} and {@link WallConfig}.
 */
@Strippable
public abstract class PaintableConfig extends ParameterizedConfig
{
    /**
     * Represents a single case.
     */
    public static class Case extends DeepObject
        implements Exportable
    {
        /** The constraints in each direction. */
        @Editable(hgroup="d")
        public boolean n, nw, w, sw, s, se, e, ne;

        /** The tiles for the case. */
        @Editable(weight=1)
        public Tile[] tiles = new Tile[0];

        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
        }

        /**
         * Returns a bit set containing the rotations of this case that match the specified
         * pattern.
         */
        public int getRotations (int pattern)
        {
            return getRotations(getPatterns(), pattern);
        }

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            for (Tile tile : tiles) {
                tile.invalidate();
            }
            _patterns = null;
            _orientations = null;
        }

        /**
         * Returns a bit set containing the rotations of this case that match the specified
         * pattern.
         */
        protected int getRotations (int[] patterns, int pattern)
        {
            int rotations = 0;
            boolean[] orientations = getOrientations();
            for (int ii = 0; ii < 4; ii++) {
                if (!orientations[ii]) {
                    continue;
                }
                int mask = patterns[ii];
                if ((pattern & mask) == mask) {
                    rotations |= (1 << ii);
                }
            }
            return rotations;
        }

        /**
         * Gets the cached pattern rotations.
         */
        protected int[] getPatterns ()
        {
            if (_patterns == null) {
                _patterns = new int[] {
                    createPattern(n, nw, w, sw, s, se, e, ne),
                    createPattern(e, ne, n, nw, w, sw, s, se),
                    createPattern(s, se, e, ne, n, nw, w, sw),
                    createPattern(w, sw, s, se, e, ne, n, nw)
                };
            }
            return _patterns;
        }

        /**
         * Gets the cached pattern orientations.
         */
        protected boolean[] getOrientations ()
        {
            if (_orientations == null) {
                _orientations = new boolean[4];
                for (Tile tile : tiles) {
                    _orientations[0] |= tile.north;
                    _orientations[1] |= tile.west;
                    _orientations[2] |= tile.south;
                    _orientations[3] |= tile.east;
                }
            }
            return _orientations;
        }

        /** Constraint patterns for each rotation. */
        @DeepOmit
        protected transient int[] _patterns;

        /** For each orientation, whether or not there are any tiles that allow it. */
        @DeepOmit
        protected transient boolean[] _orientations;
    }

    /**
     * Contains a tile that can be used for a case.
     */
    public static class Tile extends DeepObject
        implements Exportable
    {
        /** The tile reference. */
        @Editable(nullable=true)
        public ConfigReference<TileConfig> tile;

        /** Whether or not the tile can be oriented in each direction. */
        @Editable(hgroup="d")
        public boolean north = true, west = true, south = true, east = true;

        /** The elevation offset of the tile. */
        @Editable(hgroup="e")
        public int elevationOffset;

        /** The weight of the tile (affects how often it occurs). */
        @Editable(min=0, step=0.01, hgroup="e")
        public float weight = 1f;

        /**
         * Determines whether the specified tile entry matches this tile.
         */
        public boolean matches (TileEntry entry, int elevation)
        {
            if (!Objects.equal(entry.tile, tile) ||
                    entry.elevation != (elevation + elevationOffset)) {
                return false;
            }
            switch (entry.rotation) {
                default: case 0: return north;
                case 1: return west;
                case 2: return south;
                case 3: return east;
            }
        }

        /**
         * Adds the tile rotation options to the provided list.
         */
        public void getRotations (
            ConfigManager cfgmgr, ArrayList<TileRotation> rotations,
            int mask, int maxWidth, int maxHeight)
        {
            for (TileRotation tile : getRotations(cfgmgr)) {
                if ((1 << tile.rotation & mask) != 0 && tile.width <= maxWidth &&
                        tile.height <= maxHeight) {
                    rotations.add(tile);
                }
            }
        }

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            _rotations = null;
        }

        /**
         * Gets the cached tile rotations.
         */
        protected TileRotation[] getRotations (ConfigManager cfgmgr)
        {
            if (_rotations == null) {
                ArrayList<TileRotation> list = new ArrayList<TileRotation>();
                TileConfig config = cfgmgr.getConfig(TileConfig.class, tile);
                TileConfig.Original original = (config == null) ?
                    null : config.getOriginal(cfgmgr);
                if (original != null) {
                    if (north) {
                        list.add(new TileRotation(this, 0));
                    }
                    if (west) {
                        list.add(new TileRotation(this, 1));
                    }
                    if (south) {
                        list.add(new TileRotation(this, 2));
                    }
                    if (east) {
                        list.add(new TileRotation(this, 3));
                    }
                }
                _rotations = list.toArray(new TileRotation[list.size()]);
                if (_rotations.length > 0) {
                    float rweight = weight / _rotations.length;
                    for (TileRotation rotation : _rotations) {
                        rotation.width = original.getWidth(rotation.rotation);
                        rotation.height = original.getHeight(rotation.rotation);
                        rotation.weight = rweight;
                    }
                }
            }
            return _rotations;
        }

        /** Cached tile rotations. */
        @DeepOmit
        protected transient TileRotation[] _rotations;
    }

    /**
     * Creates a bit pattern with the supplied directions.
     */
    public static int createPattern (
        boolean n, boolean nw, boolean w, boolean sw,
        boolean s, boolean se, boolean e, boolean ne)
    {
        return
            (n ? 1 << 0 : 0) | (nw ? 1 << 1 : 0) |
            (w ? 1 << 2 : 0) | (sw ? 1 << 3 : 0) |
            (s ? 1 << 4 : 0) | (se ? 1 << 5 : 0) |
            (e ? 1 << 6 : 0) | (ne ? 1 << 7 : 0);
    }

    /**
     * Determines the case and allowed rotations of the edge tile that matches the specified
     * pattern.
     */
    protected static IntTuple getCaseRotations (Case[] cases, int pattern)
    {
        for (int ii = 0; ii < cases.length; ii++) {
            int rotations = cases[ii].getRotations(pattern);
            if (rotations != 0) {
                return new IntTuple(ii, rotations);
            }
        }
        return null;
    }

    /**
     * Checks whether the specified entry matches the specified case index/rotation pair.
     */
    protected static boolean matchesAny (
        Case[] cases, TileEntry entry, IntTuple caseRotations, int elevation)
    {
        return entry != null && ((1 << entry.rotation & caseRotations.right) != 0) &&
            matchesAny(cases[caseRotations.left].tiles, entry, elevation);
    }

    /**
     * Checks whether the specified entry matches any of the tiles in the given array.
     */
    protected static boolean matchesAny (Tile[] tiles, TileEntry entry, int elevation)
    {
        if (entry != null) {
            for (Tile tile : tiles) {
                if (tile.matches(entry, elevation)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates a random entry from the specified tile array with the supplied maximum width, and
     * height.
     */
    protected static TileEntry createRandomEntry (
        ConfigManager cfgmgr, Tile[] tiles, int maxWidth, int maxHeight, int elevation)
    {
        return createRandomEntry(cfgmgr, tiles, 0x0F, maxWidth, maxHeight, elevation);
    }

    /**
     * Creates a random entry from the specified tile array with the supplied rotation mask,
     * maximum width, and maximum height.
     */
    protected static TileEntry createRandomEntry (
        ConfigManager cfgmgr, Tile[] tiles, int mask, int maxWidth, int maxHeight, int elevation)
    {
        ArrayList<TileRotation> rotations = new ArrayList<TileRotation>();
        for (Tile tile : tiles) {
            tile.getRotations(cfgmgr, rotations, mask, maxWidth, maxHeight);
        }
        return createRandomEntry(rotations, elevation);
    }

    /**
     * Creates an entry using one of the supplied tile rotations.
     */
    protected static TileEntry createRandomEntry (ArrayList<TileRotation> rotations, int elevation)
    {
        float tweight = 0f;
        for (int ii = 0, nn = rotations.size(); ii < nn; ii++) {
            tweight += rotations.get(ii).weight;
        }
        float random = Randoms.threadLocal().getFloat(tweight);
        tweight = 0f;
        for (int ii = 0, nn = rotations.size(); ii < nn; ii++) {
            TileRotation rotation = rotations.get(ii);
            if (random < (tweight += rotation.weight)) {
                return rotation.createEntry(elevation);
            }
        }
        return null;
    }

    /**
     * Represents a rotated tile.
     */
    protected static class TileRotation
    {
        /** The owning tile. */
        public Tile tile;

        /** The tile rotation. */
        public int rotation;

        /** The width of the rotated tile. */
        public int width;

        /** The height of the rotated tile. */
        public int height;

        /** The weight of the rotation. */
        public float weight;

        /**
         * Creates a new tile rotation.
         */
        public TileRotation (Tile tile, int rotation)
        {
            this.tile = tile;
            this.rotation = rotation;
        }

        /**
         * Creates and returns a tile entry based on this configuration.
         */
        public TileEntry createEntry (int elevation)
        {
            TileEntry entry = new TileEntry();
            entry.tile = tile.tile;
            entry.rotation = rotation;
            entry.elevation = elevation + tile.elevationOffset;
            return entry;
        }
    }
}
