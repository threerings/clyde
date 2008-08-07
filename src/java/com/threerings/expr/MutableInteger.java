//
// $Id$

package com.threerings.expr;

/**
 * A mutable equivalent to {@link Integer}.
 */
public class MutableInteger
{
    /** The value of this variable. */
    public int value;

    /**
     * Creates a mutable integer with the supplied value.
     */
    public MutableInteger (int value)
    {
        this.value = value;
    }

    /**
     * Creates a mutable integer with a value of zero.
     */
    public MutableInteger ()
    {
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return value;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other instanceof MutableInteger && ((MutableInteger)other).value == value;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return String.valueOf(value);
    }
}
