//
// $Id$

package com.threerings.util;

/**
 * An interface for objects that can be copied.
 */
public interface Copyable
{
    /**
     * Creates a copy of this object, (re)populating the supplied destination object if possible.
     *
     * @return either a reference to the destination object, if it could be repopulated, or a new
     * object containing the copied state.
     */
    public Object copy (Object dest);
}
