//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.expr.Scope;

import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.GlContext;

/**
 * A model config wrapper for using an actor's model config.
 */
public abstract class ActorModelConfig extends ModelConfig
{
    /**
     * An actor model config wrapper.
     */
    public static class Wrapper extends Implementation
    {
        /** The actor reference. */
        @Editable(nullable=true)
        public ConfigReference<ActorConfig> actor;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(ActorConfig.class, actor);
        }

        @Override // documentation inherited
        public ConfigManager getConfigManager (ConfigManager cfgmgr)
        {
            ModelConfig config = getModelConfig(cfgmgr);
            return (config == null) ? cfgmgr : config.getConfigManager();
        }

        @Override // documentation inherited
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            ModelConfig config = getModelConfig(ctx.getConfigManager());
            return (config == null) ? null : config.getModelImplementation(ctx, scope, impl);
        }

        @Override // documentation inherited
        public GeometryConfig getParticleGeometry (GlContext ctx)
        {
            ModelConfig config = getModelConfig(ctx.getConfigManager());
            return (config == null) ? null : config.getParticleGeometry(ctx);
        }

        @Override // documentation inherited
        public ConfigReference<MaterialConfig> getParticleMaterial (GlContext ctx)
        {
            ModelConfig config = getModelConfig(ctx.getConfigManager());
            return (config == null) ? null : config.getParticleMaterial(ctx);
        }

        /**
         * Get the model config from the actor.
         */
        protected ModelConfig getModelConfig (ConfigManager cfgmgr)
        {
            ActorConfig config = cfgmgr.getConfig(ActorConfig.class, actor);
            if (config != null) {
                ActorConfig.Original original = config.getOriginal(cfgmgr);
                return cfgmgr.getConfig(ModelConfig.class, original.sprite.model);
            }
            return null;
        }
    }
}
