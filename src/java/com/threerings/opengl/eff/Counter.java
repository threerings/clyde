//
// $Id$

package com.threerings.opengl.eff;

import com.threerings.opengl.effect.Particle;

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
