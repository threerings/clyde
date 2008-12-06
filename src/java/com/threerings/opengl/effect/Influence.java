//
// $Id: Influence.java 286 2008-08-29 00:55:46Z andrzej $

package com.threerings.opengl.effect;

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
