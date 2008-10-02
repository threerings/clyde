//
// $Id$

package com.threerings.tudey.tools;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;

import com.threerings.tudey.config.AreaConfig;

/**
 * The area definer tool.
 */
public class AreaDefiner extends ConfigTool<AreaConfig>
{
    /**
     * Creates the area definer tool.
     */
    public AreaDefiner (SceneEditor editor)
    {
        super(editor, AreaConfig.class, new AreaReference());
    }

    /**
     * Allows us to edit the area reference.
     */
    protected static class AreaReference extends EditableReference<AreaConfig>
    {
        /** The area reference. */
        @Editable(nullable=true)
        public ConfigReference<AreaConfig> area;

        @Override // documentation inherited
        public ConfigReference<AreaConfig> getReference ()
        {
            return area;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference<AreaConfig> ref)
        {
            area = ref;
        }
    }
}
