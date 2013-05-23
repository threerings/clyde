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
 * An autonomous agent controlled by the computer.
 */
public class Agent extends Active
{
    /** A flag indicating that the actor is turning left. */
    public static final int TURNING_LEFT = (Active.LAST_FLAG << 1);

    /** A flag indicating that the actor is turning right. */
    public static final int TURNING_RIGHT = (Active.LAST_FLAG << 2);

    /** The value of the last flag defined in this class. */
    public static final int LAST_FLAG = TURNING_RIGHT;

    /**
     * Creates a new agent.
     */
    public Agent (
        ConfigReference<ActorConfig> config, int id, int created,
        Vector2f translation, float rotation)
    {
        super(config, id, created, translation, rotation);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Agent ()
    {
    }

    /**
     * Sets the turn direction.
     */
    public void setTurnDirection (int dir)
    {
        set(TURNING_LEFT, dir == +1);
        set(TURNING_RIGHT, dir == -1);
    }

    @Override
    public float getSpeed ()
    {
        return _speed;
    }

    /**
     * Set the speed.
     */
    public void setSpeed (float speed)
    {
        _speed = speed;
    }

    @Override
    public int getTurnDirection ()
    {
        return isSet(TURNING_LEFT) ? +1 : (isSet(TURNING_RIGHT) ? -1 : 0);
    }

    @Override
    public float getTurnRate ()
    {
        return ((ActorConfig.Agent)_original).turnRate;
    }

    /** The agent speed. */
    protected float _speed;
}
