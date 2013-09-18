//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
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

import java.util.List;

import com.samskivert.util.ArrayUtil;

import com.threerings.expr.MutableInteger;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Vector4f;
import com.threerings.util.ShallowObject;

import com.threerings.opengl.compositor.Dependency;
import com.threerings.opengl.compositor.Enqueueable;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.geometry.Geometry;
import com.threerings.opengl.material.config.TechniqueConfig;
import com.threerings.opengl.material.config.TechniqueConfig.CompoundEnqueuer;
import com.threerings.opengl.material.config.TechniqueConfig.Enqueuer;
import com.threerings.opengl.material.config.TechniqueConfig.EnqueuerWrapper;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.util.GlContext;

/**
 * Represents a projection onto a surface.
 */
public class Projection extends ShallowObject
{
    /**
     * Rewrites a technique to include the supplied projections.
     */
    public static TechniqueConfig rewrite (TechniqueConfig technique, Projection[] projections)
    {
        technique = (TechniqueConfig)technique.clone();
        CompoundEnqueuer nenqueuer = new CompoundEnqueuer();
        nenqueuer.enqueuers = new Enqueuer[1 + projections.length];
        nenqueuer.enqueuers[0] = technique.enqueuer;
        technique.enqueuer = nenqueuer;
        for (int ii = 0; ii < projections.length; ii++) {
            Projection projection = projections[ii];
            TechniqueConfig ptech = projection.getTechnique();
            if (ptech.dependencies.length > 0) {
                technique.dependencies = ArrayUtil.concatenate(
                    technique.dependencies, ptech.dependencies);
            }
            nenqueuer.enqueuers[ii + 1] = ptech.enqueuer;
        }
        return technique;
    }

    /**
     * Creates a new projection.
     *
     * @param technique the material technique to use to render the projection.
     */
    public Projection (TechniqueConfig technique, ColorState colorState)
    {
        // wrap the enqueuer up to insert our scope
        _technique = new TechniqueConfig();
        _technique.dependencies = technique.dependencies;
        _technique.enqueuer = new EnqueuerWrapper(technique.enqueuer) {
            public Enqueueable createEnqueueable (
                GlContext ctx, Scope scope, Geometry geometry, boolean update,
                RenderQueue.Group group, List<Dependency.Adder> adders, MutableInteger pidx) {
                SimpleScope wscope = new SimpleScope(scope) {
                    public <T> T get (String name, Class<T> clazz) {
                        return ScopeUtil.get(Projection.this, name, clazz);
                    }
                };
                return super.createEnqueueable(
                    ctx, wscope, geometry, update, group, adders, pidx);
            }
        };
        _colorState = colorState;
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

    /** The projection's color state. */
    @Scoped
    protected ColorState _colorState;

    /** The s texture coordinate generation plane. */
    @Scoped
    protected Vector4f _genPlaneS = new Vector4f(1f, 0f, 0f, 0f);

    /** The t texture coordinate generation plane. */
    @Scoped
    protected Vector4f _genPlaneT = new Vector4f(0f, 1f, 0f, 0f);

    /** The r texture coordinate generation plane. */
    @Scoped
    protected Vector4f _genPlaneR = new Vector4f(0f, 0f, 0f, 0f);

    /** The q texture coordinate generation plane. */
    @Scoped
    protected Vector4f _genPlaneQ = new Vector4f(0f, 0f, 0f, 0f);
}
