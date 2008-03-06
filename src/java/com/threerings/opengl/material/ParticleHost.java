//
// $Id$

package com.threerings.opengl.material;

import com.threerings.opengl.effect.Particle;

/**
 * Extends {@link SurfaceHost} to provide access to particle parameters.
 */
public interface ParticleHost extends SurfaceHost
{
    /** The different alignment modes. */
    public enum Alignment { FIXED, BILLBOARD, VELOCITY };

    /**
     * Returns a reference to the array of particles.
     */
    public Particle[] getParticles ();

    /**
     * Returns the number of living particles.
     */
    public int getLiving ();

    /**
     * Returns the type of alignment with which to render the particles.
     */
    public Alignment getAlignment ();

    /**
     * Returns the number of texture divisions in the S direction.
     */
    public int getTextureDivisionsS ();

    /**
     * Returns the number of texture divisions in the T direction.
     */
    public int getTextureDivisionsT ();

    /**
     * Returns the particles' view space depth (for sorting).
     */
    public float getDepth ();
}
