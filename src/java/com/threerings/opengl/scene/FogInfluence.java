//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.opengl.renderer.state.FogState;

/**
 * Represents the influence of fog.
 */
public class FogInfluence extends SceneInfluence
{
    /**
     * Creates a new fog influence object with the supplied state.
     */
    public FogInfluence (FogState state)
    {
        _state = state;
    }

    /**
     * Returns a reference to the fog state.
     */
    public FogState getState ()
    {
        return _state;
    }

    /** The fog state. */
    protected FogState _state;
}
