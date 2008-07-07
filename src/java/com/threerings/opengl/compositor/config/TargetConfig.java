//
// $Id$

package com.threerings.opengl.compositor.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.config.TextureConfig;

/**
 * Represents a single target to update within the post effect.
 */
@EditorTypes({ TargetConfig.Texture.class })
public abstract class TargetConfig extends DeepObject
    implements Exportable
{
    /** The available target inputs: either nothing or the result of the previous post effect. */
    public enum Input { NONE, PREVIOUS };

    /**
     * Renders to a texture.
     */
    public static class Texture extends TargetConfig
    {
        /** The texture to which we render. */
        @Editable(weight=-1, nullable=true)
        public ConfigReference<TextureConfig> texture;
    }

    /**
     * Renders to the post effect output.
     */
    public static class Output extends TargetConfig
    {
    }

    /** The input to the target. */
    @Editable
    public Input input = Input.PREVIOUS;

    /** The steps required to update the target. */
    @Editable
    public StepConfig[] steps = new StepConfig[0];
}
