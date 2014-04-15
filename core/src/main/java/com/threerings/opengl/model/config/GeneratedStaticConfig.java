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

package com.threerings.opengl.model.config;

import java.lang.ref.SoftReference;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.material.config.GeometryMaterial;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.Static;
import com.threerings.opengl.model.config.StaticConfig.Resolved;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

/**
 * Configuration for static models generated in code.
 */
public class GeneratedStaticConfig extends ModelConfig.Implementation
{
    /**
     * The object responsible for generating the geometry.
     */
    @EditorTypes({ Quad.class })
    public static abstract class Generator extends DeepObject
        implements Exportable, Preloadable.LoadableConfig
    {
        /**
         * (Re)generates the geometry.
         */
        public abstract Resolved generate (GlContext ctx, int influenceFlags);
    }

    /**
     * Generates a simple quad with customizable level of tessellation.
     */
    public static class Quad extends Generator
    {
        /** The size in the x direction. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float sizeX = 2f;

        /** The size in the y direction. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float sizeY = 2f;

        /** The number of divisions in the x direction. */
        @Editable(min=1, hgroup="d")
        public int divisionsX = 1;

        /** The number of divisions in the y direction. */
        @Editable(min=1, hgroup="d")
        public int divisionsY = 1;

        /** The material for the quad. */
        @Editable(nullable=true)
        public ConfigReference<MaterialConfig> material;

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Config(MaterialConfig.class, material).preload(ctx);
        }

        @Override
        public Resolved generate (GlContext ctx, int influenceFlags)
        {
            float lx = -sizeX/2f, ux = +sizeX/2f;
            float ly = -sizeY/2f, uy = +sizeY/2f;
            Box bounds = new Box(new Vector3f(lx, ly, 0f), new Vector3f(ux, uy, 0f));
            CollisionMesh collision = new CollisionMesh(
                new Vector3f(lx, ly, 0f), new Vector3f(ux, uy, 0f), new Vector3f(lx, uy, 0f),
                new Vector3f(lx, ly, 0f), new Vector3f(ux, ly, 0f), new Vector3f(ux, uy, 0f));
            MaterialConfig material = ctx.getConfigManager().getConfig(
                MaterialConfig.class, this.material);
            GeometryConfig geometry = GeometryConfig.createQuad(
                sizeX, sizeY, divisionsX, divisionsY);
            GeometryMaterial[] gmats = new GeometryMaterial[] {
                new GeometryMaterial(geometry, material) };
            return new Resolved(bounds, collision, gmats, influenceFlags);
        }
    }

    /** The influences allowed to affect this generator. */
    @Editable
    public InfluenceFlagConfig influences = new InfluenceFlagConfig();

    /** The geometry generator. */
    @Editable
    public Generator generator = new Quad();

    @Override
    public void preload (GlContext ctx)
    {
        generator.preload(ctx);
    }

    @Override
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        Resolved resolved = (_resolved == null) ? null : _resolved.get();
        if (resolved == null) {
            _resolved = new SoftReference<Resolved>(
                resolved = generator.generate(ctx, influences.getFlags()));
        }
        if (impl instanceof Static) {
            ((Static)impl).setConfig(ctx, resolved);
        } else {
            impl = new Static(ctx, scope, resolved);
        }
        return impl;
    }

    @Override
    public void invalidate ()
    {
        _resolved = null;
    }

    /** The cached resolved config bits. */
    @DeepOmit
    protected transient SoftReference<Resolved> _resolved;
}
