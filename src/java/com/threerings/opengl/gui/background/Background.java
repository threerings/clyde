//
// $Id$

package com.threerings.opengl.gui.background;

import com.threerings.opengl.renderer.Renderer;

/**
 * Provides additional information about a background that is used to display
 * the backgrounds of various components.
 */
public abstract class Background
{
    /**
     * Returns the minimum width allowed by this background.
     */
    public int getMinimumWidth ()
    {
        return 1;
    }

    /**
     * Returns the minimum height allowed by this background.
     */
    public int getMinimumHeight ()
    {
        return 1;
    }

    /** Renders this background. */
    public void render (Renderer renderer, int x, int y, int width, int height,
        float alpha)
    {
    }
}
