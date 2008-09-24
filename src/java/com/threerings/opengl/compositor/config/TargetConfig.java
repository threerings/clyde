//
// $Id$

package com.threerings.opengl.compositor.config;

import org.lwjgl.opengl.PixelFormat;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.compositor.RenderEffect;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.renderer.Texture;
import com.threerings.opengl.renderer.TextureRenderer;
import com.threerings.opengl.renderer.config.TextureConfig;
import com.threerings.opengl.util.GlContext;

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
     * Renders to a (color and/or depth) texture.
     */
    public static class Texture extends TargetConfig
    {
        /** The color texture to which we render. */
        @Editable(weight=-1, nullable=true)
        public ConfigReference<TextureConfig> color;

        /** The depth texture to which we render. */
        @Editable(weight=-1, nullable=true)
        public ConfigReference<TextureConfig> depth;

        /** The number of depth bits to request. */
        @Editable(min=0)
        public int depthBits;

        /** The number of stencil bits to request. */
        @Editable(min=0)
        public int stencilBits;

        /**
         * Creates the texture renderer for this config.
         */
        public TextureRenderer createTextureRenderer (GlContext ctx)
        {
            TextureConfig cconfig = ctx.getConfigManager().getConfig(TextureConfig.class, color);
            TextureConfig dconfig = ctx.getConfigManager().getConfig(TextureConfig.class, depth);
            Dimension size = (cconfig == null) ? dconfig.getSize(ctx) : cconfig.getSize(ctx);
            return new TextureRenderer(
                ctx, size.width, size.height,
                (cconfig == null) ? null : cconfig.getTexture(ctx),
                (dconfig == null) ? null : dconfig.getTexture(ctx),
                new PixelFormat(0, depthBits, stencilBits));
        }

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(TextureConfig.class, color);
            refs.add(TextureConfig.class, depth);
        }

        @Override // documentation inherited
        public boolean isSupported (GlContext ctx)
        {
            TextureConfig cconfig = ctx.getConfigManager().getConfig(TextureConfig.class, color);
            TextureConfig dconfig = ctx.getConfigManager().getConfig(TextureConfig.class, depth);
            return (cconfig != null || dconfig != null) &&
                (cconfig == null || cconfig.isSupported(ctx)) &&
                (dconfig == null || dconfig.isSupported(ctx));
        }

        @Override // documentation inherited
        public RenderEffect.Target createEffectTarget (GlContext ctx, Scope scope)
        {
            return new RenderEffect.TextureTarget(ctx, scope, this);
        }
    }

    /**
     * Renders to the effect output.
     */
    public static class Output extends TargetConfig
    {
        @Override // documentation inherited
        public RenderEffect.Target createEffectTarget (GlContext ctx, Scope scope)
        {
            return new RenderEffect.OutputTarget(ctx, scope, this);
        }
    }

    /** The input to the target. */
    @Editable
    public Input input = Input.PREVIOUS;

    /** The steps required to update the target. */
    @Editable
    public StepConfig[] steps = new StepConfig[0];

    /**
     * Adds the target's update references to the provided set.
     */
    public void getUpdateReferences (ConfigReferenceSet refs)
    {
        // nothing by default
    }

    /**
     * Determines whether this target config is supported by the hardware.
     */
    public boolean isSupported (GlContext ctx)
    {
        for (StepConfig step : steps) {
            if (!step.isSupported(ctx)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates the effect target for this config.
     */
    public abstract RenderEffect.Target createEffectTarget (GlContext ctx, Scope scope);
}
