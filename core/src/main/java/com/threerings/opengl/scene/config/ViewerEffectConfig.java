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

package com.threerings.opengl.scene.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Color4fExpression;
import com.threerings.expr.ObjectExpression.Evaluator;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.openal.Sounder;
import com.threerings.openal.config.SounderConfig;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.compositor.config.RenderEffectConfig;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.scene.BackgroundColorEffect;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.scene.ViewerEffect;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

import com.threerings.tudey.config.TudeyViewerEffectConfig;

/**
 * The configuration of an effect.
 */
@EditorTypes({
    ViewerEffectConfig.Sound.class, ViewerEffectConfig.BackgroundColor.class,
    ViewerEffectConfig.Skybox.class, ViewerEffectConfig.Particles.class,
    ViewerEffectConfig.RenderEffect.class, ViewerEffectConfig.AmbientLightOffset.class,
    TudeyViewerEffectConfig.class })
public abstract class ViewerEffectConfig extends DeepObject
    implements Exportable, Preloadable.LoadableConfig
{
    /**
     * Plays a sound.
     */
    public static class Sound extends ViewerEffectConfig
    {
        /** The configuration of the sounder that will play the sound. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Config(SounderConfig.class, sounder).preload(ctx);
        }

        @Override
        public ViewerEffect getViewerEffect (
            final GlContext ctx, final Scope scope, ViewerEffect effect)
        {
            if (!ScopeUtil.resolve(scope, "soundEnabled", true)) {
                return getNoopEffect(effect);
            }
            class SoundEffect extends ViewerEffect {
                public Sounder sounder = new Sounder(
                    ctx, scope, ScopeUtil.resolve(scope, "worldTransform", new Transform3D()),
                    Sound.this.sounder);
                @Override public void activate (Scene scene) {
                    _activated = true;
                    sounder.start();
                }
                @Override public void deactivate () {
                    sounder.stop();
                    _activated = false;
                }
                @Override public boolean hasCompleted () {
                    return _completed;
                }
                @Override public void update () {
                    sounder.update();
                    if (!(_completed || sounder.isPlaying())) {
                        _completed = true;
                        ((Model)scope.getParentScope()).completed((Model.Implementation)scope);
                    }
                }
                @Override public void reset () {
                    if (_activated) {
                        sounder.start();
                    }
                    _completed = false;
                }
                @Override public boolean omitWhileLoading () {
                    return true;
                }
                @Override public String toString () {
                    return "Sound:" + Sound.this.sounder;
                }
                protected boolean _activated, _completed;
            }
            if (effect instanceof SoundEffect) {
                ((SoundEffect)effect).sounder.setConfig(sounder);
            } else {
                effect = new SoundEffect();
            }
            return effect;
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

        @Override
        public ViewerEffect getViewerEffect (GlContext ctx, Scope scope, ViewerEffect effect)
        {
            if (effect instanceof BackgroundColorEffect) {
                ((BackgroundColorEffect)effect).getBackgroundColor().set(color);
            } else {
                effect = new BackgroundColorEffect(color);
            }
            return effect;
        }

        @Override
        public void preload (GlContext ctx)
        {
            // Do nothing
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

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Model(model).preload(ctx);
        }

        @Override
        public ViewerEffect getViewerEffect (
            final GlContext ctx, final Scope scope, ViewerEffect effect)
        {
            class SkyboxEffect extends ViewerEffect {
                public Model model = new Model(ctx, Skybox.this.model) {
                    @Override public void completed (Model.Implementation impl) {
                        super.completed(impl);
                        ((Model)scope.getParentScope()).completed((Model.Implementation)scope);
                    }
                };
                @Override public void activate (Scene scene) {
                    (_scene = scene).add(model);
                }
                @Override public void deactivate () {
                    _scene.remove(model);
                    _scene = null;
                }
                @Override public void update () {
                    Vector3f trans = model.getLocalTransform().getTranslation();
                    translationOrigin.subtract(_translation, trans).multLocal(translationScale);
                    trans.addLocal(_translation);
                    model.updateBounds();
                }
                @Override public boolean hasCompleted () {
                    return model.hasCompleted();
                }
                @Override public void reset () {
                    model.reset();
                }
                @Override public String toString () {
                    return "Skybox:" + Skybox.this.model;
                }
                protected Vector3f _translation =
                    ctx.getCompositor().getCamera().getWorldTransform().getTranslation();
                protected Scene _scene;
            }
            if (effect instanceof SkyboxEffect) {
                ((SkyboxEffect)effect).model.setConfig(model);
            } else {
                effect = new SkyboxEffect();
            }
            return effect;
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

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Model(model).preload(ctx);
        }

        @Override
        public ViewerEffect getViewerEffect (
            final GlContext ctx, final Scope scope, ViewerEffect effect)
        {
            class ParticlesEffect extends ViewerEffect {
                public Model model = new Model(ctx, Particles.this.model) {
                    @Override public void completed (Model.Implementation impl) {
                        super.completed(impl);
                        ((Model)scope.getParentScope()).completed((Model.Implementation)scope);
                    }
                };
                @Override public void activate (Scene scene) {
                    (_scene = scene).add(model);
                }
                @Override public void deactivate () {
                    _scene.remove(model);
                    _scene = null;
                }
                @Override public void update () {
                    model.setLocalTransform(_transform);
                }
                @Override public boolean hasCompleted () {
                    return model.hasCompleted();
                }
                @Override public void reset () {
                    model.reset();
                }
                @Override public String toString () {
                    return "Particles:" + Particles.this.model;
                }
                protected Transform3D _transform =
                    ctx.getCompositor().getCamera().getWorldTransform();
                protected Scene _scene;
            }
            if (effect instanceof ParticlesEffect) {
                ((ParticlesEffect)effect).model.setConfig(model);
            } else {
                effect = new ParticlesEffect();
            }
            return effect;
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

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Config(RenderEffectConfig.class, renderEffect).preload(ctx);
        }

        @Override
        public ViewerEffect getViewerEffect (
            final GlContext ctx, final Scope scope, ViewerEffect effect)
        {
            class RenderEffectEffect extends ViewerEffect {
                public com.threerings.opengl.compositor.RenderEffect reffect =
                    new com.threerings.opengl.compositor.RenderEffect(ctx, scope, renderEffect);
                @Override public void activate (Scene scene) {
                    ctx.getCompositor().addEffect(reffect);
                }
                @Override public void deactivate () {
                    ctx.getCompositor().removeEffect(reffect);
                }
                @Override public String toString () {
                    return "RenderEffect:" + renderEffect;
                }
            }
            if (effect instanceof RenderEffectEffect) {
                ((RenderEffectEffect)effect).reffect.setConfig(RenderEffect.this.renderEffect);
            } else {
                effect = new RenderEffectEffect();
            }
            return effect;
        }
    }

    /**
     * Offsets the ambient light color.
     */
    public static class AmbientLightOffset extends ViewerEffectConfig
    {
        /** The duration of the effect, or zero for unlimited. */
        @Editable(min=0.0, step=0.01)
        public float duration;

        /** The inner radius of the effect (within which the offset will be at full amplitude). */
        @Editable(min=0.0, step=0.01, hgroup="r")
        public float innerRadius = 10f;

        /** The outer radius of the effect (outside of which the offset will not be felt). */
        @Editable(min=0.0, step=0.01, hgroup="r")
        public float outerRadius = 100f;

        /** The offset amount. */
        @Editable
        public Color4fExpression amount = new Color4fExpression.Constant();

        @Override
        public void preload (GlContext ctx)
        {
            // Do nothing
        }

        @Override
        public ViewerEffect getViewerEffect (
            final GlContext ctx, Scope scope, ViewerEffect effect)
        {
            final LightState lightState = ScopeUtil.resolve(
                scope, "lightState", null, LightState.class);
            if (lightState == null || lightState.getLights() == null) {
                return getNoopEffect(effect);
            }
            final MutableLong epoch = ScopeUtil.resolveTimestamp(scope, Scope.EPOCH);
            final MutableLong now = ScopeUtil.resolveTimestamp(scope, Scope.NOW);
            final Transform3D transform = ScopeUtil.resolve(
                scope, "worldTransform", new Transform3D());
            final Evaluator<Color4f> evaluator = amount.createEvaluator(scope);
            return new ViewerEffect() {
                @Override public void activate (Scene scene) {
                    addCurrent();
                }
                @Override public void deactivate () {
                    subtractLast();
                }
                @Override public void update () {
                    if (_completed) {
                        return;
                    }
                    subtractLast();
                    if (!(_completed = duration > 0f &&
                            (now.value - epoch.value)/1000f >= duration)) {
                        addCurrent();
                    }
                }
                @Override public boolean hasCompleted () {
                    return _completed;
                }
                @Override public void reset () {
                    _completed = false;
                }
                protected void subtractLast () {
                    if (_lastGlobalAmbient != null) {
                        _lastGlobalAmbient.r -= _lastAmount.r;
                        _lastGlobalAmbient.g -= _lastAmount.g;
                        _lastGlobalAmbient.b -= _lastAmount.b;
                        _lastGlobalAmbient = null;
                    }
                }
                protected void addCurrent () {
                    transform.extractTranslation(_translation);
                    CameraHandler camhand = ctx.getCameraHandler();
                    float dist = camhand.getViewerTranslation().distance(_translation);
                    if (dist > outerRadius) {
                        return;
                    }
                    float scale = (dist <= innerRadius) ? 1f :
                        1f - (dist - innerRadius) / (outerRadius - innerRadius);
                    _lastGlobalAmbient = lightState.getGlobalAmbient();
                    _lastAmount.set(evaluator.evaluate()).multLocal(scale);
                    _lastGlobalAmbient.r += _lastAmount.r;
                    _lastGlobalAmbient.g += _lastAmount.g;
                    _lastGlobalAmbient.b += _lastAmount.b;
                }
                protected Color4f _lastAmount = new Color4f();
                protected Color4f _lastGlobalAmbient;
                protected boolean _completed;
                protected Vector3f _translation = new Vector3f();
            };
        }

        @Override
        public void invalidate ()
        {
            amount.invalidate();
        }
    }

    /**
     * Creates the actual effect object.
     *
     * @param effect an effect to reuse, if possible.
     */
    public abstract ViewerEffect getViewerEffect (GlContext ctx, Scope scope, ViewerEffect effect);

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }

    /**
     * Creates an effect that does nothing.
     */
    protected static ViewerEffect getNoopEffect (ViewerEffect effect)
    {
        class NoopEffect extends ViewerEffect {
            // no-op
        }
        return (effect instanceof NoopEffect) ? effect : new NoopEffect();
    }
}
