//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * The configuration of a wall type.
 */
public class WallConfig extends ParameterizedConfig
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
    }

    /**
     * An original wall implementation.
     */
    public static class Original extends Implementation
    {
        /** The wall cases. */
        @Editable
        public Case[] cases = new Case[0];
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
    }

    /**
     * Represents a single case.
     */
    public static class Case extends DeepObject
        implements Exportable
    {
        /** The tiles for this case. */
        @Editable
        public Tile[] tiles = new Tile[0];
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

        /** The weight of the tile (affects how often it occurs). */
        @Editable(min=0, step=0.01)
        public float weight = 1f;
    }

    /** The actual wall implementation. */
    @Editable
    public Implementation implementation = new Original();

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
