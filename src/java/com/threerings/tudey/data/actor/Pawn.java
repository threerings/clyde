//
// $Id$

package com.threerings.tudey.data.actor;

/**
 * An actor representing an entity controlled by a user or the computer.
 */
public abstract class Pawn extends MobileActor
{
    /**
     * Creates a new pawn.
     */
    public Pawn (int id, int created)
    {
        super(id, created);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Pawn ()
    {
    }
}
