//
// $Id$

package com.threerings.opengl.mod;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;

import com.threerings.expr.Bound;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.math.Matrix4f;
import com.threerings.math.Ray;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.config.ArticulatedConfig;
import com.threerings.opengl.util.GlContext;

/**
 * An articulated model implementation.
 */
public class Articulated extends Model.Implementation
{
    /**
     * A node in the model.
     */
    public static class Node
    {
        /**
         * Returns this node's bone matrix (and flags it as a bone, if not already flagged).
         */
        public Matrix4f getBoneMatrix ()
        {
            if (_boneTransform == null) {
                _boneTransform = new Transform3D();
                _boneTransform.update(Transform3D.AFFINE);
            }
            return _boneTransform.getMatrix();
        }

        /** The bone transform, for nodes used as bones. */
        protected Transform3D _boneTransform;
    }

    /**
     * A node that contains a (visible and/or collision) mesh.
     */
    public static class MeshNode extends Node
    {

    }

    /**
     * Creates a new articulated implementation.
     */
    public Articulated (GlContext ctx, Scope parentScope, ArticulatedConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (ArticulatedConfig config)
    {
        _config = config;
        Map<String, MaterialConfig> materialConfigs = Maps.newHashMap();
        _surfaces = createSurfaces(
            _ctx, this, config.skin.visible, config.materialMappings, materialConfigs);
    }

    /**
     * Returns a reference to the bone matrix for the named node.
     */
    @Scoped
    public Matrix4f getBoneMatrix (String name)
    {
        Node node = _nodes.get(name);
        return (node == null) ? (Matrix4f)_parentGetBoneMatrix.call(name) : node.getBoneMatrix();
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
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

    /** The model configuration. */
    protected ArticulatedConfig _config;

    /** The skinned surfaces. */
    protected Surface[] _surfaces;

    /** The set of nodes, mapped by name. */
    protected HashMap<String, Node> _nodes = new HashMap<String, Node>();

    /** The parent implementation of {@link #getBoneMatrix}. */
    @Bound("getBoneMatrix")
    protected Function _parentGetBoneMatrix = Function.NULL;
}
