//
// $Id$

package com.threerings.tudey.dobj;

import com.threerings.delta.ReflectiveDelta;

import com.threerings.tudey.data.actor.Actor;

/**
 * Extends {@link FieldDelta} to include the id of the affected actor.  Declared final for
 * streaming efficiency.
 */
public final class ActorDelta extends ReflectiveDelta
{
    /**
     * Creates a new actor delta.
     */
    public ActorDelta (Actor original, Actor revised)
    {
        super(original, revised);
        _id = original.getId();
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ActorDelta ()
    {
    }

    /**
     * Returns the id of the affected actor.
     */
    public int getId ()
    {
        return _id;
    }

    /** The id of the affected actor. */
    protected int _id;
}
