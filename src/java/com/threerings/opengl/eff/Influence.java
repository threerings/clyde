//
// $Id$

package com.threerings.opengl.eff;

import com.threerings.opengl.effect.Particle;

/**
 * Represents an influence on a particle system.
 */
public interface Influence
{
    /**
     * Updates the influence for the current frame.
     */
    public void tick (float elapsed);

    /**
     * Applies this influence to the specified particle.
     */
    public void apply (Particle particle);
}
