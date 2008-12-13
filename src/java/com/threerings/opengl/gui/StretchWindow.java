//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.layout.LayoutManager;

/**
 * A window that automatically stretches to cover the entire render surface.
 */
public class StretchWindow extends Window
    implements Renderer.Observer
{
    public StretchWindow (GlContext ctx, LayoutManager layout)
    {
        super(ctx, layout);
    }

    // documentation inherited from interface Renderer.Observer
    public void sizeChanged (int width, int height)
    {
        setSize(width, height);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // update size and register as observer
        Renderer renderer = _ctx.getRenderer();
        setSize(renderer.getWidth(), renderer.getHeight());
        renderer.addObserver(this);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getRenderer().removeObserver(this);
    }
}
