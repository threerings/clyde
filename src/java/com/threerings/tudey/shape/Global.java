//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import com.threerings.tudey.space.SpaceElement;

/**
 * A non-shape.
 */
public class Global extends Shape
{
    /** Get the global shape. */
    public static Global getShape ()
    {
        if (_global == null) {
            _global = new Global();
        }
        return _global;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        // do nothing
    }

    @Override // documentation inherited
    public Vector2f getCenter (Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public Shape transform (Transform2D transform, Shape result)
    {
        return this;
    }

    @Override // documentation inherited
    public Shape expand (float amount, Shape result)
    {
        return this;
    }

    @Override // documentation inherited
    public Shape sweep (Vector2f translation, Shape result)
    {
        return this;
    }

    @Override // documentation inherited
    public Vector2f[] getPerimeterPath ()
    {
        return new Vector2f[] { new Vector2f(Vector2f.ZERO) };
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return false;
    }

    @Override // documentation inherited
    public void getNearestPoint (Vector2f point, Vector2f result)
    {
        result.set(point);
    }

    @Override // documentation inherited
    public IntersectionType getIntersectionType (Rect rect)
    {
        return IntersectionType.NONE;
    }

    @Override // documentation inherited
    public boolean intersects (SpaceElement element)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Shape shape)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Point point)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Circle circle)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Capsule capsule)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Polygon polygon)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Compound compound)
    {
        return false;
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Shape shape, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Point point, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Segment segment, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Circle circle, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Capsule capsule, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Polygon polygon, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Compound compound, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public void draw (boolean outline)
    {
        // do nothing
    }

    /**
     * Creates a global shape.
     */
    protected Global ()
    {
        _bounds.set(Rect.MAX_VALUE);
    }

    /** The global instance. */
    protected static Global _global;
}
