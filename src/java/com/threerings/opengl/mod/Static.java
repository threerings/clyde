//
// $Id$

package com.threerings.opengl.mod;

import java.util.HashMap;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.math.Box;
import com.threerings.math.Ray;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.config.ModelConfig.MeshSet;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.model.config.ModelConfig.Imported.MaterialMapping;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;

/**
 * A static model implementation.
 */
public class Static extends Model.Implementation
{
    /**
     * Creates a new static implementation.
     */
    public Static (
        GlContext ctx, Scope parentScope, MeshSet meshes, MaterialMapping[] materialMappings)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(meshes, materialMappings);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (MeshSet meshes, MaterialMapping[] materialMappings)
    {
        _meshes = meshes;
        _surfaces = createSurfaces(
            _ctx, this, meshes.visible, materialMappings, new HashMap<String, MaterialConfig>());
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // update the shared transform state
        Transform3D modelview = _transformState.getModelview();
        _parentViewTransform.compose(_localTransform, modelview);
        _transformState.setDirty(true);

        // enqueue the surfaces
        for (Surface surface : _surfaces) {
            surface.enqueue();
        }
    }

    @Override // documentation inherited
    public void updateWorldBounds ()
    {
        // update the world transform
        _parentWorldTransform.compose(_localTransform, _worldTransform);

        // and the world bounds
        _meshes.bounds.transform(_worldTransform, _worldBounds);
    }

    @Override // documentation inherited
    public void drawBounds ()
    {
        DebugBounds.draw(_worldBounds, Color4f.WHITE);
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray ray, Vector3f result)
    {
        return false;
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model meshes. */
    protected MeshSet _meshes;

    /** The surfaces corresponding to each visible mesh. */
    protected Surface[] _surfaces;

    /** The world space bounds of the model. */
    protected Box _worldBounds = new Box();

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** The shared transform state. */
    @Scoped
    protected TransformState _transformState = new TransformState();
}
