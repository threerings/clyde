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
        _fogState = state;
    }

    @Override // documentation inherited
    public FogState getFogState ()
    {
        return _fogState;
    }

    /** The fog state. */
    protected FogState _fogState;
}
