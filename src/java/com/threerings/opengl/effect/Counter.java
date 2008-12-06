//
// $Id: Counter.java 286 2008-08-29 00:55:46Z andrzej $

package com.threerings.opengl.effect;

/**
 * Determines how many particles to emit at each frame.
 */
public interface Counter
{
    /**
     * Returns the number of particles to release at this frame.
     */
    public int count (float elapsed, int maximum);

    /**
     * Resets the counter to its initial state.
     */
    public void reset ();
}
