//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.math.Box;
import com.threerings.util.ShallowObject;

import com.threerings.opengl.renderer.Color4f;

/**
 * Base class for things in the scene that affect the viewer.
 */
public abstract class ViewerEffect extends ShallowObject
    implements SceneObject
{
    /**
     * Returns the background color associated with this effect, or <code>null</code> for
     * none.
     */
    public Color4f getBackgroundColor ()
    {
        return null;
    }

    /**
     * Notes that the effect is now acting on the viewer.
     */
    public void activate ()
    {
        // nothing by default
    }

    /**
     * Notes that the effect is no longer acting on the viewer.
     */
    public void deactivate ()
    {
        // nothing by default
    }

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

    /** The bounds of the effect. */
    protected Box _bounds = new Box();

    /** The visitation id of the last visit. */
    protected int _lastVisit;
}
