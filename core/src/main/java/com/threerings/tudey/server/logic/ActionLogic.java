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

package com.threerings.tudey.server.logic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.google.inject.Inject;

import com.samskivert.util.Interval;
import com.samskivert.util.Randoms;
import com.samskivert.util.RandomUtil;

import com.threerings.crowd.data.BodyObject;
import com.threerings.presents.dobj.OidList;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.config.ConfigReference;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;
import com.threerings.probs.FloatVariable;

import com.threerings.tudey.config.ActionConfig;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.ClientActionConfig;
import com.threerings.tudey.config.EffectConfig;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.data.TudeyBodyObject;
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.server.TudeySceneRegistry;
import com.threerings.tudey.shape.Segment;

import static com.threerings.tudey.Log.log;

/**
 * Handles the server-side processing for an action.
 */
public abstract class ActionLogic extends Logic
{
    /**
     * Simple base class for actions with targets.
     */
    public static abstract class Targeted extends ActionLogic
    {
        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _target.transfer(((Targeted)source)._target, refs);
        }

        /** The target actor. */
        protected TargetLogic _target;
    }

    /**
     * Handles the "none" action.
     */
    public static class None extends ActionLogic
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            return true;
        }
    }

    /**
     * Handles a spawn actor action.
     */
    public static class SpawnActor extends ActionLogic
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            ConfigReference<ActorConfig> actor = getActorConfig(activator);
            if (actor == null) {
                return true;
            }
            _location.resolve(activator, _targets);
            boolean success = false;
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                success = spawnActor(timestamp, actor, target, activator) | success;
            }
            _targets.clear();
            return success;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _location.transfer(((SpawnActor)source)._location, refs);
        }

        @Override
        protected void didInit ()
        {
            _location = createTarget(((ActionConfig.SpawnActor)_config).location, _source);
        }

        /**
         * Returns the actor config to spawn.
         */
        public ConfigReference<ActorConfig> getActorConfig (Logic activator)
        {
            return ((ActionConfig.SpawnActor)_config).actor;
        }

        /**
         * Spawns an actor at the target
         */
        protected boolean spawnActor (
                int timestamp, ConfigReference<ActorConfig> actor, Logic target, Logic activator)
        {
            ActorLogic logic = _scenemgr.spawnActor(
                    timestamp, getTranslation(target), getRotation(target), actor);
            initLogic(logic, activator);
            return true;
        }

        /**
         * Initializes the logic.
         */
        protected void initLogic (ActorLogic logic, Logic activator)
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

        /**
         * Gets the translation for the spawned actor.
         */
        protected Vector2f getTranslation (Logic target)
        {
            return target.getTranslation();
        }

        /** The target location. */
        protected TargetLogic _location;
    }

    /**
     * Handles a spawn rotated actor action.
     */
    public static class SpawnRotatedActor extends SpawnActor
    {
        @Override
        protected float getRotation (Logic target)
        {
            ActionConfig.SpawnRotatedActor config = (ActionConfig.SpawnRotatedActor)_config;
            float rotation = config.rotation;
            if (config.rotationVariance > 0) {
                rotation += config.rotationVariance * (FloatMath.random() - 0.5f);
            }
            return FloatMath.normalizeAngle(
                (config.relative ? target.getRotation() : 0f) + rotation);
        }
    }

    /**
     * Handles a spawn transformed actor action.
     */
    public static class SpawnTransformedActor extends SpawnRotatedActor
    {
        @Override
        protected Vector2f getTranslation (Logic target)
        {
            return ((ActionConfig.SpawnTransformedActor)_config).translation.rotateAndAdd(
                target.getRotation(), target.getTranslation(), new Vector2f());
        }
    }

    /**
     * Handles a spawn random translated actor action.
     */
    public static class SpawnRandomTranslatedActor extends SpawnActor
    {
        @Override
        protected boolean spawnActor (
                int timestamp, ConfigReference<ActorConfig> actor, Logic target, Logic activator)
        {
            ActionConfig.SpawnRandomTranslatedActor config =
                (ActionConfig.SpawnRandomTranslatedActor)_config;
            Set<Vector2f> locations = Sets.newHashSet();
            int steps = config.steps;
            boolean success = false;
            Vector2f translation = null;
            if (config.collisionMask != 0) {
                ArrayList<Logic> cTargets = Lists.newArrayList();
                _cs.resolve(activator, cTargets);
                if (cTargets.size() > 0) {
                    translation = getTranslation(cTargets.get(0));
                }
            }
            Randoms r = Randoms.threadLocal();
            for (int ii = 0; ii < config.count; ii++) {
                for (int jj = 0; jj < COLLISION_ATTEMPTS; jj++) {
                    Vector2f location = getTranslation(target).add(
                            r.getInRange(-steps, steps + 1) * config.stepSize,
                            r.getInRange(-steps, steps + 1) * config.stepSize);
                    if (locations.add(location) && (translation == null || !_scenemgr.collides(
                                config.collisionMask, new Segment(translation, location)))) {
                        ActorLogic logic = _scenemgr.spawnActor(
                                timestamp, location, getRotation(target), actor);
                        initLogic(logic, activator);
                        success = true;
                        break;
                    }
                }
            }
            return success;
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            _cs = createTarget(
                    ((ActionConfig.SpawnRandomTranslatedActor)_config).collisionSource, _source);
        }

        /** The collision source. */
        protected TargetLogic _cs;

        /** The number of failed collision attempts before continuing. */
        protected static final int COLLISION_ATTEMPTS = 3;
    }

    /**
     * Handles a spawn facing actor action.
     */
    public static class SpawnFacingActor extends SpawnActor
    {
        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _facing.transfer(((SpawnFacingActor)source)._facing, refs);
        }

        @Override
        protected boolean spawnActor (
                int timestamp, ConfigReference<ActorConfig> actor, Logic target, Logic activator)
        {
            _facing.resolve(activator, _faces);
            boolean result = super.spawnActor(timestamp, actor, target, activator);
            _faces.clear();
            return result;
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            _facing = createTarget(((ActionConfig.SpawnFacingActor)_config).facing, _source);
        }

        @Override
        protected float getRotation (Logic target)
        {
            if (_faces.size() > 0) {
                Logic face = _faces.get(0);
                return target.getTranslation().direction(face.getTranslation());
            }
            return super.getRotation(target);
        }

        /** The possible facing targets. */
        protected ArrayList<Logic> _faces = Lists.newArrayList();

        /** The facing location. */
        protected TargetLogic _facing;
    }

    /**
     * Handles a destroy actor action.
     */
    public static class DestroyActor extends Targeted
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            boolean success = false;
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (target instanceof ActorLogic) {
                    ((ActorLogic)target).destroy(
                        timestamp, activator, ((ActionConfig.DestroyActor)_config).endScene);
                    success = true;
                }
            }
            _targets.clear();
            return success;
        }

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.DestroyActor)_config).target, _source);
        }
    }

    /**
     * Handles a rotate actor action.
     */
    public static class RotateActor extends Targeted
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            boolean success = false;
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (!(target instanceof ActorLogic)) {
                    continue;
                }
                ActorLogic logic = (ActorLogic)target;
                Actor actor = logic.getActor();
                Vector2f translation = actor.getTranslation();
                float rotation = ((ActionConfig.RotateActor)_config).rotation.getValue();
                logic.move(translation.x, translation.y,
                        FloatMath.normalizeAngle(actor.getRotation() + rotation));
                if (actor instanceof Mobile) {
                    Mobile mobile = (Mobile)actor;
                    mobile.setDirection(FloatMath.normalizeAngle(mobile.getDirection() + rotation));
                }
                success = true;
            }
            _targets.clear();
            return success;
        }

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.RotateActor)_config).target, _source);
        }
    }

    /**
     * Handles a warp actor action.
     */
    public static class WarpActor extends Targeted
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            boolean success = false;
            _target.resolve(activator, _targets);
            ActionConfig.WarpActor config = (ActionConfig.WarpActor)_config;
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (!(target instanceof ActorLogic)) {
                    continue;
                }
                _location.resolve(activator, _locations);
                if (_locations.isEmpty()) {
                    continue;
                }
                Logic location = Randoms.threadLocal().pick(_locations, null);
                _locations.clear();
                ActorLogic actorTarget = (ActorLogic)target;
                warp(actorTarget, location);
                if (config.resetMap) {
                    actorTarget.resetMap(location.getTranslation());
                }
                success = true;
            }
            _targets.clear();

            return success;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _location.transfer(((WarpActor)source)._location, refs);
        }

        /**
         * Warp the actor to the location.
         */
        protected void warp (ActorLogic target, Logic location)
        {
            Vector2f translation = location.getTranslation();
            target.warp(translation.x, translation.y, location.getRotation(), translation.x,
                    translation.y, true, ((ActionConfig.WarpActor)_config).maxWarpPath);
        }

        @Override
        protected void didInit ()
        {
            ActionConfig.WarpActor config = (ActionConfig.WarpActor)_config;
            _target = createTarget(config.target, _source);
            _location = createTarget(config.location, _source);
        }

        /** The location to which the actor will be warped. */
        protected TargetLogic _location;

        /** Temporary container for locations. */
        protected ArrayList<Logic> _locations = Lists.newArrayList();
    }

    /**
     * Handles a warp transformed actor action.
     */
    public static class WarpTransformedActor extends WarpActor
    {
        @Override
        protected void warp (ActorLogic target, Logic location)
        {
            ActionConfig.WarpTransformedActor config = (ActionConfig.WarpTransformedActor)_config;
            float rotation = config.rotation + location.getRotation();
            Vector2f translation = new Vector2f();
            Vector2f ltrans = location.getTranslation();
            if (config.rotatedTranslation) {
                config.translation.rotateAndAdd(rotation, ltrans, translation);
            } else {
                translation.addLocal(ltrans);
            }
            target.warp(translation.x, translation.y, rotation, ltrans.x, ltrans.y,
                    true, config.maxWarpPath);
        }
    }

    /**
     * Handles a fire effect action.
     */
    public static class FireEffect extends ActionLogic
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            ConfigReference<EffectConfig> effect = ((ActionConfig.FireEffect)_config).effect;
            if (effect == null) {
                return true;
            }
            _location.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                _scenemgr.fireEffect(timestamp, target, null, 0f, effect);
            }
            _targets.clear();
            return true;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _location.transfer(((FireEffect)source)._location, refs);
        }

        @Override
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
    public static class Signal extends Targeted
    {
        @Override
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

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.Signal)_config).target, _source);
        }
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
        @Override
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

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _target.transfer(((MoveBody)source)._target, refs);
        }

        @Override
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
        @Override
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
        @Override
        public boolean shouldExecute (Logic activator)
        {
            return _elseAction != null || _condition.isSatisfied(activator);
        }

        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            if (_condition.isSatisfied(activator)) {
                return _action.execute(timestamp, activator);
            } else if (_elseAction != null) {
                return _elseAction.execute(timestamp, activator);
            }
            return true;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            Conditional csource = (Conditional)source;
            _condition.transfer(csource._condition, refs);
            _action.transfer(csource._action, refs);
            if (_elseAction != null) {
                _elseAction.transfer(csource._elseAction, refs);
            }
        }

        @Override
        protected void didInit ()
        {
            ActionConfig.Conditional config = (ActionConfig.Conditional)_config;
            _condition = createCondition(config.condition, _source);
            _action = createAction(config.action, _source);
            if (config.elseAction != null) {
                _elseAction = createAction(config.elseAction, _source);
            }
        }

        @Override
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
     * Handles a switch action.
     */
    public static class Switch extends ActionLogic
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            for (int ii = 0; ii < _conditions.length; ii++) {
                if (_conditions[ii].isSatisfied(activator)) {
                    return _actions[ii].execute(timestamp, activator);
                }
            }
            if (_defaultAction != null) {
                return _defaultAction.execute(timestamp, activator);
            }
            return true;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            Switch ssource = (Switch)source;
            for (int ii = 0; ii < _conditions.length; ii++) {
                _conditions[ii].transfer(ssource._conditions[ii], refs);
                _actions[ii].transfer(ssource._actions[ii], refs);
            }
            if (_defaultAction != null) {
                _defaultAction.transfer(ssource._defaultAction, refs);
            }
        }

        @Override
        protected void didInit ()
        {
            ActionConfig.Switch config = (ActionConfig.Switch)_config;
            _conditions = new ConditionLogic[config.cases.length];
            _actions = new ActionLogic[config.cases.length];
            for (int ii = 0; ii < config.cases.length; ii++) {
                _conditions[ii] = createCondition(config.cases[ii].condition, _source);
                _actions[ii] = createAction(config.cases[ii].action, _source);
            }
            if (config.defaultAction != null) {
                _defaultAction = createAction(config.defaultAction, _source);
            }
        }

        @Override
        protected void wasRemoved ()
        {
            for (ActionLogic action : _actions) {
                action.removed();
            }
            if (_defaultAction != null) {
                _defaultAction.removed();
            }
        }

        /** The condition to evaluate. */
        protected ConditionLogic[] _conditions;

        /** The action to take if the condition is satisfied. */
        protected ActionLogic[] _actions;

        /** The default action to take. */
        protected ActionLogic _defaultAction;
    }

    /**
     * Handles an expression switch action.
     */
    public static class ExpressionSwitch extends ActionLogic
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            Object value = _value.evaluate(activator, null);
            for (int ii = 0; ii < _caseValues.length; ii++) {
                if (Objects.equal(value, _caseValues[ii].evaluate(activator, null))) {
                    return _actions[ii].execute(timestamp, activator);
                }
            }
            if (_defaultAction != null) {
                return _defaultAction.execute(timestamp, activator);
            }
            return true;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            ExpressionSwitch esource = (ExpressionSwitch)source;
            for (int ii = 0; ii < _actions.length; ii++) {
                _actions[ii].transfer(esource._actions[ii], refs);
            }
            if (_defaultAction != null) {
                _defaultAction.transfer(esource._defaultAction, refs);
            }
        }

        @Override
        protected void didInit ()
        {
            ActionConfig.ExpressionSwitch config = (ActionConfig.ExpressionSwitch)_config;
            _value = createExpression(config.value, _source);
            _caseValues = new ExpressionLogic[config.cases.length];
            _actions = new ActionLogic[config.cases.length];
            for (int ii = 0; ii < config.cases.length; ii++) {
                _caseValues[ii] = createExpression(config.cases[ii].value, _source);
                _actions[ii] = createAction(config.cases[ii].action, _source);
            }
            if (config.defaultAction != null) {
                _defaultAction = createAction(config.defaultAction, _source);
            }
        }

        @Override
        protected void wasRemoved ()
        {
            for (ActionLogic action : _actions) {
                action.removed();
            }
            if (_defaultAction != null) {
                _defaultAction.removed();
            }
        }

        /** The switch value. */
        protected ExpressionLogic _value;

        /** The values of the cases. */
        protected ExpressionLogic[] _caseValues;

        /** The action to take if the condition is satisfied. */
        protected ActionLogic[] _actions;

        /** The default action to take. */
        protected ActionLogic _defaultAction;
    }

    /**
     * Handles a compound action.
     */
    public static class Compound extends ActionLogic
    {
        @Override
        public boolean shouldExecute (Logic activator)
        {
            for (ActionLogic action : _actions) {
                if (action.shouldExecute(activator)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            boolean success = false;
            for (ActionLogic action : _actions) {
                success = action.execute(timestamp, activator) | success;
                if (((ActionConfig.Compound)_config).stopOnFailure && !success) {
                    return false;
                }
            }
            return success;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            ActionLogic[] sactions = ((Compound)source)._actions;
            for (int ii = 0; ii < _actions.length; ii++) {
                _actions[ii].transfer(sactions[ii], refs);
            }
        }

        @Override
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

        @Override
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
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            int idx = RandomUtil.getWeightedIndex(_weights);
            if (idx >= 0) {
                return _actions[idx].execute(timestamp, activator);
            }
            return true;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            ActionLogic[] sactions = ((Random)source)._actions;
            for (int ii = 0; ii < _actions.length; ii++) {
                _actions[ii].transfer(sactions[ii], refs);
            }
        }

        @Override
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

        @Override
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
     * Handles a delayed action.
     */
    public static class Delayed extends ActionLogic
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            if (!_scenemgr.isRunning()) {
                log.warning("Delayed action executing on shutdown SceneManager.", new Exception());
                return false;
            }

            ActionConfig.Delayed config = (ActionConfig.Delayed)_config;
            int delay = (config.variance == 0)
                ? config.delay
                : Math.max(0, config.delay +
                        Randoms.threadLocal().getInt(config.variance * 2) - config.variance);
            new ActionInterval(_scenemgr.getTimestamp() + delay, activator);
            return true;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            Delayed src = (Delayed)source;
            _action.transfer(src._action, refs);
            for (ActionInterval ai : src._intervals) {
                new ActionInterval(ai.executionStamp, (Logic)refs.get(ai.activator));
            }
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            _action = createAction(((ActionConfig.Delayed)_config).action, _source);
        }

        @Override
        protected void wasRemoved ()
        {
            _action.removed();
            for (ActionInterval ai : _intervals) {
                ai.cancel();
            }
            _intervals = null;
        }

        /** The action. */
        protected ActionLogic _action;

        /** The time intervals. */
        protected Set<ActionInterval> _intervals = Sets.newIdentityHashSet();

        /**
         * An interval that knows its intended exection time in the scene, so that
         * a clone can be created when transferring.
         */
        protected class ActionInterval extends Interval
        {
            /** The scene timestamp at which we should be executing. */
            public final int executionStamp;

            /** Our activator, passed to the action. */
            public final Logic activator;

            /**
             * Create an ActionInterval.
             */
            public ActionInterval (int executionStamp, Logic activator)
            {
                super(_scenemgr);
//                log.info("Scheduling actionInterval",
//                        "scenemgr", System.identityHashCode(_scenemgr),
//                        "exectionStamp", executionStamp,
//                        "activator", activator.getClass());
                this.executionStamp = executionStamp;
                this.activator = activator;
                _intervals.add(this);
                schedule(Math.max(0, executionStamp - _scenemgr.getTimestamp()));
            }

            @Override public void expired () {
                _intervals.remove(this);
                _action.execute(_scenemgr.getTimestamp(), activator);
            }

            @Override protected void noteRejected () {
                // it's ok: the scene has ended. (we could execute a failure action here)
                _intervals.remove(this);
            }
        }
    }

    /**
     * Handles a set variable action.
     */
    public static class SetVariable extends Targeted
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            String name = ((ActionConfig.SetVariable)_config).name;
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                target.setVariable(timestamp, _source, name,
                    _value.evaluate(activator, target.getVariable(name)));
            }
            _targets.clear();
            return true;
        }

        @Override
        protected void didInit ()
        {
            ActionConfig.SetVariable config = (ActionConfig.SetVariable)_config;
            _target = createTarget(config.target, _source);
            _value = createExpression(config.value, _source);
        }

        /** The value logic. */
        protected ExpressionLogic _value;
    }

    /**
     * Handles a set flag action.
     */
    public static class SetFlag extends Targeted
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            ActionConfig.SetFlag config = (ActionConfig.SetFlag)_config;
            _target.resolve(activator, _targets);
            boolean ret = false;
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (target instanceof ActorLogic) {
                    Actor actor = ((ActorLogic)target).getActor();
                    try {
                        Field flag = actor.getClass().getField(config.flag);
                        actor.set(flag.getInt(actor), config.on);
                        ret = true;
                    } catch (NoSuchFieldException e) {
                        // that's ok; just fall through

                    } catch (IllegalAccessException e) {
                        log.warning("Cannot access flag field for Set Flag Action.", e);
                    }
                }
            }
            _targets.clear();
            return ret;
        }

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.SetFlag)_config).target, _source);
        }
    }

    /**
     * Handles a force client action... action.
     */
    public static class ForceClientAction extends Targeted
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            ClientActionConfig action = ((ActionConfig.ForceClientAction)_config).action;
            _target.resolve(activator, _targets);
            boolean success = false;
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (!(target instanceof PawnLogic)) {
                    continue;
                }
                int pawnId = ((PawnLogic)target).getActor().getId();
                TudeyOccupantInfo info =
                    ((TudeySceneObject)_scenemgr.getPlaceObject()).getOccupantInfo(pawnId);
                if (info != null) {
                    _omgr.getObject(info.getBodyOid()).postMessage(
                        TudeyBodyObject.FORCE_CLIENT_ACTION, action, _source.getEntityKey());
                    success = true;
                }
            }
            _targets.clear();
            return success;
        }

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.ForceClientAction)_config).target, _source);
        }

        /** The distributed object manager. */
        @Inject protected PresentsDObjectMgr _omgr;
    }

    /**
     * Handles a targeted action.
     */
    public static class TargetedAction extends Targeted
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                _action.execute(timestamp, _targets.get(ii));
            }
            _targets.clear();
            return true;
        }

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.TargetedAction)_config).target, _source);
            _action = createAction(((ActionConfig.TargetedAction)_config).action, _source);
        }

        @Override
        protected void wasRemoved ()
        {
            _action.removed();
        }

        /** The action. */
        protected ActionLogic _action;
    }

    /**
     * Handles a server log action.
     */
    public static class ServerLog extends ActionLogic
    {
        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            ActionConfig.ServerLog config = (ActionConfig.ServerLog)_config;
            switch(config.level) {
            case DEBUG:
                log.debug(config.message);
                break;
            case INFO:
                log.info(config.message);
                break;
            case WARN:
                log.warning(config.message);
                break;
            case ERROR:
                log.error(config.message);
                break;
            }
            return true;
        }
    }

    /**
     * Handles a fail action.
     */
    public static class Fail extends ActionLogic
    {
        @Override
        public boolean shouldExecute (Logic activator)
        {
            return _action != null && _action.shouldExecute(activator);
        }

        @Override
        public boolean execute (int timestamp, Logic activator)
        {
            if (_action != null) {
                _action.execute(timestamp, activator);
            }
            return false;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            if (_action != null) {
                _action.transfer(((Fail)source)._action, refs);
            }
        }

        @Override
        protected void didInit ()
        {
            _action = createAction(((ActionConfig.Fail)_config).action, _source);
        }

        @Override
        protected void wasRemoved ()
        {
            if (_action != null) {
                _action.removed();
            }
        }

        /** Logic object for the action. */
        protected ActionLogic _action;
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
     * Provides a hint as to whether this action should be executed.
     */
    public boolean shouldExecute (Logic activator)
    {
        return true;
    }

    /**
     * Executes the action.
     *
     * @param activator the entity that triggered the action.
     * @return true of the action completed successfully
     */
    public abstract boolean execute (int timestamp, Logic activator);

    @Override
    public boolean isActive ()
    {
        return _source.isActive();
    }

    @Override
    public EntityKey getEntityKey ()
    {
        return _source.getEntityKey();
    }

    @Override
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override
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
