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
            // if we have exceeded the radius and are moving away from the origin, change direction
            Vector2f trans = _agent.getTranslation();
            if (trans.distance(_origin) > ((BehaviorConfig.Wander)_config).radius) {
                float angle = FloatMath.atan2(_origin.y - trans.y, _origin.x - trans.x);
                float rotation = _agent.getActor().getRotation();
                if (FloatMath.getAngularDistance(angle, rotation) > FloatMath.HALF_PI) {
                    changeDirection(angle);
                }
            }
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
        public void penetrated (Vector2f penetration)
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
            float delta = ((BehaviorConfig.Wander)_config).directionChange.getValue();
            _nextChange = Integer.MAX_VALUE;
            _agent.setTargetRotation(FloatMath.normalizeAngle(rotation + delta));
        }

        /** The translation of the actor when initialized. */
        protected Vector2f _origin = new Vector2f();

        /** The time at which we should next change directions. */
        protected int _nextChange;
    }

    /**
     * Handles the patrol behavior.
     */
    public static class Patrol extends BehaviorLogic
    {
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

    /**
     * Notifies the behavior that the agent has penetrated its environment during advancement.
     *
     * @param penetration the sum penetration vector.
     */
    public void penetrated (Vector2f penetration)
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
