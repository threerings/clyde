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

package com.threerings.tudey.data.actor;

import com.threerings.config.ConfigReference;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;

/**
 * An actor capable of moving by itself.
 */
public class Mobile extends Actor
{
    /** A flag indicating that the actor is in motion. */
    public static final int MOVING = (Actor.LAST_FLAG << 1);

    /** A flag indicating that the actor is interacting. */
    public static final int INTERACTING = (Actor.LAST_FLAG << 2);

    /** The value of the last flag defined in this class. */
    public static final int LAST_FLAG = INTERACTING;

    /**
     * Creates a new mobile actor.
     */
    public Mobile (
        ConfigReference<ActorConfig> config, int id, int created,
        Vector2f translation, float rotation)
    {
        super(config, id, created, translation, rotation);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Mobile ()
    {
    }

    /**
     * Sets the direction of motion.
     */
    public void setDirection (float direction)
    {
        _direction = direction;
    }

    /**
     * Returns the direction of motion.
     */
    public float getDirection ()
    {
        return _direction;
    }

    /**
     * Returns the (base) speed of the actor.
     */
    public float getSpeed ()
    {
        return ((ActorConfig.Mobile)_original).speed;
    }

    /**
     * Sets the action timestamp.
     */
    public void setActed (int acted)
    {
        _acted = acted;
    }

    /**
     * Returns the action timestamp.
     */
    public int getActed ()
    {
        return _acted;
    }

    /**
     * Takes an Euler step of the specified duration.
     */
    public void step (float elapsed)
    {
        if (isSet(MOVING)) {
            float length = getSpeed() * elapsed;
            _translation.addLocal(
                length * FloatMath.cos(_direction),
                length * FloatMath.sin(_direction));
        }
    }

    @Override // documentation inherited
    public Actor extrapolate (float elapsed, Actor result)
    {
        super.extrapolate(elapsed, result);

        // take a step of the indicated duration
        ((Mobile)result).step(elapsed);

        return result;
    }

    /** The direction of motion. */
    protected float _direction;

    /** The action timestamp. */
    protected int _acted;
}
