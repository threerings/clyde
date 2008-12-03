//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

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
    }

    /**
     * Returns the name of the server-side logic class for this action.
     */
    public abstract String getLogicClassName ();
}
