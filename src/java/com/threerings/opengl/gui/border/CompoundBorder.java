//
// $Id$

package com.threerings.opengl.gui.border;

import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.util.Insets;

/**
 * Combines two borders into a single compound border.
 */
public class CompoundBorder extends Border
{
    public CompoundBorder (Border outer, Border inner)
    {
        _outer = outer;
        _inner = inner;
        _insets = outer.adjustInsets(Insets.ZERO_INSETS);
    }

    // documentation inherited
    public Insets adjustInsets (Insets insets)
    {
        return _outer.adjustInsets(_inner.adjustInsets(insets));
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, int width, int height,
        float alpha)
    {
        _outer.render(renderer, x, y, width, height, alpha);
        _inner.render(renderer, x + _insets.left, y + _insets.bottom,
                      width - _insets.getHorizontal(),
                      height - _insets.getVertical(), alpha);
    }

    protected Border _outer, _inner;
    protected Insets _insets;
}
