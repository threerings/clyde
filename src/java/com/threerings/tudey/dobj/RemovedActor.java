//
// $Id$

package com.threerings.tudey.dobj;

import com.threerings.io.SimpleStreamableObject;

/**
 * Combines a removed actor id with the timestamp of the removal.
 */
public final class RemovedActor extends SimpleStreamableObject
{
    /**
     * Creates a new removed actor object.
     */
    public RemovedActor (int id, long timestamp)
    {
        _id = id;
        _timestamp = timestamp;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public RemovedActor ()
    {
    }

    /**
     * Returns the id of the removed actor.
     */
    public int getId ()
    {
        return _id;
    }

    /**
     * Returns the timestamp of the removal.
     */
    public long getTimestamp ()
    {
        return _timestamp;
    }

    /** The id of the removed actor. */
    protected int _id;

    /** The time at which the actor was removed. */
    protected long _timestamp;
}
