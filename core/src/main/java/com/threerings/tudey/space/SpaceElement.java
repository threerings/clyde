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
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.Point;
import com.threerings.tudey.shape.Segment;
import com.threerings.tudey.shape.Circle;
import com.threerings.tudey.shape.Capsule;
import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.shape.Compound;
import com.threerings.tudey.shape.None;

/**
 * Interface for elements that can be embedded into spaces.
 */
public interface SpaceElement extends SpaceObject
{
    /**
     * Returns this element's user object reference.
     */
    public Object getUserObject ();

    /**
     * Notes that the element was added to the specified space.
     */
    public void wasAdded (Space space);

    /**
     * Notes that the element will be removed from the space.
     */
    public void willBeRemoved ();

    /**
     * Finds the intersection of a ray with this element and places it in the supplied vector
     * (if it exists).
     *
     * @return true if the ray intersected the element (in which case the result will contain the
     * point of intersection), false otherwise.
     */
    public boolean getIntersection (Ray2D ray, Vector2f result);

    /**
     * Finds the nearest point of this element to the supplied point and places it in the supplied
     * vector.
     */
    public void getNearestPoint (Vector2f point, Vector2f result);

    /**
     * Determines whether this element intersects the supplied point.
     */
    public boolean intersects (Point point);

    /**
     * Determines whether this element intersects the supplied segment.
     */
    public boolean intersects (Segment segment);

    /**
     * Determines whether this element intersects the supplied circle.
     */
    public boolean intersects (Circle circle);

    /**
     * Determines whether this element intersects the supplied capsule.
     */
    public boolean intersects (Capsule capsule);

    /**
     * Determines whether this element intersects the supplied polygon.
     */
    public boolean intersects (Polygon polygon);

    /**
     * Determines whether this element intersects the supplied compound.
     */
    public boolean intersects (Compound compound);

    /**
     * Determines whether this element intersects the supplied none.
     */
    public boolean intersects (None none);
}
