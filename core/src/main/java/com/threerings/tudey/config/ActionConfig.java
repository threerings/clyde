//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
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
import com.threerings.editor.Strippable;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;
import com.threerings.probs.FloatVariable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.ActorSprite;
import com.threerings.tudey.util.Coord;

/**
 * Configurations for server-side actions.
 */
@EditorTypes({
    ActionConfig.None.class, ActionConfig.SpawnActor.class, ActionConfig.SpawnRotatedActor.class,
    ActionConfig.SpawnTransformedActor.class, ActionConfig.SpawnRandomTranslatedActor.class,
    ActionConfig.SpawnFacingActor.class,
    ActionConfig.DestroyActor.class, ActionConfig.RotateActor.class,
    ActionConfig.WarpActor.class, ActionConfig.WarpTransformedActor.class,
    ActionConfig.FireEffect.class,
    ActionConfig.Signal.class, ActionConfig.MoveBody.class,
    ActionConfig.MoveAll.class, ActionConfig.Conditional.class,
    ActionConfig.Switch.class, ActionConfig.ExpressionSwitch.class,
    ActionConfig.Compound.class, ActionConfig.Random.class,
    ActionConfig.Delayed.class, ActionConfig.StepLimitMobile.class,
    ActionConfig.SetVariable.class, ActionConfig.SetFlag.class,
    ActionConfig.ForceClientAction.class, ActionConfig.TargetedAction.class,
    ActionConfig.ServerLog.class, ActionConfig.Fail.class })
public abstract class ActionConfig extends DeepObject
    implements Exportable, Streamable
{
    /**
     * Interface for actions that require pre-execution on the owning client.
     */
    public interface PreExecutable
    {
        /**
         * Checks whether we should pre-execute this action on the owning client.
         */
        public boolean shouldPreExecute ();

        /**
         * Pre-executes the action on the owning client.
         */
        public void preExecute (TudeySceneView view, ActorSprite sprite, int timestamp);
    }

    /**
     * A non-action.
     */
    public static class None extends ActionConfig
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$None";
        }
    }

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

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$SpawnActor";
        }

        @Override
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            if (preloads.add(new Preloadable.Config(ActorConfig.class, actor))) {
                ActorConfig.Original original = getOriginal(cfgmgr);
                if (original != null) {
                    original.getPreloads(cfgmgr, preloads);
                }
            }
        }

        @Override
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
        @Strippable
        public float rotation = 0;

        /** If the rotation should be relative to the target. */
        @Editable(hgroup="r")
        @Strippable
        public boolean relative = false;

        /** The random rotation variance. */
        @Editable(min=0, max=360, scale=Math.PI/180.0)
        @Strippable
        public float rotationVariance = 0;

        @Override
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
        @Strippable
        public Vector2f translation = new Vector2f();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$SpawnTransformedActor";
        }
    }

    /**
     * Spawns a number of actors randomly translated from the location.
     */
    public static class SpawnRandomTranslatedActor extends SpawnActor
    {
        /** The number of actors to spawn. */
        @Editable
        @Strippable
        public int count = 1;

        /** The translation step. */
        @Editable(min=0.01, step=0.01, hgroup="s")
        @Strippable
        public float stepSize = 1;

        /** The number of steps to take. */
        @Editable(hgroup="s")
        @Strippable
        public int steps = 1;

        /** The collision mask to use. */
        @Editable(editor="mask", mode="collision", hgroup="c")
        @Strippable
        public int collisionMask = 0x00;

        /** The source collision location. */
        @Editable(hgroup="c")
        @Strippable
        public TargetConfig collisionSource = new TargetConfig.Source();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$SpawnRandomTranslatedActor";
        }

        @Override
        public void invalidate ()
        {
            super.invalidate();
            collisionSource.invalidate();
        }

    }

    /**
     * Spawns a new actor facing a target.
     */
    public static class SpawnFacingActor extends SpawnActor
    {
        /** The location where we'll face the spawned actor. */
        @Editable
        public TargetConfig facing = new TargetConfig.Source();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$SpawnFacingActor";
        }

        @Override
        public void invalidate ()
        {
            facing.invalidate();
        }
    }

    /**
     * Destroys an actor.
     */
    @Strippable
    public static class DestroyActor extends ActionConfig
    {
        /** The actor to destroy. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        /** If this is considered an end of scene destroy. */
        @Editable
        public boolean endScene = false;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$DestroyActor";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Rotates an actor.
     */
    public static class RotateActor extends ActionConfig
    {
        /** The actor to rotate. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        /** The rotation amount. */
        @Editable(min=-180, max=+180, scale=Math.PI/180.0)
        public FloatVariable rotation = new FloatVariable.Uniform(-FloatMath.PI, +FloatMath.PI);

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$RotateActor";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Warps an actor from one place to another.
     */
    @Strippable
    public static class WarpActor extends ActionConfig
    {
        /** The actor to warp. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        /** The location to which the actor will be warped. */
        @Editable
        public TargetConfig location = new TargetConfig.Tagged();

        /** The max warp path. */
        @Editable(min=0)
        public int maxWarpPath = 0;

        @Editable
        public boolean resetMap = false;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$WarpActor";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
            location.invalidate();
        }
    }

    /**
     * Warps an actor from on place to another transformed location.
     */
    @Strippable
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

        @Override
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

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$FireEffect";
        }

        @Override
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            EffectConfig config = cfgmgr.getConfig(EffectConfig.class, effect);
            EffectConfig.Original original = (config == null) ? null : config.getOriginal(cfgmgr);
            if (original != null) {
                original.getPreloads(cfgmgr, preloads);
            }
        }

        @Override
        public void invalidate ()
        {
            location.invalidate();
        }
    }

    /**
     * Transmits a signal.
     */
    @Strippable
    public static class Signal extends ActionConfig
    {
        /** The signal name. */
        @Editable
        public String name = "";

        /** The signal target. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Signal";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Superclass of the portal actions.
     */
    @Strippable
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

        @Override
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

        @Override
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

        @Override
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

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$MoveBody";
        }

        @Override
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
        @Override
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

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Conditional";
        }

        @Override
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            action.getPreloads(cfgmgr, preloads);
            condition.getPreloads(cfgmgr, preloads);
            if (elseAction != null) {
                elseAction.getPreloads(cfgmgr, preloads);
            }
        }

        @Override
        public void invalidate ()
        {
            condition.invalidate();
            action.invalidate();
            if (elseAction != null) {
                elseAction.invalidate();
            }
        }

        @Override
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
     * Executes the first sub-action with a satisfied condition.
     */
    public static class Switch extends ActionConfig
    {
        /** The switch cases. */
        @Editable
        public Case[] cases = new Case[0];

        /** The default action to take if no case is satisfied. */
        @Editable(nullable=true)
        public ActionConfig defaultAction;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Switch";
        }

        @Override
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            for (Case c : cases) {
                c.action.getPreloads(cfgmgr, preloads);
            }
            if (defaultAction != null) {
                defaultAction.getPreloads(cfgmgr, preloads);
            }
        }

        @Override
        public void invalidate ()
        {
            for (Case c : cases) {
                c.condition.invalidate();
                c.action.invalidate();
            }
            if (defaultAction != null) {
                defaultAction.invalidate();
            }
        }

        @Override
        public ActionConfig[] getSubActions ()
        {
            ActionConfig[] actions =
                new ActionConfig[cases.length + (defaultAction == null ? 0 : 1)];
            for (int ii = 0; ii < cases.length; ii++) {
                actions[ii] = cases[ii].action;
            }
            if (defaultAction != null) {
                actions[cases.length] = defaultAction;
            }
            return actions;
        }
    }

    /**
     * A switch case.
     */
    public static class Case extends DeepObject
        implements Streamable, Exportable
    {
        /** The case condition. */
        @Editable
        public ConditionConfig condition = new ConditionConfig.Tagged();

        /** The case action. */
        @Editable
        public ActionConfig action = new ActionConfig.SpawnActor();
    }

    /**
     * Executes the first case whose value equals the switch value.
     */
    public static class ExpressionSwitch extends ActionConfig
    {
        /** The switch value. */
        @Editable
        public ExpressionConfig value = new ExpressionConfig.Constant();

        /** The switch cases. */
        @Editable
        public ExpressionCase[] cases = new ExpressionCase[0];

        /** The default action to take if no case is satisfied. */
        @Editable(nullable=true)
        public ActionConfig defaultAction;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$ExpressionSwitch";
        }

        @Override
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            for (ExpressionCase c : cases) {
                c.action.getPreloads(cfgmgr, preloads);
            }
            if (defaultAction != null) {
                defaultAction.getPreloads(cfgmgr, preloads);
            }
        }

        @Override
        public void invalidate ()
        {
            for (ExpressionCase c : cases) {
                c.action.invalidate();
            }
            if (defaultAction != null) {
                defaultAction.invalidate();
            }
        }

        @Override
        public ActionConfig[] getSubActions ()
        {
            ActionConfig[] actions =
                new ActionConfig[cases.length + (defaultAction == null ? 0 : 1)];
            for (int ii = 0; ii < cases.length; ii++) {
                actions[ii] = cases[ii].action;
            }
            if (defaultAction != null) {
                actions[cases.length] = defaultAction;
            }
            return actions;
        }
    }

    /**
     * A switch case.
     */
    public static class ExpressionCase extends DeepObject
        implements Streamable, Exportable
    {
        /** The case expression. */
        @Editable
        public ExpressionConfig value = new ExpressionConfig.Constant();

        /** The case action. */
        @Editable
        public ActionConfig action = new ActionConfig.SpawnActor();
    }

    /**
     * Executes multiple actions simultaneously.
     */
    public static class Compound extends ActionConfig
        implements PreExecutable
    {
        /** The actions to execute. */
        @Editable
        public ActionConfig[] actions = new ActionConfig[0];

        /** If we should stop executing actions if one fails. */
        @Editable
        public boolean stopOnFailure = false;

        // documentation inherited from interface PreExecutable
        public boolean shouldPreExecute ()
        {
            for (ActionConfig action : actions) {
                if (action instanceof PreExecutable &&
                        ((PreExecutable)action).shouldPreExecute()) {
                    return true;
                }
            }
            return false;
        }

        // documentation inherited from interface PreExecutable
        public void preExecute (TudeySceneView view, ActorSprite sprite, int timestamp)
        {
            for (ActionConfig action : actions) {
                if (action instanceof PreExecutable) {
                    PreExecutable preex = (PreExecutable)action;
                    if (preex.shouldPreExecute()) {
                        preex.preExecute(view, sprite, timestamp);
                    }
                }
            }
        }

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Compound";
        }

        @Override
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            for (ActionConfig action : actions) {
                action.getPreloads(cfgmgr, preloads);
            }
        }

        @Override
        public void invalidate ()
        {
            for (ActionConfig action : actions) {
                action.invalidate();
            }
        }

        @Override
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

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Random";
        }

        @Override
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            for (WeightedAction waction : actions) {
                waction.action.getPreloads(cfgmgr, preloads);
            }
        }

        @Override
        public void invalidate ()
        {
            for (WeightedAction waction : actions) {
                waction.action.invalidate();
            }
        }

        @Override
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
        @Strippable
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
        @Editable(min=0, hgroup="d")
        @Strippable
        public int delay;

        /** The delay variance. */
        @Editable(min=0, hgroup="d")
        @Strippable
        public int variance;

        /** The action to perform. */
        @Editable
        public ActionConfig action = new SpawnActor();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Delayed";
        }

        @Override
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            action.getPreloads(cfgmgr, preloads);
        }

        @Override
        public void invalidate ()
        {
            action.invalidate();
        }

        @Override
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
    @Strippable
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

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$StepLimitMobile";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Sets a variable on the target.
     */
    @Strippable
    public static class SetVariable extends ActionConfig
    {
        /** The variable name. */
        @Editable
        public String name = "";

        /** The target to modify. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        /** The new variable value. */
        @Editable
        public ExpressionConfig value = new ExpressionConfig.Constant();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$SetVariable";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Sets a flag on the target.
     */
    @Strippable
    public static class SetFlag extends ActionConfig
    {
        /** The target to modify. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        /** The flag name */
        @Editable
        public String flag = "";

        /** The value to set. */
        @Editable
        public boolean on = true;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$SetFlag";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Forces the client associated with the target to do something.
     */
    @Strippable
    public static class ForceClientAction extends ActionConfig
    {
        /** The target to modify. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        /** The action to force. */
        @Editable
        public ClientActionConfig action = new ClientActionConfig.ControllerAction();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$ForceClientAction";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Performs an action on each target.
     */
    public static class TargetedAction extends ActionConfig
    {
        /** The target. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        /** The action. */
        @Editable
        public ActionConfig action = new ActionConfig.SpawnActor();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$TargetedAction";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
            action.invalidate();
        }
    }

    /**
     * Posts a message in the server log.
     */
    public static class ServerLog extends ActionConfig
    {
        /** The log level. */
        @Editable
        public LogLevel level = LogLevel.INFO;

        /** The log message. */
        @Editable
        public String message = "";

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$ServerLog";
        }
    }

    /**
     * Executes an action but always returns fail on the execute.
     */
    public static class Fail extends ActionConfig
        implements PreExecutable
    {
        /** The action to execute. */
        @Editable
        public ActionConfig action = new ActionConfig.SpawnActor();

        // documentation inherited from interface PreExecutable
        public boolean shouldPreExecute ()
        {
            return (action instanceof PreExecutable) && ((PreExecutable)action).shouldPreExecute();
        }

        // documentation inherited from interface PreExecutable
        public void preExecute (TudeySceneView view, ActorSprite sprite, int timestamp)
        {
            if (action instanceof PreExecutable) {
                PreExecutable preex = (PreExecutable)action;
                if (preex.shouldPreExecute()) {
                    preex.preExecute(view, sprite, timestamp);
                }
            }
        }

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActionLogic$Fail";
        }

        @Override
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            action.getPreloads(cfgmgr, preloads);
        }

        @Override
        public void invalidate ()
        {
            action.invalidate();
        }
    }

    public enum LogLevel { DEBUG, INFO, WARN, ERROR };

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
