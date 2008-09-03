//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.math.Box;

import com.threerings.opengl.util.Intersectable;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

/**
 * Interface for elements that can be embedded into scenes.
 */
public interface SceneElement extends Tickable, Intersectable, Renderable
{
    /** Determines when the {@link #tick} method must be called. */
    public enum TickPolicy { NEVER, WHEN_VISIBLE, ALWAYS };

    /**
     * Returns the policy that determines when the {@link #tick} method must be called.
     */
    public TickPolicy getTickPolicy ();

    /**
     * Returns a reference to the bounds of the element.
     */
    public Box getBounds ();

    /**
     * Checks and updates the last visit value.  This is used to determine when we have visited
     * (e.g., rendered) the element without having to clear a flag for all elements before
     * performing the operation.  Instead, we use a unique visitation id for each operation and
     * assume that any element with that id has been visited already.
     *
     * @return true if the last visit value was <em>not</em> equal to the value provided (and has
     * now been set to that value), false if the element had already been visited during the
     * current operation.
     */
    public boolean updateLastVisit (int visit);
}
