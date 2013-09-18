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

package com.threerings.tudey.space;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.Point;
import com.threerings.tudey.shape.Segment;
import com.threerings.tudey.shape.Circle;
import com.threerings.tudey.shape.Capsule;
import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.shape.Compound;
import com.threerings.tudey.shape.None;

/**
 * A simple implementation of the {@link SpaceElement} interface.
 */
public abstract class SimpleSpaceElement
    implements SpaceElement
{
    /**
     * Sets the element's user object reference.
     */
    public void setUserObject (Object object)
    {
        _userObject = object;
    }

    /**
     * Sets the transform to the specified value and promotes it to {@link Transform2D#UNIFORM},
     * then updates the bounds of the element.
     */
    public void setTransform (Transform2D transform)
    {
        _transform.set(transform);
        _transform.promote(Transform2D.UNIFORM);
        updateBounds();
    }

    /**
     * Returns a reference to the transform of the element.
     */
    public Transform2D getTransform ()
    {
        return _transform;
    }

    /**
     * Returns a reference to the space to which this element has been added, if any.
     */
    public Space getSpace ()
    {
        return _space;
    }

    /**
     * Updates the bounds of the element.
     */
    public abstract void updateBounds ();

    // documentation inherited from interface SpaceElement
    public Object getUserObject ()
    {
        return _userObject;
    }

    // documentation inherited from interface SpaceElement
    public void wasAdded (Space space)
    {
        _space = space;
    }

    // documentation inherited from interface SpaceElement
    public void willBeRemoved ()
    {
        _space = null;
    }

    // documentation inherited from interface SpaceElement
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return false;
    }

    // documentation inherited from interface SpaceElement
    public void getNearestPoint (Vector2f point, Vector2f result)
    {
        result.set(point);
    }

    // documentation inherited from interface SpaceElement
    public boolean intersects (Point point)
    {
        return false;
    }

    // documentation inherited from interface SpaceElement
    public boolean intersects (Segment segment)
    {
        return false;
    }

    // documentation inherited from interface SpaceElement
    public boolean intersects (Circle circle)
    {
        return false;
    }

    // documentation inherited from interface SpaceElement
    public boolean intersects (Capsule capsule)
    {
        return false;
    }

    // documentation inherited from interface SpaceElement
    public boolean intersects (Polygon polygon)
    {
        return false;
    }

    // documentation inherited from interface SpaceElement
    public boolean intersects (Compound compound)
    {
        return false;
    }

    // documentation inherited from interface SpaceElement
    public boolean intersects (None compound)
    {
        return false;
    }

    // documentation inherited from interface SpaceObject
    public Rect getBounds ()
    {
        return _bounds;
    }

    // documentation inherited from interface SpaceObject
    public boolean updateLastVisit (int visit)
    {
        if (_lastVisit == visit) {
            return false;
        }
        _lastVisit = visit;
        return true;
    }

    /**
     * Notes that the bounds are about to change.
     */
    protected void boundsWillChange ()
    {
        if (_space != null) {
            _space.boundsWillChange(this);
        }
    }

    /**
     * Notes that the bounds have changed.
     */
    protected void boundsDidChange ()
    {
        if (_space != null) {
            _space.boundsDidChange(this);
        }
    }

    /** The transform of the element. */
    protected Transform2D _transform = new Transform2D(Transform2D.UNIFORM);

    /** The bounds of the element. */
    protected Rect _bounds = new Rect();

    /** The element's user object. */
    protected Object _userObject;

    /** The space to which this element has been added. */
    protected Space _space;

    /** The visitation id of the last visit. */
    protected int _lastVisit;
}
