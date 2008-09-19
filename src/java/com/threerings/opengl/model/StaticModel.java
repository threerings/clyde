//
// $Id$

package com.threerings.opengl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.material.Material;
import com.threerings.opengl.material.Surface;
import com.threerings.opengl.renderer.state.TransformState;

/**
 * A model whose meshes do not move relative to each other.
 */
public class StaticModel extends Model
{
    /**
     * Creates a new static model.
     */
    public StaticModel (
        Properties props, VisibleMesh[] vmeshes, CollisionMesh cmesh)
    {
        super(props);
        _vmeshes = vmeshes;
        _cmesh = cmesh;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public StaticModel ()
    {
    }

    /**
     * Returns a reference to the model's array of visible meshes.
     */
    public VisibleMesh[] getVisibleMeshes ()
    {
        return _vmeshes;
    }

    /**
     * Returns a reference to the model's array of surfaces.
     */
    public Surface[] getSurfaces ()
    {
        return _surfaces;
    }

    // documentation inherited from interface SurfaceHost
    public Transform3D getModelview ()
    {
        return _tstate.getModelview();
    }

    // documentation inherited from interface SurfaceHost
    public TransformState getTransformState ()
    {
        return _tstate;
    }

    // documentation inherited from interface Intersectable
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        // we must transform the ray into model space before checking against the collision mesh
        if (_cmesh == null || !_worldBounds.intersects(ray) ||
            !_cmesh.getIntersection(ray.transform(_transform.invert()), result)) {
            return false;
        }
        // then transform it back if we get a hit
        _transform.transformPointLocal(result);
        return true;
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // update the shared transform state
        Transform3D modelview = _tstate.getModelview();
        _ctx.getCompositor().getCamera().getViewTransform().compose(_transform, modelview);
        _tstate.setDirty(true);

        // enqueue the surfaces
        for (Surface surface : _surfaces) {
            surface.enqueue();
        }
    }

    @Override // documentation inherited
    public void createSurfaces (String variant)
    {
        _surfaces = new Surface[_vmeshes.length];
        HashMap<String, Material> materials = new HashMap<String, Material>();
        for (int ii = 0; ii < _vmeshes.length; ii++) {
            VisibleMesh mesh = _vmeshes[ii];
            String texture = mesh.getTexture();
            Material material = materials.get(texture);
            if (material == null) {
                material = getMaterial(variant, texture);
            }
            Surface surface = material.createSurface(mesh);
            surface.setHost(this);
            _surfaces[ii] = surface;
        }
    }

    @Override // documentation inherited
    public void updateSurfaces ()
    {
        super.updateSurfaces();
        for (Surface surface : _surfaces) {
            surface.update();
        }
    }

    @Override // documentation inherited
    public boolean requiresTick ()
    {
        return false;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        StaticModel omodel = (StaticModel)super.clone();
        omodel._tstate = new TransformState(_tstate.getModelview());
        omodel._surfaces = new Surface[_surfaces.length];
        for (int ii = 0; ii < _surfaces.length; ii++) {
            omodel._surfaces[ii] = (Surface)_surfaces[ii].clone();
            omodel._surfaces[ii].setHost(omodel);
        }
        return omodel;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        // create the shared transform state
        _tstate = new TransformState();

        // expand the bounds to include those of all visible meshes
        _localBounds = new Box(Vector3f.MAX_VALUE, Vector3f.MIN_VALUE);
        for (VisibleMesh mesh : _vmeshes) {
            _localBounds.addLocal(mesh.getBounds());
        }

        // create the surfaces for the default variant
        createSurfaces(null);
    }

    @Override // documentation inherited
    protected void enqueue (Transform3D modelview)
    {
        // update the world transform
        _ctx.getCompositor().getCamera().getWorldTransform().compose(modelview, _transform);

        // update the shared transform state
        _tstate.getModelview().set(modelview);
        _tstate.setDirty(true);

        // enqueue the surfaces
        for (Surface surface : _surfaces) {
            surface.enqueue();
        }
    }

    /** The model's visible meshes. */
    protected VisibleMesh[] _vmeshes;

    /** The collision mesh, if any. */
    protected CollisionMesh _cmesh;

    /** The shared transform state. */
    protected transient TransformState _tstate;

    /** The surfaces of the model. */
    protected transient Surface[] _surfaces;
}
