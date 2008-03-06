//
// $Id$

package com.threerings.opengl.gui.icon;

import com.threerings.opengl.renderer.Renderer;

/**
 * Provides icon imagery for various components which make use of it.
 */
public abstract class Icon
{
    /** Returns the width of this icon. */
    public abstract int getWidth ();

    /** Returns the height of this icon. */
    public abstract int getHeight ();

    /** Renders this icon. */
    public void render (Renderer renderer, int x, int y, float alpha)
    {
    }
}
