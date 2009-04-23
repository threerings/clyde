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

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.BehaviorConfig;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.ActiveAdvancer;

/**
 * Controls an autonomous agent.
 */
public class AgentLogic extends ActiveLogic
{
    /**
     * Checks whether we can move.
     */
    public boolean canMove ()
    {
        return ((ActiveAdvancer)_advancer).canMove();
    }

    /**
     * Checks whether we can rotate.
     */
    public boolean canRotate ()
    {
        return ((ActiveAdvancer)_advancer).canRotate();
    }

    /**
     * Sets the target rotation to face another entity.
     */
    public void face (Logic logic)
    {
        setTargetRotation(_actor.getTranslation().direction(logic.getTranslation()));
    }

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
        if (canMove()) {
            ((Mobile)_actor).setDirection(_actor.getRotation());
            _actor.set(Mobile.MOVING);
        }
    }

    /**
     * Stops the agent.
     */
    public void stopMoving ()
    {
        _actor.clear(Mobile.MOVING);
    }

    /**
     * Creates a behavior for this agent.
     */
    public BehaviorLogic createBehavior (BehaviorConfig config)
    {
        // create the logic instance
        BehaviorLogic logic = (BehaviorLogic)_scenemgr.createLogic(config.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize, return the logic
        logic.init(_scenemgr, config, this);
        return logic;
    }

    @Override // documentation inherited
    public boolean tick (int timestamp)
    {
        // advance to current time
        super.tick(timestamp);

        // update the view shape
        ActorConfig.Agent config = (ActorConfig.Agent)_config;
        _viewShape = config.viewShape.getShape().transform(_shape.getTransform(), _viewShape);

        // update the behavior
        if (canThink()) {
            _behavior.tick(timestamp);
        }

        // compute the elapsed time since the last timestamp
        float elapsed = (timestamp - _timestamp) / 1000f;
        _timestamp = timestamp;

        // turn towards target rotation
        float rotation = _actor.getRotation();
        if (rotation != _targetRotation && canRotate()) {
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
        _behavior = createBehavior(((ActorConfig.Agent)_config).behavior);
    }

    @Override // documentation inherited
    protected void penetratedEnvironment (Vector2f penetration)
    {
        // notify the behavior
        _behavior.penetratedEnvironment(penetration);
    }

    /**
     * Checks whether we can think.
     */
    protected boolean canThink ()
    {
        return true;
    }

    /**
     * Called when we reach our target rotation.
     */
    protected void reachedTargetRotation ()
    {
        // notify the behavior
        _behavior.reachedTargetRotation();
    }

    /** The agent's view shape. */
    protected Shape _viewShape;

    /** The agent's behavior logic. */
    protected BehaviorLogic _behavior;

    /** The agent's target rotation. */
    protected float _targetRotation;

    /** The timestamp of the last tick. */
    protected int _timestamp;
}
