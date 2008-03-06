//
// $Id$

package com.threerings.tudey.client;

import com.threerings.tudey.data.Actor;
import com.threerings.tudey.data.ActorStream;
import com.threerings.tudey.data.TudeyCodes;
import com.threerings.tudey.data.TudeyPlaceObject;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents an actor in the scene.
 */
public abstract class ActorSprite
    implements TudeyCodes
{
    /**
     * Initializes this sprite with its actor and the necessary application references.
     */
    public void init (TudeyContext ctx, TudeyPlaceView view, Actor actor)
    {
        _ctx = ctx;
        _view = view;
        _tobj = _view.getTudeyPlaceObject();
        _stream = new ActorStream(_tobj.getTimestamp(), actor, MAX_LATENCY + INTERPOLATION_DELAY);
        _actor = _stream.getActor();

        // give subclasses a chance to initialize themselves
        didInit();
    }

    /**
     * Returns a reference to the most up-to-date actor state object.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Updates the sprite with newly received state.
     */
    public void update (Actor state)
    {
        _stream.updated(_tobj.getTimestamp(), state);
    }

    /**
     * Notes that the actor has been removed.
     */
    public void remove ()
    {
        _stream.removed(_tobj.getTimestamp());
    }

    /**
     * Queues up the actor to draw.
     */
    public void enqueue ()
    {
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

    /** The actor's state stream. */
    protected ActorStream _stream;

    /** The actor state. */
    protected Actor _actor;
}
