//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.opengl.util;

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.model.config.AnimationConfig;
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
        @DeepOmit
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
        @DeepOmit
        protected com.threerings.opengl.model.Model _model;
    }

    /**
     * An animation resource.
     */
    public static class Animation extends Preloadable
    {
        /**
         * Creates a new model resource.
         */
        public Animation (ConfigReference<AnimationConfig> ref)
        {
            _ref = ref;
        }

        @Override // documentation inherited
        public void preload (GlContext ctx)
        {
            _anim = new com.threerings.opengl.model.Animation(ctx, ctx.getScope());
            _anim.setConfig(null, _ref);
        }

        /** The animation config reference. */
        protected ConfigReference<AnimationConfig> _ref;

        /** The animation prototype. */
        @DeepOmit
        protected com.threerings.opengl.model.Animation _anim;
    }

    /**
     * Preloads this resource and creates a reference to it, preventing it from being
     * garbage-collected.
     */
    public abstract void preload (GlContext ctx);
}
