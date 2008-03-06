//
// $Id$

package com.threerings.tudey.client;

import com.threerings.math.FloatMath;

import com.threerings.media.timer.MediaTimer;
import com.threerings.util.TimerUtil;

/**
 * Provides a continuous estimate of the current time (with millisecond precision) based on a
 * series of discrete time updates, speeding up or slowing down (but never reversing) the local
 * clock as necessary to stay in synch.
 */
public class TimeSmoother
{
    /**
     * Creates a new smoother with the given initial clock value and paused state.
     */
    public TimeSmoother (long time, boolean paused)
    {
        if (paused) {
            _pauseTime = time;
        } else {
            _delta = _tdelta = time;
            _pauseTime = -1L;
        }
    }

    /**
     * Updates the smoother with the current value of the clock being tracked.
     */
    public void update (long time)
    {
        if (isPaused()) {
            _pauseTime = time;
        } else {
            _dstamp = _timer.getElapsedMillis();
            _tdelta = time - _dstamp;
        }
    }

    /**
     * Sets the paused state of the smoother.
     */
    public void setPaused (boolean paused)
    {
        if (isPaused() == paused) {
            return;
        }
        if (paused) {
            _pauseTime = getTime();
        } else {
            _dstamp = _timer.getElapsedMillis();
            _delta = _tdelta = _pauseTime - _dstamp;
            _pauseTime = -1L;
        }
    }

    /**
     * Determines whether the smoother is paused.
     */
    public boolean isPaused ()
    {
        return _pauseTime != -1L;
    }

    /**
     * Returns the current smoothed time estimate.
     */
    public long getTime ()
    {
        // if we're paused, return the pause time
        if (isPaused()) {
            return _pauseTime;
        }

        // approach the target delta if not yet there
        long now = _timer.getElapsedMillis();
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

    /** The backing timer. */
    protected MediaTimer _timer = TimerUtil.createTimer();

    /** The current estimated difference between the tracked time and the timer time. */
    protected long _delta;

    /** The target delta. */
    protected long _tdelta;

    /** The timestamp of the current delta value. */
    protected long _dstamp;

    /** If paused, the time at which we were paused (otherwise, -1). */
    protected long _pauseTime;

    /** The exponential rate at which we approach our target delta. */
    protected static final float CONVERGENCE_RATE = FloatMath.log(0.5f) / 250f;
}
