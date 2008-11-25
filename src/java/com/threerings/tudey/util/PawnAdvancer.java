//
// $Id$

package com.threerings.tudey.util;

import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.data.InputFrame;

/**
 * Used on the client and the server to advance the state of a pawn based on its inputs and
 * surroundings.
 */
public class PawnAdvancer
{
    /**
     * Creates a new advancer for the supplied pawn.
     */
    public PawnAdvancer (Mobile pawn, int timestamp)
    {
        _pawn = pawn;
        _timestamp = timestamp;
    }

    /**
     * Sets the pawn's current input.
     */
    public void setInput (InputFrame frame)
    {
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

    /**
     * Advances the pawn to the specified timestamp.
     */
    public void advance (int timestamp)
    {
        if (timestamp <= _timestamp) {
            return;
        }
        float elapsed = (timestamp - _timestamp) / 1000f;
        _timestamp = timestamp;

        // take an Euler step
        _pawn.step(elapsed);
    }

    /** The current pawn state. */
    protected Mobile _pawn;

    /** The timestamp at which the state is valid. */
    protected int _timestamp;
}
