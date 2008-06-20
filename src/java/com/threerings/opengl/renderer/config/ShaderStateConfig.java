//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.state.ShaderState;

/**
 * Configurable shader state.
 */
public abstract class ShaderStateConfig extends DeepObject
    implements Exportable
{
    /**
     * Disables the shader.
     */
    public static class Disabled extends ShaderStateConfig
    {
        @Override // documentation inherited
        public ShaderState getState ()
        {
            return ShaderState.DISABLED;
        }
    }

    /**
     * Enables the shader.
     */
    public static class Enabled extends ShaderStateConfig
    {
        /** The vertex shader to use. */
        @Editable
        public ConfigReference<ShaderConfig> vertex;

        /** The fragment shader to use. */
        @Editable
        public ConfigReference<ShaderConfig> fragment;

        @Override // documentation inherited
        public ShaderState getState ()
        {
            return null;
        }
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { Disabled.class, Enabled.class };
    }

    /**
     * Returns the corresponding shader state.
     */
    public abstract ShaderState getState ();
}
