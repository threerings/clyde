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

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.BehaviorConfig;
import com.threerings.tudey.config.BehaviorConfig.WeightedBehavior;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.server.TudeySceneManager;

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
        @Override // documentation inherited
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
        @Override // documentation inherited
        public void startup ()
        {
            advanceEvaluation();
        }

        @Override // documentation inherited
        public void tick (int timestamp)
        {
            // if scheduled to do so, evaluate
            if (timestamp >= _nextEvaluation) {
                evaluate();
            }
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
     * Handles the wander behavior.
     */
    public static class Wander extends Evaluating
    {
        @Override // documentation inherited
        public void startup ()
        {
            super.startup();
            _startRotating = Integer.MAX_VALUE;
            _startMoving = Integer.MAX_VALUE;
        }

        @Override // documentation inherited
        public void tick (int timestamp)
        {
            super.tick(timestamp);

            // start rotating if the time to do so has come
            if (timestamp >= _startRotating) {
                _startRotating = _startMoving = Integer.MAX_VALUE;
                _agent.setTargetRotation(_rotation);
            }

            // likewise with moving
            if (timestamp >= _startMoving) {
                scheduleNextEvaluation();
                _agent.startMoving();
                _startMoving = Integer.MAX_VALUE;
            }

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

        @Override // documentation inherited
        public void reachedTargetRotation ()
        {
            BehaviorConfig.Wander config = (BehaviorConfig.Wander)_config;
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

        @Override // documentation inherited
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

        @Override // documentation inherited
        protected void didInit ()
        {
            _origin.set(_agent.getTranslation());
        }

        @Override // documentation inherited
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
            int pause = (int)(config.preRotationPause.getValue() * 1000f);
            float rot = FloatMath.normalizeAngle(rotation + config.directionChange.getValue());
            postponeNextEvaluation();
            if (pause == 0) {
                _startRotating = _startMoving = Integer.MAX_VALUE;
                _agent.setTargetRotation(rot);
            } else {
                _rotation = rot;
                _startRotating = _scenemgr.getTimestamp() + pause;
                _startMoving = Integer.MAX_VALUE;
            }
        }

        /** The translation of the actor when initialized. */
        protected Vector2f _origin = new Vector2f();

        /** The time at which we should start rotating. */
        protected int _startRotating = Integer.MAX_VALUE;

        /** The rotation that we will face when we stop pausing. */
        protected float _rotation;

        /** The time at which we should start moving. */
        protected int _startMoving = Integer.MAX_VALUE;
    }

    /**
     * Base class for behaviors that involve following paths.
     */
    public static abstract class Pathing extends Evaluating
    {
        @Override // documentation inherited
        public void tick (int timestamp)
        {
            super.tick(timestamp);
            if (_path == null) {
                return; // nothing to do
            }

            // see if we've reached the current node (looping around in case the notification
            // sets us on a new path)
            Vector2f trans = _agent.getTranslation();
            while (_path[_pidx].distance(trans) <= getReachRadius()) {
                if (++_pidx == _path.length) {
                    _agent.stopMoving();
                    _path = null;
                    completedPath();
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
            if (FloatMath.getAngularDistance(_agent.getRotation(), rot) > 0.0001f) {
                _agent.stopMoving();
                _agent.setTargetRotation(rot);
            } else {
                _agent.startMoving();
            }
        }

        /**
         * Sets the path to follow.
         */
        protected void setPath (Vector2f[] path)
        {
            _path = path;
            _pidx = 0;
        }

        /**
         * Clears the path.
         */
        protected void clearPath ()
        {
            _agent.stopMoving();
            _path = null;
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

        /** The waypoints of the path being followed. */
        protected Vector2f[] _path;

        /** The index of the next point on the path. */
        protected int _pidx;
    }

    /**
     * Handles the patrol behavior.
     */
    public static class Patrol extends Pathing
    {
        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((BehaviorConfig.Patrol)_config).target, _agent);
        }

        @Override // documentation inherited
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
            PathCandidate candidate = RandomUtil.pickRandom(_candidates);
            _candidates.clear();

            // set off on that path
            if (candidate != null) {
                setPath(candidate.getRemainingPath(_agent.getRotation()), candidate.getTarget());
            }
        }

        @Override // documentation inherited
        protected void reachedPathIndex (int idx)
        {
            evaluate();
        }

        @Override // documentation inherited
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
        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((BehaviorConfig.Follow)_config).target, _agent);
        }

        @Override // documentation inherited
        protected void evaluate ()
        {
            super.evaluate();

            // find the closest target
            _target.resolve(_agent, _targets);
            Vector2f trans = _agent.getTranslation();
            Logic ctarget = null;
            float cdist = Float.MAX_VALUE;
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                float dist = target.getTranslation().distanceSquared(trans);
                if (dist < cdist) {
                    ctarget = target;
                    cdist = dist;
                }
            }
            _targets.clear();

            // if we're within our distance bounds, stop and face the target
            if (ctarget == null) {
                return;
            }
            BehaviorConfig.Follow config = (BehaviorConfig.Follow)_config;
            float min2 = config.minimumDistance*config.minimumDistance;
            float max2 = config.maximumDistance*config.maximumDistance;
            if (FloatMath.isWithin(cdist, min2, max2)) {
                clearPath();
                _agent.face(ctarget);
                return;
            }

            // compute a path to the target
            Vector2f loc = ctarget.getTranslation();
            Vector2f[] path = _scenemgr.getPathfinder().getPath(
                _agent, MAX_FOLLOW_PATH_LENGTH, loc.x, loc.y, true, true);
            if (path == null) {
                clearPath();
                _agent.face(ctarget);
                return;
            }

            // start out on the path
            setPath(path);
        }

        /** The target to follow. */
        protected TargetLogic _target;

        /** Holds targets during processing. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Handles the random behavior.
     */
    public static class Random extends Evaluating
    {
        @Override // documentation inherited
        public void tick (int timestamp)
        {
            super.tick(timestamp);
            if (_active != null) {
                _active.tick(timestamp);
            }
        }

        @Override // documentation inherited
        public void reachedTargetRotation ()
        {
            if (_active != null) {
                _active.reachedTargetRotation();
            }
        }

        @Override // documentation inherited
        public void penetratedEnvironment (Vector2f penetration)
        {
            if (_active != null) {
                _active.penetratedEnvironment(penetration);
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            WeightedBehavior[] wbehaviors = ((BehaviorConfig.Random)_config).behaviors;
            _weights = new float[wbehaviors.length];
            _behaviors = new BehaviorLogic[wbehaviors.length];
            for (int ii = 0; ii < wbehaviors.length; ii++) {
                WeightedBehavior wbehavior = wbehaviors[ii];
                _weights[ii] = wbehavior.weight;
                _behaviors[ii] = _agent.createBehavior(wbehavior.behavior);
            }
        }

        @Override // documentation inherited
        protected void evaluate ()
        {
            super.evaluate();
            int nidx = RandomUtil.getWeightedIndex(_weights);
            BehaviorLogic nactive = (nidx == -1) ? null : _behaviors[nidx];
            if (nactive == _active) {
                return;
            }
            if ((_active = nactive) != null) {
                _active.startup();
            }
        }

        /** The behavior weights. */
        protected float[] _weights;

        /** The component behaviors. */
        protected BehaviorLogic[] _behaviors;

        /** The active behavior. */
        protected BehaviorLogic _active;
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

    @Override // documentation inherited
    public boolean isActive ()
    {
        return _agent.isActive();
    }

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _agent.getTranslation();
    }

    @Override // documentation inherited
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
