//
// $Id$

package com.threerings.opengl;

import com.threerings.opengl.util.Renderable;

/**
 * A simple base class for OpenGL views.
 */
public abstract class GlView
    implements Renderable
{
    /**
     * Notifies the view that it is going to be rendered.
     */
    public void wasAdded ()
    {
    }

    /**
     * Notifies the view that it will no longer be rendered.
     */
    public void wasRemoved ()
    {
    }

    /**
     * Performs any per-frame updates that are necessary even when not rendering.
     */
    public void tick (float elapsed)
    {
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
    }
}
