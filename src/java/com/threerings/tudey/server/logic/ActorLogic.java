//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Controls the state of an actor on the server.
 */
public abstract class ActorLogic extends Logic
{
    /**
     * Creates a new actor logic object.
     */
    public ActorLogic (TudeySceneManager scenemgr)
    {
        super(scenemgr);
    }

    /**
     * Returns a reference to the current state of the actor.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /** The current state of the actor. */
    protected Actor _actor;
}
