//
// $Id$

package com.threerings.opengl.eff;

import com.threerings.opengl.effect.Particle;

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
