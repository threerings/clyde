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

package com.threerings.opengl.material.config;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Matrix4f;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector4f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.compositor.RenderScheme;
import com.threerings.opengl.material.Projection;
import com.threerings.opengl.renderer.config.ColorStateConfig;
import com.threerings.opengl.util.GlContext;

/**
 * Contains the configuration of a surface projection.
 */
@EditorTypes({ ProjectionConfig.Perspective.class, ProjectionConfig.Orthographic.class })
public abstract class ProjectionConfig extends DeepObject
    implements Exportable
{
    /**
     * A perspective projection.
     */
    public static class Perspective extends ProjectionConfig
    {
        /** The distance to the near plane. */
        @Editable(min=0.0, step=0.01, hgroup="d")
        public float near = 1f;

        @Override
        protected String getScheme ()
        {
            return RenderScheme.PROJECTION_STQ;
        }

        @Override
        protected Updater createUpdater (GlContext ctx, Scope scope, final Projection projection)
        {
            final Transform3D viewTransform = ScopeUtil.resolve(
                scope, "viewTransform", new Transform3D());
            return new Updater() {
                public void update () {
                    viewTransform.invert(_viewTransformInv).update(Transform3D.AFFINE);
                    Matrix4f mat = _viewTransformInv.getMatrix();
                    float ns = (near == 0f) ? 0f : (-1f / near);
                    Vector4f gpq = projection.getGenPlaneQ();
                    gpq.set(ns*mat.m02, ns*mat.m12, ns*mat.m22, ns*mat.m32);
                    float ss = (width == 0f) ? 0f : (1f / width);
                    projection.getGenPlaneS().set(
                        ss*mat.m00 + 0.5f*gpq.x, ss*mat.m10 + 0.5f*gpq.y,
                        ss*mat.m20 + 0.5f*gpq.z, ss*mat.m30 + 0.5f*gpq.w);
                    float ts = (height == 0f) ? 0f : (1f / height);
                    projection.getGenPlaneT().set(
                        ts*mat.m01 + 0.5f*gpq.x, ts*mat.m11 + 0.5f*gpq.y,
                        ts*mat.m21 + 0.5f*gpq.z, ts*mat.m31 + 0.5f*gpq.w);
                }
                protected Transform3D _viewTransformInv = new Transform3D();
            };
        }
    }

    /**
     * An orthographic projection.
     */
    public static class Orthographic extends ProjectionConfig
    {
        @Override
        protected String getScheme ()
        {
            return RenderScheme.PROJECTION_ST;
        }

        @Override
        protected Updater createUpdater (GlContext ctx, Scope scope, final Projection projection)
        {
            final Transform3D viewTransform = ScopeUtil.resolve(
                scope, "viewTransform", new Transform3D());
            return new Updater() {
                public void update () {
                    viewTransform.invert(_viewTransformInv).update(Transform3D.AFFINE);
                    Matrix4f mat = _viewTransformInv.getMatrix();
                    float ss = (width == 0f) ? 0f : (1f / width);
                    projection.getGenPlaneS().set(
                        ss*mat.m00, ss*mat.m10, ss*mat.m20, ss*mat.m30 + 0.5f);
                    float ts = (height == 0f) ? 0f : (1f / height);
                    projection.getGenPlaneT().set(
                        ts*mat.m01, ts*mat.m11, ts*mat.m21, ts*mat.m31 + 0.5f);
                }
                protected Transform3D _viewTransformInv = new Transform3D();
            };
        }
    }

    /** The projection material. */
    @Editable(nullable=true)
    public ConfigReference<MaterialConfig> material;

    /** The color state for the projection. */
    @Editable(nullable=true)
    public ColorStateConfig colorState = new ColorStateConfig();

    /** The width of the projected image. */
    @Editable(min=0.0, step=0.01, hgroup="d")
    public float width = 1f;

    /** The height of the projected image. */
    @Editable(min=0.0, step=0.01, hgroup="d")
    public float height = 1f;

    /**
     * Creates a projection object corresponding to this configuration.
     */
    public Projection createProjection (GlContext ctx, Scope scope, ArrayList<Updater> updaters)
    {
        MaterialConfig mconfig = ctx.getConfigManager().getConfig(MaterialConfig.class, material);
        TechniqueConfig technique = (mconfig == null) ?
            null : mconfig.getTechnique(ctx, getScheme());
        if (technique == null) {
            return null;
        }
        Projection projection = new Projection(
            technique, (colorState == null) ? null : colorState.getState());
        updaters.add(createUpdater(ctx, scope, projection));
        return projection;
    }

    /**
     * Returns the name of the render scheme to use for this projection config.
     */
    protected abstract String getScheme ();

    /**
     * Creates the updater for the projection.
     */
    protected abstract Updater createUpdater (GlContext ctx, Scope scope, Projection projection);
}
