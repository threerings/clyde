//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.opengl.renderer.Color4f;

/**
 * Represents the influence of the ambient light level.
 */
public interface AmbientLightInfluence extends SceneInfluence
{
    /**
     * Returns a reference to the ambient light color.
     */
    public Color4f getAmbientLight ();
}
