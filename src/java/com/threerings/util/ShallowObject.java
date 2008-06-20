//
// $Id$

package com.threerings.util;

/**
 * As a counterpart to {@link DeepObject}, this class does nothing except enforce the default
 * reference semantics of {@link Object#equals} and {@link Object#hashCode}.
 */
public abstract class ShallowObject
{
    @Override // documentation inherited
    public final boolean equals (Object other)
    {
        return super.equals(other);
    }

    @Override // documentation inherited
    public final int hashCode ()
    {
        return super.hashCode();
    }
}
