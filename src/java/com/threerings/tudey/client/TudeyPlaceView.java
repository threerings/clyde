//
// $Id$

package com.threerings.tudey.client;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.tudey.data.TudeyPlaceObject;
import com.threerings.tudey.util.TudeyContext;

/**
 * A view of the place.
 */
public class TudeyPlaceView
    implements PlaceView
{
    /**
     * Initializes the view.
     */
    public void init (TudeyContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Returns a casted reference to the place object.
     */
    public TudeyPlaceObject getTudeyPlaceObject ()
    {
        return _tobj;
    }

    /**
     * Enqueues the contents of the view for rendering.
     */
    public void enqueue ()
    {
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _plobj = plobj;
        _tobj = (TudeyPlaceObject)plobj;
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        _plobj = null;
        _tobj = null;
    }

    /** The application context. */
    protected TudeyContext _ctx;

    /** The place object. */
    protected PlaceObject _plobj;

    /** A casted reference to the place object. */
    protected TudeyPlaceObject _tobj;
}
