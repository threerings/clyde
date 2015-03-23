//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
        @Editable(min=0, weight=-1, hgroup="b")
        public int depthBits;

        /** The number of stencil bits to request. */
        @Editable(min=0, weight=-1, hgroup="b")
        public int stencilBits;

        /**
         * Retrieves the texture renderer for this config.
         */
        public TextureRenderer getTextureRenderer (GlContext ctx)
        {
            TextureConfig cconfig = ctx.getConfigManager().getConfig(TextureConfig.class, color);
            com.threerings.opengl.renderer.Texture ctex =
                (cconfig == null) ? null : cconfig.getTexture(ctx);
            TextureConfig dconfig = ctx.getConfigManager().getConfig(TextureConfig.class, depth);
            com.threerings.opengl.renderer.Texture dtex =
                (dconfig == null) ? null : dconfig.getTexture(ctx);
            int alphaBits = (ctex != null && ctex.hasAlpha()) ? 1 : 0;
            return TextureRenderer.getInstance(
                ctx, ctex, dtex, new PixelFormat(alphaBits, depthBits, stencilBits));
        }

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            TextureConfig cconfig = ctx.getConfigManager().getConfig(TextureConfig.class, color);
            TextureConfig dconfig = ctx.getConfigManager().getConfig(TextureConfig.class, depth);
            return (cconfig != null || dconfig != null) &&
                (cconfig == null || cconfig.isSupported(ctx, fallback)) &&
                (dconfig == null || dconfig.isSupported(ctx, fallback));
        }

        @Override
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
        @Override
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

    @Deprecated
    public void getUpdateReferences (ConfigReferenceSet refs)
    {
        // nothing by default
    }

    /**
     * Determines whether this target config is supported by the hardware.
     */
    public boolean isSupported (GlContext ctx, boolean fallback)
    {
        for (StepConfig step : steps) {
            if (!step.isSupported(ctx, fallback)) {
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
