//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigManager;
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
    }

    /**
     * Superclass of the original implementations.
     */
    public static abstract class Original extends Implementation
    {
        /**
         * Returns the name of the server-side logic class to use for the global, or
         * <code>null</code> for none.
         */
        public String getLogicClassName ()
        {
            return null;
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
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
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
