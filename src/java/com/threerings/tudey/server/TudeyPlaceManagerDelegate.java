//
// $Id$

package com.threerings.tudey.server;

import com.samskivert.util.Interval;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.PresentsServer;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManagerDelegate;

import com.threerings.media.timer.MediaTimer;
import com.threerings.util.TimerUtil;

import com.threerings.tudey.data.Tile;
import com.threerings.tudey.data.TudeyCodes;
import com.threerings.tudey.data.TudeyPlaceObject;

/**
 * Manages the 2D scene.
 */
public class TudeyPlaceManagerDelegate extends PlaceManagerDelegate
    implements TudeyCodes, TudeyProvider
{
    @Override // documentation inherited
    public void didStartup (PlaceObject plobj)
    {
        _plobj = plobj;
        _tobj = (TudeyPlaceObject)plobj;

        // create the timer, start the ticker
        _timer = TimerUtil.createTimer();
        _ticker = new Interval(PresentsServer.omgr) {
            public void expired () {
                tick();
            }
        };
        _ticker.schedule(getTickInterval(), true);
    }

    @Override // documentation inherited
    public void didShutdown ()
    {
        // stop the ticker
        _ticker.cancel();
        _ticker = null;
    }

    /**
     * Returns a casted reference to the place object.
     */
    public TudeyPlaceObject getTudeyPlaceObject ()
    {
        return _tobj;
    }

    // documentation inherited from interface TudeyProvider
    public void placeTile (ClientObject caller, Tile tile)
    {

    }

    /**
     * Updates the state of the place on a regular basis.
     */
    protected void tick ()
    {
        // get the elapsed time and reset the timer
        long timestamp = _timer.getElapsedMillis();
        long emillis = (_lastTick > 0L) ? (timestamp - _lastTick) : 0L;
        float elapsed = emillis / 1000f;
        _lastTick = timestamp;

        // advance the
        _plobj.startTransaction();
        try {


        } finally {
            _plobj.commitTransaction();
        }
    }

    /**
     * Returns the interval, in milliseconds, at which to call the tick method.
     */
    protected long getTickInterval ()
    {
        return 100L;
    }

    /**
     * Resets the updater timer.
     */
    protected void resetTimer ()
    {
        _lastTick = 0L;
    }

    /** The uncasted place object. */
    protected PlaceObject _plobj;

    /** A casted reference to the place object. */
    protected TudeyPlaceObject _tobj;

    /** Calls the tick method at a regular rate. */
    protected Interval _ticker;

    /** Used to measure elapsed time. */
    protected MediaTimer _timer;

    /** The time of the last update tick. */
    protected long _lastTick;
}
