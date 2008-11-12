//
// $Id$

package com.threerings.tudey.config;

import com.samskivert.util.IntTuple;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.tudey.data.TudeySceneModel.TileEntry;

/**
 * The configuration of a ground type.
 */
public class GroundConfig extends PaintableConfig
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
        public Case[] edgeCases = new Case[0];

        /**
         * Checks whether the specified entry qualifies as a floor tile.
         */
        public boolean isFloor (TileEntry entry, int elevation)
        {
            return matchesAny(floor, entry, elevation);
        }

        /**
         * Creates a new floor tile with the supplied maximum dimensions.
         */
        public TileEntry createFloor (ConfigManager cfgmgr, int maxWidth, int maxHeight)
        {
            return createRandomEntry(cfgmgr, floor, maxWidth, maxHeight);
        }

        /**
         * Checks whether the specified entry matches any edge case.
         */
        public boolean isEdge (TileEntry entry, int elevation)
        {
            for (Case caze : edgeCases) {
                if (matchesAny(caze.tiles, entry, elevation)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Determines the case and allowed rotations of the edge tile that matches the specified
         * pattern.
         */
        public IntTuple getEdgeCaseRotations (int pattern)
        {
            return getCaseRotations(edgeCases, pattern);
        }

        /**
         * Checks whether the specified entry qualifies as an edge tile.
         */
        public boolean isEdge (TileEntry entry, IntTuple caseRotations, int elevation)
        {
            return matchesAny(edgeCases, entry, caseRotations, elevation);
        }

        /**
         * Creates a new edge tile with the supplied case/rotations and maximum dimensions.
         */
        public TileEntry createEdge (
            ConfigManager cfgmgr, IntTuple caseRotations, int maxWidth, int maxHeight)
        {
            return createRandomEntry(
                cfgmgr, edgeCases[caseRotations.left].tiles,
                caseRotations.right, maxWidth, maxHeight);
        }

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            for (Tile tile : floor) {
                refs.add(TileConfig.class, tile.tile);
            }
            for (Case caze : edgeCases) {
                caze.getUpdateReferences(refs);
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
            for (Case caze : edgeCases) {
                caze.invalidate();
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
}
