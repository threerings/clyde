//
// $Id$

package com.threerings.tudey.space;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

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
     * Updates the bounds of the element.  The default implementation transforms the bounds
     * returned by {@link #getLocalBounds}.
     */
    public void updateBounds ()
    {
        // and the world bounds
        computeBounds(_nbounds);
        if (!_bounds.equals(_nbounds)) {
            boundsWillChange();
            _bounds.set(_nbounds);
            boundsDidChange();
        }
    }

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
     * Computes the bounds of the element and places them in the provided result object.  The
     * default implementation simply transforms the bounds returned by {@link #getLocalBounds}.
     */
    protected void computeBounds (Rect result)
    {
        getLocalBounds().transform(_transform, result);
    }

    /**
     * Returns the local bounds of the element.  Default implementation returns {@link Rect#ZERO};
     * override to return actual local bounds.
     */
    protected Rect getLocalBounds ()
    {
        return Rect.ZERO;
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

    /** Holds the new bounds of the element when updating. */
    protected Rect _nbounds = new Rect();

    /** The element's user object. */
    protected Object _userObject;

    /** The space to which this element has been added. */
    protected Space _space;

    /** The visitation id of the last visit. */
    protected int _lastVisit;
}
