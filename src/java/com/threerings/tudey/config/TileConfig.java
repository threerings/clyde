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

import com.threerings.opengl.model.config.ModelConfig;

/**
 * The configuration of a tile.
 */
public class TileConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the tile.
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
     * An original tile implementation.
     */
    public static class Original extends Implementation
    {
        /** The width of the tile. */
        @Editable(min=1, hgroup="d")
        public int width = 1;

        /** The height of the tile. */
        @Editable(min=1, hgroup="d")
        public int height = 1;

        /** The model to use for the tile. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** Indicates where the tile is passable. */
        @Editable(width=3)
        public boolean[][] passable = new boolean[][] { { false } };

        /** Indicates where the tile is penetrable. */
        @Editable(width=3)
        public boolean[][] penetrable = new boolean[][] { { false } };
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The tile reference. */
        @Editable(nullable=true)
        public ConfigReference<TileConfig> tile;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(TileConfig.class, tile);
        }
    }

    /** The actual tile implementation. */
    @Editable
    public Implementation implementation = new Original();

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
