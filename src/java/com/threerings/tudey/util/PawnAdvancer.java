//
// $Id$

package com.threerings.tudey.util;

import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.data.actor.Pawn;
import com.threerings.tudey.data.InputFrame;

/**
 * Used on the client and the server to advance the state of a pawn based on its inputs and
 * surroundings.
 */
public class PawnAdvancer extends MobileAdvancer
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

    /** A casted reference to the pawn. */
    protected Pawn _pawn;
}
