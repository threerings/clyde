//
// $Id$

package com.threerings.tudey.util;

import com.threerings.math.Vector2f;

import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.data.actor.Pawn;
import com.threerings.tudey.data.InputFrame;

/**
 * Used on the client and the server to advance the state of a pawn based on its inputs and
 * surroundings.
 */
public class PawnAdvancer extends ActorAdvancer
{
    /**
     * Creates a new advancer for the supplied pawn.
     */
    public PawnAdvancer (Environment environment, Pawn pawn, int timestamp)
    {
        super(environment, pawn, timestamp);
    }

    /**
     * Advances to the timestamp of the provided input frame and sets the pawn's current input.
     */
    public void advance (InputFrame frame)
    {
        // advance to the input timestamp
        advance(frame.getTimestamp());

        // modify the pawn's state based on the input
        float direction = frame.getDirection();
        if (!frame.isSet(InputFrame.STRAFE)) {
            _pawn.setRotation(direction);
        }
        if (frame.isSet(InputFrame.MOVE)) {
            _pawn.setDirection(direction);
            _pawn.set(Mobile.MOVING);
        } else {
            _pawn.clear(Mobile.MOVING);
        }
    }

    @Override // documentation inherited
    public void init (Actor actor, int timestamp)
    {
        super.init(actor, timestamp);
        _pawn = (Pawn)actor;
    }

    @Override // documentation inherited
    protected void step (float elapsed)
    {
        float stepsize = 1f / 60f; // TODO: something smarter
        while (elapsed > 0f) {
            substep(Math.min(elapsed, stepsize));
            elapsed -= stepsize;
        }
    }

    /**
     * Executes a substep of the specified duration.
     */
    protected void substep (float elapsed)
    {
        // save the pawn's translation
        _otrans.set(_pawn.getTranslation());

        // take a step
        _pawn.step(elapsed);

        // make sure we actually moved
        if (_pawn.getTranslation().equals(_otrans)) {
            return;
        }

        // in several attempts, compute the penetration vector and use it to separate the pawn
        // from whatever it's penetrating
        for (int ii = 0; ii < 3; ii++) {
            updateShape();
            if (!_environment.getPenetration(_pawn, _shape, _penetration)) {
                return;
            }
            // add a little more than we need, to be safe
            _pawn.getTranslation().addScaledLocal(_penetration, 1.001f);
        }

        // if the pawn is still penetrating, just revert to the original translation
        _pawn.getTranslation().set(_otrans);
    }

    /** A casted reference to the pawn. */
    protected Pawn _pawn;

    /** Stores the penetration vector. */
    protected Vector2f _penetration = new Vector2f();

    /** Used to store the pawn's original translation. */
    protected Vector2f _otrans = new Vector2f();
}
