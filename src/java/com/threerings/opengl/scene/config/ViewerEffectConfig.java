//
// $Id$

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
import com.threerings.opengl.compositor.config.PostEffectConfig;
import com.threerings.opengl.mod.Model;
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
    ViewerEffectConfig.PostEffect.class })
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
                    model.getLocalTransform().getTranslation().set(translation);
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
     * Adds a post effect.
     */
    public static class PostEffect extends ViewerEffectConfig
    {
        /** The configuration of the post effect. */
        @Editable(nullable=true)
        public ConfigReference<PostEffectConfig> postEffect;

        @Override // documentation inherited
        public ViewerEffect createViewerEffect (GlContext ctx, Scope scope)
        {
            return new ViewerEffect() {
                public void activate (Scene scene) {
                }
                public void deactivate () {
                }
            };
        }
    }

    /**
     * Creates the actual effect object.
     */
    public abstract ViewerEffect createViewerEffect (GlContext ctx, Scope scope);
}
