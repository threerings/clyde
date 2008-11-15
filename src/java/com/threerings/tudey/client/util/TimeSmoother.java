//
// $Id$

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
    public TimeSmoother (long time)
    {
        _delta = _tdelta = time - RunAnywhere.currentTimeMillis();
    }

    /**
     * Updates the smoother with the current value of the clock being tracked.
     */
    public void update (long time)
    {
        _dstamp = RunAnywhere.currentTimeMillis();
        _tdelta = time - _dstamp;
    }

    /**
     * Returns the current smoothed time estimate.
     */
    public long getTime ()
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
        return now + _delta;
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
