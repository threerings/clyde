//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.math.Box;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.state.FogState;

/**
 * Base class for things that influence scene elements.
 */
public abstract class SceneInfluence
    implements SceneObject
{
    /**
     * Returns the ambient light color associated with this influence, or <code>null</code> for
     * none.
     */
    public Color4f getAmbientLight ()
    {
        return null;
    }

    /**
     * Returns the fog state associated with this influence, or <code>null</code> for none.
     */
    public FogState getFogState ()
    {
        return null;
    }

    /**
     * Returns the light associated with this influence, or <code>null</code> for none.
     */
    public Light getLight ()
    {
        return null;
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

    /** The bounds of the influence. */
    protected Box _bounds = new Box();

    /** The visitation id of the last visit. */
    protected int _lastVisit;
}
