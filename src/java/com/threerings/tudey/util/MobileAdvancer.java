//
// $Id$

package com.threerings.tudey.util;

import com.threerings.math.Vector2f;

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

    @Override // documentation inherited
    public void init (Actor actor, int timestamp)
    {
        super.init(actor, timestamp);
        _mobile = (Mobile)actor;
    }

    @Override // documentation inherited
    protected void step (float elapsed)
    {
        while (elapsed > 0f) {
            substep(Math.min(elapsed, MAX_SUBSTEP));
            elapsed -= MAX_SUBSTEP;
        }
    }

    /**
     * Executes a substep of the specified duration.
     */
    protected void substep (float elapsed)
    {
        // save the mobile's translation
        _otrans.set(_mobile.getTranslation());

        // take a step
        _mobile.step(elapsed);

        // make sure we actually moved
        if (_mobile.getTranslation().equals(_otrans)) {
            return;
        }

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

    /** A casted reference to the mobile. */
    protected Mobile _mobile;

    /** Stores the penetration vector. */
    protected Vector2f _penetration = new Vector2f();

    /** Used to store the mobile's original translation. */
    protected Vector2f _otrans = new Vector2f();

    /** The length, in seconds, of the longest substep we're willing to take. */
    protected static final float MAX_SUBSTEP = 1f / 60f;
}
