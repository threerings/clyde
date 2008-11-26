//
// $Id$

package com.threerings.tudey.util;

import com.threerings.tudey.data.actor.Actor;

/**
 * Used on the client and the server to advance the state of an actor based on its state and
 * surroundings.
 */
public class ActorAdvancer
{
    /**
     * Creates a new advancer for the supplied actor.
     */
    public ActorAdvancer (Actor actor, int timestamp)
    {
        init(actor, timestamp);
    }

    /**
     * (Re)initializes the advancer.
     */
    public void init (Actor actor, int timestamp)
    {
        _actor = actor;
        _timestamp = timestamp;
    }

    /**
     * Advances the actor to the specified timestamp.
     */
    public void advance (int timestamp)
    {
        if (timestamp <= _timestamp) {
            return;
        }
        float elapsed = (timestamp - _timestamp) / 1000f;
        _timestamp = timestamp;

        // take a step
        step(elapsed);
    }

    /**
     * Takes an Euler step of the specified length.
     */
    protected void step (float elapsed)
    {
        // nothing by default
    }

    /** The current actor state. */
    protected Actor _actor;

    /** The timestamp at which the state is valid. */
    protected int _timestamp;
}
