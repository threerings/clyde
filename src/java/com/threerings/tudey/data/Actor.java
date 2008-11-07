//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Represents an active, stateful element of the scene.
 */
public abstract class Actor extends SimpleStreamableObject
{
    /**
     * Returns the actor's unique identifier.
     */
    public int getId ()
    {
        return _id;
    }

    /** The actor's unique identifier. */
    protected int _id;
}
