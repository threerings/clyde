//
// $Id$

package com.threerings.tudey.tools;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;

import com.threerings.tudey.config.PathConfig;

/**
 * The path definer tool.
 */
public class PathDefiner extends ConfigTool<PathConfig>
{
    /**
     * Creates the path definer tool.
     */
    public PathDefiner (SceneEditor editor)
    {
        super(editor, PathConfig.class, new PathReference());
    }

    /**
     * Allows us to edit the path reference.
     */
    protected static class PathReference extends EditableReference<PathConfig>
    {
        /** The path reference. */
        @Editable(nullable=true)
        public ConfigReference<PathConfig> path;

        @Override // documentation inherited
        public ConfigReference<PathConfig> getReference ()
        {
            return path;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference<PathConfig> ref)
        {
            path = ref;
        }
    }
}
