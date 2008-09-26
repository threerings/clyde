//
// $Id$

package com.threerings.tudey.space;

import com.threerings.math.Rect;

/**
 * Superinterface.
 */
public interface SpaceObject
{
    /**
     * Returns a reference to the bounds of the object.
     */
    public Rect getBounds ();

    /**
     * Checks and updates the last visit value.  This is used to determine when we have visited
     * (e.g., rendered) the object without having to clear a flag for all objects before
     * performing the operation.  Instead, we use a unique visitation id for each operation and
     * assume that any object with that id has been visited already.
     *
     * @return true if the last visit value was <em>not</em> equal to the value provided (and has
     * now been set to that value), false if the object had already been visited during the
     * current operation.
     */
    public boolean updateLastVisit (int visit);
}
