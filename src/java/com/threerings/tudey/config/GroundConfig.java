//
// $Id$

package com.threerings.tudey.config;

import java.util.ArrayList;

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
        /** The floor tiles. */
        @Editable
        public Tile[] floor = new Tile[0];

        /**
         * Checks whether the specified entry qualifies as a floor tile.
         */
        public boolean isFloor (TileEntry entry)
        {
            if (entry != null) {
                for (Tile tile : floor) {
                    if (tile.matches(entry)) {
                        return true;
                    }
                }
            }
            return false;
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
            for (TileRotation rotation : getRotations(cfgmgr)) {
                if (rotation.width <= maxWidth && rotation.height <= maxHeight) {
                    rotations.add(rotation);
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
