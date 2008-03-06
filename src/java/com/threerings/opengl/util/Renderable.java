//
// $Id$

package com.threerings.opengl.util;

/**
 * A generic interface for objects that can enqueue themselves for rendering.
 */
public interface Renderable
{
    /**
     * Enqueues this object for rendering.
     */
    public void enqueue ();
}
