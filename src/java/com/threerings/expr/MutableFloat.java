//
// $Id$

package com.threerings.expr;

/**
 * A mutable equivalent to {@link Float}.
 */
public class MutableFloat
{
    /** The value of this variable. */
    public float value;

    /**
     * Creates a mutable float with the supplied value.
     */    
    public MutableFloat (float value)
    {
        this.value = value;
    }
    
    /**
     * Creates a mutable float with a value of zero.
     */
    public MutableFloat ()
    {
    }
    
    @Override // documentation inherited
    public int hashCode ()
    {
        return Float.floatToIntBits(value);
    }
    
    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other instanceof MutableFloat && ((MutableFloat)other).value == value;
    }
    
    @Override // documentation inherited
    public String toString ()
    {
        return String.valueOf(value);
    }
}
