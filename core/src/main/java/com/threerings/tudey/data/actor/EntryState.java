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

package com.threerings.tudey.data.actor;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;

/**
 * An actor that represents the state of an entry.  Entries themselves cannot have dynamic state
 * per se, so stateful entries must create actor instances to represent their dynamic state.  The
 * sprites for these actors may manipulate the entries' sprites.
 */
public class EntryState extends Actor
{
    /**
     * Creates a new entry state actor.
     */
    public EntryState (
        ConfigReference<ActorConfig> config, int id, int created,
        Vector2f translation, float rotation)
    {
        super(config, id, created, translation, rotation);
        _stateEntered = created;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public EntryState ()
    {
    }

    /**
     * Sets the key of the entry to which this actor corresponds.
     */
    public void setKey (Object key)
    {
        _key = key;
        setDirty(true);
    }

    /**
     * Returns the key of the entry to which this actor corresponds.
     */
    public Object getKey ()
    {
        return _key;
    }

    /**
     * Sets the state that the entry is in.
     */
    public void setState (int state, int entered)
    {
        _state = state;
        _stateEntered = entered;
        setDirty(true);
    }

    /**
     * Returns the state that the entry is in.
     */
    public int getState ()
    {
        return _state;
    }

    /**
     * Returns the time at which the entry entered its current state.
     */
    public int getStateEntered ()
    {
        return _stateEntered;
    }

    /** The key identifying the entry to which this actor corresponds. */
    protected Object _key;

    /** The state of the entry. */
    protected int _state;

    /** The time at which the entry entered its current state. */
    protected int _stateEntered;
}
