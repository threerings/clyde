//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.opengl.renderer.Color4f;

/**
 * An effect that changes the background color.
 */
public class BackgroundColorEffect extends ViewerEffect
{
    /**
     * Creates a new background color effect.
     */
    public BackgroundColorEffect (Color4f color)
    {
        _backgroundColor.set(color);
    }

    @Override // documentation inherited
    public Color4f getBackgroundColor ()
    {
        return _backgroundColor;
    }

    /** The background color. */
    protected Color4f _backgroundColor = new Color4f();
}
