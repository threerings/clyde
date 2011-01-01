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

package com.threerings.tudey.data.actor;

import com.threerings.io.Streamable;

import com.threerings.math.FloatMath;

/**
 * Limits the directions a mobile can step in.
 */
public class StepLimiter
    implements Streamable
{
    /**
     * Creates a new StepLimiter.
     */
    public StepLimiter (float minDirection, float maxDirection)
    {
        _minDirection = FloatMath.normalizeAngle(minDirection);
        _maxDirection = FloatMath.normalizeAngle(maxDirection);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public StepLimiter ()
    {
    }

    /**
     * Returns true if the direction is valid for stepping.
     */
    public boolean canStep (float direction)
    {
        if (_minDirection < _maxDirection) {
            return (direction >= _minDirection) && (direction <= _maxDirection);
        }
        return (direction >= _minDirection) || (direction <= _maxDirection);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (other instanceof StepLimiter) {
            StepLimiter olimiter = (StepLimiter)other;
            return olimiter._minDirection == _minDirection &&
                olimiter._maxDirection == _maxDirection;
        }
        return false;
    }

    /** The minimum angle for valid steps. */
    protected float _minDirection;

    /** The maximum angle for valid steps. */
    protected float _maxDirection;
}
