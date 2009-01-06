//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

/**
 * Configurations for server-side actions.
 */
@EditorTypes({
    ActionConfig.SpawnActor.class, ActionConfig.FireEffect.class,
    ActionConfig.Compound.class })
public abstract class ActionConfig extends DeepObject
    implements Exportable
{
    /**
     * Spawns a new actor.
     */
    public static class SpawnActor extends ActionConfig
    {
        /** The configuration of the actor to spawn. */
        @Editable(nullable=true)
        public ConfigReference<ActorConfig> actor;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$SpawnActor";
        }

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            ActorConfig config = cfgmgr.getConfig(ActorConfig.class, actor);
            ActorConfig.Original original = (config == null) ? null : config.getOriginal(cfgmgr);
            if (original != null) {
                original.getPreloads(cfgmgr, preloads);
            }
        }
    }

    /**
     * Fires an effect.
     */
    public static class FireEffect extends ActionConfig
    {
        /** The configuration of the effect to fire. */
        @Editable(nullable=true)
        public ConfigReference<EffectConfig> effect;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$FireEffect";
        }

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            EffectConfig config = cfgmgr.getConfig(EffectConfig.class, effect);
            EffectConfig.Original original = (config == null) ? null : config.getOriginal(cfgmgr);
            if (original != null) {
                original.getPreloads(cfgmgr, preloads);
            }
        }
    }

    /**
     * Executes multiple actions simultaneously.
     */
    public static class Compound extends ActionConfig
    {
        /** The actions to execute. */
        @Editable
        public ActionConfig[] actions = new ActionConfig[0];

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Compound";
        }

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            for (ActionConfig action : actions) {
                action.getPreloads(cfgmgr, preloads);
            }
        }
    }

    /**
     * Returns the name of the server-side logic class for this action.
     */
    public abstract String getLogicClassName ();

    /**
     * Adds the resources to preload for this action into the provided set.
     */
    public abstract void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads);
}
