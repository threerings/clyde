//
// $Id: Placer.java 286 2008-08-29 00:55:46Z andrzej $

package com.threerings.opengl.effect;

/**
 * Determines particles' initial positions.
 */
public interface Placer
{
    /**
     * Configures the supplied particle with an initial position.
     */
    public void place (Particle particle);
}
