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
    public static ScriptLogic createScriptLogic (
            TudeySceneManager scenemgr, ScriptConfig config, AgentLogic agent)
    {
        // create the logic instance
        ScriptLogic logic = (ScriptLogic)scenemgr.createLogic(config.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize then return the logic
        logic.init(scenemgr, config, agent);
        return logic;
    }

    /**
     * Handles a wait script.
     */
    public static class Wait extends ScriptLogic
    {
        @Override // documentation inherited
        public void start (int timestamp)
        {
            _agent.stopMoving();
            _agent.clearTargetRotation();
            _started = timestamp;
        }

        @Override // documentation inherited
        public boolean tick (int timestamp)
        {
            return timestamp - _started > ((ScriptConfig.Wait)_config).wait;
        }

        /** When the wait started. */
        protected int _started;
    }

    /**
     * Handles a move script.
     */
    public static class Move extends ScriptLogic
    {
        @Override // documentation inherited
        public void start (int timestamp)
        {
            createPath();
        }

        @Override // documentation inherited
        public boolean tick (int timestamp)
        {
            if (_path == null || finishedMove()) {
                return true;
            }

            // see if we've reached the current node (looping around in case the notification
            // sets us on a new path)
            Vector2f trans = _agent.getTranslation();
            boolean completedPath = false;
            while (_path[_pidx].distance(trans) <= getReachRadius()) {
                if (++_pidx == _path.length) {
                    _agent.stopMoving();
                    createPath();
                }
                if (_path == null) {
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
            float dist = _finalTarget.getTranslation().distanceSquared(_agent.getTranslation());
            return dist*dist < getReachRadius();
        }

        @Override // documentation inherited
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
     * Handles the condition script.
     */
    public static class Condition extends ScriptLogic
    {
        @Override // documentation inherited
        public void start (int timestamp)
        {
            _agent.stopMoving();
            _agent.clearTargetRotation();
        }

        @Override // documentation inherited
        public boolean tick (int timestamp)
        {
            return _condition.isSatisfied(_agent);
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            super.didInit();

            _condition = createCondition(((ScriptConfig.Condition)_config).condition, _agent);
        }

        /** The condition to evaluate. */
        protected ConditionLogic _condition;
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, ScriptConfig config, AgentLogic agent)
    {
        super.init(scenemgr);
        _config = config;
        _agent = agent;

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
        return false;
    }

    /**
     * Called when we are about to start the current script.
     */
    public void start (int timestamp)
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

    /** The maximum path length for a move. */
    protected static final float MAX_PATH_LENGTH = 10f;
}
