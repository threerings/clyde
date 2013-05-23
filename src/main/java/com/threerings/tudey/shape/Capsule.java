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

import org.lwjgl.opengl.GL11;

import com.threerings.math.FloatMath;
import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.config.ShapeConfig;
import com.threerings.tudey.space.SpaceElement;

/**
 * A capsule.
 */
public class Capsule extends Shape
{
    /** The radius of the capsule. */
    public float radius;

    /**
     * Creates a capsule with the supplied center and radius.
     */
    public Capsule (Vector2f start, Vector2f end, float radius)
    {
        _start.set(start);
        _end.set(end);
        this.radius = radius;
        updateBounds();
    }

    /**
     * Creates an uninitialized capsule.
     */
    public Capsule ()
    {
    }

    /**
     * Returns a reference to the start vertex.
     */
    public Vector2f getStart ()
    {
        return _start;
    }

    /**
     * Returns a reference to the end vertex.
     */
    public Vector2f getEnd ()
    {
        return _end;
    }

    /**
     * Checks whether the capsule contains the specified point.
     */
    public boolean contains (Vector2f pt)
    {
        return contains(pt.x, pt.y);
    }

    /**
     * Checks whether the capsule contains the specified point.
     */
    public boolean contains (float x, float y)
    {
        // see if the point lies before the start (this also handles the case where start and
        // end are equal)
        float a = _end.x - _start.x, b = _end.y - _start.y;
        float dp = a*x + b*y;
        if (dp <= a*_start.x + b*_start.y) {
            float dx = x - _start.x, dy = y - _start.y;
            return dx*dx + dy*dy <= radius*radius;
        }
        // now see if it lies after the end
        if (dp >= a*_end.x + b*_end.y) {
            float dx = x - _end.x, dy = y - _end.y;
            return dx*dx + dy*dy <= radius*radius;
        }
        // it's in the middle, so check the distance to the line
        a = _start.y - _end.y;
        b = _end.x - _start.x;
        float d = a*(x - _start.x) + b*(y - _start.y);
        return d*d <= radius*radius * (a*a + b*b);
    }

    @Override
    public void updateBounds ()
    {
        _bounds.setToEmpty();
        _bounds.addLocal(_start);
        _bounds.addLocal(_end);
        _bounds.expandLocal(radius, radius);
    }

    @Override
    public Vector2f getCenter (Vector2f result)
    {
        return _start.add(_end, result).multLocal(0.5f);
    }

    @Override
    public Shape transform (Transform2D transform, Shape result)
    {
        Capsule cresult = (result instanceof Capsule) ? ((Capsule)result) : new Capsule();
        transform.transformPoint(_start, cresult._start);
        transform.transformPoint(_end, cresult._end);
        cresult.radius = radius * transform.approximateUniformScale();
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public Shape expand (float amount, Shape result)
    {
        Capsule cresult = (result instanceof Capsule) ? ((Capsule)result) : new Capsule();
        cresult.getStart().set(_start);
        cresult.getEnd().set(_end);
        cresult.radius = radius + amount;
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public Shape sweep (Vector2f translation, Shape result)
    {
        // TODO: not supported at present; would need rounded polygons
        Capsule cresult = (result instanceof Capsule) ? ((Capsule)result) : new Capsule();
        cresult.getStart().set(_start);
        cresult.getEnd().set(_end);
        cresult.radius = radius;
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return ray.getIntersection(_start, _end, radius, result);
    }

    @Override
    public void getNearestPoint (Vector2f point, Vector2f result)
    {
        if (contains(point)) {
            result.set(point);
            return;
        }
        nearestPointOnSegment(_start, _end, point, result);
        Vector2f line = result.subtract(point);
        float length = line.length();
        line.mult((length - radius)/length);
        result.set(point).add(line);
    }

    @Override
    public IntersectionType getIntersectionType (Rect rect)
    {
        // check the corners of the rectangle
        Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
        int ccount =
            (contains(min.x, min.y) ? 1 : 0) +
            (contains(max.x, min.y) ? 1 : 0) +
            (contains(max.x, max.y) ? 1 : 0) +
            (contains(min.x, max.y) ? 1 : 0);
        if (ccount > 0) {
            return (ccount == 4) ? IntersectionType.CONTAINS : IntersectionType.INTERSECTS;
        }
        // see if the rectangle contains the start or end points
        if (rect.contains(_start) || rect.contains(_end)) {
            return IntersectionType.INTERSECTS;
        }
        // handle non-corner cases
        float dx = _end.x - _start.x, dy = _end.y - _start.y;
        if (_start.x <= min.x) { // left
            if (intersectsLeft(rect, dx, dy)) {
                return IntersectionType.INTERSECTS;
            }
        } else if (_start.x >= max.x) { // right
            if (intersectsRight(rect, dx, dy)) {
                return IntersectionType.INTERSECTS;
            }
        }
        if (_start.y <= min.y) { // bottom
            if (intersectsBottom(rect, dx, dy)) {
                return IntersectionType.INTERSECTS;
            }
        } else if (_start.y >= max.y) { // top
            if (intersectsTop(rect, dx, dy)) {
                return IntersectionType.INTERSECTS;
            }
        }
        return IntersectionType.NONE;
    }

    @Override
    public boolean intersects (SpaceElement element)
    {
        return element.intersects(this);
    }

    @Override
    public boolean intersects (Shape shape)
    {
        return shape.intersects(this);
    }

    @Override
    public boolean intersects (Point point)
    {
        return contains(point.getLocation());
    }

    @Override
    public boolean intersects (Segment segment)
    {
        return intersects(_start, _end, radius, segment.getStart(), segment.getEnd());
    }

    @Override
    public boolean intersects (Circle circle)
    {
        // this test is equivalent to checking the line segment from _start to _end against
        // a circle with the same center as the parameter and a radius equal to the sum of
        // the capsule radius and the circle radius

        // see if we start or end inside the circle
        Vector2f center = circle.getCenter();
        float r = circle.radius + radius, r2 = r*r;
        if (_start.distanceSquared(center) <= r2 || _end.distanceSquared(center) <= r2) {
            return true;
        }
        // then if we intersect the circle
        float ax = _start.x - center.x, ay = _start.y - center.y;
        float dx = _end.x - _start.x, dy = _end.y - _start.y;
        float a = dx*dx + dy*dy;
        float b = 2f*(dx*ax + dy*ay);
        float c = ax*ax + ay*ay - r2;
        float radicand = b*b - 4f*a*c;
        if (radicand < 0f) {
            return false;
        }
        float t = (-b - FloatMath.sqrt(radicand)) / (2f*a);
        return t >= 0f && t <= 1f;
    }

    @Override
    public boolean intersects (Capsule capsule)
    {
        return intersects(
            _start, _end, radius + capsule.radius, capsule.getStart(), capsule.getEnd());
    }

    @Override
    public boolean intersects (Polygon polygon)
    {
        return polygon.intersects(this);
    }

    @Override
    public boolean intersects (Compound compound)
    {
        return compound.intersects(this);
    }

    @Override
    public Vector2f getPenetration (Shape shape, Vector2f result)
    {
        return shape.getPenetration(this, result).negateLocal();
    }

    @Override
    public Vector2f getPenetration (Point point, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override
    public Vector2f getPenetration (Segment segment, Vector2f result)
    {
        Vector2f[] cv = new Vector2f[2];
        cv[0] = _start;
        cv[1] = _end;
        Vector2f[] sv = new Vector2f[2];
        sv[0] = segment.getStart();
        sv[1] = segment.getEnd();
        Vector2f minDistance = getMinMinkowskyDifference(cv, sv, radius, null);
        minDistance = getMinMinkowskyDifference(sv, cv, radius, minDistance);
        return result.set(minDistance);
    }

    @Override
    public Vector2f getPenetration (Circle circle, Vector2f result)
    {
        Vector2f center = circle.getCenter();
        Vector2f D = center.subtract(_start);
        Vector2f axis = _end.subtract(_start);
        float d = D.dot(axis);
        d = FloatMath.clamp(d, 0, 1);
        _start.add(axis.multLocal(d), D);
        float dist = center.distance(D);
        return (dist == 0f) ? result.set(Vector2f.ZERO) :
            center.subtract(D, result).multLocal((circle.radius + radius) / dist - 1f);
    }

    @Override
    public Vector2f getPenetration (Capsule capsule, Vector2f result)
    {
        Vector2f[] cv = new Vector2f[2];
        cv[0] = _start;
        cv[1] = _end;
        Vector2f[] ov = new Vector2f[2];
        ov[0] = capsule.getStart();
        ov[1] = capsule.getEnd();
        float rad = radius + capsule.radius;
        Vector2f minDistance = getMinMinkowskyDifference(cv, ov, rad, null);
        minDistance = getMinMinkowskyDifference(ov, cv, rad, minDistance);
        return result.set(minDistance);
    }

    @Override
    public Vector2f getPenetration (Polygon polygon, Vector2f result)
    {
        return polygon.getPenetration(this, result).negateLocal();
    }

    @Override
    public Vector2f getPenetration (Compound compound, Vector2f result)
    {
        return compound.getPenetration(this, result).negateLocal();
    }

    @Override
    public void draw (boolean outline)
    {
        float offset = FloatMath.atan2(_end.x - _start.x, _start.y - _end.y);
        GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_POLYGON);
        for (int ii = 0, nn = CIRCLE_SEGMENTS / 2; ii <= nn; ii++) {
            float angle = ii * CIRCLE_INCREMENT + offset;
            GL11.glVertex2f(
                _start.x + FloatMath.cos(angle) * radius,
                _start.y + FloatMath.sin(angle) * radius);
        }
        for (int ii = 0, nn = CIRCLE_SEGMENTS / 2; ii <= nn; ii++) {
            float angle = ii * CIRCLE_INCREMENT - offset;
            GL11.glVertex2f(
                _end.x + FloatMath.cos(angle) * radius,
                _end.y + FloatMath.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    @Override
    public ShapeConfig createConfig ()
    {
        ShapeConfig.Capsule capsule = new ShapeConfig.Capsule();
        capsule.radius = radius;
        capsule.length = _start.distance(_end);
        ShapeConfig.TransformedShape transformed = new ShapeConfig.TransformedShape();
        transformed.shape = capsule;
        transformed.transform.set(_start.add(_end).multLocal(0.5f), _start.direction(_end));
        ShapeConfig.Compound compound = new ShapeConfig.Compound();
        compound.shapes = new ShapeConfig.TransformedShape[] { transformed };
        return compound;
    }

    @Override
    public String toString ()
    {
        return "[start=" + _start.toString() + ", end=" + _end.toString() + ", radius=" + radius +
            ", bounds=" + _bounds + "]";
    }

    /**
     * Helper method for {@link #getIntersectionType}.  Determines whether the capsule intersects
     * the rectangle on the left side.
     */
    protected boolean intersectsLeft (Rect rect, float dx, float dy)
    {
        Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
        float left = min.x - radius;
        if (_start.x >= left) {
            return _start.y >= min.y && _start.y <= max.y; // starts inside
        }
        if (Math.abs(dx) < FloatMath.EPSILON) {
            return false; // parallel to edge
        }
        float t = (left - _start.x) / dx;
        if (t < 0f || t > 1f) {
            return false; // outside segment
        }
        float iy = _start.y + t*dy;
        return iy >= min.y && iy <= max.y;
    }

    /**
     * Helper method for {@link #getIntersectionType}.  Determines whether the capsule intersects
     * the rectangle on the left side.
     */
    protected boolean intersectsRight (Rect rect, float dx, float dy)
    {
        Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
        float right = max.x + radius;
        if (_start.x <= right) {
            return _start.y >= min.y && _start.y <= max.y; // starts inside
        }
        if (Math.abs(dx) < FloatMath.EPSILON) {
            return false; // parallel to edge
        }
        float t = (right - _start.x) / dx;
        if (t < 0f || t > 1f) {
            return false; // outside segment
        }
        float iy = _start.y + t*dy;
        return iy >= min.y && iy <= max.y;
    }

    /**
     * Helper method for {@link #getIntersectionType}.  Determines whether the capsule intersects
     * the rectangle on the left side.
     */
    protected boolean intersectsBottom (Rect rect, float dx, float dy)
    {
        Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
        float bottom = min.y - radius;
        if (_start.y >= bottom) {
            return _start.x >= min.x && _start.x <= max.x; // starts inside
        }
        if (Math.abs(dy) < FloatMath.EPSILON) {
            return false; // parallel to edge
        }
        float t = (bottom - _start.y) / dy;
        if (t < 0f || t > 1f) {
            return false; // outside segment
        }
        float ix = _start.x + t*dx;
        return ix >= min.x && ix <= max.x;
    }

    /**
     * Helper method for {@link #getIntersectionType}.  Determines whether the capsule intersects
     * the rectangle on the left side.
     */
    protected boolean intersectsTop (Rect rect, float dx, float dy)
    {
        Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
        float top = max.y + radius;
        if (_start.y <= top) {
            return _start.x >= min.x && _start.x <= max.x; // starts inside
        }
        if (Math.abs(dy) < FloatMath.EPSILON) {
            return false; // parallel to edge
        }
        float t = (top - _start.y) / dy;
        if (t < 0f || t > 1f) {
            return false; // outside segment
        }
        float ix = _start.x + t*dx;
        return ix >= min.x && ix <= max.x;
    }

    /** The start and end vertices of the capsule. */
    protected Vector2f _start = new Vector2f(), _end = new Vector2f();
}
