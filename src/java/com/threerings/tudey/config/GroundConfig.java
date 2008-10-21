//
// $Id$

package com.threerings.tudey.config;

import java.util.ArrayList;

import com.samskivert.util.IntTuple;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * The configuration of a ground type.
 */
public class GroundConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the ground type.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Adds the implementation's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Returns a reference to the config's underlying original implementation.
         */
        public abstract Original getOriginal (ConfigManager cfgmgr);

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * An original ground implementation.
     */
    public static class Original extends Implementation
    {
        /** Whether or not to extend the edge. */
        @Editable
        public boolean extendEdge;

        /** The floor tiles. */
        @Editable
        public Tile[] floor = new Tile[0];

        /** The edge cases (in order of priority). */
        @Editable
        public EdgeCase[] edgeCases = new EdgeCase[0];

        /**
         * Checks whether the specified entry qualifies as a floor tile.
         */
        public boolean isFloor (TileEntry entry)
        {
            return entry != null && matchesAny(floor, entry);
        }

        /**
         * Checks whether the specified entry qualifies as an edge tile.
         */
        public boolean isEdge (TileEntry entry, IntTuple caseRotations)
        {
            return entry != null && ((1 << entry.rotation & caseRotations.right) != 0) &&
                matchesAny(edgeCases[caseRotations.left].tiles, entry);
        }

        /**
         * Creates a new floor tile with the supplied maximum dimensions.
         */
        public TileEntry createFloor (ConfigManager cfgmgr, int maxWidth, int maxHeight)
        {
            ArrayList<TileRotation> rotations = new ArrayList<TileRotation>();
            for (Tile tile : floor) {
                tile.getRotations(cfgmgr, rotations, maxWidth, maxHeight);
            }
            return createRandomEntry(rotations);
        }

        /**
         * Determines the case and allowed rotations of the edge tile that matches the specified
         * pattern.
         */
        public IntTuple getCaseRotations (int pattern)
        {
            for (int ii = 0; ii < edgeCases.length; ii++) {
                int rotations = edgeCases[ii].getRotations(pattern);
                if (rotations != 0) {
                    return new IntTuple(ii, rotations);
                }
            }
            return null;
        }

        /**
         * Creates a new edge tile with the supplied case/rotations and maximum dimensions.
         */
        public TileEntry createEdge (
            ConfigManager cfgmgr, IntTuple caseRotations, int maxWidth, int maxHeight)
        {
            ArrayList<TileRotation> rotations = new ArrayList<TileRotation>();
            for (Tile tile : edgeCases[caseRotations.left].tiles) {
                tile.getRotations(cfgmgr, rotations, caseRotations.right, maxWidth, maxHeight);
            }
            return createRandomEntry(rotations);
        }

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            for (Tile tile : floor) {
                refs.add(TileConfig.class, tile.tile);
            }
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            for (Tile tile : floor) {
                tile.invalidate();
            }
            for (EdgeCase edgeCase : edgeCases) {
                edgeCase.invalidate();
            }
        }

        /**
         * Creates an entry using one of the supplied tile rotations.
         */
        protected TileEntry createRandomEntry (ArrayList<TileRotation> rotations)
        {
            float tweight = 0f;
            for (int ii = 0, nn = rotations.size(); ii < nn; ii++) {
                tweight += rotations.get(ii).weight;
            }
            float random = RandomUtil.getFloat(tweight);
            tweight = 0f;
            for (int ii = 0, nn = rotations.size(); ii < nn; ii++) {
                TileRotation rotation = rotations.get(ii);
                if (random < (tweight += rotation.weight)) {
                    return rotation.createEntry();
                }
            }
            return null;
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The ground reference. */
        @Editable(nullable=true)
        public ConfigReference<GroundConfig> ground;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(GroundConfig.class, ground);
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            GroundConfig config = cfgmgr.getConfig(GroundConfig.class, ground);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }
    }

    /**
     * Represents a single edge case.
     */
    public static class EdgeCase extends DeepObject
        implements Exportable
    {
        /** The constraints in each direction. */
        @Editable(hgroup="d")
        public boolean n, nw, w, sw, s, se, e, ne;

        /** The tiles for the case. */
        @Editable
        public Tile[] tiles = new Tile[0];

        /**
         * Returns a bit set containing the rotations of this case that match the specified
         * pattern.
         */
        public int getRotations (int pattern)
        {
            int rotations = 0;
            int[] patterns = getPatterns();
            for (int ii = 0; ii < 4; ii++) {
                int mask = patterns[ii];
                if ((pattern & mask) == mask) {
                    rotations |= (1 << ii);
                }
            }
            return rotations;
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

        /** Constraint patterns for each rotation. */
        @DeepOmit
        protected transient int[] _patterns;
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

        /** The weight of the tile (affects how often it occurs). */
        @Editable(min=0, step=0.01)
        public float weight = 1f;

        /**
         * Determines whether the specified tile entry matches this tile.
         */
        public boolean matches (TileEntry entry)
        {
            if (!ObjectUtil.equals(entry.tile, tile)) {
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
            ConfigManager cfgmgr, ArrayList<TileRotation> rotations, int maxWidth, int maxHeight)
        {
            for (TileRotation tile : getRotations(cfgmgr)) {
                if (tile.width <= maxWidth && tile.height <= maxHeight) {
                    rotations.add(tile);
                }
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
                        list.add(new TileRotation(tile, 0));
                    }
                    if (west) {
                        list.add(new TileRotation(tile, 1));
                    }
                    if (south) {
                        list.add(new TileRotation(tile, 2));
                    }
                    if (east) {
                        list.add(new TileRotation(tile, 3));
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
        return (n ? 1 : 0) << 7 | (nw ? 1 : 0) << 6 |
            (w ? 1 : 0) << 5 | (sw ? 1 : 0) << 4 |
            (s ? 1 : 0) << 3 | (se ? 1 : 0) << 2 |
            (e ? 1 : 0) << 1 | (ne ? 1 : 0) << 0;
    }

    /** The actual ground implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Returns a reference to the config's underlying original implementation.
     */
    public Original getOriginal (ConfigManager cfgmgr)
    {
        return implementation.getOriginal(cfgmgr);
    }

    @Override // documentation inherited
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }

    /**
     * Checks whether the specified entry matches any of the tiles in the given array.
     */
    protected static boolean matchesAny (Tile[] tiles, TileEntry entry)
    {
        for (Tile tile : tiles) {
            if (tile.matches(entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Represents a rotated tile.
     */
    protected static class TileRotation
    {
        /** The tile config reference. */
        public ConfigReference<TileConfig> tile;

        /** The tile rotation. */
        public int rotation;

        /** The width of the rotated tile. */
        public int width;

        /** The height of the rotated tile. */
        public int height;

        /** The weight of the rotation. */
        public float weight;

        public TileRotation (ConfigReference<TileConfig> tile, int rotation)
        {
            this.tile = tile;
            this.rotation = rotation;
        }

        /**
         * Creates and returns a tile entry based on this configuration.
         */
        public TileEntry createEntry ()
        {
            TileEntry entry = new TileEntry();
            entry.tile = tile;
            entry.rotation = rotation;
            return entry;
        }
    }
}
