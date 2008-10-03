//
// $Id$

package com.threerings.tudey.tools;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;

import com.threerings.tudey.config.WallConfig;

/**
 * The wall brush tool.
 */
public class WallBrush extends ConfigTool<WallConfig>
{
    /**
     * Creates the wall brush tool.
     */
    public WallBrush (SceneEditor editor)
    {
        super(editor, WallConfig.class, new WallReference());
    }

    /**
     * Allows us to edit the wall reference.
     */
    protected static class WallReference extends EditableReference<WallConfig>
    {
        /** The wall reference. */
        @Editable(nullable=true)
        public ConfigReference<WallConfig> wall;

        @Override // documentation inherited
        public ConfigReference<WallConfig> getReference ()
        {
            return wall;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference<WallConfig> ref)
        {
            wall = ref;
        }
    }
}
