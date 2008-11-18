//
// $Id$

package com.threerings.tudey.dobj;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.tudey.data.actor.Actor;

/**
 * Combines a newly added actor with the timestamp of the addition.
 */
public final class AddedActor extends SimpleStreamableObject
{
    /**
     * Creates a new added actor.
     */
    public AddedActor (Actor actor, long timestamp)
    {
        _actor = actor;
        _timestamp = timestamp;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public AddedActor ()
    {
    }

    /**
     * Returns a reference to the added actor.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Returns the timestamp of the addition.
     */
    public long getTimestamp ()
    {
        return _timestamp;
    }

    /** The added actor. */
    protected Actor _actor;

    /** The time at which the actor was added. */
    protected long _timestamp;
}
