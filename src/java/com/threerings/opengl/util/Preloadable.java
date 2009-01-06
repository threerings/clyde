//
// $Id$

package com.threerings.opengl.util;

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.config.ModelConfig;

/**
 * Represents a resource to be preloaded and "pinned" in the cache by retaining a strong reference.
 */
public abstract class Preloadable extends DeepObject
{
    /**
     * A generic config resource.
     */
    public static class Config extends Preloadable
    {
        /**
         * Creates a new config resource.
         */
        public <T extends ManagedConfig> Config (Class<T> clazz, String name)
        {
            this(clazz, new ConfigReference<T>(name));
        }

        /**
         * Creates a new config resource.
         */
        public <T extends ManagedConfig> Config (Class<T> clazz, ConfigReference<T> ref)
        {
            @SuppressWarnings("unchecked") Class<ManagedConfig> mclazz =
                (Class<ManagedConfig>)clazz;
            _clazz = mclazz;
            @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> mref =
                (ConfigReference<ManagedConfig>)ref;
            _ref = mref;
        }

        @Override // documentation inherited
        public void preload (GlContext ctx)
        {
            _config = ctx.getConfigManager().getConfig(_clazz, _ref);
        }

        /** The configuration class. */
        protected Class<ManagedConfig> _clazz;

        /** The configuration reference. */
        protected ConfigReference<ManagedConfig> _ref;

        /** The reference to the resolved configuration. */
        protected ManagedConfig _config;
    }

    /**
     * A model resource.
     */
    public static class Model extends Preloadable
    {
        /**
         * Creates a new model resource.
         */
        public Model (ConfigReference<ModelConfig> ref)
        {
            _ref = ref;
        }

        @Override // documentation inherited
        public void preload (GlContext ctx)
        {
            _model = new com.threerings.opengl.model.Model(ctx, _ref);
        }

        /** The model config reference. */
        protected ConfigReference<ModelConfig> _ref;

        /** The model prototype. */
        protected com.threerings.opengl.model.Model _model;
    }

    /**
     * Preloads this resource and creates a reference to it, preventing it from being
     * garbage-collected.
     */
    public abstract void preload (GlContext ctx);
}
