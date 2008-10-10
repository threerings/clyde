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
        /** The constraint on the north edge. */
        @Editable(nullable=true)
        public Edge north;

        /** The constraint on the west edge. */
        @Editable(nullable=true)
        public Edge west;

        /** The constraint on the south edge. */
        @Editable(nullable=true)
        public Edge south;

        /** The constraint on the east edge. */
        @Editable(nullable=true)
        public Edge east;

        /** The tiles for this case. */
        @Editable
        public Tile[] tiles = new Tile[0];
    }

    /**
     * Represents a constraint on a tile edge.
     */
    public static class Edge extends DeepObject
        implements Exportable
    {
        /** The relative elevation. */
        @Editable
        public int elevation;
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
