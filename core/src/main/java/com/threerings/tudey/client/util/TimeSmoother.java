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

package com.threerings.tudey.client.util;

import com.samskivert.util.RunAnywhere;

import com.threerings.math.FloatMath;

/**
 * Provides a continuous estimate of the current time (with millisecond precision) based on a
 * series of discrete time updates, speeding up or slowing down (but never reversing) the local
 * clock as necessary to stay in synch.
 */
public class TimeSmoother
{
    /**
     * Creates a new smoother with the given initial clock value.
     */
    public TimeSmoother (int time)
    {
        _delta = _tdelta = time - RunAnywhere.currentTimeMillis();
    }

    /**
     * Updates the smoother with the current value of the clock being tracked.
     */
    public void update (int time)
    {
        _dstamp = RunAnywhere.currentTimeMillis();
        _tdelta = time - _dstamp;
    }

    /**
     * Returns the current smoothed time estimate.
     */
    public int getTime ()
    {
        // approach the target delta if not yet there
        long now = RunAnywhere.currentTimeMillis();
        if (_delta != _tdelta && now != _dstamp) {
            long elapsed = now - _dstamp;
            float pct = 1f - FloatMath.exp(CONVERGENCE_RATE * elapsed);
            long diff = (long)((_tdelta - _delta) * pct);
            if (diff == 0) {
                diff = (_tdelta < _delta) ? -1L : +1L;
            }
            // make sure we don't go back in time
            _delta += Math.max(-elapsed, diff);
            _dstamp = now;
        }
        return (int)(now + _delta);
    }

    /** The current estimated difference between the tracked time and the timer time. */
    protected long _delta;

    /** The target delta. */
    protected long _tdelta;

    /** The timestamp of the current delta value. */
    protected long _dstamp;

    /** The exponential rate at which we approach our target delta. */
    protected static final float CONVERGENCE_RATE = FloatMath.log(0.5f) / 250f;
}
