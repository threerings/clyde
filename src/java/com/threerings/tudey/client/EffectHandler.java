//
// $Id$

package com.threerings.tudey.client;

import com.threerings.tudey.data.Effect;
import com.threerings.tudey.data.TudeyPlaceObject;
import com.threerings.tudey.util.TudeyContext;

/**
 * Handles the application and display of an effect.
 */
public abstract class EffectHandler
{
    /**
     * Initializes the handler for display.
     */
    public void init (TudeyContext ctx, TudeyPlaceView view, Effect effect)
    {
        _ctx = ctx;
        _view = view;
        _tobj = _view.getTudeyPlaceObject();
        _effect = effect;

        // give subclasses a chance to initialize themselves
        didInit();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
    }

    /** The application context. */
    protected TudeyContext _ctx;

    /** The containing view. */
    protected TudeyPlaceView _view;

    /** The place object. */
    protected TudeyPlaceObject _tobj;

    /** The effect being handled. */
    protected Effect _effect;
}
