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
        updateMovement(frame);
    }

    @Override // documentation inherited
    public void init (Actor actor, int timestamp)
    {
        super.init(actor, timestamp);
        _pawn = (Pawn)actor;
    }

    /**
     * Updates the pawn's movement state based on the given input frame.
     */
    protected void updateMovement (InputFrame frame)
    {
        updateRotation(frame);
        if (frame.isSet(InputFrame.MOVE)) {
            _pawn.setDirection(frame.getDirection());
            _pawn.set(Mobile.MOVING);
        } else {
            _pawn.clear(Mobile.MOVING);
        }
    }

    /**
     * Updates the pawn's rotation state based on the given input frame.
     */
    protected void updateRotation (InputFrame frame)
    {
        if (!frame.isSet(InputFrame.STRAFE)) {
            _pawn.setRotation(frame.getDirection());
        }
    }

    /** A casted reference to the pawn. */
    protected Pawn _pawn;
}
