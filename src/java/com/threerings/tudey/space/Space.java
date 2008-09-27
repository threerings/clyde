//
// $Id$

package com.threerings.tudey.space;

import java.util.ArrayList;
import java.util.Collection;

import com.samskivert.util.Predicate;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

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
        Predicate<SpaceElement> filter = Predicate.trueInstance();
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
        Ray2D ray, Vector2f location, Predicate<SpaceElement> filter);

    /**
     * Retrieves all space elements that intersect the provided intersector.
     *
     * @param results a collection to hold the results of the search.
     */
    public abstract void getIntersecting (
        Intersector intersector, Collection<SpaceElement> results);

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
        Predicate<SpaceElement> filter)
    {
        SpaceElement closest = null;
        Vector2f origin = ray.getOrigin();
        for (int ii = 0, nn = elements.size(); ii < nn; ii++) {
            SpaceElement element = elements.get(ii);
            if (filter.isMatch(element) && element.getIntersection(ray, _result) &&
                    (closest == null || origin.distanceSquared(_result) <
                        origin.distanceSquared(location))) {
                closest = element;
                location.set(_result);
            }
        }
        return closest;
    }

    /**
     * Adds all elements from the provided list that intersect the given intersector to the
     * specified results collection.
     */
    protected static void getIntersecting (
        ArrayList<SpaceElement> elements, Intersector intersector,
        Collection<SpaceElement> results)
    {
        for (int ii = 0, nn = elements.size(); ii < nn; ii++) {
            SpaceElement element = elements.get(ii);
            if (intersector.intersects(element)) {
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

    /** Result vector for intersection testing. */
    protected Vector2f _result = new Vector2f();
}
