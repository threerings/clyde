//
// $Id$

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
import com.threerings.util.DeepObject;

import com.threerings.opengl.compositor.RenderScheme;
import com.threerings.opengl.material.Projection;
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

        @Override // documentation inherited
        protected String getScheme ()
        {
            return RenderScheme.PERSPECTIVE_PROJECTION;
        }

        @Override // documentation inherited
        protected Updater createUpdater (GlContext ctx, Scope scope, final Projection projection)
        {
            final Transform3D viewTransform = ScopeUtil.resolve(
                scope, "viewTransform", new Transform3D());
            return new Updater() {
                public void update () {
                    viewTransform.invert(_viewTransformInv).update(Transform3D.AFFINE);
                    Matrix4f mat = _viewTransformInv.getMatrix();
                    projection.getGenPlaneS().set(mat.m00, mat.m10, mat.m20, mat.m30);
                    projection.getGenPlaneT().set(mat.m01, mat.m11, mat.m21, mat.m31);
                    projection.getGenPlaneQ().set(mat.m02, mat.m12, mat.m22, mat.m32);
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
        @Override // documentation inherited
        protected String getScheme ()
        {
            return RenderScheme.ORTHOGRAPHIC_PROJECTION;
        }

        @Override // documentation inherited
        protected Updater createUpdater (GlContext ctx, Scope scope, final Projection projection)
        {
            final Transform3D viewTransform = ScopeUtil.resolve(
                scope, "viewTransform", new Transform3D());
            return new Updater() {
                public void update () {
                    viewTransform.invert(_viewTransformInv).update(Transform3D.AFFINE);
                    Matrix4f mat = _viewTransformInv.getMatrix();
                    projection.getGenPlaneS().set(mat.m00, mat.m10, mat.m20, mat.m30);
                    projection.getGenPlaneT().set(mat.m01, mat.m11, mat.m21, mat.m31);
                }
                protected Transform3D _viewTransformInv = new Transform3D();
            };
        }
    }

    /** The projection material. */
    @Editable(nullable=true)
    public ConfigReference<MaterialConfig> material;

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
        Projection projection = new Projection(technique);
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
