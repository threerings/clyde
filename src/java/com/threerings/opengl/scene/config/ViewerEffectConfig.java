//
// $Id$

package com.threerings.opengl.scene.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.openal.Sounder;
import com.threerings.openal.config.SounderConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.scene.BackgroundColorEffect;
import com.threerings.opengl.scene.ViewerEffect;
import com.threerings.opengl.util.GlContext;

/**
 * The configuration of an effect.
 */
@EditorTypes({ ViewerEffectConfig.Sound.class, ViewerEffectConfig.BackgroundColor.class })
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
            final Sounder sounder = new Sounder(ctx, scope, this.sounder);
            return new ViewerEffect() {
                public void activate () {
                    sounder.start();
                }
                public void deactivate () {
                    sounder.stop();
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
     * Creates the actual effect object.
     */
    public abstract ViewerEffect createViewerEffect (GlContext ctx, Scope scope);
}
