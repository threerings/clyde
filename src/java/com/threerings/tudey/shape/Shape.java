//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.FloatMath;
import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.space.SpaceElement;

/**
 * A Tudey shape.
 */
public abstract class Shape
{
    /** Intersection types indicating that the shape does not intersect, intersects, or fully
     * contains, respectively, the parameter. */
    public enum IntersectionType { NONE, INTERSECTS, CONTAINS };

    /**
     * Returns a reference to the bounds of the shape.
     */
    public Rect getBounds ()
    {
        return _bounds;
    }

    /**
     * Updates the bounds of the shape.
     */
    public abstract void updateBounds ();

    /**
     * Retrieves the center of the shape.
     *
     * @return a new vector containing the result.
     */
    public Vector2f getCenter ()
    {
        return getCenter(new Vector2f());
    }

    /**
     * Retrieves the center of the shape and places it in the supplied vector.
     *
     * @return a reference to the result vector, for chaining.
     */
    public abstract Vector2f getCenter (Vector2f result);

    /**
     * Transforms this shape in-place.
     *
     * @return a reference to this shape, for chaining.
     */
    public Shape transformLocal (Transform2D transform)
    {
        return transform(transform, this);
    }

    /**
     * Transforms this shape.
     *
     * @return a new shape containing the result.
     */
    public Shape transform (Transform2D transform)
    {
        return transform(transform, null);
    }

    /**
     * Transforms this shape, placing the result in the provided object if possible.
     *
     * @return a reference to the result object, if it was reused; otherwise, a new object
     * containing the result.
     */
    public abstract Shape transform (Transform2D transform, Shape result);

    /**
     * Finds the intersection of a ray with this shape and places it in the supplied vector
     * (if it exists).
     *
     * @return true if the ray intersected the shape (in which case the result will contain the
     * point of intersection), false otherwise.
     */
    public abstract boolean getIntersection (Ray2D ray, Vector2f result);

    /**
     * Checks whether the intersector intersects the specified rect.
     */
    public abstract IntersectionType getIntersectionType (Rect rect);

    /**
     * Determines whether this shape intersects the specified element.  Uses double-dispatch to
     * invoke the appropriate specialization of {@link SpaceElement#intersects}.
     */
    public abstract boolean intersects (SpaceElement element);

    /**
     * Determines whether this shape intersects the specified shape.  Uses double-dispatch to
     * invoke the appropriate method specialization.
     */
    public abstract boolean intersects (Shape shape);

    /**
     * Checks for an intersection with this shape and the specified point.
     */
    public abstract boolean intersects (Point point);

    /**
     * Checks for an intersection with this shape and the specified segment.
     */
    public abstract boolean intersects (Segment segment);

    /**
     * Checks for an intersection with this shape and the specified circle.
     */
    public abstract boolean intersects (Circle circle);

    /**
     * Checks for an intersection with this shape and the specified capsule.
     */
    public abstract boolean intersects (Capsule capsule);

    /**
     * Checks for an intersection with this shape and the specified polygon.
     */
    public abstract boolean intersects (Polygon polygon);

    /**
     * Checks for an intersection with this shape and the specified compound.
     */
    public abstract boolean intersects (Compound compound);

    /**
     * Draws this shape in immediate mode.
     *
     * @param outline if true, draw the outline of the shape; otherwise, the solid form.
     */
    public abstract void draw (boolean outline);

    /**
     * Updates the value of the closest point and returns a new result vector reference.
     */
    protected static Vector2f updateClosest (Vector2f origin, Vector2f result, Vector2f closest)
    {
        if (result == closest) {
            return new Vector2f();
        }
        if (origin.distanceSquared(result) < origin.distanceSquared(closest)) {
            closest.set(result);
        }
        return result;
    }

    /**
     * Determines whether the capsule from <code>start</code> to <code>end</code> with the
     * specified radius intersects the line segment with the supplied origin and terminus.
     */
    protected static boolean intersects (
        Vector2f start, Vector2f end, float radius, Vector2f origin, Vector2f terminus)
    {
        // compute the segment's line parameters
        float a = start.y - end.y, b = end.x - start.x;
        float len = FloatMath.hypot(a, b);
        if (len < FloatMath.EPSILON) { // start equals end; check as circle
            return intersects(start, radius, origin, terminus);
        }
        float rlen = 1f / len;
        a *= rlen;
        b *= rlen;
        float c = -a*start.x - b*start.y;

        // find out where the origin lies with respect to the top and bottom
        float dist = a*origin.x + b*origin.y + c;
        boolean above = (dist > +radius), below = (dist < -radius);
        float x, y;
        if (above || below) { // check the intersection with the top/bottom boundary
            float dx = terminus.x - origin.x, dy = terminus.y - origin.y;
            float divisor = a*dx + b*dy;
            if (Math.abs(divisor) < FloatMath.EPSILON) { // lines are parallel
                return false;
            }
            c += (above ? -radius : +radius);
            float t = (-a*origin.x - b*origin.y - c) / divisor;
            if (t < 0f || t > 1f) { // outside segment boundaries
                return false;
            }
            x = origin.x + t*dx;
            y = origin.y + t*dy;

        } else { // middle; check the origin
            x = origin.x;
            y = origin.y;
        }
        // see where the test point lies with respect to the start and end boundaries
        float tmp = a;
        a = b;
        b = -tmp;
        c = -a*start.x - b*start.y;
        dist = a*x + b*y + c;
        if (dist < 0f) { // before start
            return intersects(start, radius, origin, terminus);
        } else if (dist > len) { // after end
            return intersects(end, radius, origin, terminus);
        } else { // middle
            return true;
        }
    }

    /**
     * Determines whether the line segment from <code>start</code> to <code>end</code> intersects
     * the circle with the given center and radius.
     */
    protected static boolean intersects (
        Vector2f center, float radius, Vector2f start, Vector2f end)
    {
        // see if we start or end inside the circle
        float r2 = radius*radius;
        if (center.distanceSquared(start) <= r2 || center.distanceSquared(end) <= r2) {
            return true;
        }
        // then if we intersect the circle
        float ax = start.x - center.x, ay = start.y - center.y;
        float dx = end.x - start.x, dy = end.y - start.y;
        float a = dx*dx + dy*dy;
        if (a < FloatMath.EPSILON) {
            return false; // degenerate segment
        }
        float b = 2f*(dx*ax + dy*ay);
        float c = ax*ax + ay*ay - r2;
        float radicand = b*b - 4f*a*c;
        if (radicand < 0f) {
            return false;
        }
        float t = (-b - FloatMath.sqrt(radicand)) / (2f*a);
        return t >= 0f && t <= 1f;
    }

    /** The bounds of the shape. */
    protected Rect _bounds = new Rect();

    /** The number of segments to use when we render circles. */
    protected static final int CIRCLE_SEGMENTS = 16;

    /** The increment angle between circle segments. */
    protected static final float CIRCLE_INCREMENT = FloatMath.TWO_PI / CIRCLE_SEGMENTS;
}
