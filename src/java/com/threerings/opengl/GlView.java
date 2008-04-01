//
// $Id$

package com.threerings.opengl;

import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

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

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
    }
}
