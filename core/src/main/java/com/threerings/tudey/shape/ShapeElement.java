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

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.config.ShapeConfig;
import com.threerings.tudey.space.SimpleSpaceElement;

/**
 * A shape element.
 */
public class ShapeElement extends SimpleSpaceElement
{
    /**
     * Creates a new shape element.
     */
    public ShapeElement (ShapeConfig config)
    {
        setConfig(config);
    }

    /**
     * Creates a new shape element.
     */
    public ShapeElement (Shape localShape)
    {
        setLocalShape(localShape);
    }

    /**
     * Sets the configuration of the shape.
     */
    public void setConfig (ShapeConfig config)
    {
        setLocalShape(config.getShape());
    }

    /**
     * Sets the local shape reference.
     */
    public void setLocalShape (Shape shape)
    {
        _localShape = shape;
        updateBounds();
    }

    /**
     * Returns a reference to the element's local shape.
     */
    public Shape getLocalShape ()
    {
        return _localShape;
    }

    /**
     * Returns a reference to the element's transformed shape.
     */
    public Shape getWorldShape ()
    {
        return _worldShape;
    }

    @Override
    public void updateBounds ()
    {
        _worldShape = _localShape.transform(_transform, _worldShape);
        Rect sbounds = _worldShape.getBounds();
        if (!_bounds.equals(sbounds)) {
            boundsWillChange();
            _bounds.set(sbounds);
            boundsDidChange();
        }
    }

    @Override
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return _worldShape.getIntersection(ray, result);
    }

    @Override
    public void getNearestPoint (Vector2f point, Vector2f result)
    {
        _worldShape.getNearestPoint(point, result);
    }

    @Override
    public boolean intersects (Point point)
    {
        return _worldShape.intersects(point);
    }

    @Override
    public boolean intersects (Segment segment)
    {
        return _worldShape.intersects(segment);
    }

    @Override
    public boolean intersects (Circle circle)
    {
        return _worldShape.intersects(circle);
    }

    @Override
    public boolean intersects (Capsule capsule)
    {
        return _worldShape.intersects(capsule);
    }

    @Override
    public boolean intersects (Polygon polygon)
    {
        return _worldShape.intersects(polygon);
    }

    @Override
    public boolean intersects (Compound compound)
    {
        return _worldShape.intersects(compound);
    }

    /** The untransformed shape. */
    protected Shape _localShape;

    /** The transformed shape. */
    protected Shape _worldShape;
}
