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
 * A circle.
 */
public class Circle extends Shape
{
    /** The radius of the circle. */
    public float radius;

    /**
     * Creates a circle with the supplied center and radius.
     */
    public Circle (Vector2f center, float radius)
    {
        _center.set(center);
        this.radius = radius;
        updateBounds();
    }

    /**
     * Creates an uninitialized circle.
     */
    public Circle ()
    {
    }

    /**
     * Returns a reference to the center of the circle.
     */
    public Vector2f getCenter ()
    {
        return _center;
    }

    /**
     * Checks whether the circle contains the specified point.
     */
    public boolean contains (Vector2f pt)
    {
        return _center.distanceSquared(pt) <= radius*radius;
    }

    @Override
    public void updateBounds ()
    {
        _bounds.getMinimumExtent().set(_center);
        _bounds.getMaximumExtent().set(_center);
        _bounds.expandLocal(radius, radius);
    }

    @Override
    public Vector2f getCenter (Vector2f result)
    {
        return result.set(_center);
    }

    @Override
    public Shape transform (Transform2D transform, Shape result)
    {
        Circle cresult = (result instanceof Circle) ? ((Circle)result) : new Circle();
        transform.transformPoint(_center, cresult._center);
        cresult.radius = radius * transform.approximateUniformScale();
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public Shape expand (float amount, Shape result)
    {
        Circle cresult = (result instanceof Circle) ? ((Circle)result) : new Circle();
        cresult.getCenter().set(_center);
        cresult.radius = radius + amount;
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public Shape sweep (Vector2f translation, Shape result)
    {
        Capsule cresult = (result instanceof Capsule) ? ((Capsule)result) : new Capsule();
        cresult.getStart().set(_center);
        _center.add(translation, cresult.getEnd());
        cresult.radius = radius;
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return ray.getIntersection(_center, radius, result);
    }

    @Override
    public void getNearestPoint (Vector2f point, Vector2f result)
    {
        getIntersection(new Ray2D(point, _center.subtract(point).normalize()), result);
    }

    @Override
    public IntersectionType getIntersectionType (Rect rect)
    {
        // test the points of the rect against the circle
        Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
        float r2 = radius*radius;
        int ccount =
            (contains(min.x, min.y, r2) ? 1 : 0) +
            (contains(max.x, min.y, r2) ? 1 : 0) +
            (contains(max.x, max.y, r2) ? 1 : 0) +
            (contains(min.x, max.y, r2) ? 1 : 0);
        if (ccount > 0) {
            return (ccount == 4) ? IntersectionType.CONTAINS : IntersectionType.INTERSECTS;
        }
        // handle the non-corner cases
        if (_center.x < min.x) { // left
            return (_center.y >= min.y && _center.y <= max.y && (min.x - _center.x) <= radius) ?
                IntersectionType.INTERSECTS : IntersectionType.NONE;

        } else if (_center.x > max.x) { // right
            return (_center.y >= min.y && _center.y <= max.y && (_center.x - max.x) <= radius) ?
                IntersectionType.INTERSECTS : IntersectionType.NONE;

        } else { // middle
            if (_center.y < min.y) { // middle-bottom
                return (min.y - _center.y) <= radius ?
                    IntersectionType.INTERSECTS : IntersectionType.NONE;

            } else if (_center.y > max.y) { // middle-top
                return (_center.y - max.y) <= radius ?
                    IntersectionType.INTERSECTS : IntersectionType.NONE;

            } else { // middle-middle
                return IntersectionType.INTERSECTS;
            }
        }
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
        return intersects(_center, radius, segment.getStart(), segment.getEnd());
    }

    @Override
    public boolean intersects (Circle circle)
    {
        return circle.getCenter().distance(_center) <= (circle.radius + radius);
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
        Vector2f location = point.getLocation();
        float dist = location.distance(_center);
        return (dist == 0f) ? result.set(Vector2f.ZERO) :
            location.subtract(_center, result).multLocal(radius / dist - 1f);
    }

    @Override
    public Vector2f getPenetration (Segment segment, Vector2f result)
    {
        return segment.getPenetration(this, result).negateLocal();
    }

    @Override
    public Vector2f getPenetration (Circle circle, Vector2f result)
    {
        Vector2f ocenter = circle.getCenter();
        float dist = ocenter.distance(_center);
        return (dist == 0f) ? result.set(Vector2f.ZERO) :
            ocenter.subtract(_center, result).multLocal((radius + circle.radius) / dist - 1f);
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
        GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_POLYGON);
        for (int ii = 0; ii < CIRCLE_SEGMENTS; ii++) {
            float angle = ii * CIRCLE_INCREMENT;
            GL11.glVertex2f(
                _center.x + FloatMath.cos(angle) * radius,
                _center.y + FloatMath.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    @Override
    public ShapeConfig createConfig ()
    {
        ShapeConfig.Circle circle = new ShapeConfig.Circle();
        circle.radius = radius;
        ShapeConfig.TransformedShape transformed = new ShapeConfig.TransformedShape();
        transformed.shape = circle;
        transformed.transform.set(_center, 0f);
        ShapeConfig.Compound compound = new ShapeConfig.Compound();
        compound.shapes = new ShapeConfig.TransformedShape[] { transformed };
        return compound;
    }

    @Override
    public String toString ()
    {
        return "[center=" + _center + ", radius=" + radius + ", bounds=" + _bounds + "]";
    }

    /**
     * Checks whether the circle contains the specified point.
     */
    protected boolean contains (float x, float y, float r2)
    {
        float dx = x - _center.x, dy = y - _center.y;
        return dx*dx + dy*dy <= r2;
    }

    /** The center of the circle. */
    protected Vector2f _center = new Vector2f();
}
