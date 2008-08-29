//
// $Id$

package com.threerings.opengl.eff;

import com.threerings.math.Vector3f;

import com.threerings.opengl.effect.Particle;

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
