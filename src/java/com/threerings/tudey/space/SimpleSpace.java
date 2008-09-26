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
 * A simple, "flat" space implementation.
 */
public class SimpleSpace extends Space
{
    @Override // documentation inherited
    public SpaceElement getIntersection (
        Ray2D ray, Vector2f location, Predicate<SpaceElement> filter)
    {
        return getIntersection(_elements, ray, location, filter);
    }

    @Override // documentation inherited
    public void getElements (Rect bounds, Collection<SpaceElement> results)
    {
        getIntersecting(_elements, bounds, results);
    }

    @Override // documentation inherited
    protected void addToSpatial (SpaceElement element)
    {
        _elements.add(element);
    }

    @Override // documentation inherited
    protected void removeFromSpatial (SpaceElement element)
    {
        _elements.remove(element);
    }

    /** The list of all space elements. */
    protected ArrayList<SpaceElement> _elements = new ArrayList<SpaceElement>();
}
