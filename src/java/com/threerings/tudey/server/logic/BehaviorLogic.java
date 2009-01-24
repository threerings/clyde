//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.BehaviorConfig;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Handles the server-side processing for agent behavior.
 */
public abstract class BehaviorLogic extends Logic
{
    /**
     * Handles the idle behavior.
     */
    public static class Idle extends BehaviorLogic
    {
    }

    /**
     * Handles the wander behavior.
     */
    public static class Wander extends BehaviorLogic
    {
        @Override // documentation inherited
        public void tick (int timestamp)
        {
            if (timestamp >= _nextChange) {
                changeDirection();
            }
        }

        @Override // documentation inherited
        public void reachedTargetRotation ()
        {
            _agent.startMoving();
            _nextChange = _scenemgr.getTimestamp() +
                (int)(((BehaviorConfig.Wander)_config).directionChangeInterval.getValue() * 1000f);
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            changeDirection();
        }

        /**
         * Changes the direction of the agent.
         */
        protected void changeDirection ()
        {
            _agent.stopMoving();
            float delta = ((BehaviorConfig.Wander)_config).directionChange.getValue();
            _agent.setTargetRotation(FloatMath.normalizeAngle(
                _agent.getActor().getRotation() + delta));
            _nextChange = Integer.MAX_VALUE;
        }

        /** The time at which we should next change directions. */
        protected int _nextChange;
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, BehaviorConfig config, AgentLogic agent)
    {
        super.init(scenemgr);
        _config = config;
        _agent = agent;

        // give subclasses a chance to initialize
        didInit();
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

    /** The behavior configuration. */
    protected BehaviorConfig _config;

    /** The controlled agent. */
    protected AgentLogic _agent;
}
