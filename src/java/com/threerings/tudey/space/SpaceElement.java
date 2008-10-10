//
// $Id$

package com.threerings.tudey.space;

import com.threerings.math.Ray2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.Point;
import com.threerings.tudey.shape.Segment;
import com.threerings.tudey.shape.Circle;
import com.threerings.tudey.shape.Capsule;
import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.shape.Compound;

/**
 * Interface for elements that can be embedded into spaces.
 */
public interface SpaceElement extends SpaceObject
{
    /**
     * Returns this element's user object reference.
     */
    public Object getUserObject ();

    /**
     * Notes that the element was added to the specified space.
     */
    public void wasAdded (Space space);

    /**
     * Notes that the element will be removed from the space.
     */
    public void willBeRemoved ();

    /**
     * Finds the intersection of a ray with this element and places it in the supplied vector
     * (if it exists).
     *
     * @return true if the ray intersected the element (in which case the result will contain the
     * point of intersection), false otherwise.
     */
    public boolean getIntersection (Ray2D ray, Vector2f result);

    /**
     * Determines whether this element intersects the supplied point.
     */
    public boolean intersects (Point point);

    /**
     * Determines whether this element intersects the supplied segment.
     */
    public boolean intersects (Segment segment);

    /**
     * Determines whether this element intersects the supplied circle.
     */
    public boolean intersects (Circle circle);

    /**
     * Determines whether this element intersects the supplied capsule.
     */
    public boolean intersects (Capsule capsule);

    /**
     * Determines whether this element intersects the supplied polygon.
     */
    public boolean intersects (Polygon polygon);

    /**
     * Determines whether this element intersects the supplied compound.
     */
    public boolean intersects (Compound compound);
}
