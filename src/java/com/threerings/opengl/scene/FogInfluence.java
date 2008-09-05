//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.opengl.renderer.state.FogState;

/**
 * Represents the influence of fog.
 */
public interface FogInfluence extends SceneInfluence
{
    /**
     * Returns a reference to the fog state.
     */
    public FogState getFogState ();
}
