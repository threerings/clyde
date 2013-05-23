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

import com.samskivert.util.StringUtil;

import com.threerings.math.FloatMath;
import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.config.ShapeConfig;
import com.threerings.tudey.space.SpaceElement;

/**
 * A compound shape.
 */
public class Compound extends Shape
{
    /**
     * Creates a compound containing the supplied shapes (which will be referenced, not copied).
     */
    public Compound (Shape... shapes)
    {
        _shapes = shapes;
        updateBounds();
    }

    /**
     * Creates an uninitialized compound.
     */
    public Compound (int scount)
    {
        _shapes = new Shape[scount];
    }

    /**
     * Returns the number of shapes in this compound.
     */
    public int getShapeCount ()
    {
        return _shapes.length;
    }

    /**
     * Returns a reference to the indexed shape.
     */
    public Shape getShape (int idx)
    {
        return _shapes[idx];
    }

    /**
     * Returns a reference to the array of shapes.
     */
    public Shape[] getShapes ()
    {
        return _shapes;
    }

    @Override
    public void updateBounds ()
    {
        _bounds.setToEmpty();
        for (Shape shape : _shapes) {
            shape.updateBounds();
            _bounds.addLocal(shape.getBounds());
        }
    }

    @Override
    public Vector2f getCenter (Vector2f result)
    {
        result.set(0f, 0f);
        Vector2f pt = new Vector2f();
        for (Shape shape : _shapes) {
            result.addLocal(shape.getCenter(pt));
        }
        return result.multLocal(1f / _shapes.length);
    }

    @Override
    public Shape transform (Transform2D transform, Shape result)
    {
        Compound cresult = (result instanceof Compound) ?
            ((Compound)result) : new Compound(_shapes.length);
        if (cresult.getShapeCount() != _shapes.length) {
            cresult._shapes = new Shape[_shapes.length];
        }
        for (int ii = 0; ii < _shapes.length; ii++) {
            cresult._shapes[ii] = _shapes[ii].transform(transform, cresult._shapes[ii]);
        }
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public Shape expand (float amount, Shape result)
    {
        Compound cresult = (result instanceof Compound) ?
            ((Compound)result) : new Compound(_shapes.length);
        if (cresult.getShapeCount() != _shapes.length) {
            cresult._shapes = new Shape[_shapes.length];
        }
        for (int ii = 0; ii < _shapes.length; ii++) {
            cresult._shapes[ii] = _shapes[ii].expand(amount, cresult._shapes[ii]);
        }
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public Shape sweep (Vector2f translation, Shape result)
    {
        Compound cresult = (result instanceof Compound) ?
            ((Compound)result) : new Compound(_shapes.length);
        if (cresult.getShapeCount() != _shapes.length) {
            cresult._shapes = new Shape[_shapes.length];
        }
        for (int ii = 0; ii < _shapes.length; ii++) {
            cresult._shapes[ii] = _shapes[ii].sweep(translation, cresult._shapes[ii]);
        }
        cresult.updateBounds();
        return cresult;
    }

    @Override
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        if (!_bounds.intersects(ray)) {
            return false;
        }
        Vector2f closest = result;
        for (Shape shape : _shapes) {
            if (shape.getIntersection(ray, result)) {
                result = updateClosest(ray.getOrigin(), result, closest);
            }
        }
        return (result != closest);
    }

    @Override
    public void getNearestPoint (Vector2f point, Vector2f result)
    {
        Vector2f currentResult = new Vector2f();
        float minDist = Float.MAX_VALUE;
        float dist;
        for (Shape shape : _shapes) {
            shape.getNearestPoint(point, currentResult);
            dist = point.distanceSquared(currentResult);
            if (dist < minDist) {
                minDist = dist;
                result.set(currentResult);
                if (Math.abs(minDist) < FloatMath.EPSILON) {
                    return;
                }
            }
        }
    }

    @Override
    public IntersectionType getIntersectionType (Rect rect)
    {
        if (!rect.intersects(_bounds)) {
            return IntersectionType.NONE;
        }
        IntersectionType type = IntersectionType.NONE;
        for (Shape shape : _shapes) {
            switch (shape.getIntersectionType(rect)) {
                case CONTAINS:
                    return IntersectionType.CONTAINS;
                case INTERSECTS:
                    type = IntersectionType.INTERSECTS;
            }
        }
        return type;
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
        if (!point.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (shape.intersects(point)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean intersects (Segment segment)
    {
        if (!segment.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (shape.intersects(segment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean intersects (Circle circle)
    {
        if (!circle.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (shape.intersects(circle)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean intersects (Capsule capsule)
    {
        if (!capsule.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (shape.intersects(capsule)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean intersects (Polygon polygon)
    {
        if (!polygon.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (shape.intersects(polygon)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean intersects (Compound compound)
    {
        if (!compound.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (compound.intersects(shape)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Vector2f getPenetration (Shape shape, Vector2f result)
    {
        return shape.getPenetration(this, result).negateLocal();
    }

    @Override
    public Vector2f getPenetration (Point point, Vector2f result)
    {
        return getSimplePenetration(point, result);
    }

    @Override
    public Vector2f getPenetration (Segment segment, Vector2f result)
    {
        return getSimplePenetration(segment, result);
    }

    @Override
    public Vector2f getPenetration (Circle circle, Vector2f result)
    {
        return getSimplePenetration(circle, result);
    }

    @Override
    public Vector2f getPenetration (Capsule capsule, Vector2f result)
    {
        return getSimplePenetration(capsule, result);
    }

    @Override
    public Vector2f getPenetration (Polygon polygon, Vector2f result)
    {
        return getSimplePenetration(polygon, result);
    }

    @Override
    public Vector2f getPenetration (Compound compound, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override
    public void draw (boolean outline)
    {
        for (Shape shape : _shapes) {
            shape.draw(outline);
        }
    }

    @Override
    public ShapeConfig createConfig ()
    {
        ShapeConfig.Compound compound = new ShapeConfig.Compound();
        compound.shapes = new ShapeConfig.TransformedShape[_shapes.length];
        for (int ii = 0; ii < _shapes.length; ii++) {
            ShapeConfig.TransformedShape tshape = compound.shapes[ii] =
                new ShapeConfig.TransformedShape();
            tshape.shape = _shapes[ii].createConfig();
        }
        return compound;
    }

    @Override
    public String toString ()
    {
        return "Comp:(" + StringUtil.join(_shapes) + ")";
    }

    /**
     * Performs a simple penetration test against all shapes in the compound.
     */
    protected Vector2f getSimplePenetration (Shape shape, Vector2f result)
    {
        // start with zero penetration
        result.set(Vector2f.ZERO);

        // check for intersection with each shape
        Vector2f oresult = new Vector2f();
        for (Shape cshape : _shapes) {
            if (cshape.intersects(shape)) {
                cshape.getPenetration(shape, oresult);
                if (oresult.lengthSquared() > result.lengthSquared()) {
                    result.set(oresult);
                }
            }
        }
        return result;
    }

    /** The shapes of which this shape is composed. */
    protected Shape[] _shapes;
}
