//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.opengl.scene.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.openal.Sounder;
import com.threerings.openal.config.SounderConfig;
import com.threerings.opengl.compositor.RenderEffect;
import com.threerings.opengl.compositor.config.RenderEffectConfig;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.scene.BackgroundColorEffect;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.scene.ViewerEffect;
import com.threerings.opengl.util.GlContext;

/**
 * The configuration of an effect.
 */
@EditorTypes({
    ViewerEffectConfig.Sound.class, ViewerEffectConfig.BackgroundColor.class,
    ViewerEffectConfig.Skybox.class, ViewerEffectConfig.Particles.class,
    ViewerEffectConfig.RenderEffect.class })
public abstract class ViewerEffectConfig extends DeepObject
    implements Exportable
{
    /**
     * Plays a sound.
     */
    public static class Sound extends ViewerEffectConfig
    {
        /** The configuration of the sounder that will play the sound. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;

        @Override // documentation inherited
        public ViewerEffect createViewerEffect (GlContext ctx, Scope scope)
        {
            if (!ScopeUtil.resolve(scope, "soundEnabled", true)) {
                return createNoopEffect();
            }
            Transform3D transform = ScopeUtil.resolve(scope, "worldTransform", new Transform3D());
            final Sounder sounder = new Sounder(ctx, scope, transform, this.sounder);
            return new ViewerEffect() {
                public void activate (Scene scene) {
                    sounder.start();
                }
                public void deactivate () {
                    sounder.stop();
                }
                public void update () {
                    sounder.update();
                }
            };
        }
    }

    /**
     * Changes the background color.
     */
    public static class BackgroundColor extends ViewerEffectConfig
    {
        /** The background color. */
        @Editable
        public Color4f color = new Color4f(0f, 0f, 0f, 1f);

        @Override // documentation inherited
        public ViewerEffect createViewerEffect (GlContext ctx, Scope scope)
        {
            return new BackgroundColorEffect(color);
        }
    }

    /**
     * Sets the skybox.
     */
    public static class Skybox extends ViewerEffectConfig
    {
        /** The configuration of the skybox model. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The skybox translation scale. */
        @Editable(step=0.001, hgroup="t")
        public Vector3f translationScale = new Vector3f();

        /** The skybox translation origin. */
        @Editable(step=0.01, hgroup="t")
        public Vector3f translationOrigin = new Vector3f();

        @Override // documentation inherited
        public ViewerEffect createViewerEffect (GlContext ctx, Scope scope)
        {
            final Model model = new Model(ctx, this.model);
            final Vector3f translation =
                ctx.getCompositor().getCamera().getWorldTransform().getTranslation();
            return new ViewerEffect() {
                public void activate (Scene scene) {
                    (_scene = scene).add(model);
                }
                public void deactivate () {
                    _scene.remove(model);
                    _scene = null;
                }
                public void update () {
                    Vector3f trans = model.getLocalTransform().getTranslation();
                    translationOrigin.subtract(translation, trans).multLocal(translationScale);
                    trans.addLocal(translation);
                    model.updateBounds();
                }
                protected Scene _scene;
            };
        }
    }

    /**
     * Adds a particle effect.
     */
    public static class Particles extends ViewerEffectConfig
    {
        /** The configuration of the particle system model. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        @Override // documentation inherited
        public ViewerEffect createViewerEffect (GlContext ctx, Scope scope)
        {
            final Model model = new Model(ctx, this.model);
            final Transform3D transform = ctx.getCompositor().getCamera().getWorldTransform();
            return new ViewerEffect() {
                public void activate (Scene scene) {
                    (_scene = scene).add(model);
                }
                public void deactivate () {
                    _scene.remove(model);
                    _scene = null;
                }
                public void update () {
                    model.setLocalTransform(transform);
                }
                protected Scene _scene;
            };
        }
    }

    /**
     * Adds a render effect.
     */
    public static class RenderEffect extends ViewerEffectConfig
    {
        /** The configuration of the render effect. */
        @Editable(nullable=true)
        public ConfigReference<RenderEffectConfig> renderEffect;

        @Override // documentation inherited
        public ViewerEffect createViewerEffect (final GlContext ctx, Scope scope)
        {
            RenderEffectConfig config = ctx.getConfigManager().getConfig(
                RenderEffectConfig.class, renderEffect);
            final com.threerings.opengl.compositor.RenderEffect effect =
                new com.threerings.opengl.compositor.RenderEffect(ctx, scope, config);
            return new ViewerEffect() {
                public void activate (Scene scene) {
                    ctx.getCompositor().addEffect(effect);
                }
                public void deactivate () {
                    ctx.getCompositor().removeEffect(effect);
                }
            };
        }
    }

    /**
     * Creates the actual effect object.
     */
    public abstract ViewerEffect createViewerEffect (GlContext ctx, Scope scope);

    /**
     * Creates an effect that does nothing.
     */
    protected static ViewerEffect createNoopEffect ()
    {
        return new ViewerEffect() { };
    }
}
