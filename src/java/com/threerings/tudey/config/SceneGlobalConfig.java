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

package com.threerings.tudey.config;

import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.FloatMath;
import com.threerings.math.SphereCoords;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.sprite.GlobalSprite;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of a global scene object.
 */
public class SceneGlobalConfig extends ParameterizedConfig
{
    /** Used when we can't resolve the global's underlying original implementation. */
    public static final Original NULL_ORIGINAL = new EnvironmentModel();

    /**
     * Contains the actual implementation of the global.
     */
    @EditorTypes({ EnvironmentModel.class, Camera.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Adds the implementation's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Returns a reference to the config's underlying original implementation.
         */
        public abstract Original getOriginal (ConfigManager cfgmgr);

        /**
         * Creates or updates a sprite implementation for this configuration.
         *
         * @param scope the global's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract GlobalSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, GlobalSprite.Implementation impl);

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * Superclass of the original implementations.
     */
    public static abstract class Original extends Implementation
    {
        /** Tags used to identify the global within the scene. */
        @Editable
        public TagConfig tags = new TagConfig();

        /** The global's event handlers. */
        @Editable
        public HandlerConfig[] handlers = new HandlerConfig[0];

        /**
         * Returns the name of the server-side logic class to use for the global, or
         * <code>null</code> for none.
         */
        public String getLogicClassName ()
        {
            return (tags.getLength() == 0 && handlers.length == 0) ? null :
                "com.threerings.tudey.server.logic.EntryLogic";
        }

        /**
         * Adds the resources to preload for this global into the provided set.
         */
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            for (HandlerConfig handler : handlers) {
                handler.action.getPreloads(cfgmgr, preloads);
            }
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            for (HandlerConfig handler : handlers) {
                handler.invalidate();
            }
        }
    }

    /**
     * A simple environment model.
     */
    public static class EnvironmentModel extends Original
    {
        /** The model to load. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The transform to apply to the model. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            super.getPreloads(cfgmgr, preloads);
            preloads.add(new Preloadable.Model(model));
        }

        @Override // documentation inherited
        public GlobalSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, GlobalSprite.Implementation impl)
        {
            if (impl instanceof GlobalSprite.EnvironmentModel) {
                ((GlobalSprite.EnvironmentModel)impl).setConfig(this);
            } else {
                impl = new GlobalSprite.EnvironmentModel(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A set of camera parameters that override the defaults.
     */
    public static class Camera extends Original
    {
        /** The vertical field of view. */
        @Editable(min=0.0, max=180.0, scale=Math.PI/180.0, hgroup="f")
        public float fov = FloatMath.PI/3f;

        /** The distance to the near clip plane. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float near = 1f;

        /** The distance to the far clip plane. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float far = 100f;

        /** The coordinates about the target. */
        @Editable
        public SphereCoords coords = new SphereCoords(0f, FloatMath.PI/4f, 10f);

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.EntryLogic$Camera";
        }

        @Override // documentation inherited
        public GlobalSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, GlobalSprite.Implementation impl)
        {
            if (impl instanceof GlobalSprite.Camera) {
                ((GlobalSprite.Camera)impl).setConfig(this);
            } else {
                impl = new GlobalSprite.Camera(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The global reference. */
        @Editable(nullable=true)
        public ConfigReference<SceneGlobalConfig> sceneGlobal;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(SceneGlobalConfig.class, sceneGlobal);
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            SceneGlobalConfig config = cfgmgr.getConfig(SceneGlobalConfig.class, sceneGlobal);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }

        @Override // documentation inherited
        public GlobalSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, GlobalSprite.Implementation impl)
        {
            SceneGlobalConfig config = ctx.getConfigManager().getConfig(
                SceneGlobalConfig.class, sceneGlobal);
            return (config == null) ? null : config.getSpriteImplementation(ctx, scope, impl);
        }
    }

    /** The actual global implementation. */
    @Editable
    public Implementation implementation = new EnvironmentModel();

    /**
     * Returns a reference to the config's underlying original implementation.
     */
    public Original getOriginal (ConfigManager cfgmgr)
    {
        return implementation.getOriginal(cfgmgr);
    }

    /**
     * Creates or updates a sprite implementation for this configuration.
     *
     * @param scope the global's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public GlobalSprite.Implementation getSpriteImplementation (
        TudeyContext ctx, Scope scope, GlobalSprite.Implementation impl)
    {
        return implementation.getSpriteImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
