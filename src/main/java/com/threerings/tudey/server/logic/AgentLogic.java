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

import java.util.Map;

import com.threerings.config.ConfigReference;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.BehaviorConfig;
import com.threerings.tudey.data.actor.Agent;
import com.threerings.tudey.data.actor.Mobile;
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
     * Checks whether we can evaluate our behavior.
     */
    public boolean canThink ()
    {
        return _behavior != null;
    }

    /**
     * Returns the logic currently being targeted by our behavior, if any.
     */
    public Logic getBehaviorTarget ()
    {
        return _behavior == null ? null : _behavior.getCurrentTarget();
    }

    /**
     * Sets the target rotation to face another entity.
     */
    public void face (Logic logic)
    {
        face(logic, false);
    }

    /**
     * Sets the target rotation to face another entity.
     */
    public void face (Logic logic, boolean force)
    {
        float rotation = _actor.getTranslation().direction(logic.getTranslation());
        if (force && canRotate()) {
            _actor.setRotation(rotation);
            clearTargetRotation();
        } else {
            setTargetRotation(rotation);
        }
    }

    /**
     * Sets the target rotation for the agent to turn towards.
     */
    public void setTargetRotation (float rotation)
    {
        if ((_targetRotation = rotation) == _actor.getRotation()) {
            reachedTargetRotation();
        }
    }

    /**
     * Returns the target rotation.
     */
    public float getTargetRotation ()
    {
        return _targetRotation;
    }

    /**
     * Clears the agent's target rotation.
     */
    public void clearTargetRotation ()
    {
        _targetRotation = _actor.getRotation();
        ((Agent)_actor).setTurnDirection(0);
    }

    /**
     * Sets the turn rate.
     */
    public void setTurnRate (float rate)
    {
        if (rate > 0) {
            _turnRate = rate;
        } else {
            clearTurnRate();
        }
    }

    /**
     * Clears the turn rate.
     */
    public void clearTurnRate ()
    {
        _turnRate = ((ActorConfig.Agent)_config).turnRate;
    }

    /**
     * Sets the speed.
     */
    public void setSpeed (float speed)
    {
        ((Agent)_actor).setSpeed(speed);
    }

    /**
     * Clears a modified speed.
     */
    public void clearSpeed ()
    {
        ((Agent)_actor).setSpeed(((ActorConfig.Agent)_config).speed);
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
    public BehaviorLogic createBehavior (ConfigReference<BehaviorConfig> ref)
    {
        // create the logic instance
        BehaviorConfig config = _scenemgr.getConfigManager().getConfig(BehaviorConfig.class, ref);
        BehaviorConfig.Original original = config == null ? null :
            config.getOriginal(_scenemgr.getConfigManager());
        if (original == null) {
            original = new BehaviorConfig.Original();
        }
        BehaviorLogic logic = (BehaviorLogic)_scenemgr.createLogic(original.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize, return the logic
        logic.init(_scenemgr, original, this);
        return logic;
    }

    @Override
    public void transfer (Logic source, Map<Object, Object> refs)
    {
        super.transfer(source, refs);

        AgentLogic asource = (AgentLogic)source;
        _targetRotation = asource._targetRotation;
        _turnRate = asource._turnRate;
        _timestamp = asource._timestamp;
        _behavior.transfer(asource._behavior, refs);
    }

    @Override
    public boolean tick (int timestamp)
    {
        // advance to current time
        super.tick(timestamp);

        // update the behavior
        if (_behavior != null) {
            _behavior.tick(timestamp);
        }

        // compute the elapsed time since the last timestamp
        float elapsed = (timestamp - _timestamp) / 1000f;
        _timestamp = timestamp;

        // turn towards target rotation
        float rotation = _actor.getRotation();
        if (rotation != _targetRotation && canRotate()) {
            float diff = FloatMath.getAngularDifference(_targetRotation, rotation);
            float angle = elapsed * _turnRate;
            if (Math.abs(diff) - angle < FloatMath.EPSILON) {
                _actor.setRotation(_targetRotation);
                reachedTargetRotation();
            } else {
                float dir = Math.signum(diff);
                ((Agent)_actor).setTurnDirection((int)dir);
                _actor.setRotation(FloatMath.normalizeAngle(rotation + angle * dir));
            }
        }

        return true;
    }

    @Override
    protected void wasDestroyed ()
    {
        super.wasDestroyed();
        _behavior.shutdown();
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        // initialize the target
        _targetRotation = _actor.getRotation();
        _timestamp = _actor.getCreated();

        // initialize the behavior logic
        _behavior = createBehavior(((ActorConfig.Agent)_config).behavior);
        _behavior.startup();

        clearTurnRate();
        clearSpeed();
    }

    @Override
    protected void penetratedEnvironment (Vector2f penetration)
    {
        // notify the behavior
        _behavior.penetratedEnvironment(penetration);
    }

    /**
     * Called when we reach our target rotation.
     */
    protected void reachedTargetRotation ()
    {
        // clear turn direction
        ((Agent)_actor).setTurnDirection(0);

        // notify the behavior
        _behavior.reachedTargetRotation();
    }

    /** The agent's behavior logic. */
    protected BehaviorLogic _behavior;

    /** The agent's target rotation. */
    protected float _targetRotation;

    /** The agent's turn rate. */
    protected float _turnRate;

    /** The timestamp of the last tick. */
    protected int _timestamp;
}
