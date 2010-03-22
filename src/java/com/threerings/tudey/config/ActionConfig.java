//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

import com.threerings.io.Streamable;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.util.Coord;

/**
 * Configurations for server-side actions.
 */
@EditorTypes({
    ActionConfig.SpawnActor.class, ActionConfig.SpawnRotatedActor.class,
    ActionConfig.SpawnTransformedActor.class, ActionConfig.DestroyActor.class,
    ActionConfig.WarpActor.class, ActionConfig.WarpTransformedActor.class,
    ActionConfig.FireEffect.class, ActionConfig.Signal.class,
    ActionConfig.MoveBody.class, ActionConfig.MoveAll.class,
    ActionConfig.Conditional.class, ActionConfig.Compound.class,
    ActionConfig.Random.class, ActionConfig.Delayed.class,
    ActionConfig.StepLimitMobile.class })
public abstract class ActionConfig extends DeepObject
    implements Exportable, Streamable
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
            if (preloads.add(new Preloadable.Config(ActorConfig.class, actor))) {
                ActorConfig.Original original = getOriginal(cfgmgr);
                if (original != null) {
                    original.getPreloads(cfgmgr, preloads);
                }
            }
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            location.invalidate();
        }

        /**
         * Gets the original configuration of the spawned actor.
         */
        public ActorConfig.Original getOriginal (ConfigManager cfgmgr)
        {
            ActorConfig config = cfgmgr.getConfig(ActorConfig.class, actor);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }
    }

    /**
     * Spawns a new actor with a fixed rotation.
     */
    public static class SpawnRotatedActor extends SpawnActor
    {
        /** The fixed rotation for the new actor. */
        @Editable(min=-180, max=+180, scale=Math.PI/180.0, hgroup="r")
        public float rotation = 0;

        /** If the rotation should be relative to the target. */
        @Editable(hgroup="r")
        public boolean relative = false;

        /** The random rotation variance. */
        @Editable(min=0, max=360, scale=Math.PI/180.0)
        public float rotationVariance = 0;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$SpawnRotatedActor";
        }
    }

    /**
     * Spawns a new actor that can have a translation and rotation.
     */
    public static class SpawnTransformedActor extends SpawnRotatedActor
    {
        /** The translation from the target for the new actor. */
        @Editable
        public Vector2f translation = new Vector2f();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$SpawnTransformedActor";
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

        @Override // documentation inherited
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Warps an actor from one place to another.
     */
    public static class WarpActor extends ActionConfig
    {
        /** The actor to warp. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        /** The location to which the actor will be warped. */
        @Editable
        public TargetConfig location = new TargetConfig.Tagged();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$WarpActor";
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            target.invalidate();
            location.invalidate();
        }
    }

    /**
     * Warps an actor from on place to another transformed location.
     */
    public static class WarpTransformedActor extends WarpActor
    {
        /** The fixed rotation for the new actor. */
        @Editable(min=-180, max=+180, scale=Math.PI/180.0, hgroup="t")
        public float rotation = 0;

        /** The translation from the target for the new actor. */
        @Editable(hgroup="t")
        public Vector2f translation = new Vector2f();

        /** If the transform should be relative to the target. */
        @Editable(hgroup="r")
        public boolean rotatedTranslation = true;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$WarpTransformedActor";
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

        @Override // documentation inherited
        public void invalidate ()
        {
            location.invalidate();
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

        @Override // documentation inherited
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Superclass of the portal actions.
     */
    public static abstract class AbstractMove extends ActionConfig
    {
        /** The id of the destination scene. */
        @Editable(min=0)
        public int sceneId;

        /** The key of the portal in the destination scene. */
        @Editable
        public Portal portal = new TaggedPortal();
    }

    /**
     * Identifies a portal within the target scene.
     */
    @EditorTypes({ TaggedPortal.class, TilePortal.class, EntryPortal.class })
    public static abstract class Portal extends DeepObject
        implements Exportable
    {
        /**
         * Returns the key identifying the portal.
         */
        public abstract Object getKey ();
    }

    /**
     * A portal identified by its tag.
     */
    public static class TaggedPortal extends Portal
    {
        /** The portal tag. */
        @Editable
        public String tag = "";

        @Override // documentation inherited
        public Object getKey ()
        {
            return tag;
        }
    }

    /**
     * A tile portal identified by its coordinates.
     */
    public static class TilePortal extends Portal
    {
        /** The tile coordinates. */
        @Editable(hgroup="c")
        public int x, y;

        @Override // documentation inherited
        public Object getKey ()
        {
            return new Coord(x, y);
        }
    }

    /**
     * A (non-tile) scene model entry portal identified by its entry id.
     */
    public static class EntryPortal extends Portal
    {
        /** The entry id. */
        @Editable(min=0)
        public int id;

        @Override // documentation inherited
        public Object getKey ()
        {
            return id;
        }
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

        @Override // documentation inherited
        public void invalidate ()
        {
            target.invalidate();
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

        /** The action to take if the condition is not satisfied. */
        @Editable(nullable=true)
        public ActionConfig elseAction;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Conditional";
        }

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            action.getPreloads(cfgmgr, preloads);
            if (elseAction != null) {
                elseAction.getPreloads(cfgmgr, preloads);
            }
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            condition.invalidate();
            action.invalidate();
            if (elseAction != null) {
                elseAction.invalidate();
            }
        }

        @Override // documentation inherited
        public ActionConfig[] getSubActions ()
        {
            ActionConfig[] actions = new ActionConfig[(elseAction == null ? 1 : 2)];
            actions[0] = action;
            if (elseAction != null) {
                actions[1] = elseAction;
            }
            return actions;
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

        @Override // documentation inherited
        public void invalidate ()
        {
            for (ActionConfig action : actions) {
                action.invalidate();
            }
        }

        @Override // documentation inherited
        public ActionConfig[] getSubActions ()
        {
            return actions;
        }
    }

    /**
     * Executes an action from a weighted list.
     */
    public static class Random extends ActionConfig
    {
        /** The contained actions. */
        @Editable
        public WeightedAction[] actions = new WeightedAction[0];

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Random";
        }

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            for (WeightedAction waction : actions) {
                waction.action.getPreloads(cfgmgr, preloads);
            }
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            for (WeightedAction waction : actions) {
                waction.action.invalidate();
            }
        }

        @Override // documentation inherited
        public ActionConfig[] getSubActions ()
        {
            ActionConfig[] subActions = new ActionConfig[actions.length];
            for (int ii = 0; ii < actions.length; ii++) {
                subActions[ii] = actions[ii].action;
            }
            return subActions;
        }
    }

    /**
     * Combines an action with a weight.
     */
    public static class WeightedAction extends DeepObject
        implements Exportable
    {
        /** The weight of the action. */
        @Editable(min=0, step=0.01)
        public float weight = 1f;

        /** The action itself. */
        @Editable
        public ActionConfig action = new SpawnActor();
    }

    /**
     * Executes an action after some delay.
     */
    public static class Delayed extends ActionConfig
    {
        /** The delay. */
        @Editable(min=0)
        public int delay;

        /** The action to perform. */
        @Editable
        public ActionConfig action = new SpawnActor();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Delayed";
        }

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            action.getPreloads(cfgmgr, preloads);
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            action.invalidate();
        }

        @Override // documentation inherited
        public ActionConfig[] getSubActions ()
        {
            ActionConfig[] subActions = new ActionConfig[1];
            subActions[0] = action;
            return subActions;
        }
    }

    /**
     * Sets a step limiter on mobile.
     */
    public static class StepLimitMobile extends ActionConfig
    {
        /** If we're setting or removing the limit. */
        @Editable
        public boolean remove = false;

        /** The minimum direction. */
        @Editable(min=-180.0, max=+180.0, scale=Math.PI/180.0, hgroup="m")
        public float minDirection;

        /** The maximum direction. */
        @Editable(min=-180.0, max=+180.0, scale=Math.PI/180.0, hgroup="m")
        public float maxDirection;

        /** The mobile to step limit. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$StepLimitMobile";
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            target.invalidate();
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

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }

    /**
     * Returns an array of any contained ActionConfigs.
     */
    public ActionConfig[] getSubActions ()
    {
        return null;
    }
}
