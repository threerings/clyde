//
// $Id$

package com.threerings.opengl.gui.border;

import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.util.Insets;

/**
 * Defines a border with no rendered geometry but that simply takes up
 * space.
 */
public class EmptyBorder extends Border
{
    public EmptyBorder (int left, int top, int right, int bottom)
    {
        _insets = new Insets(left, top, right, bottom);
    }

    // documentation inherited
    public Insets adjustInsets (Insets insets)
    {
        return _insets.add(insets);
    }

    protected Insets _insets;
}
