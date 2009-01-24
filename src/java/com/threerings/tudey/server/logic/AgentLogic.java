//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.math.FloatMath;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.actor.Mobile;

/**
 * Controls an autonomous agent.
 */
public class AgentLogic extends MobileLogic
{
    /**
     * Sets the target rotation for the agent to turn towards.
     */
    public void setTargetRotation (float rotation)
    {
        if (_actor.getRotation() == rotation) {
            reachedTargetRotation();
        } else {
            _targetRotation = rotation;
        }
    }

    /**
     * Sets the agent in motion.
     */
    public void startMoving ()
    {
        ((Mobile)_actor).setDirection(_actor.getRotation());
        _actor.set(Mobile.MOVING);
    }

    /**
     * Stops the agent.
     */
    public void stopMoving ()
    {
        _actor.clear(Mobile.MOVING);
    }

    @Override // documentation inherited
    public boolean tick (int timestamp)
    {
        // advance to current time
        super.tick(timestamp);

        // update the behavior
        _behavior.tick(timestamp);

        // compute the elapsed time since the last timestamp
        float elapsed = (timestamp - _timestamp) / 1000f;
        _timestamp = timestamp;

        // turn towards target rotation
        float rotation = _actor.getRotation();
        if (rotation != _targetRotation) {
            ActorConfig.Agent config = (ActorConfig.Agent)_config;
            float diff = FloatMath.getAngularDifference(_targetRotation, rotation);
            float angle = elapsed * config.turnRate;
            if (Math.abs(diff) <= angle) {
                _actor.setRotation(_targetRotation);
                reachedTargetRotation();
            } else {
                _actor.setRotation(FloatMath.normalizeAngle(rotation + angle * Math.signum(diff)));
            }
        }

        return true;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // initialize the target
        _targetRotation = _actor.getRotation();
        _timestamp = _actor.getCreated();

        // initialize the behavior logic
        ActorConfig.Agent aconfig = (ActorConfig.Agent)_config;
        _behavior = (BehaviorLogic)_scenemgr.createLogic(aconfig.behavior.getLogicClassName());
        if (_behavior == null) {
            _behavior = new BehaviorLogic.Idle();
        }
        _behavior.init(_scenemgr, aconfig.behavior, this);
    }

    /**
     * Called when we reach our target rotation.
     */
    protected void reachedTargetRotation ()
    {
        // notify the behavior
        _behavior.reachedTargetRotation();
    }

    /** The agent's behavior logic. */
    protected BehaviorLogic _behavior;

    /** The agent's target rotation. */
    protected float _targetRotation;

    /** The timestamp of the last tick. */
    protected int _timestamp;
}
