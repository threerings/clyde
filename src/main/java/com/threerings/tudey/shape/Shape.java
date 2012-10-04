//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.tudey.shape;

import com.threerings.math.FloatMath;
import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.config.ShapeConfig;
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
     * Expands this shape in-place by the specified amount.
     *
     * @return a reference to this shape, for chaining.
     */
    public Shape expandLocal (float amount)
    {
        return expand(amount, this);
    }

    /**
     * Expands this shape by the specified amount.
     *
     * @return a new shape containing the result.
     */
    public Shape expand (float amount)
    {
        return expand(amount, null);
    }

    /**
     * Expands this shape, placing the result in the provided object if possible.
     *
     * @return a reference to the result object, if it was reused; otherwise, a new object
     * containing the result.
     */
    public abstract Shape expand (float amount, Shape result);

    /**
     * Computes the shape created by sweeping this shape along the specified translation vector.
     *
     * @return a new shape containing the result.
     */
    public Shape sweep (Vector2f translation)
    {
        return sweep(translation, null);
    }

    /**
     * Computes the shape created by sweeping this shape along the specified translation vector,
     * placing the result in the provided object if possible.
     *
     * @return a reference to the result object, if it was reused; otherwise, a new object
     * containing the result.
     */
    public abstract Shape sweep (Vector2f translation, Shape result);

    /**
     * Returns a perimeter path for this shape.
     */
    public Vector2f[] getPerimeterPath ()
    {
        return _bounds.getPerimeterPath();
    }

    /**
     * Finds the intersection of a ray with this shape and places it in the supplied vector
     * (if it exists).
     *
     * @return true if the ray intersected the shape (in which case the result will contain the
     * point of intersection), false otherwise.
     */
    public abstract boolean getIntersection (Ray2D ray, Vector2f result);

    /**
     * Fins the  nearest point of this element to the supplied point and places it in the supplied
     * vector.
     */
    public abstract void getNearestPoint (Vector2f point, Vector2f result);

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
     * Shapes can never intersect none.
     */
    public boolean intersects (None none)
    {
        return false;
    }

    /**
     * Finds the penetration of the specified shape into this one, assuming that they intersect.
     * The penetration vector represents the minimum translation required to separate the two
     * shapes.  Uses double-dispatch to invoke the appropriate method specialization.
     *
     * @return a reference to the result vector, for chaining.
     */
    public abstract Vector2f getPenetration (Shape shape, Vector2f result);

    /**
     * Finds the penetration of the specified point into this shape.
     */
    public abstract Vector2f getPenetration (Point point, Vector2f result);

    /**
     * Finds the penetration of the specified segment into this shape.
     */
    public abstract Vector2f getPenetration (Segment segment, Vector2f result);

    /**
     * Finds the penetration of the specified circle into this shape.
     */
    public abstract Vector2f getPenetration (Circle circle, Vector2f result);

    /**
     * Finds the penetration of the specified capsule into this shape.
     */
    public abstract Vector2f getPenetration (Capsule capsule, Vector2f result);

    /**
     * Finds the penetration of the specified polygon into this shape.
     */
    public abstract Vector2f getPenetration (Polygon polygon, Vector2f result);

    /**
     * Finds the penetration of the specified compound into this shape.
     */
    public abstract Vector2f getPenetration (Compound compound, Vector2f result);

    /**
     * Shapes never penetrate none.
     */
    public Vector2f getPenetration (None none, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    /**
     * Draws this shape in immediate mode.
     *
     * @param outline if true, draw the outline of the shape; otherwise, the solid form.
     */
    public abstract void draw (boolean outline);

    /**
     * Creates a config corresponding to this shape.
     */
    public abstract ShapeConfig createConfig ();

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

    /**
     * Finds the nearest point on the line segment from <code>start</code> to <code>end</code> to
     * a point <code>point</code> and stores it in <code>result</code>.
     */
    protected static void nearestPointOnSegment (
        Vector2f start, Vector2f end, Vector2f point, Vector2f result)
    {
        // Calculate the perpendicular projection of point onto the line of the segment
        float r = point.subtract(start).dot(end.subtract(start)) / start.distanceSquared(end);
        // The nearest point on the line is before the start
        if (r <= 0) {
            result.set(start);
        // The nearest point on the line is after the start
        } else if (r >= 1) {
            result.set(end);
        // The nearest point on the line is in the segment
        } else {
            result.set(start.add(end.subtract(start).mult(r)));
        }
    }

    protected static void getOutsideLinePenetration (
        Vector2f start, Vector2f end, float radius, Vector2f point, Vector2f result)
    {
        nearestPointOnSegment(start, end, point, result);
        if (radius > 0) {
            Vector2f perp = new Vector2f(start.y - end.y, end.x - start.x);
            Vector2f line = result.subtract(point);
            float sign = Math.signum(perp.dot(line));
            if (sign > 0 && result.lengthSquared() > radius * radius) {
                result.set(Vector2f.ZERO);
            } else {
                result.addLocal(line.normalizeLocal().multLocal(sign * -radius));
            }
        }
    }

    /**
     * Calculates the minimum distance to the origin for the A polygon edges in the convex
     * hull of the Minkowski difference of the A and B polygons.
     */
    protected static Vector2f getMinMinkowskyDifference (
        Vector2f[] A, Vector2f[] B, float radius, Vector2f minDistance)
    {
        if (Vector2f.ZERO.equals(minDistance)) {
            return minDistance;
        }
        boolean flip = minDistance != null;
        for (int ii = 0, nn = A.length; ii < nn; ii++) {
            Vector2f start = A[ii];
            Vector2f end = A[(ii + 1) % nn];
            Vector2f sprime = Vector2f.ZERO;
            Vector2f eprime = Vector2f.ZERO;
            Vector2f perp = new Vector2f(start.y - end.y, end.x - start.x);
            float dot = Float.NEGATIVE_INFINITY;
            int dj = 0;
            for (int jj = 0, mm = B.length; jj < mm; jj++) {
                float odot = perp.dot(B[jj]);
                if (odot > dot) {
                    dot = odot;
                    sprime = B[jj];
                    eprime = sprime;
                    dj = jj;
                } else if (FloatMath.epsilonEquals(odot, dot)) {
                    Vector2f perp2 = new Vector2f(B[jj].y - sprime.y, sprime.x - B[jj].x);
                    if (perp.dot(perp2) < 0) {
                        sprime = B[jj];
                    } else {
                        eprime = B[jj];
                    }
                }
            }
            if (flip) {
                sprime = sprime.subtract(start);
                eprime = eprime.subtract(end);
            } else {
                sprime = start.subtract(sprime);
                eprime = end.subtract(eprime);
            }
            Vector2f distance = new Vector2f();
            getOutsideLinePenetration(sprime, eprime, radius, Vector2f.ZERO, distance);
            if (minDistance == null || minDistance.distanceSquared(Vector2f.ZERO) >
                    distance.distanceSquared(Vector2f.ZERO)) {
                minDistance = distance;
                if (minDistance.equals(Vector2f.ZERO)) {
                    break;
                }
            }
        }
        return minDistance;
    }

    /** The bounds of the shape. */
    protected Rect _bounds = new Rect();

    /** The number of segments to use when we render circles. */
    protected static final int CIRCLE_SEGMENTS = 16;

    /** The increment angle between circle segments. */
    protected static final float CIRCLE_INCREMENT = FloatMath.TWO_PI / CIRCLE_SEGMENTS;
}
