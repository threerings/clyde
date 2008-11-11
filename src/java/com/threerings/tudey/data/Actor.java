//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.Streamable;

import com.threerings.delta.Deltable;
import com.threerings.util.DeepObject;

/**
 * Represents an active, stateful element of the scene.
 */
public abstract class Actor extends DeepObject
    implements Streamable, Deltable
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
