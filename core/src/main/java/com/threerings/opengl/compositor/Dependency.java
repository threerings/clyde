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

package com.threerings.opengl.compositor;

import org.lwjgl.opengl.PixelFormat;

import com.threerings.expr.Updater;
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Frustum;
import com.threerings.math.Matrix3f;
import com.threerings.math.Plane;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.math.Vector4f;

import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.compositor.config.RenderEffectConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.Texture;
import com.threerings.opengl.renderer.TextureRenderer;
import com.threerings.opengl.renderer.config.TextureConfig;
import com.threerings.opengl.scene.config.ShadowConfig;
import com.threerings.opengl.util.GlContext;

/**
 * Represents a dependency to be resolved in the course of rendering: shadows from a particular
 * light, a reflection off a plane, etc.
 */
public abstract class Dependency
{
    /**
     * An interface for objects that add dependendencies to the compositor.
     */
    public interface Adder
    {
        /**
         * Adds the dependency.
         *
         * @return true if the dependency was added, false if the dependency cannot be added
         * and the current {@link Compositable} should not be rendered.
         */
        public boolean add ();
    }

    /**
     * The base class of the various planar reflection/refraction dependencies.
     */
    public static abstract class Planar extends Dependency
    {
        /** The world space plane of reflection or refraction. */
        public Plane worldPlane = new Plane();

        /** The eye space plane. */
        public Plane eyePlane = new Plane();

        /** The bounds of the affected region. */
        public Box bounds = new Box();

        /**
         * Creates a new planar dependency.
         */
        public Planar (GlContext ctx)
        {
            super(ctx);
        }

        @Override
        public void merge (Dependency dependency)
        {
            bounds.addLocal(((Planar)dependency).bounds);
        }

        @Override
        public int hashCode ()
        {
            return worldPlane.hashCode();
        }

        @Override
        public boolean equals (Object other)
        {
            return getClass() == other.getClass() && ((Planar)other).worldPlane.equals(worldPlane);
        }
    }

    /**
     * A stencil reflection.
     */
    public static class StencilReflection extends Planar
    {
        /**
         * Creates a new stencil reflection dependency.
         */
        public StencilReflection (GlContext ctx)
        {
            super(ctx);
        }
    }

    /**
     * A stencil refraction.
     */
    public static class StencilRefraction extends Planar
    {
        /** The refraction ratio (index of refraction below the surface over index of refraction
         * above the surface). */
        public float ratio = 1f;

        /**
         * Creates a new stencil refraction dependency.
         */
        public StencilRefraction (GlContext ctx)
        {
            super(ctx);
        }

        @Override
        public boolean equals (Object other)
        {
            return super.equals(other) && ((StencilRefraction)other).ratio == ratio;
        }
    }

    /**
     * The base class for planar reflection/refraction textures.
     */
    public static abstract class PlanarTexture extends Planar
    {
        /** The texture to which we render. */
        public Texture texture;

        /** The config from whose pool the texture was created. */
        public TextureConfig config;

        /**
         * Creates a new planar texture dependency.
         */
        public PlanarTexture (GlContext ctx)
        {
            super(ctx);
        }

        @Override
        public void merge (Dependency dependency)
        {
            super.merge(dependency);
            PlanarTexture odep = (PlanarTexture)dependency;
            texture = odep.texture;
            config = odep.config;
        }

        @Override
        public void cleanup ()
        {
            config.returnToPool(_ctx, texture);
        }
    }

    /**
     * A planar reflection texture.
     */
    public static class ReflectionTexture extends PlanarTexture
    {
        /**
         * Creates a new reflection texture dependency.
         */
        public ReflectionTexture (GlContext ctx)
        {
            super(ctx);
        }

        @Override
        public void resolve ()
        {
            Compositor compositor = _ctx.getCompositor();
            Camera ocamera = compositor.getCamera();
            Compositor.State cstate = compositor.prepareSubrender();
            Camera ncamera = compositor.getCamera();
            Vector3f normal = eyePlane.getNormal();
            float near = eyePlane.constant / normal.z;
            float scale = ocamera.isOrtho() ? 1f : (near / ocamera.getNear());
            ncamera.setProjection(
                ocamera.getLeft() * scale, ocamera.getRight() * scale,
                ocamera.getBottom() * scale, ocamera.getTop() * scale,
                near, near + ocamera.getFar() - ocamera.getNear(), normal, ocamera.isOrtho(),
                !ocamera.isMirrored());
            Transform3D transform = ncamera.getWorldTransform();
            transform.setType(Transform3D.AFFINE);
            transform.getMatrix().setToReflection(eyePlane);
            ocamera.getWorldTransform().compose(transform, transform);
            ncamera.updateTransform();
            TextureRenderer renderer = TextureRenderer.getInstance(
                _ctx, texture, null, new PixelFormat(8, 16, 8));
            renderer.startRender();
            try {
                compositor.performSubrender(this);
            } finally {
                renderer.commitRender();
                compositor.cleanupSubrender(cstate);
            }
        }
    }

    /**
     * A planar refraction texture.
     */
    public static class RefractionTexture extends PlanarTexture
    {
        /** The refraction ratio (index of refraction below the surface over index of refraction
         * above the surface). */
        public float ratio = 1f;

        /**
         * Creates a new refraction texture dependency.
         */
        public RefractionTexture (GlContext ctx)
        {
            super(ctx);
        }

        @Override
        public void resolve ()
        {
            Compositor compositor = _ctx.getCompositor();
            Camera ocamera = compositor.getCamera();
            Compositor.State cstate = compositor.prepareSubrender();
            Camera ncamera = compositor.getCamera();
            Vector3f normal = eyePlane.getNormal();
            float near = eyePlane.constant / normal.z;
            float scale = ocamera.isOrtho() ? 1f : (near / ocamera.getNear());
            ncamera.setProjection(
                ocamera.getLeft() * scale, ocamera.getRight() * scale,
                ocamera.getBottom() * scale, ocamera.getTop() * scale,
                near, near + ocamera.getFar() - ocamera.getNear(), normal, ocamera.isOrtho(),
                ocamera.isMirrored());
            FloatMath.refract(_v0.set(0f, 0f, -1f), normal, ratio, _v1);
            _v1.multLocal(normal.dot(_v1));
            Transform3D transform = ncamera.getWorldTransform();
            transform.setType(Transform3D.AFFINE);
            transform.getMatrix().setToSkew(eyePlane, _v0.set(_v1.x, _v1.y, _v1.z - normal.z));
            ocamera.getWorldTransform().compose(transform, transform);
            ncamera.updateTransform();
            TextureRenderer renderer = TextureRenderer.getInstance(
                _ctx, texture, null, new PixelFormat(8, 16, 8));
            renderer.startRender();
            try {
                compositor.performSubrender(this);
            } finally {
                renderer.commitRender();
                compositor.cleanupSubrender(cstate);
            }
        }

        @Override
        public boolean equals (Object other)
        {
            return super.equals(other) && ((RefractionTexture)other).ratio == ratio;
        }

        /** Temporary vectors. */
        protected Vector3f _v0 = new Vector3f(), _v1 = new Vector3f();
    }

    /**
     * A cube map texture.
     */
    public static class CubeTexture extends Dependency
    {
        /** The render origin in world space. */
        public Vector3f origin = new Vector3f();

        /** Distances to the near and far clip planes. */
        public float near, far;

        /** The texture to which we render. */
        public Texture texture;

        /** A bitmask of the faces to include. */
        public int faces;

        /** The config from whose pool the texture was created. */
        public TextureConfig config;

        /**
         * Creates a new cube texture dependency.
         */
        public CubeTexture (GlContext ctx)
        {
            super(ctx);
        }

        @Override
        public void merge (Dependency dependency)
        {
            CubeTexture odep = (CubeTexture)dependency;
            texture = odep.texture;
            config = odep.config;
            near = Math.min(near, odep.near);
            far = Math.max(far, odep.far);
            faces |= odep.faces;
        }

        @Override
        public void resolve ()
        {
            Compositor compositor = _ctx.getCompositor();
            Camera ocamera = compositor.getCamera();
            Compositor.State cstate = compositor.prepareSubrender();
            Camera ncamera = compositor.getCamera();
            ncamera.setFrustum(-near, +near, -near, +near, near, far);
            Quaternion rot = new Quaternion();
            ocamera.getWorldTransform().extractRotation(rot);
            ncamera.getWorldTransform().set(origin, rot, 1f);
            TextureRenderer renderer = TextureRenderer.getInstance(
                _ctx, texture, null, new PixelFormat(8, 16, 8));
            try {
                for (int ii = 0; ii < 6; ii++) {
                    if ((faces & (1 << ii)) == 0) {
                        continue;
                    }
                    rot.mult(CUBE_FACE_ROTATIONS[ii], ncamera.getWorldTransform().getRotation());
                    ncamera.updateTransform();
                    renderer.startRender(0, ii);
                    try {
                        compositor.performSubrender(this);
                    } finally {
                        renderer.commitRender();
                    }
                }
            } finally {
                compositor.cleanupSubrender(cstate);
            }
        }

        @Override
        public void cleanup ()
        {
            config.returnToPool(_ctx, texture);
        }

        @Override
        public int hashCode ()
        {
            return origin.hashCode();
        }

        @Override
        public boolean equals (Object other)
        {
            return getClass() == other.getClass() && ((CubeTexture)other).origin.equals(origin);
        }
    }

    /**
     * The base class for shadows from a single light.
     */
    public static abstract class Shadows extends Dependency
    {
        /** The data object shared between dependency and influence updater. */
        public ShadowConfig.Data data;

        /**
         * Creates a new shadow dependency.
         */
        public Shadows (GlContext ctx)
        {
            super(ctx);
        }

        @Override
        public void resolve ()
        {
            data.updater.update();
        }

        @Override
        public int hashCode ()
        {
            return data.hashCode();
        }

        @Override
        public boolean equals (Object other)
        {
            return getClass() == other.getClass() && ((Shadows)other).data.equals(data);
        }
    }

    /**
     * Stencil shadow volumes.
     */
    public static class ShadowVolumes extends Shadows
    {
        /**
         * Creates a new shadow volume dependency.
         */
        public ShadowVolumes (GlContext ctx)
        {
            super(ctx);
        }
    }

    /**
     * A shadow depth texture.
     */
    public static class ShadowTexture extends Shadows
    {
        /** The color texture, if any. */
        public Texture color;

        /** The config from whose pool the color texture was created. */
        public TextureConfig colorConfig;

        /** The depth texture, if any. */
        public Texture depth;

        /** The config from whose pool the depth texture was created. */
        public TextureConfig depthConfig;

        /**
         * Creates a new shadow texture dependency.
         */
        public ShadowTexture (GlContext ctx)
        {
            super(ctx);
        }

        @Override
        public void merge (Dependency dependency)
        {
            ShadowTexture odep = (ShadowTexture)dependency;
            color = odep.color;
            colorConfig = odep.colorConfig;
            depth = odep.depth;
            depthConfig = odep.depthConfig;
        }

        @Override
        public void resolve ()
        {
            super.resolve();
            ShadowConfig.TextureData data = (ShadowConfig.TextureData)this.data;
            Compositor compositor = _ctx.getCompositor();
            Camera ocamera = compositor.getCamera();
            Color4f obackground = compositor.getBackgroundColor();
            compositor.setBackgroundColor(Color4f.TRANSPARENT_BLACK);
            Compositor.State cstate = compositor.prepareSubrender();
            Camera ncamera = compositor.getCamera();
            Transform3D transform = ncamera.getWorldTransform();
            Light.Type lightType = data.light.getType();
            Vector4f pos = data.light.position;
            TextureRenderer renderer = TextureRenderer.getInstance(
                _ctx, color, depth, new PixelFormat(8, 16, 8));
            if (lightType == Light.Type.POINT) {
                ncamera.setFrustum(-data.near, +data.near, -data.near, +data.near,
                    data.near, data.far);
                transform.getTranslation().set(pos.x, pos.y, pos.z);
                data.transform.set(transform.getTranslation(), Quaternion.IDENTITY);
                try {
                    for (int ii = 0; ii < 6; ii++) {
                        transform.getRotation().set(CUBE_FACE_ROTATIONS[ii]);
                        ncamera.updateTransform();
                        renderer.startRender(0, ii);
                        try {
                            compositor.performSubrender(this);
                        } finally {
                            renderer.commitRender();
                        }
                    }
                } finally {
                    compositor.cleanupSubrender(cstate);
                    compositor.setBackgroundColor(obackground);
                }
                return;
            }
            if (lightType == Light.Type.DIRECTIONAL) {
                // use the new camera's frustum as a temporary to find the extents under rotation
                Frustum volume = ncamera.getWorldVolume();
                if (ocamera.isOrtho()) {
                    volume.setToOrtho(ocamera.getLeft(), ocamera.getRight(),
                        ocamera.getBottom(), ocamera.getTop(), data.near, data.far);
                } else {
                    float ns = data.near / ocamera.getNear();
                    volume.setToFrustum(ns*ocamera.getLeft(), ns*ocamera.getRight(),
                        ns*ocamera.getBottom(), ns*ocamera.getTop(), data.near, data.far);
                }
                volume.transformLocal(ocamera.getWorldTransform());
                Quaternion trot = transform.getRotation();
                trot.fromVectorFromNegativeZ(-pos.x, -pos.y, -pos.z);
                Matrix3f mat = new Matrix3f().setToRotation(trot).transposeLocal();
                Box box = volume.getBoundsUnderRotation(mat, new Box());
                Vector3f min = box.getMinimumExtent();
                Vector3f max = box.getMaximumExtent();
                data.width = max.x - min.x;
                data.height = max.y - min.y;
                data.depth = max.z - min.z;
                float hwidth = data.width * 0.5f;
                float hheight = data.height * 0.5f;
                ncamera.setOrtho(-hwidth, +hwidth, -hheight, +hheight, 0f, data.depth);
                trot.transformLocal(transform.getTranslation().set(
                    min.x + hwidth, min.y + hheight, max.z));

            } else { // lightType == Light.Type.SPOT
                ncamera.setPerspective(FloatMath.toRadians(data.light.spotCutoff)*2f,
                    1f, data.near, data.far);
                transform.getTranslation().set(pos.x, pos.y, pos.z);
                transform.getRotation().fromVectorFromNegativeZ(data.light.spotDirection);
            }
            data.transform.set(transform);
            ncamera.updateTransform();
            renderer.startRender();
            try {
                compositor.performSubrender(this);
            } finally {
                renderer.commitRender();
                compositor.cleanupSubrender(cstate);
                compositor.setBackgroundColor(obackground);
            }
        }

        @Override
        public void cleanup ()
        {
            if (colorConfig != null) {
                colorConfig.returnToPool(_ctx, color);
            }
            if (depthConfig != null) {
                depthConfig.returnToPool(_ctx, depth);
            }
        }
    }

    /**
     * A render effect.
     */
    public static class RenderEffect extends Dependency
    {
        /** The configuration of the effect. */
        public RenderEffectConfig config;

        /**
         * Creates a new render effect dependency.
         */
        public RenderEffect (GlContext ctx)
        {
            super(ctx);
        }

        @Override
        public void resolve ()
        {
            _ctx.getCompositor().addDependencyEffect(config);
        }

        @Override
        public int hashCode ()
        {
            return System.identityHashCode(config);
        }

        @Override
        public boolean equals (Object other)
        {
            return other instanceof RenderEffect && ((RenderEffect)other).config == config;
        }
    }

    /**
     * Creates a new dependency.
     */
    public Dependency (GlContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Merges another dependency (for which {@link #equals} returns true) into this one.
     */
    public void merge (Dependency dependency)
    {
        // nothing by default
    }

    /**
     * Resolves this dependency.
     */
    public void resolve ()
    {
        // ...
    }

    /**
     * Performs any necessary cleanup.
     */
    public void cleanup ()
    {
        // nothing by default
    }

    /** The render context. */
    protected GlContext _ctx;

    /** Rotations for each of the six cube faces. */
    protected static final Quaternion[] CUBE_FACE_ROTATIONS = {
        new Quaternion().fromAnglesXY(FloatMath.PI, +FloatMath.HALF_PI),
        new Quaternion().fromAnglesXY(FloatMath.PI, -FloatMath.HALF_PI),
        new Quaternion().fromAnglesXY(+FloatMath.HALF_PI, 0f),
        new Quaternion().fromAnglesXY(-FloatMath.HALF_PI, 0f),
        new Quaternion().fromAnglesXY(FloatMath.PI, 0f),
        new Quaternion().fromAnglesXZ(0f, FloatMath.PI) };
}
