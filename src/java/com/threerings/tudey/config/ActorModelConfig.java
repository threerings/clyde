//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;

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
    public static class Wrapper extends BaseWrapper
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
