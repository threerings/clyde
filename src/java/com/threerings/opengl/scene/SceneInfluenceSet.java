//
// $Id$

package com.threerings.opengl.scene;

import java.util.HashSet;

import com.threerings.math.Box;

import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;

/**
 * A set of scene influences.
 */
public class SceneInfluenceSet extends HashSet<SceneInfluence>
{
    /**
     * Returns the fog state for this influence set.
     *
     * @param bounds the bounds used to resolve conflicts.
     * @param state an existing state to reuse, if possible.
     */
    public FogState getFogState (Box bounds, FogState state)
    {
        return null;
    }

    /**
     * Returns the light state for this influence set.
     *
     * @param bounds the bounds used to resolve conflicts.
     * @param state an existing state to reuse, if possible.
     */
    public LightState getLightState (Box bounds, LightState state)
    {
        return null;
    }
}
