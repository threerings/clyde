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

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ScriptConfig;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Performs script step logic for a scripted behavior.
 */
public abstract class ScriptLogic extends Logic
{
    /**
     * Creates a script logic.
     */
    public static ScriptLogic createScriptLogic (TudeySceneManager scenemgr, ScriptConfig config,
            AgentLogic agent, BehaviorLogic.Scripted scripted)
    {
        // create the logic instance
        ScriptLogic logic = (ScriptLogic)scenemgr.createLogic(config.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize then return the logic
        logic.init(scenemgr, config, agent, scripted);
        return logic;
    }

    /**
     * Handles a wait script.
     */
    public static class Wait extends ScriptLogic
    {
        @Override
        public void start (int timestamp)
        {
            _agent.stopMoving();
            _agent.clearTargetRotation();
            _started = timestamp;
        }

        @Override
        public boolean tick (int timestamp)
        {
            return timestamp - _started > ((ScriptConfig.Wait)_config).wait;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _started = ((Wait)source)._started;
        }

        /** When the wait started. */
        protected int _started;
    }

    /**
     * Handles a move script.
     */
    public static class Move extends ScriptLogic
    {
        @Override
        public void start (int timestamp)
        {
            createPath();
        }

        @Override
        public boolean tick (int timestamp)
        {
            if (_path == null || finishedMove()) {
                finishMove();
                return true;
            }

            // see if we've reached the current node (looping around in case the notification
            // sets us on a new path)
            Vector2f trans = _agent.getTranslation();
            boolean completedPath = false;
            while (_path[_pidx].distanceSquared(trans) <= getReachRadiusSquared()) {
                if (++_pidx == _path.length) {
                    _agent.stopMoving();
                    _path = null;
                    if (completedPath) {
                        finishMove();
                        return true;
                    }
                    createPath();
                    completedPath = true;
                }
                if (_path == null) {
                    finishMove();
                    return true;
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
            return false;
        }

        @Override
        public void suspend ()
        {
            _agent.stopMoving();
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            Move msource = (Move)source;
            _target.transfer(msource._target, refs);
            _path = msource._path;
            _pidx = msource._pidx;
            _finalTarget = (Logic)refs.get(msource._finalTarget);
        }

        /**
         * Returns the radius within which we can be consider ourselves to have reached a node
         * (which depends on the actor's speed, since it's possible to overshoot).
         */
        protected float getReachRadiusSquared ()
        {
            // radius is the distance we can travel in a single tick
            float dist = ((Mobile)_agent.getActor()).getSpeed() / _scenemgr.getTicksPerSecond();
            return dist * dist;
        }

        /**
         * Create the movement path.
         */
        protected void createPath ()
        {
            _path = null;
            ArrayList<Logic> targets = Lists.newArrayList();
            _target.resolve(_agent, targets);
            if (targets.isEmpty()) {
                return;
            }
            _finalTarget = targets.get(0);
            if (finishedMove()) {
                return;
            }
            Vector2f loc = _finalTarget.getTranslation();
            _path = _scenemgr.getPathfinder().getPath(
                    _agent, MAX_PATH_LENGTH, loc.x, loc.y, true, true);
            _pidx = 0;
        }

        /**
         * Checks if we've reached our target.
         */
        protected boolean finishedMove ()
        {
            float dist2 = getReachRadiusSquared();
            return _finalTarget.getTranslation().distanceSquared(_agent.getTranslation()) <= dist2;
        }

        /**
         * Position ourselves exactly on our target.
         */
        protected void finishMove ()
        {
            if (_finalTarget != null) {
                Vector2f translation = _finalTarget.getTranslation();
                _agent.move(translation.x, translation.y, _agent.getRotation());
            }
            _agent.stopMoving();
        }

        @Override
        protected void didInit ()
        {
            super.didInit();

            _target = createTarget(((ScriptConfig.Move)_config).target, _agent);
        }

        /** The move target. */
        protected TargetLogic _target;

        /** The path. */
        protected Vector2f[] _path;

        /** The index of the next point on the path. */
        protected int _pidx;

        /** The target location. */
        protected Logic _finalTarget;
    }

    /**
     * Handles the rotate script.
     */
    public static class Rotate extends ScriptLogic
    {
        @Override
        public void start (int timestamp)
        {
            ScriptConfig.Rotate config = (ScriptConfig.Rotate)_config;
            _agent.stopMoving();
            float rotation = config.direction +
                FloatMath.random(-config.rotationVariance, config.rotationVariance);
            if (config.relative) {
                rotation += _agent.getRotation();
            }
            _complete = false;
            _agent.setTargetRotation(FloatMath.normalizeAnglePositive(rotation));
        }

        @Override
        public void reachedTargetRotation ()
        {
            _complete = true;
        }

        @Override
        public boolean tick (int timestamp)
        {
            return _complete;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _complete = ((Rotate)source)._complete;
        }

        /** If we're done. */
        protected boolean _complete;
    }

    /**
     * Handles the condition script.
     */
    public static class Condition extends ScriptLogic
    {
        @Override
        public void start (int timestamp)
        {
            _agent.stopMoving();
            _agent.clearTargetRotation();
        }

        @Override
        public boolean tick (int timestamp)
        {
            return _condition.isSatisfied(_agent);
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _condition.transfer(((Condition)source)._condition, refs);
        }

        @Override
        protected void didInit ()
        {
            super.didInit();

            _condition = createCondition(((ScriptConfig.Condition)_config).condition, _agent);
        }

        /** The condition to evaluate. */
        protected ConditionLogic _condition;
    }

    /**
     * Handles the conditional script script.
     */
    public static class ConditionalScript extends Condition
    {
        @Override
        public void start (int timestamp)
        {
            super.start(timestamp);
            _satisfied = _condition.isSatisfied(_agent);
            if (_satisfied) {
                _success.start(timestamp);
            } else {
                _failure.start(timestamp);
            }
        }

        @Override
        public boolean tick (int timestamp)
        {
            return _satisfied ? _success.tick(timestamp) : _failure.tick(timestamp);
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _success.transfer(((ConditionalScript)source)._success, refs);
            _failure.transfer(((ConditionalScript)source)._failure, refs);
            _satisfied = ((ConditionalScript)source)._satisfied;
        }

        @Override
        protected void didInit ()
        {
            super.didInit();

            ScriptConfig.ConditionalScript config = (ScriptConfig.ConditionalScript)_config;
            _success = createScriptLogic(_scenemgr, config.success, _agent, _scripted);
            _failure = createScriptLogic(_scenemgr, config.failure, _agent, _scripted);
        }

        /** The condition to evaluate. */
        protected ConditionLogic _condition;

        /** The scriptlogic. */
        protected ScriptLogic _success;
        protected ScriptLogic _failure;

        /** The success state. */
        public boolean _satisfied;
    }

    /**
     * Handles the goto script.
     */
    public static class Goto extends ScriptLogic
    {
        @Override
        public void start (int timestamp)
        {
            _scripted.setCurrentStep(((ScriptConfig.Goto)_config).step, timestamp);
        }
    }

    /**
     * Handles an action script.
     */
    public static class Action extends ScriptLogic
    {
        @Override
        public void start (int timestamp)
        {
        }

        @Override
        public boolean tick (int timestamp)
        {
            _action.execute(timestamp, _agent);
            return true;
        }

        @Override
        public void shutdown ()
        {
            _action.removed();
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _action.transfer(((Action)source)._action, refs);
        }

        @Override
        protected void didInit ()
        {
            _action = createAction(((ScriptConfig.Action)_config).action, _agent);
        }

        /** The action to perform. */
        protected ActionLogic _action;
    }

    /**
     * Handles the set speed script.
     */
    public static class SetSpeed extends ScriptLogic
    {
        @Override
        public void start (int timestamp)
        {
            _agent.setSpeed(((ScriptConfig.SetSpeed)_config).speed);
        }
    }

    /**
     * Handles the clear speed script.
     */
    public static class ClearSpeed extends ScriptLogic
    {
        @Override
        public void start (int timestamp)
        {
            _agent.clearSpeed();
        }
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, ScriptConfig config, AgentLogic agent,
            BehaviorLogic.Scripted scripted)
    {
        super.init(scenemgr);
        _config = config;
        _agent = agent;
        _scripted = scripted;

        // give subclasses a change to initialize
        didInit();
    }

    /**
     * Ticks the script.
     *
     * @return true if the script has completed
     */
    public boolean tick (int timestamp)
    {
        return true;
    }

    /**
     * Called when we are about to start the current script.
     */
    public void start (int timestamp)
    {
        // nothing by default
    }

    /**
     * Called when we are suspending the current script.
     */
    public void suspend ()
    {
        // nothing by default
    }

    /**
     * Called when the behavior is shutdown.
     */
    public void shutdown ()
    {
        // nothing by default
    }

    /**
     * Called when the agent reached its target rotation.
     */
    public void reachedTargetRotation ()
    {
        // nothing by default
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /** Our configuration. */
    protected ScriptConfig _config;

    /** The controlled agent. */
    protected AgentLogic _agent;

    /** The scripted logic. */
    protected BehaviorLogic.Scripted _scripted;

    /** The maximum path length for a move. */
    protected static final float MAX_PATH_LENGTH = 10f;
}
