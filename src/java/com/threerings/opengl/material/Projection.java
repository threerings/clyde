//
// $Id$

package com.threerings.opengl.material;

import com.threerings.math.Vector4f;

import com.threerings.opengl.material.config.TechniqueConfig;

/**
 * Represents a projection onto a surface.
 */
public class Projection
{
    /**
     * Rewrites a technique to include the supplied projections.
     */
    public static TechniqueConfig rewrite (TechniqueConfig technique, Projection[] projections)
    {
        return technique;
    }

    /**
     * Creates a new projection.
     *
     * @param technique the material technique to use to render the projection.
     */
    public Projection (TechniqueConfig technique)
    {
        _technique = technique;
    }

    /**
     * Returns a reference to the technique to use to render this projection.
     */
    public TechniqueConfig getTechnique ()
    {
        return _technique;
    }

    /**
     * Returns a reference to the s texture coordinate generation plane.
     */
    public Vector4f getGenPlaneS ()
    {
        return _genPlaneS;
    }

    /**
     * Returns a reference to the t texture coordinate generation plane.
     */
    public Vector4f getGenPlaneT ()
    {
        return _genPlaneT;
    }

    /**
     * Returns a reference to the r texture coordinate generation plane.
     */
    public Vector4f getGenPlaneR ()
    {
        return _genPlaneR;
    }

    /**
     * Returns a reference to the q texture coordinate generation plane.
     */
    public Vector4f getGenPlaneQ ()
    {
        return _genPlaneQ;
    }

    /** The technique to use to render the projection. */
    protected TechniqueConfig _technique;

    /** The s texture coordinate generation plane. */
    protected Vector4f _genPlaneS = new Vector4f(1f, 0f, 0f, 0f);

    /** The t texture coordinate generation plane. */
    protected Vector4f _genPlaneT = new Vector4f(0f, 1f, 0f, 0f);

    /** The r texture coordinate generation plane. */
    protected Vector4f _genPlaneR = new Vector4f(0f, 0f, 0f, 0f);

    /** The q texture coordinate generation plane. */
    protected Vector4f _genPlaneQ = new Vector4f(0f, 0f, 0f, 0f);
}
