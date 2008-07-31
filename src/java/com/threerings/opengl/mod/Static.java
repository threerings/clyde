//
// $Id$

package com.threerings.opengl.mod;

import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.math.Ray;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;

/**
 * A static model implementation.
 */
public class Static extends Model.Implementation
{
    /**
     * Creates a new static implementation.
     */
    public Static (GlContext ctx, Scope parentScope, ModelConfig.MeshSet meshes)
    {
        super(parentScope);
        _ctx = ctx;
        setMeshes(meshes);
    }

    /**
     * Sets the set of meshes in this model.
     */
    public void setMeshes (ModelConfig.MeshSet meshes)
    {

    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // update the shared transform state
        Transform3D modelview = _transformState.getModelview();
//        _ctx.getCompositor().getCamera().getViewTransform().compose(_transform, modelview);
        _transformState.setDirty(true);

        // enqueue the surfaces
        for (Surface surface : _surfaces) {
            surface.enqueue();
        }
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray ray, Vector3f result)
    {
        return false;
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model meshes. */
    protected ModelConfig.MeshSet _meshes;

    /** The surfaces corresponding to each visible mesh. */
    protected Surface[] _surfaces;

    /** The shared transform state. */
    @Scoped
    protected TransformState _transformState = new TransformState();
}
