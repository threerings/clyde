//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.opengl.renderer.Color4f;

/**
 * Represents the influence of the ambient light level.
 */
public class AmbientLightInfluence extends SceneInfluence
{
    /**
     * Creates a new ambient light influence with the specified color.
     */
    public AmbientLightInfluence (Color4f color)
    {
        _ambientLight.set(color);
    }

    @Override // documentation inherited
    public Color4f getAmbientLight ()
    {
        return _ambientLight;
    }

    /** The ambient light color. */
    protected Color4f _ambientLight = new Color4f();
}
