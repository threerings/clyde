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

import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.ActorAdvancer;

/**
 * Controls the state of a mobile actor.
 */
public class MobileLogic extends ActorLogic
    implements TudeySceneManager.TickParticipant, ActorAdvancer.Environment
{
    /**
     * Returns the direction of the mobile.
     */
    public float getDirection ()
    {
        return ((Mobile)getActor()).getDirection();
    }

    // documentation inherited from interface TudeySceneManager.TickParticipant
    public boolean tick (int timestamp)
    {
        // if enough time has elapsed without our being seen, enter stasis
        int stasisInterval = ((ActorConfig.Mobile)_config).stasisInterval;
        if (stasisInterval > 0 && timestamp - _snaptime > stasisInterval) {
            _scenemgr.removeTickParticipant(this);
            _inStasis = true;
            enteredStasis();
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
    public TudeySceneModel getSceneModel ()
    {
        return _scenemgr.getSceneModel();
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

    // documentation inherited from ActorAdvancer.Environment
    public boolean collides (Actor actor, Shape shape)
    {
        return _scenemgr.collides(actor, shape);
    }

    // documentation inherited from ActorAdvancer.Environment
    public boolean collides (int mask, Shape shape)
    {
        return _scenemgr.collides(mask, shape);
    }

    // documentation inherited from ActorAdvancer.Environment
    public int getDirections (Actor actor, Shape shape)
    {
        return _scenemgr.getDirections(actor, shape);
    }

    @Override
    public Actor getSnapshot ()
    {
        // wake the actor up if it's in stasis
        if (_inStasis) {
            _scenemgr.addTickParticipant(this);
            _inStasis = false;
            _advancer.jump(_scenemgr.getTimestamp());
            leftStasis();
        }
        return super.getSnapshot();
    }

    @Override
    public void transfer (Logic source, Map<Object, Object> refs)
    {
        super.transfer(source, refs);

        MobileLogic msource = (MobileLogic)source;
        _advancer.transfer(msource._advancer);
        if (_inStasis == msource._inStasis) {
            return;
        }
        if (_inStasis = msource._inStasis) {
            _scenemgr.removeTickParticipant(this);
            enteredStasis();
        } else {
            _scenemgr.addTickParticipant(this);
            leftStasis();
        }
    }

    @Override
    protected void didInit ()
    {
        // create advancer
        _advancer = createAdvancer();

        // start ticking immediately if we don't start out in stasis
        ActorConfig.Mobile config = (ActorConfig.Mobile)_config;
        if (!(_inStasis = config.startInStasis)) {
            _scenemgr.addTickParticipant(this);
            leftStasis();
        }
    }

    @Override
    protected void wasDestroyed ()
    {
        super.wasDestroyed();

        // deregister as tick participant
        _scenemgr.removeTickParticipant(this);
    }

    /**
     * Creates the advancer to use to update the actor.
     */
    protected ActorAdvancer createAdvancer ()
    {
        return _actor.createAdvancer(this, _actor.getCreated());
    }

    /**
     * Called when the actor leaves stasis.
     */
    protected void leftStasis ()
    {
        // nothing by default
    }

    /**
     * Called when the actor enters stasis.
     */
    protected void enteredStasis ()
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

    @Override
    protected void updateShape ()
    {
        super.updateShape();
        Rect bounds = _shape.getBounds();
        float step = bounds.getShortestEdge() / 2;
        ((Mobile)getActor()).setMaxStep(step);
    }

    /** Used to advance the state of the actor. */
    protected ActorAdvancer _advancer;

    /** Whether or not the actor is in stasis. */
    protected boolean _inStasis;

    /** The number of penetrations. */
    protected int _penetrationCount;

    /** The penetration vector sum. */
    protected Vector2f _penetrationSum = new Vector2f();
}
