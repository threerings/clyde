//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.math.Box;

/**
 * Base class for things that influence scene elements.
 */
public abstract class SceneInfluence
    implements SceneObject
{
    // documentation inherited from interface SceneObject
    public Box getBounds ()
    {
        return _bounds;
    }

    // documentation inherited from interface SceneObject
    public boolean updateLastVisit (int visit)
    {
        if (_lastVisit == visit) {
            return false;
        }
        _lastVisit = visit;
        return true;
    }

    /** The bounds of the influence. */
    protected Box _bounds = new Box();

    /** The visitation id of the last visit. */
    protected int _lastVisit;
}
