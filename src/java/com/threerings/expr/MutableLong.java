//
// $Id$

package com.threerings.expr;

/**
 * A mutable equivalent to {@link Long}.
 */
public class MutableLong
{
    /** The value of this variable. */
    public long value;

    /**
     * Creates a mutable long with the supplied value.
     */
    public MutableLong (long value)
    {
        this.value = value;
    }

    /**
     * Creates a mutable long with a value of zero.
     */
    public MutableLong ()
    {
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return (int)(value ^ (value >>> 32));
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other instanceof MutableLong && ((MutableLong)other).value == value;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return String.valueOf(value);
    }
}
