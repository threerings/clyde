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
 * The configuration of a wall type.
 */
public class WallConfig extends PaintableConfig
{
    /**
     * Contains the actual implementation of the wall type.
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
     * An original wall implementation.
     */
    public static class Original extends Implementation
    {
        /** The ground underlying the wall. */
        @Editable(nullable=true)
        public ConfigReference<GroundConfig> ground;

        /** The wall cases. */
        @Editable
        public Case[] cases = new Case[0];

        /**
         * Determines the case and allowed rotations of the wall tile that matches the specified
         * pattern.
         */
        public IntTuple getWallCaseRotations (int pattern)
        {
            return getCaseRotations(cases, pattern);
        }

        /**
         * Checks whether the specified entry qualifies as a wall tile.
         */
        public boolean isWall (TileEntry entry, IntTuple caseRotations, int elevation)
        {
            return matchesAny(cases, entry, caseRotations, elevation);
        }

        /**
         * Creates a new wall tile with the supplied case/rotations and maximum dimensions.
         */
        public TileEntry createWall (
            ConfigManager cfgmgr, IntTuple caseRotations, int maxWidth, int maxHeight)
        {
            return createRandomEntry(
                cfgmgr, cases[caseRotations.left].tiles,
                caseRotations.right, maxWidth, maxHeight);
        }

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            for (Case caze : cases) {
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
            for (Case caze : cases) {
                caze.invalidate();
            }
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The wall reference. */
        @Editable(nullable=true)
        public ConfigReference<WallConfig> wall;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(WallConfig.class, wall);
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            WallConfig config = cfgmgr.getConfig(WallConfig.class, wall);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }
    }

    /** The actual wall implementation. */
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
