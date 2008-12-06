//
// $Id: Shooter.java 286 2008-08-29 00:55:46Z andrzej $

package com.threerings.opengl.effect;

import com.threerings.math.Vector3f;

/**
 * Determines particles' initial velocities.
 */
public interface Shooter
{
    /**
     * Configures the supplied particle with an initial velocity.
     *
     * @return a reference to the particle's velocity, for chaining.
     */
    public Vector3f shoot (Particle particle);
}
