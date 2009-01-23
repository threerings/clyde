//
// $Id$

package com.threerings.tudey.space;

import java.util.ArrayList;
import java.util.Collection;

import com.samskivert.util.ObserverList;
import com.samskivert.util.Predicate;

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
     * Provides notification when elements are added, removed, or updated.
     */
    public interface Observer
    {
        /**
         * Notes that an element has been added to the space.
         */
        public void elementAdded (SpaceElement element);

        /**
         * Notes that an element has been removed from the space.
         */
        public void elementRemoved (SpaceElement element);

        /**
         * Notes that an element's bounds are about to change.
         */
        public void elementBoundsWillChange (SpaceElement element);

        /**
         * Notes that an element's bounds have changed.
         */
        public void elementBoundsDidChange (SpaceElement element);
    }

    /**
     * Adds an observer for element updates.
     */
    public void addObserver (Observer observer)
    {
        _observers.add(observer);
    }

    /**
     * Removes an observer.
     */
    public void removeObserver (Observer observer)
    {
        _observers.remove(observer);
    }

    /**
     * Adds an element to this space.
     */
    public void add (SpaceElement element)
    {
        // add to spatial data structure
        addToSpatial(element);

        // notify the element
        element.wasAdded(this);

        // notify the observers
        _elementAddedOp.init(element);
        _observers.apply(_elementAddedOp);
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

        // notify the observers
        _elementRemovedOp.init(element);
        _observers.apply(_elementRemovedOp);
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
     * Retrieves all space elements that intersect the provided shape.
     *
     * @param results a collection to hold the results of the search.
     */
    public abstract void getIntersecting (Shape shape, Collection<SpaceElement> results);

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
        // notify the observers
        _elementBoundsWillChangeOp.init(element);
        _observers.apply(_elementBoundsWillChangeOp);
    }

    /**
     * Notes that the specified space element's bounds have changed.
     */
    public void boundsDidChange (SpaceElement element)
    {
        // notify the observers
        _elementBoundsDidChangeOp.init(element);
        _observers.apply(_elementBoundsDidChangeOp);
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
     * Adds all elements from the provided list that intersect the given shape to the
     * specified results collection.
     */
    protected static void getIntersecting (
        ArrayList<SpaceElement> elements, Shape shape, Collection<SpaceElement> results)
    {
        for (int ii = 0, nn = elements.size(); ii < nn; ii++) {
            SpaceElement element = elements.get(ii);
            if (shape.intersects(element)) {
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

    /**
     * Base class for element operations.
     */
    protected static abstract class ElementOp
        implements ObserverList.ObserverOp<Observer>
    {
        /**
         * (Re)initializes the operation.
         */
        public void init (SpaceElement element)
        {
            _element = element;
        }

        /** The affected element. */
        protected SpaceElement _element;
    }

    /** Observers interested in element changes. */
    protected ObserverList<Observer> _observers = ObserverList.newFastUnsafe();

    /** Notifies observers that an element has been added. */
    protected ElementOp _elementAddedOp = new ElementOp() {
        public boolean apply (Observer observer) {
            observer.elementAdded(_element);
            return true;
        }
    };

    /** Notifies observers that an element has been removed. */
    protected ElementOp _elementRemovedOp = new ElementOp() {
        public boolean apply (Observer observer) {
            observer.elementRemoved(_element);
            return true;
        }
    };

    /** Notifies observers that an element's bounds are about to change. */
    protected ElementOp _elementBoundsWillChangeOp = new ElementOp() {
        public boolean apply (Observer observer) {
            observer.elementBoundsWillChange(_element);
            return true;
        }
    };

    /** Notifies observers that an element's bounds have changed. */
    protected ElementOp _elementBoundsDidChangeOp = new ElementOp() {
        public boolean apply (Observer observer) {
            observer.elementBoundsDidChange(_element);
            return true;
        }
    };

    /** Result vector for intersection testing. */
    protected Vector2f _result = new Vector2f();
}
