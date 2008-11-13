//
// $Id$

package com.threerings.opengl;

import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

/**
 * A simple interface for OpenGL views.
 */
public interface GlView extends Tickable, Renderable
{
    /**
     * Notifies the view that it is going to be rendered.
     */
    public void wasAdded ();

    /**
     * Notifies the view that it will no longer be rendered.
     */
    public void wasRemoved ();
}
