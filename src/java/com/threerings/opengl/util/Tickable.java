//
// $Id$

package com.threerings.opengl.util;

/**
 * A generic interface for objects that can participate in a per-frame tick.
 */
public interface Tickable
{
    /**
     * Updates the state of this object based on the elapsed time in seconds.
     */
    public void tick (float elapsed);
}
