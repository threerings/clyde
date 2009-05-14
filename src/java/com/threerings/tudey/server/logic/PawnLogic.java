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

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Pawn;
import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.server.ClientLiaison;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.PawnAdvancer;

/**
 * Handles the state of a player-controlled actor.
 */
public class PawnLogic extends ActiveLogic
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
        _input.add(frame);
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

    @Override // documentation inherited
    public boolean tick (int timestamp)
    {
        // process the enqueued input
        while (!_input.isEmpty() && _input.get(0).getTimestamp() <= timestamp) {
            _advancer.advance(_input.remove(0));
        }

        // advance to the current timestamp, etc.
        return super.tick(timestamp);
    }

    @Override // documentation inherited
    protected Actor createActor (
        ConfigReference<ActorConfig> ref, int id, int timestamp,
        Vector2f translation, float rotation)
    {
        return new Pawn(ref, id, timestamp, translation, rotation);
    }

    @Override // documentation inherited
    protected ActorAdvancer createAdvancer ()
    {
        return (_advancer = ((Pawn)_actor).createAdvancer(this, _actor.getCreated()));
    }

    @Override // documentation inherited
    protected int getActivityAdvance ()
    {
        return getControlDelta()/2; // split the difference
    }

    /** A casted reference to the advancer. */
    protected PawnAdvancer _advancer;

    /** The liaison for the controlling client. */
    protected ClientLiaison _client;

    /** The list of pending input frames for the pawn. */
    protected ArrayList<InputFrame> _input = new ArrayList<InputFrame>();
}
