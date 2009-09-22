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

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import com.google.inject.Inject;

import com.samskivert.util.RandomUtil;

import com.threerings.crowd.data.BodyObject;
import com.threerings.presents.dobj.OidList;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActionConfig;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.EffectConfig;
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.server.TudeySceneRegistry;

import static com.threerings.tudey.Log.*;

/**
 * Handles the server-side processing for an action.
 */
public abstract class ActionLogic extends Logic
{
    /**
     * Handles a spawn actor action.
     */
    public static class SpawnActor extends ActionLogic
    {
        @Override // documentation inherited
        public boolean execute (int timestamp, Logic activator)
        {
            ConfigReference<ActorConfig> actor = ((ActionConfig.SpawnActor)_config).actor;
            if (actor == null) {
                return true;
            }
            _location.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                ActorLogic logic = _scenemgr.spawnActor(
                    timestamp, target.getTranslation(), getRotation(target), actor);
                initLogic(logic, timestamp, activator);
            }
            _targets.clear();
            return true;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _location = createTarget(((ActionConfig.SpawnActor)_config).location, _source);
        }

        /**
         * Initialize the logic.
         */
        protected void initLogic (ActorLogic logic, int timestamp, Logic activator)
        {
            if (logic != null) {
                logic.setSource(_source);
                logic.setActivator(activator);
            }
        }

        /**
         * Gets the rotation for the spawned actor.
         */
        protected float getRotation (Logic target)
        {
            return target.getRotation();
        }

        /** The target location. */
        protected TargetLogic _location;
    }

    /**
     * Handles a spawn rotated actor action.
     */
    public static class SpawnRotatedActor extends SpawnActor
    {
        @Override // documentation inherited
        protected float getRotation (Logic target)
        {
            ActionConfig.SpawnRotatedActor config = (ActionConfig.SpawnRotatedActor)_config;
            return (config.relative ? target.getRotation() : 0f) + config.rotation;
        }
    }

    /**
     * Handles a destroy actor action.
     */
    public static class DestroyActor extends ActionLogic
    {
        @Override // documentation inherited
        public boolean execute (int timestamp, Logic activator)
        {
            boolean success = false;
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (target instanceof ActorLogic) {
                    ((ActorLogic)target).destroy(timestamp);
                    success = true;
                }
            }
            _targets.clear();
            return success;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.DestroyActor)_config).target, _source);
        }

        /** The target actor. */
        protected TargetLogic _target;
    }

    /**
     * Handles a warp actor action.
     */
    public static class WarpActor extends ActionLogic
    {
        @Override // documentation inherited
        public boolean execute (int timestamp, Logic activator)
        {
            boolean success = false;
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (!(target instanceof ActorLogic)) {
                    continue;
                }
                _location.resolve(activator, _locations);
                if (_locations.isEmpty()) {
                    continue;
                }
                Logic location = RandomUtil.pickRandom(_locations);
                _locations.clear();
                Vector2f translation = location.getTranslation();
                ((ActorLogic)target).warp(translation.x, translation.y, location.getRotation());
                success = true;
            }
            _targets.clear();
            return success;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ActionConfig.WarpActor config = (ActionConfig.WarpActor)_config;
            _target = createTarget(config.target, _source);
            _location = createTarget(config.location, _source);
        }

        /** The target actor. */
        protected TargetLogic _target;

        /** The location to which the actor will be warped. */
        protected TargetLogic _location;

        /** Temporary container for locations. */
        protected ArrayList<Logic> _locations = Lists.newArrayList();
    }

    /**
     * Handles a fire effect action.
     */
    public static class FireEffect extends ActionLogic
    {
        @Override // documentation inherited
        public boolean execute (int timestamp, Logic activator)
        {
            ConfigReference<EffectConfig> effect = ((ActionConfig.FireEffect)_config).effect;
            _location.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                _scenemgr.fireEffect(
                    timestamp, target.getTranslation(), target.getRotation(), effect);
            }
            _targets.clear();
            return true;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _location = createTarget(((ActionConfig.FireEffect)_config).location, _source);
        }

        /** The target location. */
        protected TargetLogic _location;
    }

    /**
     * Handles a signal action.
     */
    public static class Signal extends ActionLogic
    {
        @Override // documentation inherited
        public boolean execute (int timestamp, Logic activator)
        {
            String name = ((ActionConfig.Signal)_config).name;
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                _targets.get(ii).signal(timestamp, _source, name);
            }
            _targets.clear();
            return true;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.Signal)_config).target, _source);
        }

        /** The target entity. */
        protected TargetLogic _target;
    }

    /**
     * Superclass of the move logic classes.
     */
    public static abstract class AbstractMove extends ActionLogic
    {
        /**
         * Moves a single body to the destination.
         */
        protected void moveBody (int bodyOid)
        {
            BodyObject body = (BodyObject)_omgr.getObject(bodyOid);
            ActionConfig.MoveBody mconfig = (ActionConfig.MoveBody)_config;
            _screg.moveBody(body, mconfig.sceneId, mconfig.portal.getKey());
        }

        /** The distributed object manager. */
        @Inject protected PresentsDObjectMgr _omgr;

        /** The scene registry. */
        @Inject protected TudeySceneRegistry _screg;
    }

    /**
     * Handles a move body action.
     */
    public static class MoveBody extends AbstractMove
    {
        @Override // documentation inherited
        public boolean execute (int timestamp, Logic activator)
        {
            boolean success = false;
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (!(target instanceof PawnLogic)) {
                    continue;
                }
                int pawnId = ((PawnLogic)target).getActor().getId();
                TudeyOccupantInfo info =
                    ((TudeySceneObject)_scenemgr.getPlaceObject()).getOccupantInfo(pawnId);
                if (info != null) {
                    moveBody(info.getBodyOid());
                    success = true;
                }
            }
            _targets.clear();
            return success;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.MoveBody)_config).target, _source);
        }

        /** The target actor. */
        protected TargetLogic _target;
    }

    /**
     * Handles a move all action.
     */
    public static class MoveAll extends AbstractMove
    {
        @Override // documentation inherited
        public boolean execute (int timestamp, Logic activator)
        {
            OidList occupants = _scenemgr.getPlaceObject().occupants;
            for (int ii = 0, nn = occupants.size(); ii < nn; ii++) {
                moveBody(occupants.get(ii));
            }
            return true;
        }
    }

    /**
     * Handles a conditional action.
     */
    public static class Conditional extends ActionLogic
    {
        @Override // documentation inherited
        public boolean execute (int timestamp, Logic activator)
        {
            if (_condition.isSatisfied(activator)) {
                return _action.execute(timestamp, activator);
            } else if (_elseAction != null) {
                return _elseAction.execute(timestamp, activator);
            }
            return true;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ActionConfig.Conditional config = (ActionConfig.Conditional)_config;
            _condition = createCondition(config.condition, _source);
            _action = createAction(config.action, _source);
            if (config.elseAction != null) {
                _elseAction = createAction(config.elseAction, _source);
            }
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            _action.removed();
            if (_elseAction != null) {
                _elseAction.removed();
            }
        }

        /** The condition to evaluate. */
        protected ConditionLogic _condition;

        /** The action to take if the condition is satisfied. */
        protected ActionLogic _action;

        /** The action to take if the condition is not satisfied. */
        protected ActionLogic _elseAction;
    }

    /**
     * Handles a compound action.
     */
    public static class Compound extends ActionLogic
    {
        @Override // documentation inherited
        public boolean execute (int timestamp, Logic activator)
        {
            boolean success = false;
            for (ActionLogic action : _actions) {
                success = action.execute(timestamp, activator) | success;
            }
            return success;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ArrayList<ActionLogic> actions = new ArrayList<ActionLogic>();
            for (ActionConfig config : ((ActionConfig.Compound)_config).actions) {
                ActionLogic action = createAction(config, _source);
                if (action != null) {
                    actions.add(action);
                }
            }
            _actions = actions.toArray(new ActionLogic[actions.size()]);
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            for (ActionLogic action : _actions) {
                action.removed();
            }
        }

        /** Logic objects for the actions. */
        protected ActionLogic[] _actions;
    }

    /**
     * Handles a random action.
     */
    public static class Random extends ActionLogic
    {
        @Override // documentation inherited
        public boolean execute (int timestamp, Logic activator)
        {
            int idx = RandomUtil.getWeightedIndex(_weights);
            if (idx >= 0) {
                return _actions[idx].execute(timestamp, activator);
            }
            return true;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ActionConfig.WeightedAction[] wactions = ((ActionConfig.Random)_config).actions;
            _weights = new float[wactions.length];
            _actions = new ActionLogic[wactions.length];
            for (int ii = 0; ii < _actions.length; ii++) {
                ActionConfig.WeightedAction waction = wactions[ii];
                _weights[ii] = waction.weight;
                _actions[ii] = createAction(waction.action, _source);
            }
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            for (ActionLogic action : _actions) {
                action.removed();
            }
        }

        /** Weights for the actions. */
        protected float[] _weights;

        /** Logic objects for the actions. */
        protected ActionLogic[] _actions;
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, ActionConfig config, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _source = source;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Notes that the action has been removed.
     */
    public void removed ()
    {
        // give subclasses a chance to cleanup
        wasRemoved();
    }

    /**
     * Executes the action.
     *
     * @param activator the entity that triggered the action.
     * @return true of the action completed successfully
     */
    public abstract boolean execute (int timestamp, Logic activator);

    @Override // documentation inherited
    public boolean isActive ()
    {
        return _source.isActive();
    }

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override // documentation inherited
    public float getRotation ()
    {
        return _source.getRotation();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /**
     * Override to perform custom cleanup.
     */
    protected void wasRemoved ()
    {
        // nothing by default
    }

    /** The action configuration. */
    protected ActionConfig _config;

    /** The action source. */
    protected Logic _source;

    /** Temporary container for targets. */
    protected ArrayList<Logic> _targets = Lists.newArrayList();
}
