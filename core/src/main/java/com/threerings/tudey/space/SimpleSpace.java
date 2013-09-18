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

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.base.Predicate;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.Shape;

/**
 * A simple, "flat" space implementation.
 */
public class SimpleSpace extends Space
{
    @Override
    public SpaceElement getIntersection (
        Ray2D ray, Vector2f location, Predicate<? super SpaceElement> filter)
    {
        return getIntersection(_elements, ray, location, filter);
    }

    @Override
    public void getIntersecting (
            Shape shape, Predicate<? super SpaceElement> filter, Collection<SpaceElement> results)
    {
        getIntersecting(_elements, shape, filter, results);
    }

    @Override
    public void getElements (Rect bounds, Collection<SpaceElement> results)
    {
        getIntersecting(_elements, bounds, results);
    }

    @Override
    protected void addToSpatial (SpaceElement element)
    {
        _elements.add(element);
    }

    @Override
    protected void removeFromSpatial (SpaceElement element)
    {
        _elements.remove(element);
    }

    /** The list of all space elements. */
    protected ArrayList<SpaceElement> _elements = new ArrayList<SpaceElement>();
}
