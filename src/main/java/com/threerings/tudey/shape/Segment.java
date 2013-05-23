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
 * A line segment.
 */
public class Segment extends Shape
{
    /**
     * Creates a segment between the specified points.
     */
    public Segment (Vector2f start, Vector2f end)
    {
        _start.set(start);
        _end.set(end);
        updateBounds();
    }

    /**
     * Creates an uninitialized segment.
     */
    public Segment ()
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
     * Determines whether this segment intersects the specified point.
     */
    public boolean intersects (Vector2f pt)
    {
        return intersects(_start, _end, pt);
    }

    /**
     * Determines whether this segment intersects the segment with the provided start
     * and end points.
     */
    public boolean intersects (Vector2f ostart, Vector2f oend)
    {
        // this is a + t*b, other is c + s*d
        float ax = _start.x, ay = _start.y;
        float bx = _end.x - _start.x, by = _end.y - _start.y;
        float cx = ostart.x, cy = ostart.y;
        float dx = oend.x - ostart.x, dy = oend.y - ostart.y;

        float divisor = bx*dy - by*dx;
        if (Math.abs(divisor) < FloatMath.EPSILON) {
            // the segments are parallel (or zero-length)
            return intersects(ostart) || intersects(oend) || intersects(ostart, oend, _start);
        }
        float cxax = cx - ax, cyay = cy - ay;
        float s = (by*cxax - bx*cyay) / divisor;
        if (s < 0f || s > 1f) {
            return false;
        }
        float t = (dy*cxax - dx*cyay) / divisor;
        return (t >= 0f && t <= 1f);
    }

    @Override
    public void updateBounds ()
    {
        _bounds.setToEmpty();
        _bounds.addLocal(_start);
        _bounds.addLocal(_end);
    }

    @Override
    public Vector2f getCenter (Vector2f result)
    {
        return _start.add(_end, result).multLocal(0.5f);
    }

    @Override
    public Shape transform (Transform2D transform, Shape result)
    {
        Segment sresult = (result instanceof Segment) ? ((Segment)result) : new Segment();
        transform.transformPoint(_start, sresult._start);
        transform.transformPoint(_end, sresult._end);
        sresult.updateBounds();
        return sresult;
    }

    @Override
    public Shape expand (float amount, Shape result)
    {
        Capsule cresult = (result instanceof Capsule) ? ((Capsule)result) : new Capsule();
        cresult.getStart().set(_start);
        cresult.getEnd().set(_end);
        cresult.radius = amount;
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public Shape sweep (Vector2f translation, Shape result)
    {
        Polygon presult = (result instanceof Polygon) ? ((Polygon)result) : new Polygon(4);
        if (presult.getVertexCount() != 4) {
            presult.initVertices(4);
        }
        float cp = (_end.x - _start.x)*translation.y - (_end.y - _start.y)*translation.x;
        if (cp >= 0f) {
            presult.getVertex(0).set(_start);
            presult.getVertex(1).set(_end);
            _end.add(translation, presult.getVertex(2));
            _start.add(translation, presult.getVertex(3));
        } else {
            _start.add(translation, presult.getVertex(0));
            _end.add(translation, presult.getVertex(1));
            presult.getVertex(2).set(_end);
            presult.getVertex(3).set(_start);
        }
        presult.updateBounds();
        return presult;
    }

    @Override
    public Vector2f[] getPerimeterPath ()
    {
        return new Vector2f[] { new Vector2f(_start), new Vector2f(_end) };
    }

    @Override
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return ray.getIntersection(_start, _end, result);
    }

    @Override
    public void getNearestPoint (Vector2f point, Vector2f result)
    {
        nearestPointOnSegment(_start, _end, point, result);
    }

    @Override
    public IntersectionType getIntersectionType (Rect rect)
    {
        // see if we start or end inside the rectangle
        if (rect.contains(_start) || rect.contains(_end)) {
            return IntersectionType.INTERSECTS;
        }
        // then whether we intersect one of its sides
        float dx = _end.x - _start.x, dy = _end.y - _start.y;
        Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
        return
            Math.abs(dx) > FloatMath.EPSILON &&
                (intersectsX(rect, min.x, dx, dy) || intersectsX(rect, max.x, dx, dy)) ||
            Math.abs(dy) > FloatMath.EPSILON &&
                (intersectsY(rect, min.y, dx, dy) || intersectsY(rect, max.y, dx, dy)) ?
                    IntersectionType.INTERSECTS : IntersectionType.NONE;
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
        return intersects(point.getLocation());
    }

    @Override
    public boolean intersects (Segment segment)
    {
        return intersects(segment.getStart(), segment.getEnd());
    }

    @Override
    public boolean intersects (Circle circle)
    {
        return circle.intersects(this);
    }

    @Override
    public boolean intersects (Capsule capsule)
    {
        return capsule.intersects(this);
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
        return result.set(Vector2f.ZERO);
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
            center.subtract(D, result).multLocal(circle.radius / dist - 1f);
    }

    @Override
    public Vector2f getPenetration (Capsule capsule, Vector2f result)
    {
        return capsule.getPenetration(this, result).negateLocal();
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
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(_start.x, _start.y);
        GL11.glVertex2f(_end.x, _end.y);
        GL11.glEnd();
    }

    @Override
    public ShapeConfig createConfig ()
    {
        ShapeConfig.Segment segment = new ShapeConfig.Segment();
        segment.length = _start.distance(_end);
        ShapeConfig.TransformedShape transformed = new ShapeConfig.TransformedShape();
        transformed.shape = segment;
        transformed.transform.set(_start.add(_end).multLocal(0.5f), _start.direction(_end));
        ShapeConfig.Compound compound = new ShapeConfig.Compound();
        compound.shapes = new ShapeConfig.TransformedShape[] { transformed };
        return compound;
    }

    @Override
    public String toString ()
    {
        return "Seg:(" + _start + ", " + _end + ")";
    }

    /**
     * Helper method for {@link #getIntersectionType}.  Determines whether the segment intersects
     * the rectangle at the line where x equals the value specified.
     */
    protected boolean intersectsX (Rect rect, float x, float dx, float dy)
    {
        float t = (x - _start.x) / dx;
        if (t < 0f || t > 1f) {
            return false;
        }
        float iy = _start.y + t*dy;
        return iy >= rect.getMinimumExtent().y && iy <= rect.getMaximumExtent().y;
    }

    /**
     * Helper method for {@link #getIntersectionType}.  Determines whether the segment intersects
     * the rectangle at the line where y equals the value specified.
     */
    protected boolean intersectsY (Rect rect, float y, float dx, float dy)
    {
        float t = (y - _start.y) / dy;
        if (t < 0f || t > 1f) {
            return false;
        }
        float ix = _start.x + t*dx;
        return ix >= rect.getMinimumExtent().x && ix <= rect.getMaximumExtent().x;
    }

    /**
     * Returns the parameter of the segment when it intersects the supplied point, or
     * {@link Float#MAX_VALUE} if there is no such intersection.
     */
    protected float getIntersection (Vector2f pt)
    {
        float dx = _end.x - _start.x, dy = _end.y - _start.y;
        if (dx == 0f && dy == 0f) {
            return _start.equals(pt) ? 0f : Float.MAX_VALUE;
        } else if (Math.abs(dx) > Math.abs(dy)) {
            float t = (pt.x - _start.x) / dx;
            return (t >= 0f && t <= 1f && _start.y + t*dy == pt.y) ? t : Float.MAX_VALUE;
        } else {
            float t = (pt.y - _start.y) / dy;
            return (t >= 0f && t <= 1f && _start.x + t*dx == pt.x) ? t : Float.MAX_VALUE;
        }
    }

    /**
     * Checks whether the segment from start to end intersects the specified point.
     */
    protected static boolean intersects (Vector2f start, Vector2f end, Vector2f pt)
    {
        float dx = end.x - start.x, dy = end.y - start.y;
        if (dx == 0f && dy == 0f) {
            return start.equals(pt);
        } else if (Math.abs(dx) > Math.abs(dy)) {
            float t = (pt.x - start.x) / dx;
            return t >= 0f && t <= 1f && start.y + t*dy == pt.y;
        } else {
            float t = (pt.y - start.y) / dy;
            return t >= 0f && t <= 1f && start.x + t*dx == pt.x;
        }
    }

    /** The start and end vertices. */
    protected Vector2f _start = new Vector2f(), _end = new Vector2f();
}
