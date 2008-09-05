//
// $Id$

package com.threerings.opengl.mod;

import java.util.HashMap;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.Scoped;
import com.threerings.math.Box;
import com.threerings.math.Ray;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.CollisionMesh;
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
        _materialMappings = materialMappings;
        updateFromConfig();
    }

    @Override // documentation inherited
    public Box getBounds ()
    {
        return _bounds;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        // update the world transform
        if (_parentWorldTransform == null) {
            return;
        }
        _parentWorldTransform.compose(_localTransform, _worldTransform);

        // and the world bounds
        _meshes.bounds.transform(_worldTransform, _nbounds);
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange();
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange();
        }
    }

    @Override // documentation inherited
    public void drawBounds ()
    {
        DebugBounds.draw(_bounds, Color4f.WHITE);
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray ray, Vector3f result)
    {
        // we must transform the ray into model space before checking against the collision mesh
        CollisionMesh collision = _meshes.collision;
        if (collision == null || !_bounds.intersects(ray) ||
                !collision.getIntersection(ray.transform(_worldTransform.invert()), result)) {
            return false;
        }
        // then transform it back if we get a hit
        _worldTransform.transformPointLocal(result);
        return true;
    }

    @Override // documentation inherited
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
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        updateBounds();
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        if (_surfaces != null) {
            for (Surface surface : _surfaces) {
                surface.dispose();
            }
        }
        _surfaces = createSurfaces(
            _ctx, this, _meshes.visible, _materialMappings, new HashMap<String, MaterialConfig>());
        updateBounds();
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model meshes. */
    protected MeshSet _meshes;

    /** The material mappings. */
    protected MaterialMapping[] _materialMappings;

    /** The surfaces corresponding to each visible mesh. */
    protected Surface[] _surfaces;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** The shared transform state. */
    @Scoped
    protected TransformState _transformState = new TransformState();

    /** The bounds of the model. */
    protected Box _bounds = new Box();

    /** Holds the new bounds of the model when updating. */
    protected Box _nbounds = new Box();
}
