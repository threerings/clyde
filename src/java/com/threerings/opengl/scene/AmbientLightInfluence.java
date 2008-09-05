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
        _color.set(color);
    }

    /**
     * Returns a reference to the color.
     */
    public Color4f getColor ()
    {
        return _color;
    }

    /** The light color. */
    protected Color4f _color = new Color4f();
}
