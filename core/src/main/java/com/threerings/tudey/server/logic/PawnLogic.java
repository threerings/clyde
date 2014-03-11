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

import java.util.ArrayDeque;

import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.server.ClientLiaison;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.PawnAdvancer;

/**
 * Handles the state of a player-controlled actor.
 */
public class PawnLogic extends ActiveLogic
    implements NonTransferable
{
    /**
     * Notes that the controlling client has entered.
     */
    public void bodyEntered (ClientLiaison client)
    {
        _client = client;
    }

    /**
     * Enqueues a single frame of input for processing.
     */
    public void enqueueInput (InputFrame frame)
    {
        _input.addLast(frame);
    }

    /**
     * Computes and returns the difference between the time at which the controlling client depicts
     * this actor (its advanced time) and the time at which it depicts all other actors (its
     * delayed time).
     */
    public int getControlDelta ()
    {
        return (_client == null) ? 0 : _client.getControlDelta();
    }

    @Override
    public int getActivityAdvance ()
    {
        return getControlDelta()/2; // split the difference
    }

    @Override
    public boolean tick (int timestamp)
    {
        // process the enqueued input
        while (!_input.isEmpty() && _input.peekFirst().getTimestamp() <= timestamp) {
            _advancer.advance(_input.pollFirst());
        }

        // advance to the current timestamp, etc.
        return super.tick(timestamp);
    }

    @Override
    protected ActorAdvancer createAdvancer ()
    {
        return (_advancer = (PawnAdvancer)_actor.createAdvancer(this, _actor.getCreated()));
    }

    /** A casted reference to the advancer. */
    protected PawnAdvancer _advancer;

    /** The liaison for the controlling client. */
    protected ClientLiaison _client;

    /** The list of pending input frames for the pawn. */
    protected ArrayDeque<InputFrame> _input = new ArrayDeque<InputFrame>(4);
}
