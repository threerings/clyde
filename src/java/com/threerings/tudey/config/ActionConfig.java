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
    ActionConfig.SpawnActor.class, ActionConfig.DestroyActor.class,
    ActionConfig.FireEffect.class, ActionConfig.Signal.class,
    ActionConfig.MoveBody.class, ActionConfig.MoveAll.class,
    ActionConfig.Conditional.class, ActionConfig.Compound.class })
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

        /** The location at which to spawn the actor. */
        @Editable
        public TargetConfig location = new TargetConfig.Source();

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
     * Destroys an actor.
     */
    public static class DestroyActor extends ActionConfig
    {
        /** The actor to destroy. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$DestroyActor";
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

        /** The location at which to fire the effect. */
        @Editable
        public TargetConfig location = new TargetConfig.Source();

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
     * Transmits a signal.
     */
    public static class Signal extends ActionConfig
    {
        /** The signal name. */
        @Editable
        public String name = "";

        /** The signal target. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Signal";
        }
    }

    /**
     * Superclass of the portal actions.
     */
    public static abstract class AbstractMove extends ActionConfig
    {
        /** The id of the destination scene. */
        public int sceneId;

        /** The key of the portal in the destination scene. */
        public Object portalKey;
    }

    /**
     * Moves a player pawn to a new scene.
     */
    public static class MoveBody extends AbstractMove
    {
        /** The pawn to move. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$MoveBody";
        }
    }

    /**
     * Moves all players to a new scene.
     */
    public static class MoveAll extends AbstractMove
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$MoveAll";
        }
    }

    /**
     * Executes a sub-action if a condition is satisfied.
     */
    public static class Conditional extends ActionConfig
    {
        /** The condition that must be satisfied. */
        @Editable
        public ConditionConfig condition = new ConditionConfig.Tagged();

        /** The action to take if the condition is satisfied. */
        @Editable
        public ActionConfig action = new ActionConfig.SpawnActor();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Conditional";
        }

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            action.getPreloads(cfgmgr, preloads);
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
    public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
    {
        // nothing by default
    }
}
