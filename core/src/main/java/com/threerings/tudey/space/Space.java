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
import com.google.common.base.Predicates;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.Shape;

/**
 * Base class for spaces.
 */
public abstract class Space
{
    /**
     * Adds an element to this space.
     */
    public void add (SpaceElement element)
    {
        // add to spatial data structure
        addToSpatial(element);

        // notify the element
        element.wasAdded(this);
    }

    /**
     * Removes an element from the space.
     */
    public void remove (SpaceElement element)
    {
        if (_disposed) {
            return; // don't bother with the extra computation
        }

        // notify element
        element.willBeRemoved();

        // remove from spatial data structure
        removeFromSpatial(element);
    }

    /**
     * Checks for an intersection between the provided ray and the contents of the space.
     *
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first element intersected by the ray, or <code>null</code> for
     * none.
     */
    public SpaceElement getIntersection (Ray2D ray, Vector2f location)
    {
        Predicate<SpaceElement> filter = Predicates.alwaysTrue();
        return getIntersection(ray, location, filter);
    }

    /**
     * Checks for an intersection between the provided ray and the contents of the space.
     *
     * @param filter a predicate to use in filtering the results of the test.
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first element intersected by the ray, or <code>null</code> for
     * none.
     */
    public abstract SpaceElement getIntersection (
        Ray2D ray, Vector2f location, Predicate<? super SpaceElement> filter);

    /**
     * Retrieves all space elements that intersect the provided shape.
     *
     * @param results a collection to hold the results of the search.
     */
    public void getIntersecting (Shape shape, Collection<SpaceElement> results)
    {
        Predicate<SpaceElement> filter = Predicates.alwaysTrue();
        getIntersecting(shape, filter, results);
    }

    /**
     * Retrieves all space elements that intersect the provided shape.
     *
     * @param results a collection to hold the results of the search.
     */
    public abstract void getIntersecting (
            Shape shape, Predicate<? super SpaceElement> filter, Collection<SpaceElement> results);

    /**
     * Retrieves all space elements whose bounds intersect the provided region.
     *
     * @param results a list to hold the results of the search.
     */
    public abstract void getElements (Rect bounds, Collection<SpaceElement> results);

    /**
     * Notes that the specified space element's bounds are about to change.  Will be followed by a
     * call to {@link #boundsDidChange(SpaceElement)} when the change has been effected.
     */
    public void boundsWillChange (SpaceElement element)
    {
        // nothing by default
    }

    /**
     * Notes that the specified space element's bounds have changed.
     */
    public void boundsDidChange (SpaceElement element)
    {
        // nothing by default
    }

    /**
     * Flags this space as having been disposed.
     */
    public void dispose ()
    {
        _disposed = true;
    }

    /**
     * Adds an element to the space's spatial data structure.
     */
    protected abstract void addToSpatial (SpaceElement element);

    /**
     * Removes an element from the space's spatial data structure.
     */
    protected abstract void removeFromSpatial (SpaceElement element);

    /**
     * Searches for an intersection with the supplied elements.
     */
    protected SpaceElement getIntersection (
        ArrayList<SpaceElement> elements, Ray2D ray, Vector2f location,
        Predicate<? super SpaceElement> filter)
    {
        SpaceElement closest = null;
        Vector2f origin = ray.getOrigin();
        for (int ii = 0, nn = elements.size(); ii < nn; ii++) {
            SpaceElement element = elements.get(ii);
            if (filter.apply(element) && element.getIntersection(ray, _result) &&
                    (closest == null || origin.distanceSquared(_result) <
                        origin.distanceSquared(location))) {
                closest = element;
                location.set(_result);
            }
        }
        return closest;
    }

    /**
     * Adds all elements from the provided list that intersect the given shape to the
     * specified results collection.
     */
    protected static void getIntersecting (ArrayList<SpaceElement> elements, Shape shape,
            Predicate<? super SpaceElement> filter, Collection<SpaceElement> results)
    {
        for (int ii = 0, nn = elements.size(); ii < nn; ii++) {
            SpaceElement element = elements.get(ii);
            if (filter.apply(element) && shape.intersects(element)) {
                results.add(element);
            }
        }
    }

    /**
     * Adds all objects from the provided list that intersect the given bounds to the specified
     * results list.
     */
    protected static <T extends SpaceObject> void getIntersecting (
        ArrayList<T> objects, Rect bounds, Collection<T> results)
    {
        for (int ii = 0, nn = objects.size(); ii < nn; ii++) {
            T object = objects.get(ii);
            if (object.getBounds().intersects(bounds)) {
                results.add(object);
            }
        }
    }

    /** Set when we've been disposed. */
    protected boolean _disposed;

    /** Result vector for intersection testing. */
    protected Vector2f _result = new Vector2f();
}
