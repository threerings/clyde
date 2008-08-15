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
import com.threerings.expr.SimpleScope;
import com.threerings.math.Matrix4f;
import com.threerings.math.Ray;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.config.ArticulatedConfig;
import com.threerings.opengl.model.config.ModelConfig.Imported.MaterialMapping;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;

/**
 * An articulated model implementation.
 */
public class Articulated extends Model.Implementation
{
    /**
     * A node in the model.
     */
    public static class Node extends SimpleScope
    {
        /**
         * Creates a new node.
         */
        public Node (GlContext ctx, Scope parentScope, ArticulatedConfig.Node config)
        {
            super(parentScope);
            _ctx = ctx;
            _viewTransform = new Transform3D();
            setConfig(config);
        }

        /**
         * Sets the configuration of this node.
         */
        public void setConfig (ArticulatedConfig.Node config)
        {
            _config = config;
            _localTransform.set(config.transform);

            // reconfigure the children
            Node[] ochildren = _children;
            if (_children == null || _children.length != config.children.length) {
                _children = new Node[config.children.length];
            }
            for (int ii = 0; ii < _children.length; ii++) {
                Node ochild = (ochildren == null || ochildren.length <= ii) ? null : ochildren[ii];
                _children[ii] = config.children[ii].getArticulatedNode(_ctx, _parentScope, ochild);
            }
        }

        /**
         * Updates the node map.
         */
        public void updateNodeMap (HashMap<String, Node> nodes)
        {
            nodes.put(_config.name, this);
            for (Node child : _children) {
                child.updateNodeMap(nodes);
            }
        }

        /**
         * Creates the surfaces of this node.
         */
        public void createSurfaces (
            MaterialMapping[] materialMappings, Map<String, MaterialConfig> materialConfigs)
        {
            for (Node child : _children) {
                child.createSurfaces(materialMappings, materialConfigs);
            }
        }

        /**
         * Returns this node's bone matrix (and flags it as a bone, if not already flagged).
         */
        public Matrix4f getBoneMatrix ()
        {
            if (_boneTransform == null) {
                _boneTransform = _viewTransform.compose(_config.invRefTransform);
                _boneTransform.update(Transform3D.AFFINE);
            }
            return _boneTransform.getMatrix();
        }

        /**
         * Enqueues this node for rendering.
         *
         * @param parentViewTransform the view transform of the parent node.
         */
        public void enqueue (Transform3D parentViewTransform)
        {
            // compose parent view transform with local transform
            parentViewTransform.compose(_localTransform, _viewTransform);

            // update bone transform if necessary
            if (_boneTransform != null) {
                _viewTransform.compose(_config.invRefTransform, _boneTransform);
                _boneTransform.update(Transform3D.AFFINE);
            }

            // enqueue children
            for (Node child : _children) {
                child.enqueue(_viewTransform);
            }
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "node";
        }

        /**
         * Constructor for subclasses.
         */
        protected Node (GlContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
        }

        /** The application context. */
        protected GlContext _ctx;

        /** The node configuration. */
        protected ArticulatedConfig.Node _config;

        /** The node's local transform. */
        protected Transform3D _localTransform = new Transform3D();

        /** The node's view transform. */
        @Scoped
        protected Transform3D _viewTransform;

        /** The bone transform, for nodes used as bones. */
        protected Transform3D _boneTransform;

        /** The children of this node. */
        protected Node[] _children;
    }

    /**
     * A node that contains a (visible and/or collision) mesh.
     */
    public static class MeshNode extends Node
    {
        /**
         * Creates a new mesh node.
         */
        public MeshNode (GlContext ctx, Scope parentScope, ArticulatedConfig.MeshNode config)
        {
            super(ctx, parentScope);
            _viewTransform = _transformState.getModelview();
            setConfig(config);
        }

        @Override // documentation inherited
        public void createSurfaces (
            MaterialMapping[] materialMappings, Map<String, MaterialConfig> materialConfigs)
        {
            super.createSurfaces(materialMappings, materialConfigs);
            VisibleMesh mesh = ((ArticulatedConfig.MeshNode)_config).visible;
            if (mesh != null) {
                _surface = createSurface(_ctx, this, mesh, materialMappings, materialConfigs);
            }
        }

        @Override // documentation inherited
        public void enqueue (Transform3D parentViewTransform)
        {
            super.enqueue(parentViewTransform);
            if (_surface != null) {
                _transformState.setDirty(true);
                _surface.enqueue();
            }
        }

        /** The mesh's transform state. */
        @Scoped
        protected TransformState _transformState = new TransformState();

        /** The mesh surface. */
        protected Surface _surface;
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

        // configure the node hierarchy
        _root = config.root.getArticulatedNode(_ctx, this, _root);

        // update the node map
        _nodes.clear();
        _root.updateNodeMap(_nodes);

        // create the node surfaces
        Map<String, MaterialConfig> materialConfigs = Maps.newHashMap();
        _root.createSurfaces(config.materialMappings, materialConfigs);

        // create the skinned surfaces
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
        // update the view transform and the transform hierarchy
        _parentViewTransform.compose(_localTransform, _viewTransform);
        _root.enqueue(_viewTransform);

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

    /** The root node. */
    protected Node _root;

    /** The skinned surfaces. */
    protected Surface[] _surfaces;

    /** The set of nodes, mapped by name. */
    protected HashMap<String, Node> _nodes = new HashMap<String, Node>();

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The parent implementation of {@link #getBoneMatrix}. */
    @Bound("getBoneMatrix")
    protected Function _parentGetBoneMatrix = Function.NULL;

    /** The view transform. */
    @Scoped
    protected Transform3D _viewTransform = new Transform3D();
}
