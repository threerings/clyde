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

package com.threerings.tudey.util;

import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;

/**
 * Advancer for mobile actors.
 */
public class MobileAdvancer extends ActorAdvancer
{
    /**
     * Creates a new advancer for the supplied mobile.
     */
    public MobileAdvancer (Environment environment, Mobile mobile, int timestamp)
    {
        super(environment, mobile, timestamp);
    }

    @Override
    public void init (Actor actor, int timestamp)
    {
        super.init(actor, timestamp);
        _mobile = (Mobile)actor;

        // set the mobile in motion if just created and so configured
        if (timestamp == _mobile.getCreated() &&
                ((ActorConfig.Mobile)_mobile.getOriginal()).startMoving) {
            _mobile.setDirection(_mobile.getRotation());
            _mobile.set(Mobile.MOVING);
        }
    }

    @Override
    protected void step (float elapsed)
    {
        takeSubsteps(elapsed);
    }

    /**
     * Executes the substeps required for the current step.
     */
    protected void takeSubsteps (float elapsed)
    {
        while (elapsed > 0f) {
            float nelapsed = Math.max(elapsed - MAX_SUBSTEP, 0f);
            substep(elapsed - nelapsed, _timestamp - (int)(nelapsed*1000f));
            elapsed = nelapsed;
        }
    }

    /**
     * Executes a substep of the specified duration.
     *
     * @param timestamp the timestamp at the end of the substep.
     */
    protected void substep (float elapsed, int timestamp)
    {
        // save the mobile's translation
        _otrans.set(_mobile.getTranslation());

        // take a step
        mobileStep(elapsed, timestamp);

        // make sure we actually moved
        Vector2f translation = _mobile.getTranslation();
        if (translation.equals(_otrans)) {
            return;

        // make sure we didn't move too far
        } else if (translation.distanceSquared(_otrans) > _mobile.getMaxStepSquared()) {
            Vector2f step = translation.subtract(_otrans).normalizeLocal().
                multLocal(_mobile.getMaxStep());
            _otrans.add(step, translation);
        }
        _mobile.setDirty(true);

        // in several attempts, compute the penetration vector and use it to separate the mobile
        // from whatever it's penetrating
        for (int ii = 0; ii < 3; ii++) {
            updateShape();
            if (!_environment.getPenetration(_mobile, _shape, _penetration)) {
                return;
            }
            // add a little more than we need, to be safe
            _mobile.getTranslation().addScaledLocal(_penetration, 1.001f);
        }

        // if the mobile is still penetrating, just revert to the original translation
        _mobile.getTranslation().set(_otrans);
    }

    /**
     * Executes a step on the mobile.
     */
    protected void mobileStep (float elapsed, int timestamp)
    {
        _mobile.step(elapsed, timestamp, _environment.getDirections(_mobile, _shape));
    }

    /** A casted reference to the mobile. */
    protected Mobile _mobile;

    /** Stores the penetration vector. */
    protected Vector2f _penetration = new Vector2f();

    /** Used to store the mobile's original translation. */
    protected Vector2f _otrans = new Vector2f();

    /** The length, in seconds, of the longest substep we're willing to take. */
    protected static final float MAX_SUBSTEP = 1f / 60f;
}
