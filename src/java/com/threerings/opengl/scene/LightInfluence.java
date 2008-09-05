//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.opengl.renderer.Light;

/**
 * Represents the influence of a light.
 */
public interface LightInfluence extends SceneInfluence
{
    /**
     * Returns a reference to the light object.
     */
    public Light getLight ();
}
