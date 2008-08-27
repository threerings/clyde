//
// $Id$

package com.threerings.tudey.data;

import com.threerings.util.DeepObject;

import com.threerings.tudey.util.TudeyContext;

/**
 * Identifies a resource to be preloaded and pinned in the cache before the scene is started.
 */
public abstract class SceneResource extends DeepObject
{
    /**
     * A model resource.
     */
    public static class Model extends SceneResource
    {
        /** The name of the model. */
        public String name;

        /** The model variant. */
        public String variant;

        public Model (String name)
        {
            int idx = name.indexOf('|');
            if (idx == -1) {
                this.name = name;
            } else {
                this.name = name.substring(0, idx);
                variant = name.substring(idx + 1);
            }
        }

        public Model (String name, String variant)
        {
            this.name = name;
            this.variant = variant;
        }

        @Override // documentation inherited
        public void preload (TudeyContext ctx)
        {
            _prototype = ctx.getModelCache().getPrototype(name, variant);
        }

        /** The model prototype. */
        protected com.threerings.opengl.model.Model _prototype;
    }

    /**
     * An animation resource.
     */
    public static class Animation extends SceneResource
    {
        /** The name of the animation. */
        public String name;

        public Animation (String name)
        {
            this.name = name;
        }

        @Override // documentation inherited
        public void preload (TudeyContext ctx)
        {
            _anim = ctx.getModelCache().getAnimation(name);
        }

        /** The animation. */
        protected com.threerings.opengl.model.Animation _anim;
    }

    /**
     * A UI image resource.
     */
    public static class Image extends SceneResource
    {
        /** The path of the image. */
        public String path;

        public Image (String path)
        {
            this.path = path;
        }

        @Override // documentation inherited
        public void preload (TudeyContext ctx)
        {
            _image = new com.threerings.opengl.gui.
                Image(ctx.getImageCache().getBufferedImage(path));
        }

        /** The image object. */
        protected com.threerings.opengl.gui.Image _image;
    }

    /**
     * A sound effect resource.
     */
    public static class Sound extends SceneResource
    {
        /** The name of the effect. */
        public String name;

        public Sound (String name)
        {
            this.name = name;
        }

        @Override // documentation inherited
        public void preload (TudeyContext ctx)
        {
            ctx.getSoundManager().loadClip(ctx.getClipProvider(), "sound/effect/" + name + ".ogg");
        }
    }

    /**
     * Preloads the resource and retains an internal reference to it, effectively pinning it within
     * the cache (preventing it from being garbage collected).
     */
    public abstract void preload (TudeyContext ctx);
}
