//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.sprite.GlobalSprite;

/**
 * The configuration of a global scene object.
 */
public class SceneGlobalConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the global.
     */
    @EditorTypes({ EnvironmentModel.class, Derived.class })
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
         * Creates or updates a sprite implementation for this configuration.
         *
         * @param scope the global's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract GlobalSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, GlobalSprite.Implementation impl);
    }

    /**
     * A simple environment model.
     */
    public static class EnvironmentModel extends Implementation
    {
        /** The model to load. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The transform to apply to the model. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();

        @Override // documentation inherited
        public GlobalSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, GlobalSprite.Implementation impl)
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
        public GlobalSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, GlobalSprite.Implementation impl)
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
     * Creates or updates a sprite implementation for this configuration.
     *
     * @param scope the global's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public GlobalSprite.Implementation getSpriteImplementation (
        GlContext ctx, Scope scope, GlobalSprite.Implementation impl)
    {
        return implementation.getSpriteImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
