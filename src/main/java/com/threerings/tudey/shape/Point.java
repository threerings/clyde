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

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.config.ShapeConfig;
import com.threerings.tudey.space.SpaceElement;

/**
 * A point.
 */
public class Point extends Shape
{
    /**
     * Creates a point at the specified location.
     */
    public Point (Vector2f location)
    {
        _location.set(location);
        updateBounds();
    }

    /**
     * Creates a point at the specified location.
     */
    public Point (float x, float y)
    {
        _location.set(x, y);
        updateBounds();
    }

    /**
     * Creates an uninitialized point.
     */
    public Point ()
    {
    }

    /**
     * Returns a reference to the location of the point.
     */
    public Vector2f getLocation ()
    {
        return _location;
    }

    @Override
    public void updateBounds ()
    {
        _bounds.getMinimumExtent().set(_location);
        _bounds.getMaximumExtent().set(_location);
    }

    @Override
    public Vector2f getCenter (Vector2f result)
    {
        return result.set(_location);
    }

    @Override
    public Shape transform (Transform2D transform, Shape result)
    {
        Point presult = (result instanceof Point) ? ((Point)result) : new Point();
        transform.transformPoint(_location, presult._location);
        presult.updateBounds();
        return presult;
    }

    @Override
    public Shape expand (float amount, Shape result)
    {
        Circle cresult = (result instanceof Circle) ? ((Circle)result) : new Circle();
        cresult.getCenter().set(_location);
        cresult.radius = amount;
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public Shape sweep (Vector2f translation, Shape result)
    {
        Segment sresult = (result instanceof Segment) ? ((Segment)result) : new Segment();
        sresult.getStart().set(_location);
        _location.add(translation, sresult.getEnd());
        sresult.updateBounds();
        return sresult;
    }

    @Override
    public Vector2f[] getPerimeterPath ()
    {
        return new Vector2f[] { new Vector2f(_location) };
    }

    @Override
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        boolean isect = ray.intersects(_location);
        if (isect) {
            result.set(_location);
        }
        return isect;
    }

    @Override
    public void getNearestPoint (Vector2f point, Vector2f result)
    {
        result.set(_location);
    }

    @Override
    public IntersectionType getIntersectionType (Rect rect)
    {
        return rect.contains(_location) ? IntersectionType.INTERSECTS : IntersectionType.NONE;
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
        return point.getLocation().equals(_location);
    }

    @Override
    public boolean intersects (Segment segment)
    {
        return segment.intersects(this);
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
        return segment.getPenetration(this, result).negateLocal();
    }

    @Override
    public Vector2f getPenetration (Circle circle, Vector2f result)
    {
        return circle.getPenetration(this, result).negateLocal();
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
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(_location.x, _location.y);
        GL11.glEnd();
    }

    @Override
    public ShapeConfig createConfig ()
    {
        ShapeConfig.Point point = new ShapeConfig.Point();
        ShapeConfig.TransformedShape transformed = new ShapeConfig.TransformedShape();
        transformed.shape = point;
        transformed.transform.set(_location, 0f);
        ShapeConfig.Compound compound = new ShapeConfig.Compound();
        compound.shapes = new ShapeConfig.TransformedShape[] { transformed };
        return compound;
    }

    /** The location of the point. */
    protected Vector2f _location = new Vector2f();
}
