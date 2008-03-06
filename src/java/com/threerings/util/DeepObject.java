//
// $Id$

package com.threerings.util;

import com.samskivert.util.StringUtil;

/**
 * A base class for objects that uses the methods of {@link DeepUtil} to implement {@link #clone},
 * {@link #equals}, and {@link #hashCode} reflectively.
 */
public abstract class DeepObject
    implements Cloneable, Copyable
{
    @Override // documentation inherited
    public Object clone ()
    {
        return copy(null);
    }

    // documentation inherited from interface Copyable
    public Object copy (Object dest)
    {
        return DeepUtil.copy(this, dest);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return DeepUtil.equals(this, other);
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return DeepUtil.hashCode(this);
    }

    @Override // documentation inherited
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
