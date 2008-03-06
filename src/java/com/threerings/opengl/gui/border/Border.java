//
// $Id$

package com.threerings.opengl.gui.border;

import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.util.Insets;

/**
 * Configures a border around a component that may or may not have
 * associated geometric elements. <em>Note:</em> a border must only be
 * used with a single component at a time.
 */
public abstract class Border
{
    /**
     * Adds the supplied insets to this border's insets and returns adjusted
     * insets.
     */
    public abstract Insets adjustInsets (Insets insets);

    /** Renders this border. */
    public void render (Renderer renderer, int x, int y, int width, int height,
        float alpha)
    {
    }
}
