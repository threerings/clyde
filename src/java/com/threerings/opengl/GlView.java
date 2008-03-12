//
// $Id$

package com.threerings.opengl;

/**
 * A simple base class for OpenGL views.
 */
public abstract class GlView
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
    public void update ()
    {
    }

    /**
     * Renders the view.
     */
    public void render ()
    {
    }
}
