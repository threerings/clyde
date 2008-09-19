//
// $Id$

package com.threerings.opengl.util;

import com.threerings.math.Ray3D;
import com.threerings.math.Vector3f;

/**
 * A generic interface for objects that can be tested for intersections.
 */
public interface Intersectable
{
    /**
     * Finds the intersection of a ray with this object and places it in the supplied vector
     * (if it exists).
     *
     * @return true if the ray intersected the object (in which case the result will contain the
     * point of intersection), false otherwise.
     */
    public boolean getIntersection (Ray3D ray, Vector3f result);
}
