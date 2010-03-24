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

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.data.actor.StepLimiter;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.MobileAdvancer;

/**
 * Controls the state of a mobile actor.
 */
public class MobileLogic extends ActorLogic
    implements TudeySceneManager.TickParticipant, ActorAdvancer.Environment
{
    /**
     * Adds or removes a step limiter.
     */
    public void stepLimit (float minDirection, float maxDirection, boolean remove)
    {
        StepLimiter limiter = new StepLimiter(minDirection, maxDirection);
        Mobile mobile = (Mobile)getActor();
        if (remove) {
            mobile.removeStepLimiter(limiter);
        } else {
            mobile.addStepLimiter(limiter);
        }
    }

    // documentation inherited from interface TudeySceneManager.TickParticipant
    public boolean tick (int timestamp)
    {
        // if enough time has elapsed without our being seen, go to sleep
        int sleepInterval = ((ActorConfig.Mobile)_config).sleepInterval;
        if (sleepInterval > 0 && timestamp - _snaptime > sleepInterval) {
            _scenemgr.removeTickParticipant(this);
            _awake = false;
            wentToSleep();
        }

        // advance to the current timestamp
        _advancer.advance(timestamp);

        // note and clear penetration
        if (_penetrationCount > 0) {
            penetratedEnvironment(_penetrationSum.multLocal(1f / _penetrationCount));
            _penetrationCount = 0;
            _penetrationSum.set(Vector2f.ZERO);
        }

        // update the actor's shape, notify any sensors
        updateShape();
        _scenemgr.triggerIntersectionSensors(timestamp, this);

        return true;
    }

    // documentation inherited from ActorAdvancer.Environment
    public boolean getPenetration (Actor actor, Shape shape, Vector2f result)
    {
        if (!_scenemgr.getPenetration(actor, shape, result)) {
            return false;
        }
        // record penetration info
        _penetrationCount++;
        _penetrationSum.addLocal(result);
        return true;
    }

    @Override // documentation inherited
    public Actor getSnapshot ()
    {
        // wake the actor up if it's asleep
        if (!_awake) {
            _scenemgr.addTickParticipant(this);
            _awake = true;
            _advancer.jump(_scenemgr.getTimestamp());
            wokeUp();
        }
        return super.getSnapshot();
    }

    @Override // documentation inherited
    public void destroy (int timestamp, Logic activator)
    {
        super.destroy(timestamp, activator);

        // deregister as tick participant
        _scenemgr.removeTickParticipant(this);
    }

    @Override // documentation inherited
    protected Actor createActor (
        ConfigReference<ActorConfig> ref, int id, int timestamp,
        Vector2f translation, float rotation)
    {
        return new Mobile(ref, id, timestamp, translation, rotation);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        // create advancer
        _advancer = createAdvancer();

        // set the actor in motion if appropriate
        ActorConfig.Mobile config = (ActorConfig.Mobile)_config;
        if (config.moving) {
            ((Mobile)_actor).setDirection(_actor.getRotation());
            _actor.set(Mobile.MOVING);
        }

        // start ticking immediately if we start out awake
        if (_awake = config.awake) {
            _scenemgr.addTickParticipant(this);
            wokeUp();
        }
    }

    /**
     * Creates the advancer to use to update the actor.
     */
    protected ActorAdvancer createAdvancer ()
    {
        return new MobileAdvancer(this, (Mobile)_actor, _actor.getCreated());
    }

    /**
     * Called when the actor wakes up.
     */
    protected void wokeUp ()
    {
        // nothing by default
    }

    /**
     * Called when the actor has gone back to sleep.
     */
    protected void wentToSleep ()
    {
        // nothing by default
    }

    /**
     * Notes that the actor collided with one or more things during its advancement.
     *
     * @param penetration the sum of the penetration vectors.
     */
    protected void penetratedEnvironment (Vector2f penetration)
    {
        // nothing by default
    }

    /** Used to advance the state of the actor. */
    protected ActorAdvancer _advancer;

    /** Whether or not the actor is awake. */
    protected boolean _awake;

    /** The number of penetrations. */
    protected int _penetrationCount;

    /** The penetration vector sum. */
    protected Vector2f _penetrationSum = new Vector2f();
}
