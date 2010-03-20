//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

import com.samskivert.util.ArrayUtil;

import com.threerings.config.ConfigReference;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;
import com.threerings.util.DeepOmit;

import com.threerings.tudey.config.ActorConfig;

/**
 * An actor capable of moving by itself.
 */
public class Mobile extends Actor
{
    /** A flag indicating that the actor is in motion. */
    public static final int MOVING = (Actor.LAST_FLAG << 1);

    /** The value of the last flag defined in this class. */
    public static final int LAST_FLAG = MOVING;

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
        setDirty(true);
    }

    /**
     * Returns the direction of motion.
     */
    public float getDirection ()
    {
        return _direction;
    }

    /**
     * Returns the direction of rotation (+1 if counterclockwise, -1 if clockwise, 0 if none).
     */
    public int getTurnDirection ()
    {
        return 0;
    }

    /**
     * Returns the rate of turning (radians per second).
     */
    public float getTurnRate ()
    {
        return 0f;
    }

    /**
     * Adds a step limiter.
     */
    public void addStepLimiter (StepLimiter limiter)
    {
        if (_limiters == null) {
            _limiters = new StepLimiter[1];
            _limiters[0] = limiter;
        } else {
            _limiters = ArrayUtil.append(_limiters, limiter);
        }
        setDirty(true);
    }

    /**
     * Removes a step limiter.
     */
    public void removeStepLimiter (StepLimiter limiter)
    {
        if (_limiters != null) {
            int idx = ArrayUtil.indexOf(_limiters, limiter);
            if (idx == 0 && _limiters.length == 1) {
                _limiters = null;
                setDirty(true);

            } else if (idx > -1) {
                _limiters = ArrayUtil.splice(_limiters, idx, 1);
                setDirty(true);
            }
        }
    }

    /**
     * Returns the (base) speed of the actor.
     */
    public float getSpeed ()
    {
        return ((ActorConfig.Mobile)_original).speed;
    }

    /**
     * Takes an Euler step of the specified duration.
     */
    public void step (float elapsed)
    {
        if (isSet(MOVING) && !isLimited(_direction)) {
            float length = getSpeed() * elapsed;
            _translation.addLocal(
                length * FloatMath.cos(_direction),
                length * FloatMath.sin(_direction));
            setDirty(true);
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

    @Override // documentation inherited
    public Object copy (Object dest)
    {
        Mobile result = (Mobile)super.copy(dest);
        result._direction = _direction;
        return result;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (!super.equals(other)) {
            return false;
        }
        Mobile omobile = (Mobile)other;
        return _direction == omobile._direction;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        int hash = super.hashCode();
        hash = 31*hash + Float.floatToIntBits(_direction);
        return hash;
    }

    /**
     * Returns true if a limited stops movement in the direction.
     */
    protected boolean isLimited (float direction)
    {
        if (_limiters != null) {
            for (StepLimiter limiter : _limiters) {
                if (!limiter.canStep(direction)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The step limiters. */
    protected StepLimiter[] _limiters;

    /** The direction of motion. */
    @DeepOmit
    protected float _direction;
}
