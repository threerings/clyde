//
// $Id$

package com.threerings.tudey.server;

import com.samskivert.util.Interval;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.server.SceneManager;

import com.threerings.tudey.data.TudeySceneObject;

/**
 * Manager for Tudey scenes.
 */
public class TudeySceneManager extends SceneManager
{
    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return (_tsobj = new TudeySceneObject());
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // start the ticker
        _ticker = new Interval(_omgr) {
            public void expired () {
                tick();
            }
        };
        _ticker.schedule(getTickInterval(), true);
    }

    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();

        // stop the ticker
        _ticker.cancel();
        _ticker = null;
    }

    /**
     * Returns the interval at which we call the {@link #tick} method.
     */
    protected long getTickInterval ()
    {
        return 50L;
    }

    /**
     * Updates the scene.
     */
    protected void tick ()
    {
    }

    /** A casted reference to the Tudey scene object. */
    protected TudeySceneObject _tsobj;

    /** The tick interval. */
    protected Interval _ticker;
}
