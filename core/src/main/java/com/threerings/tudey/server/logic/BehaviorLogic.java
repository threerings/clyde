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

import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Randoms;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.BehaviorConfig;
import com.threerings.tudey.config.BehaviorConfig.WeightedBehavior;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.server.TudeySceneManager;

import static com.threerings.tudey.Log.log;

/**
 * Handles the server-side processing for agent behavior.
 */
public abstract class BehaviorLogic extends Logic
{
    /** The maximum path length for following. */
    public static final float MAX_FOLLOW_PATH_LENGTH = 8f;

    /**
     * Handles the idle behavior.
     */
    public static class Idle extends BehaviorLogic
    {
        @Override
        public void startup ()
        {
            _agent.stopMoving();
            _agent.clearTargetRotation();
        }
    }

    /**
     * Superclass of the evaluating behaviors.
     */
    public static abstract class Evaluating extends BehaviorLogic
    {
        @Override
        public void startup ()
        {
            advanceEvaluation();
        }

        @Override
        public void tick (int timestamp)
        {
            // if scheduled to do so, evaluate
            if (_agent.canThink() && timestamp >= _nextEvaluation) {
                evaluate();
            }
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _nextEvaluation = ((Evaluating)source)._nextEvaluation;
        }

        /**
         * Performs an evaluation.  Default implementation simply schedules the next evaluation.
         */
        protected void evaluate ()
        {
            scheduleNextEvaluation();
        }

        /**
         * Schedules the next evaluation.
         */
        protected void scheduleNextEvaluation ()
        {
            _nextEvaluation = _scenemgr.getTimestamp() +
                (int)(((BehaviorConfig.Evaluating)_config).evaluationInterval.getValue() * 1000f);
        }

        /**
         * Postpones the next evaluation until the next rescheduling.
         */
        protected void postponeNextEvaluation ()
        {
            _nextEvaluation = Integer.MAX_VALUE;
        }

        /**
         * Ensures that we will evaluate on the next tick.
         */
        protected void advanceEvaluation ()
        {
            _nextEvaluation = _scenemgr.getTimestamp();
        }

        /** The time for which the next evaluation is scheduled. */
        protected int _nextEvaluation;
    }

    /**
     * Handles the base wander behavior.
     */
    public static class BaseWander extends Evaluating
    {
        @Override
        public void startup ()
        {
            super.startup();
            _startRotating = Integer.MAX_VALUE;
            _startMoving = Integer.MAX_VALUE;
        }

        @Override
        public void tick (int timestamp)
        {
            super.tick(timestamp);

            // start rotating if the time to do so has come
            if (_agent.canRotate() && timestamp >= _startRotating) {
                _startRotating = _startMoving = Integer.MAX_VALUE;
                _agent.setTargetRotation(_rotation);
            }

            // likewise with moving
            if (_agent.canMove() && timestamp >= _startMoving) {
                scheduleNextEvaluation();
                _agent.startMoving();
                _startMoving = Integer.MAX_VALUE;
            }
        }

        @Override
        public void reachedTargetRotation ()
        {
            BehaviorConfig.BaseWander config = (BehaviorConfig.BaseWander)_config;
            int pause = (int)(config.postRotationPause.getValue() * 1000f);
            if (pause == 0) {
                scheduleNextEvaluation();
                _agent.startMoving();
                _startRotating = _startMoving = Integer.MAX_VALUE;
            } else {
                _startMoving = _scenemgr.getTimestamp() + pause;
                _startRotating = Integer.MAX_VALUE;
            }
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            BaseWander wsource = (BaseWander)source;
            _startRotating = wsource._startRotating;
            _rotation = wsource._rotation;
            _startMoving = wsource._startMoving;
        }

        /**
         * Sets the new direction on the agent.
         */
        protected void setDirectionChange (float rotation)
        {
            int pause = (int)
                (((BehaviorConfig.BaseWander)_config).preRotationPause.getValue() * 1000f);
            postponeNextEvaluation();
            _rotation = rotation;
            if (pause == 0) {
                _startRotating = _startMoving = Integer.MAX_VALUE;
                _agent.setTargetRotation(_rotation);
            } else {
                _startRotating = _scenemgr.getTimestamp() + pause;
                _startMoving = Integer.MAX_VALUE;
            }
        }

        /** The time at which we should start rotating. */
        protected int _startRotating = Integer.MAX_VALUE;

        /** The rotation that we will face when we stop pausing. */
        protected float _rotation;

        /** The time at which we should start moving. */
        protected int _startMoving = Integer.MAX_VALUE;
    }

    /**
     * Handles the wander behavior.
     */
    public static class Wander extends BaseWander
    {
        @Override
        public void tick (int timestamp)
        {
            super.tick(timestamp);

            // if we have exceeded the radius and are moving away from the origin, change direction
            Actor actor = _agent.getActor();
            Vector2f trans = _agent.getTranslation();
            if (actor.isSet(Mobile.MOVING) &&
                    trans.distance(_origin) > ((BehaviorConfig.Wander)_config).radius) {
                float angle = FloatMath.atan2(_origin.y - trans.y, _origin.x - trans.x);
                float rotation = _agent.getActor().getRotation();
                if (FloatMath.getAngularDistance(angle, rotation) > FloatMath.HALF_PI) {
                    changeDirection(angle);
                }
            }
        }

        @Override
        public void penetratedEnvironment (Vector2f penetration)
        {
            // change the direction, using the reflected direction as a base
            float rotation = FloatMath.normalizeAngle(
                _agent.getActor().getRotation() + FloatMath.PI);
            if (penetration.length() > FloatMath.EPSILON) {
                float angle = FloatMath.atan2(penetration.y, penetration.x);
                rotation = FloatMath.normalizeAngle(
                    angle - FloatMath.getAngularDifference(rotation, angle));
            }
            changeDirection(rotation);
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            Wander wsource = (Wander)source;
            _origin.set(wsource._origin);
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            _origin.set(_agent.getTranslation());
        }

        @Override
        protected void evaluate ()
        {
            super.evaluate();
            changeDirection();
        }

        /**
         * Changes the direction of the agent.
         */
        protected void changeDirection ()
        {
            changeDirection(_agent.getActor().getRotation());
        }

        /**
         * Changes the direction of the agent.
         *
         * @param rotation the rotation to use as a base.
         */
        protected void changeDirection (float rotation)
        {
            _agent.stopMoving();
            BehaviorConfig.Wander config = (BehaviorConfig.Wander)_config;
            setDirectionChange(
                    FloatMath.normalizeAngle(rotation + config.directionChange.getValue()));
        }

        /** The translation of the actor when initialized. */
        protected Vector2f _origin = new Vector2f();
    }

    /**
     * Handles the wander collision behavior.
     */
    public static class WanderCollision extends BaseWander
    {
        @Override
        public void penetratedEnvironment (Vector2f penetration)
        {
            _action.execute(_scenemgr.getTimestamp(), _agent);
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            WanderCollision wsource = (WanderCollision)source;
            _action = (ActionLogic)refs.get(wsource._action);
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            _action = createAction(((BehaviorConfig.WanderCollision)_config).action, _agent);
        }

        protected ActionLogic _action;
    }

    /**
     * Handles the grid wander behavior.
     */
    public static class GridWander extends BaseWander
    {
        @Override
        public void startup ()
        {
            super.startup();
            if (!((BehaviorConfig.GridWander)_config).evaluationRotate) {
                setDirectionChange(_rotation);
            }
        }

        @Override
        public void penetratedEnvironment (Vector2f penetration)
        {
            BehaviorConfig.GridWander config = (BehaviorConfig.GridWander)_config;

            changeDirection();
        }

        @Override
        protected void evaluate ()
        {
            super.evaluate();
            if (((BehaviorConfig.GridWander)_config).evaluationRotate) {
                changeDirection();
            }
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            _rotation = _agent.getActor().getRotation();
        }

        /**
         * Changes the direction of the agent.
         */
        protected void changeDirection ()
        {
            _agent.stopMoving();
            float rotation = _rotation;
            switch(((BehaviorConfig.GridWander)_config).gridTurn) {
            case REVERSE:
                rotation += FloatMath.PI;
                break;
            case LEFT:
                rotation += FloatMath.HALF_PI;
                break;
            case RIGHT:
                rotation -= FloatMath.HALF_PI;
                break;
            case RANDOM:
                rotation += Randoms.threadLocal().getBoolean()
                    ? FloatMath.HALF_PI : -FloatMath.HALF_PI;
                break;
            }
            rotation = FloatMath.normalizeAnglePositive(rotation);
            if (rotation > FloatMath.TWO_PI - FloatMath.QUARTER_PI) {
                rotation = 0;
            } else if (rotation > FloatMath.PI + FloatMath.QUARTER_PI) {
                rotation = -FloatMath.HALF_PI;
            } else if (rotation > FloatMath.PI - FloatMath.QUARTER_PI) {
                rotation = FloatMath.PI;
            } else if (rotation > FloatMath.QUARTER_PI) {
                rotation = FloatMath.HALF_PI;
            } else {
                rotation = 0;
            }
            setDirectionChange(rotation);
        }
    }

    /**
     * Base class for behaviors that involve following paths.
     */
    public static abstract class Pathing extends Evaluating
    {
        @Override
        public void tick (int timestamp)
        {
            super.tick(timestamp);
            if (_path == null) {
                return; // nothing to do
            }

            // see if we've reached the current node (looping around in case the notification
            // sets us on a new path)
            Vector2f trans = _agent.getTranslation();
            boolean completedPath = false;
            while (_path[_pidx].distance(trans) <= getReachRadius()) {
                if (++_pidx == _path.length) {
                    _agent.stopMoving();
                    _path = null;
                    // If we've already completed a path then just exit to prevent an infinite loop
                    if (completedPath) {
                        return;
                    }
                    completedPath();
                    completedPath = true;
                } else {
                    reachedPathIndex(_pidx - 1);
                }
                if (_path == null) {
                    return;
                }
            }
            // make sure we're facing the right direction
            Vector2f node = _path[_pidx];
            float rot = FloatMath.atan2(node.y - trans.y, node.x - trans.x);
            float dist = FloatMath.getAngularDistance(_agent.getRotation(), rot);
            if (dist > 0.0001f) {
                _agent.setTargetRotation(rot);
            }
            if (dist > _moveFaceRange) {
                _agent.stopMoving();
            } else {
                _agent.startMoving();
            }
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            Pathing psource = (Pathing)source;
            _path = psource._path;
            _pidx = psource._pidx;
        }

        /**
         * Sets the path to follow.
         */
        protected void setPath (Vector2f[] path)
        {
            if (_path != path) {
                _path = path;
                _pidx = 0;
            }
        }

        /**
         * Clears the path.
         */
        protected void clearPath ()
        {
            if (_path != null) {
                _agent.stopMoving();
                _path = null;
            }
        }

        /**
         * Returns the radius within which we can be consider ourselves to have reached a node
         * (which depends on the actor's speed, since it's possible to overshoot).
         */
        protected float getReachRadius ()
        {
            // radius is the distance we can travel in a single tick
            float speed = ((Mobile)_agent.getActor()).getSpeed();
            return speed / _scenemgr.getTicksPerSecond();
        }

        /**
         * Called when we reach each node in the path (except for the last one, for which we call
         * {@link #completedPath}.
         */
        protected void reachedPathIndex (int idx)
        {
            // nothing by default
        }

        /**
         * Called when we complete the set path.
         */
        protected void completedPath ()
        {
            // nothing by default
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            _moveFaceRange = ((BehaviorConfig.Pathing)_config).moveFaceRange;
            if (_moveFaceRange == 0f) {
                _moveFaceRange = 0.001f;
            }
        }

        /** The waypoints of the path being followed. */
        protected Vector2f[] _path;

        /** The index of the next point on the path. */
        protected int _pidx;

        /** The angular range we need to be within before starting to move. */
        protected float _moveFaceRange;
    }

    /**
     * Handles the patrol behavior.
     */
    public static class Patrol extends Pathing
    {
        @Override
        public Logic getCurrentTarget ()
        {
            return _currentTarget;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            Patrol psource = (Patrol)source;
            _target.transfer(psource._target, refs);
            _currentTarget = (Logic)refs.get(psource._currentTarget);
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            _target = createTarget(((BehaviorConfig.Patrol)_config).target, _agent);
        }

        @Override
        protected void evaluate ()
        {
            super.evaluate();

            // determine the square of the branch radius
            float br2;
            if (_path == null) {
                br2 = Float.MAX_VALUE;
            } else {
                float radius = ((BehaviorConfig.Patrol)_config).branchRadius;
                if (radius < 0f) {
                    return; // no possibility of branching
                }
                br2 = radius*radius;
            }

            // resolve the target paths
            _target.resolve(_agent, _targets);
            Vector2f trans = _agent.getTranslation();
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (target == _currentTarget) {
                    continue;
                }
                Vector2f[] path = _targets.get(ii).getPatrolPath();
                if (path == null) {
                    continue;
                }
                // find the index of the closest node on the path
                float cdist = Float.MAX_VALUE;
                int cidx = -1;
                for (int jj = 0, mm = path.length; jj < mm; jj++) {
                    float dist = path[jj].distanceSquared(trans);
                    if (dist < cdist) {
                        cidx = jj;
                        cdist = dist;
                    }
                }
                if (cdist <= br2) {
                    _candidates.add(new PathCandidate(target, path, cidx));
                }
            }
            _targets.clear();

            // pick a candidate at random
            if (_candidates.isEmpty()) {
                return;
            }
            if (_path != null) {
                _candidates.add(null); // represents the current path
            }
            PathCandidate candidate = Randoms.threadLocal().pick(_candidates, null);
            _candidates.clear();

            // set off on that path
            if (candidate != null) {
                setPath(candidate.getRemainingPath(_agent.getRotation()), candidate.getTarget());
            }
        }

        @Override
        protected void reachedPathIndex (int idx)
        {
            evaluate();
        }

        @Override
        protected void completedPath ()
        {
            _currentTarget = null;
            evaluate();
        }

        /**
         * Sets the path to traverse.
         */
        protected void setPath (Vector2f[] path, Logic currentTarget)
        {
            super.setPath(path);
            _currentTarget = currentTarget;
        }

        /** The target to patrol. */
        protected TargetLogic _target;

        /** Holds targets during processing. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();

        /** Holds candidate paths during processing. */
        protected ArrayList<PathCandidate> _candidates = Lists.newArrayList();

        /** The logic corresponding to the current path, if any. */
        protected Logic _currentTarget;
    }

    /**
     * Handles the follow behavior.
     */
    public static class Follow extends Pathing
    {
        @Override
        public Logic getCurrentTarget ()
        {
            return _currentTarget;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _target.transfer(((Follow)source)._target, refs);
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            _target = createTarget(((BehaviorConfig.Follow)_config).target, _agent);
        }

        @Override
        protected void evaluate ()
        {
            super.evaluate();

            // find the closest target
            _target.resolve(_agent, _targets);
            Vector2f trans = _agent.getTranslation();
            _currentTarget = null;
            float cdist = Float.MAX_VALUE;
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                float dist = target.getTranslation().distanceSquared(trans);
                if (dist < cdist) {
                    _currentTarget = target;
                    cdist = dist;
                }
            }
            _targets.clear();

            // if we're within our distance bounds, stop and face the target
            if (_currentTarget == null) {
                return;
            }
            BehaviorConfig.Follow config = (BehaviorConfig.Follow)_config;
            float min2 = config.minimumDistance*config.minimumDistance;
            float max2 = config.maximumDistance*config.maximumDistance;
            if (FloatMath.isWithin(cdist, min2, max2)) {
                clearPath();
                _agent.face(_currentTarget);
                return;
            }

            // compute a path to the target
            Vector2f loc = _currentTarget.getTranslation();
            Vector2f[] path = _scenemgr.getPathfinder().getPath(
                _agent, MAX_FOLLOW_PATH_LENGTH, loc.x, loc.y, true, true);
            if (path == null) {
                clearPath();
                _agent.face(_currentTarget);
                return;
            }

            // start out on the path
            setPath(path);
        }

        /** The target to follow. */
        protected TargetLogic _target;

        /** Holds targets during processing. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();

        /** The current target. */
        protected Logic _currentTarget;
    }

    /**
     * Handles the random behavior.
     */
    public static class Random extends Evaluating
    {
        @Override
        public void tick (int timestamp)
        {
            super.tick(timestamp);
            if (_active != null) {
                _active.tick(timestamp);
            }
        }

        @Override
        public void reachedTargetRotation ()
        {
            if (_active != null) {
                _active.reachedTargetRotation();
            }
        }

        @Override
        public void penetratedEnvironment (Vector2f penetration)
        {
            if (_active != null) {
                _active.penetratedEnvironment(penetration);
            }
        }

        @Override
        public Logic getCurrentTarget ()
        {
            return _active == null ? null : _active.getCurrentTarget();
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            BehaviorLogic[] sbehaviors = ((Random)source)._behaviors;
            for (int ii = 0; ii < _behaviors.length; ii++) {
                if (_behaviors[ii] != null) {
                    _behaviors[ii].transfer(sbehaviors[ii], refs);
                }
            }
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            WeightedBehavior[] wbehaviors = ((BehaviorConfig.Random)_config).behaviors;
            _behaviors = new BehaviorLogic[wbehaviors.length];
            _behaviorWeights = Maps.newHashMap();
            for (int ii = 0; ii < wbehaviors.length; ii++) {
                WeightedBehavior wbehavior = wbehaviors[ii];
                _behaviors[ii] = _agent.createBehavior(wbehavior.behavior);
                _behaviorWeights.put(_behaviors[ii], wbehavior.weight);
            }
        }

        @Override
        protected void evaluate ()
        {
            super.evaluate();
            BehaviorLogic nactive = Randoms.threadLocal().pick(_behaviorWeights, null);
            if (nactive == _active) {
                return;
            }
            if ((_active = nactive) != null) {
                _active.startup();
            }
        }

        /** The component behaviors. */
        protected BehaviorLogic[] _behaviors;

        /** The active behavior. */
        protected BehaviorLogic _active;

        /** The behavior weight map. */
        protected Map<BehaviorLogic, Float> _behaviorWeights;
    }

    /**
     * Handles the scripted behavior.
     */
    public static class Scripted extends BehaviorLogic
    {
        /**
         * Sets the current step.
         */
        public void setCurrentStep (int step, int timestamp)
        {
            _currentStep = Math.min(_steps.length, step);
            _steps[_currentStep].start(timestamp);
        }

        @Override
        public void tick (int timestamp)
        {
            if (_currentStep < _steps.length) {
                if (_start) {
                    _steps[_currentStep].start(timestamp);
                }
                if (_start = _steps[_currentStep].tick(timestamp)) {
                    _currentStep++;
                }
            }
        }

        @Override
        public void reachedTargetRotation ()
        {
            if (_currentStep < _steps.length) {
                _steps[_currentStep].reachedTargetRotation();
            }
        }

        @Override
        public void startup ()
        {
            _currentStep = 0;
            _start = true;
        }

        @Override
        public void suspend ()
        {
            if (_currentStep < _steps.length && !_start) {
                _steps[_currentStep].suspend();
            }
        }

        @Override
        public void shutdown ()
        {
            for (ScriptLogic logic : _steps) {
                logic.shutdown();
            }
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            Scripted ssource = (Scripted)source;
            for (int ii = 0; ii < _steps.length; ii++) {
                _steps[ii].transfer(ssource._steps[ii], refs);
            }
            _currentStep = ssource._currentStep;
            _start = ssource._start;
        }

        @Override
        protected void didInit ()
        {
            super.didInit();

            BehaviorConfig.Scripted config = (BehaviorConfig.Scripted)_config;
            _steps = new ScriptLogic[config.steps.length];
            for (int ii = 0; ii < _steps.length; ii++) {
                _steps[ii] = ScriptLogic.createScriptLogic(
                        _scenemgr, config.steps[ii], _agent, this);
            }
        }

        /** The script logics. */
        protected ScriptLogic[] _steps;

        /** The current step. */
        protected int _currentStep;

        /** If we need to start the next step. */
        protected boolean _start = true;
    }

    /**
     * Handles the combined behavior.
     */
    public static class Combined extends BehaviorLogic
    {
        @Override
        public void startup ()
        {
            if (_first != null) {
                _first.startup();
            }
            if (_second != null) {
                _second.startup();
            }
        }

        @Override
        public void shutdown ()
        {
            if (_first != null) {
                _first.shutdown();
            }
            if (_second != null) {
                _second.shutdown();
            }
        }

        @Override
        public void tick (int timestamp)
        {
            if (_first != null) {
                _first.tick(timestamp);
            }
            if (_second != null) {
                _second.tick(timestamp);
            }
        }

        @Override
        public void reachedTargetRotation ()
        {
            if (_first != null) {
                _first.reachedTargetRotation();
            }
            if (_second != null) {
                _second.reachedTargetRotation();
            }
        }

        @Override
        public void penetratedEnvironment (Vector2f penetration)
        {
            if (_first != null) {
                _first.penetratedEnvironment(penetration);
            }
            if (_second != null) {
                _second.penetratedEnvironment(penetration);
            }
        }

        @Override
        public Logic getCurrentTarget ()
        {
            Logic target = null;
            if (_first != null) {
                target = _first.getCurrentTarget();
            }
            if (_second != null && target == null) {
                target = _second.getCurrentTarget();
            }
            return target;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            Combined csource = (Combined)source;
            if (_first != null) {
                _first.transfer(csource._first, refs);
            }
            if (_second != null) {
                _second.transfer(csource._second, refs);
            }
        }

        @Override
        protected void didInit ()
        {
            super.didInit();
            BehaviorConfig.Combined config = (BehaviorConfig.Combined)_config;
            _first = _agent.createBehavior(config.first);
            if (_first != null) {
                _first.didInit();
            }
            _second = _agent.createBehavior(config.second);
            if (_second != null) {
                _second.didInit();
            }
        }

        /** Our first behavior. */
        protected BehaviorLogic _first;

        /** Our second behavior. */
        protected BehaviorLogic _second;
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, BehaviorConfig.Original config, AgentLogic agent)
    {
        super.init(scenemgr);
        _config = config;
        _agent = agent;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Starts up the behavior after initialization or suspension.
     */
    public void startup ()
    {
        // nothing by default
    }

    /**
     * Suspends the behavior.
     */
    public void suspend ()
    {
        // nothing by default
    }

    /**
     * Shuts down the behavior when the agent is destroyed.
     */
    public void shutdown ()
    {
        // nothing by default
    }

    /**
     * Ticks the behavior.
     */
    public void tick (int timestamp)
    {
        // nothing by default
    }

    /**
     * Notifies the behavior that the agent has reached its target rotation.
     */
    public void reachedTargetRotation ()
    {
        // nothing by default
    }

    /**
     * Notifies the behavior that the agent has penetrated its environment during advancement.
     *
     * @param penetration the sum penetration vector.
     */
    public void penetratedEnvironment (Vector2f penetration)
    {
        // nothing by default
    }

    /**
     * Returns the currently targeted logic, if any.
     */
    public Logic getCurrentTarget ()
    {
        return null;
    }

    @Override
    public boolean isActive ()
    {
        return _agent.isActive();
    }

    @Override
    public EntityKey getEntityKey ()
    {
        return _agent.getEntityKey();
    }

    @Override
    public Vector2f getTranslation ()
    {
        return _agent.getTranslation();
    }

    @Override
    public float getRotation ()
    {
        return _agent.getRotation();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /**
     * A candidate path under consideration for branching.
     */
    protected static class PathCandidate
    {
        /**
         * Creates a new path candidate.
         */
        public PathCandidate (Logic target, Vector2f[] path, int cidx)
        {
            _target = target;
            _path = path;
            _cidx = cidx;
        }

        /**
         * Returns a reference to the logic associated with the path.
         */
        public Logic getTarget ()
        {
            return _target;
        }

        /**
         * Returns the portion of the path from the closest node to the end, using the supplied
         * angle to determine which direction to travel along the path in cases of ambiguity.
         * This clobbers the contained path.
         */
        public Vector2f[] getRemainingPath (float angle)
        {
            if (_cidx == _path.length - 1) { // last node; use reverse direction
                ArrayUtil.reverse(_path);
            } else if (_cidx != 0) { // internal node; alignment determines direction
                Vector2f prev = _path[_cidx - 1], node = _path[_cidx], next = _path[_cidx + 1];
                float df = FloatMath.atan2(next.y - node.y, next.x - node.x);
                float dr = FloatMath.atan2(prev.y - node.y, prev.x - node.x);
                if (FloatMath.getAngularDistance(angle, dr) <
                        FloatMath.getAngularDistance(angle, df)) {
                    ArrayUtil.reverse(_path = ArrayUtil.splice(_path, _cidx + 1));
                } else {
                    _path = ArrayUtil.splice(_path, 0, _cidx);
                }
            }
            return _path;
        }

        /** The logic with which the path is associated. */
        protected Logic _target;

        /** The base path. */
        protected Vector2f[] _path;

        /** The index of the closest node on the path. */
        protected int _cidx;
    }

    /** The behavior configuration. */
    protected BehaviorConfig.Original _config;

    /** The controlled agent. */
    protected AgentLogic _agent;
}
