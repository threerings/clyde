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
 * A non-shape.
 */
public class None extends Shape
{
    /**
     * Creates a point at the specified location.
     */
    public None (Vector2f location)
    {
        _location.set(location);
        updateBounds();
    }

    /**
     * Creates an uninitialized point.
     */
    public None ()
    {
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
        None nresult = (result instanceof None) ? ((None)result) : new None();
        transform.transformPoint(_location, nresult._location);
        nresult.updateBounds();
        return nresult;
    }

    @Override
    public Shape expand (float amount, Shape result)
    {
        None nresult = (result instanceof None) ? ((None)result) : new None();
        nresult.getCenter().set(_location);
        nresult.updateBounds();
        return nresult;
    }

    @Override
    public Shape sweep (Vector2f translation, Shape result)
    {
        None nresult = (result instanceof None) ? ((None)result) : new None();
        _location.add(translation, nresult.getCenter());
        nresult.updateBounds();
        return nresult;
    }

    @Override
    public Vector2f[] getPerimeterPath ()
    {
        return new Vector2f[] { new Vector2f(_location) };
    }

    @Override
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return false;
    }

    @Override
    public void getNearestPoint (Vector2f point, Vector2f result)
    {
        result.set(_location);
    }

    @Override
    public IntersectionType getIntersectionType (Rect rect)
    {
        return IntersectionType.NONE;
    }

    @Override
    public boolean intersects (SpaceElement element)
    {
        return false;
    }

    @Override
    public boolean intersects (Shape shape)
    {
        return false;
    }

    @Override
    public boolean intersects (Point point)
    {
        return false;
    }

    @Override
    public boolean intersects (Segment segment)
    {
        return false;
    }

    @Override
    public boolean intersects (Circle circle)
    {
        return false;
    }

    @Override
    public boolean intersects (Capsule capsule)
    {
        return false;
    }

    @Override
    public boolean intersects (Polygon polygon)
    {
        return false;
    }

    @Override
    public boolean intersects (Compound compound)
    {
        return false;
    }

    @Override
    public Vector2f getPenetration (Shape shape, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
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
        return result.set(Vector2f.ZERO);
    }

    @Override
    public Vector2f getPenetration (Capsule capsule, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override
    public Vector2f getPenetration (Polygon polygon, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override
    public Vector2f getPenetration (Compound compound, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
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
        ShapeConfig.None none = new ShapeConfig.None();
        ShapeConfig.TransformedShape transformed = new ShapeConfig.TransformedShape();
        transformed.shape = none;
        transformed.transform.set(_location, 0f);
        ShapeConfig.Compound compound = new ShapeConfig.Compound();
        compound.shapes = new ShapeConfig.TransformedShape[] { transformed };
        return compound;
    }

    /** The location of the none. */
    protected Vector2f _location = new Vector2f();
}
