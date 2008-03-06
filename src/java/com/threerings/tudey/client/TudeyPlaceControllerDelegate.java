//
// $Id$

package com.threerings.tudey.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceControllerDelegate;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.tudey.data.TudeyPlaceObject;

/**
 * Controls the 2D scene.
 */
public class TudeyPlaceControllerDelegate extends PlaceControllerDelegate
{
    public TudeyPlaceControllerDelegate (PlaceController ctrl)
    {
        super(ctrl);
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        _plobj = plobj;
        _tobj = (TudeyPlaceObject)_plobj;
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        _plobj = null;
        _tobj = null;
    }

    /** A reference to the place object. */
    protected PlaceObject _plobj;

    /** A casted reference to the place object. */
    protected TudeyPlaceObject _tobj;
}
