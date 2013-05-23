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

package com.threerings.opengl.scene.config;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.Updater;
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Matrix3f;
import com.threerings.math.Matrix4f;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.math.Vector4f;
import com.threerings.util.DeepObject;
import com.threerings.util.ShallowObject;

import com.threerings.opengl.compositor.RenderScheme;
import com.threerings.opengl.material.Projection;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.material.config.TechniqueConfig;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.config.ColorStateConfig;
import com.threerings.opengl.renderer.config.LightConfig;
import com.threerings.opengl.scene.LightInfluence;
import com.threerings.opengl.scene.SceneInfluence;
import com.threerings.opengl.util.GlContext;

/**
 * Represents a means of generating shadows from a light.
 */
@EditorTypes({ ShadowConfig.Texture.class })
public abstract class ShadowConfig extends DeepObject
    implements Exportable
{
    /**
     * Generates shadows by rendering silhouettes or a depth map from the perspective
     * of the light into a texture and projecting that texture onto shadow receivers.
     */
    public static class Texture extends ShadowConfig
    {
        /** The distance to the near clip plane. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float near = 1f;

        /** The distance to the far clip plane. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float far = 100f;

        /** The projection material. */
        @Editable(nullable=true)
        public ConfigReference<MaterialConfig> material;

        /** The color state for the projection. */
        @Editable(nullable=true)
        public ColorStateConfig colorState = new ColorStateConfig();

        @Override
        public SceneInfluence createInfluence (
            final GlContext ctx, Scope scope, LightConfig lightConfig,
            final Light viewLight, ArrayList<Updater> updaters)
        {
            MaterialConfig mconfig = ctx.getConfigManager().getConfig(
                MaterialConfig.class, material);
            Light.Type lightType = viewLight.getType();
            TechniqueConfig technique = (mconfig == null) ?
                null : mconfig.getTechnique(ctx, getProjectionScheme(lightType));
            if (technique == null) {
                return new LightInfluence(viewLight);
            }
            final TextureData data = new TextureData();
            data.near = near;
            data.far = far;
            ArrayList<Updater> worldUpdaters = new ArrayList<Updater>(1);
            data.light = lightConfig.createLight(ctx, scope, true, worldUpdaters);
            data.updater = worldUpdaters.get(0);
            final Projection projection = new Projection(technique,
                    (colorState == null) ? null : colorState.getState()) {
                @Scoped protected TextureData _data = data;
            };
            if (lightType == Light.Type.DIRECTIONAL) {
                updaters.add(new Updater() {
                    public void update () {
                        ctx.getCompositor().getCamera().getViewTransform().compose(
                            data.transform, _viewTransformInv);
                        _viewTransformInv.invertLocal().update(Transform3D.AFFINE);
                        Matrix4f mat = _viewTransformInv.getMatrix();
                        float ss = 1f / data.width;
                        projection.getGenPlaneS().set(
                            ss*mat.m00, ss*mat.m10, ss*mat.m20, ss*mat.m30 + 0.5f);
                        float ts = 1f / data.height;
                        projection.getGenPlaneT().set(
                            ts*mat.m01, ts*mat.m11, ts*mat.m21, ts*mat.m31 + 0.5f);
                        float rs = -1f / data.depth;
                        projection.getGenPlaneR().set(
                            rs*mat.m02, rs*mat.m12, rs*mat.m22, rs*mat.m32);
                    }
                    protected Transform3D _viewTransformInv = new Transform3D();
                });
            } else if (lightType == Light.Type.POINT) {
                updaters.add(new Updater() {
                    public void update () {
                        ctx.getCompositor().getCamera().getViewTransform().compose(
                            data.transform, _viewTransformInv);
                        _viewTransformInv.invertLocal().update(Transform3D.AFFINE);
                        Matrix4f mat = _viewTransformInv.getMatrix();
                        projection.getGenPlaneS().set(mat.m00, mat.m10, mat.m20, mat.m30);
                        projection.getGenPlaneT().set(mat.m01, mat.m11, mat.m21, mat.m31);
                        projection.getGenPlaneR().set(mat.m02, mat.m12, mat.m22, mat.m32);
                    }
                    protected Transform3D _viewTransformInv = new Transform3D();
                });
            } else { // lightType == Light.Type.SPOT
                updaters.add(new Updater() {
                    public void update () {
                        ctx.getCompositor().getCamera().getViewTransform().compose(
                            data.transform, _viewTransformInv);
                        _viewTransformInv.invertLocal().update(Transform3D.AFFINE);
                        Matrix4f mat = _viewTransformInv.getMatrix();
                        Vector4f gpq = projection.getGenPlaneQ();
                        gpq.set(-mat.m02, -mat.m12, -mat.m22, -mat.m32);
                        float ss = 0.5f / FloatMath.tan(FloatMath.toRadians(viewLight.spotCutoff));
                        projection.getGenPlaneS().set(
                            ss*mat.m00 + 0.5f*gpq.x, ss*mat.m10 + 0.5f*gpq.y,
                            ss*mat.m20 + 0.5f*gpq.z, ss*mat.m30 + 0.5f*gpq.w);
                        projection.getGenPlaneT().set(
                            ss*mat.m01 + 0.5f*gpq.x, ss*mat.m11 + 0.5f*gpq.y,
                            ss*mat.m21 + 0.5f*gpq.z, ss*mat.m31 + 0.5f*gpq.w);
                        float rs = far / (near - far);
                        projection.getGenPlaneR().set(
                            rs*mat.m02, rs*mat.m12, rs*mat.m22, rs*mat.m32 + near*rs);
                    }
                    protected Transform3D _viewTransformInv = new Transform3D();
                });
            }
            return new LightInfluence(viewLight) {
                @Override public Projection getProjection () {
                    return projection;
                }
            };
        }
    }

    /**
     * A simple container for several state elements shared between the updater and the dependency.
     */
    public static abstract class Data extends ShallowObject
    {
        /** The light details in world space. */
        public Light light;

        /** An updater for the world light's transform. */
        public Updater updater;
    }

    /**
     * Data elements specific to texture shadows.
     */
    public static class TextureData extends Data
    {
        /** The light's transform in world space. */
        public Transform3D transform = new Transform3D();

        /** The distances to the near and far clip planes. */
        public float near, far;

        /** The dimensions of the light projection. */
        public float width, height, depth;
    }

    /**
     * Creates the scene influence corresponding to this config.
     *
     * @param updaters a list to populate with required updaters.
     */
    public abstract SceneInfluence createInfluence (
        GlContext ctx, Scope scope, LightConfig lightConfig,
        Light viewLight, ArrayList<Updater> updaters);

    /**
     * Returns the render scheme to use for the projection of the specified light type.
     */
    protected static String getProjectionScheme (Light.Type type)
    {
        switch (type) {
            case DIRECTIONAL: return RenderScheme.PROJECTION_STR;
            case POINT: return RenderScheme.PROJECTION_STR;
            case SPOT: return RenderScheme.PROJECTION_STRQ;
            default: return null;
        }
    }
}
