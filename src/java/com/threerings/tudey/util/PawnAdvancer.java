//
// $Id$

package com.threerings.tudey.util;

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
    public PawnAdvancer (Pawn pawn, int timestamp)
    {
        super(pawn, timestamp);
        _pawn = pawn;
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
    protected void step (float elapsed)
    {
        _pawn.step(elapsed);
    }

    /** A casted reference to the pawn. */
    protected Pawn _pawn;
}
