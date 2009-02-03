//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
