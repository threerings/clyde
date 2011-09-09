//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.data.actor.Pawn;
import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.shape.Shape;

/**
 * Used on the client and the server to advance the state of a pawn based on its inputs and
 * surroundings.
 */
public class PawnAdvancer extends ActiveAdvancer
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
        // save the input frame and advance
        _frame = frame;
        advance(frame.getTimestamp());
    }

    @Override // documentation inherited
    public void init (Actor actor, int timestamp)
    {
        super.init(actor, timestamp);
        _pawn = (Pawn)actor;
        _frame = null;
    }

    @Override // documentation inherited
    protected void step (float elapsed)
    {
        super.step(elapsed);

        // update based on most recent input
        if (_frame != null) {
            updateInput();
        }
    }

    @Override // documentation inherited
    protected void takeSubsteps (float elapsed)
    {
        // if the input frame provides a computed position, we shall attempt to validate
        if (_frame == null || _timestamp != _frame.getTimestamp() ||
                _frame.getTranslation() == null || ignoreInputPosition()) {
            super.takeSubsteps(elapsed);
            return;
        }

        // make sure this is cleared in case we don't take any mobile steps
        if (!canMove()) {
            _active.clear(Mobile.MOVING);
        }

        // make sure they haven't exceeded their speed
        Vector2f ptrans = _pawn.getTranslation();
        Vector2f ftrans = _frame.getTranslation();
        float distance = ptrans.distance(ftrans);
        if (distance == 0f) {
            return; // no movement, no problem
        }
        if (distance > _pawn.getSpeed() * elapsed + 0.5f) {
            super.takeSubsteps(elapsed);
            return;
        }

        // make sure they didn't run into anything
        updateShape();
        _swept = _shape.sweep(ftrans.subtract(ptrans, _penetration), _swept);
        if (_environment.collides(_pawn, _swept)) {
            super.takeSubsteps(elapsed);
            return;
        }

        // otherwise, assume validity
        ptrans.set(ftrans);
        _pawn.setDirty(true);
    }

    /**
     * Checks whether we should ignore the input position and instead perform the full movement
     * simulation.
     */
    protected boolean ignoreInputPosition ()
    {
        return false;
    }

    /**
     * Updates the pawn's state based on the current input frame.
     */
    protected void updateInput ()
    {
        Activity activity = getActivity();
        if (activity != null) {
            activity.updateInput();
        }
        if (canRotate()) {
            updateRotation();
        }
        if (_frame.isSet(InputFrame.MOVE) && canMove()) {
            _pawn.setDirection(_frame.getDirection());
            _pawn.set(Mobile.MOVING);
        } else {
            _pawn.clear(Mobile.MOVING);
        }
    }

    /**
     * Called to update a pawns rotation based on input.
     */
    protected void updateRotation ()
    {
        _pawn.setRotation(_frame.getRotation());
    }

    /** A casted reference to the pawn. */
    protected Pawn _pawn;

    /** The most current input frame. */
    protected InputFrame _frame;

    /** Holds the actor's swept shape. */
    protected Shape _swept;
}
