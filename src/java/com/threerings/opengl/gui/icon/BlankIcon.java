//
// $Id$

package com.threerings.opengl.gui.icon;

import com.threerings.opengl.renderer.Renderer;

/**
 * Takes up space.
 */
public class BlankIcon extends Icon
{
    public BlankIcon (int width, int height)
    {
        _width = width;
        _height = height;
    }

    // documentation inherited
    public int getWidth ()
    {
        return _width;
    }

    // documentation inherited
    public int getHeight ()
    {
        return _height;
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, float alpha)
    {
        // nothing doing
    }

    protected int _width, _height;
}
