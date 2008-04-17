//
// $Id$

package com.threerings.tudey.server;

import com.threerings.tudey.data.Actor;
import com.threerings.tudey.data.ActorStream;
import com.threerings.tudey.data.ActorUpdate;
import com.threerings.tudey.data.TudeyCodes;

/**
 * Handles the behavior of an actor on the server.
 */
public class ActorLogic extends ServerLogic
{
    /**
     * Initializes the logic.
     */
    public void init (TudeyPlaceManagerDelegate delegate, Actor actor)
    {
        super.init(delegate);
        _actor = (Actor)actor.clone();
        _stream = new ActorStream(_tobj.getTimestamp(), actor, TudeyCodes.MAX_LATENCY + TudeyCodes.INTERPOLATION_DELAY);

        // allow subclasses to perform custom initialization
        didInit();
    }

    /**
     * Returns a reference to the current state.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Publishes the state of the actor if it has changed.
     */
    public void maybePublish (ActorUpdate update)
    {
        Actor state = _tobj.getActors().get(_actor.getKey());
        if (state != null && !state.equals(_actor)) {
            state = (Actor)_actor.clone();
            _stream.updated(_tobj.getTimestamp(), state);
            update.add(state);
        }
    }

    /**
     * Computes and returns the actor's state at the specified timestamp by
     * interpolating between or extrapolating from the historical states.
     */
    public Actor getState (long timestamp)
    {
        _stream.seek(timestamp);
        return _stream.getActor();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
    }

    /** The "rubber stamp" actor state. */
    protected Actor _actor;

    /** The actor state stream. */
    protected ActorStream _stream;
}
