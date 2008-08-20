//
// $Id$

package com.threerings.opengl.mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import com.threerings.expr.Bound;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.Updater;
import com.threerings.math.Matrix4f;
import com.threerings.math.Ray;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.config.ArticulatedConfig;
import com.threerings.opengl.model.config.ArticulatedConfig.AnimationMapping;
import com.threerings.opengl.model.config.ArticulatedConfig.Attachment;
import com.threerings.opengl.model.config.ArticulatedConfig.NodeTransform;
import com.threerings.opengl.model.config.ModelConfig.Imported.MaterialMapping;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.*;

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
        /** The value of the update counter at the last update (used when applying animation
         * transforms to determine which transform is being applied first). */
        public int lastUpdate;

        /** The total weight of the animation transforms applied to this node. */
        public float totalWeight;

        /**
         * Creates a new node.
         */
        public Node (
            Scope parentScope, ArticulatedConfig.Node config, Transform3D parentViewTransform)
        {
            super(parentScope);
            _viewTransform = new Transform3D(Transform3D.UNIFORM);
            setConfig(config, parentViewTransform);
        }

        /**
         * Sets the configuration of this node.
         */
        public void setConfig (ArticulatedConfig.Node config, Transform3D parentViewTransform)
        {
            _config = config;
            _localTransform.set(config.transform);
            _parentViewTransform = parentViewTransform;
            _updater = null;
        }

        /**
         * Returns a reference to the configuration of this node.
         */
        public ArticulatedConfig.Node getConfig ()
        {
            return _config;
        }

        /**
         * Returns a reference to this node's local transform.
         */
        public Transform3D getLocalTransform ()
        {
            return _localTransform;
        }

        /**
         * Returns a reference to this node's view transform.
         */
        public Transform3D getViewTransform ()
        {
            return _viewTransform;
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
         * Sets the updater to call after updating the view transform.
         */
        public void setUpdater (Updater updater)
        {
            _updater = updater;
        }

        /**
         * Creates the surfaces of this node.
         */
        public void createSurfaces (
            GlContext ctx, MaterialMapping[] materialMappings,
            Map<String, MaterialConfig> materialConfigs)
        {
            // nothing by default
        }

        /**
         * Enqueues this node for rendering.
         */
        public void enqueue ()
        {
            // compose parent view transform with local transform
            _parentViewTransform.compose(_localTransform, _viewTransform);

            /// apply our updater post-transform
            if (_updater != null) {
                _updater.update();
            }

            // update bone transform if necessary
            if (_boneTransform != null) {
                _viewTransform.compose(_config.invRefTransform, _boneTransform);
                _boneTransform.update(Transform3D.AFFINE);
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
        protected Node (Scope parentScope)
        {
            super(parentScope);
        }

        /** The node configuration. */
        protected ArticulatedConfig.Node _config;

        /** A reference to the parent view transform. */
        protected Transform3D _parentViewTransform;

        /** The node's local transform. */
        protected Transform3D _localTransform = new Transform3D();

        /** The node's view transform. */
        @Scoped
        protected Transform3D _viewTransform;

        /** The bone transform, for nodes used as bones. */
        protected Transform3D _boneTransform;

        /** An updater to apply after updating the view transform. */
        protected Updater _updater;
    }

    /**
     * A node that contains a (visible and/or collision) mesh.
     */
    public static class MeshNode extends Node
    {
        /**
         * Creates a new mesh node.
         */
        public MeshNode (
            Scope parentScope, ArticulatedConfig.MeshNode config, Transform3D parentViewTransform)
        {
            super(parentScope);
            _viewTransform = _transformState.getModelview();
            setConfig(config, parentViewTransform);
        }

        @Override // documentation inherited
        public void createSurfaces (
            GlContext ctx, MaterialMapping[] materialMappings,
            Map<String, MaterialConfig> materialConfigs)
        {
            VisibleMesh mesh = ((ArticulatedConfig.MeshNode)_config).visible;
            if (mesh != null) {
                _surface = createSurface(ctx, this, mesh, materialMappings, materialConfigs);
            }
        }

        @Override // documentation inherited
        public void enqueue ()
        {
            super.enqueue();
            if (_surface != null) {
                _transformState.setDirty(true);
                _surface.enqueue();
            }
        }

        /** The surface transform state. */
        @Scoped
        protected TransformState _transformState = new TransformState(Transform3D.UNIFORM);

        /** The surface. */
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
        updateFromConfig();
    }

    /**
     * Returns a reference to the bone matrix for the named node.
     */
    @Scoped
    public Matrix4f getBoneMatrix (String name)
    {
        Node node = _nodesByName.get(name);
        return (node == null) ? (Matrix4f)_parentGetBoneMatrix.call(name) : node.getBoneMatrix();
    }

    /**
     * Returns a reference to the node with the specified name.
     */
    @Scoped
    public Node getNode (String name)
    {
        return _nodesByName.get(name);
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // update the local transforms based on the animation
        updateTransforms();

        // update the view transform
        _parentViewTransform.compose(_localTransform, _viewTransform);

        // update/enqueue the nodes
        for (Node node : _nodes) {
            node.enqueue();
        }

        // enqueue the surfaces
        for (Surface surface : _surfaces) {
            surface.enqueue();
        }

        // enqueue the configured attachments
        for (Model model : _configAttachments) {
            model.enqueue();
        }

        // and the user attachments
        for (int ii = 0, nn = _userAttachments.size(); ii < nn; ii++) {
            _userAttachments.get(ii).enqueue();
        }
    }

    @Override // documentation inherited
    public void attach (String point, Model model, boolean replace)
    {
        Node node = getAttachmentNode(point);
        if (node == null) {
            return;
        }
        if (replace) {
            detachAll(node);
        }
        model.setParentScope(node);
        _userAttachments.add(model);
    }

    @Override // documentation inherited
    public void detach (Model model)
    {
        for (int ii = 0, nn = _userAttachments.size(); ii < nn; ii++) {
            if (_userAttachments.get(ii) == model) {
                _userAttachments.remove(ii);
                return;
            }
        }
        log.warning("Missing attachment to remove.", "model", model);
    }

    @Override // documentation inherited
    public void detachAll (String point)
    {
        detachAll(getAttachmentNode(point));
    }

    @Override // documentation inherited
    public List<Animation> getPlayingAnimations ()
    {
        return _playing;
    }

    @Override // documentation inherited
    public Animation getAnimation (String name)
    {
        return _animationsByName.get(name);
    }

    @Override // documentation inherited
    public Animation[] getAnimations ()
    {
        return _animations;
    }

    @Override // documentation inherited
    public boolean requiresTick ()
    {
        return true;
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        // copy the tracks to an array so that callbacks can manipulate the list;
        // note if any tracks have completed
        boolean completed = false;
        _playingArray = _playing.toArray(_playingArray);
        for (int ii = 0, nn = _playing.size(); ii < nn; ii++) {
            completed |= _playingArray[ii].tick(elapsed);
        }

        // if any tracks have completed, issue a final update and remove them
        if (completed) {
            updateTransforms();
            for (int ii = _playing.size() - 1; ii >= 0; ii--) {
                Animation animation = _playing.get(ii);
                if (animation.hasCompleted()) {
                    _playing.remove(ii);
                }
            }
        }

        // tick the configured attachments
        for (Model model : _configAttachments) {
            model.tick(elapsed);
        }

        // and the user attachments
        for (int ii = 0, nn = _userAttachments.size(); ii < nn; ii++) {
            _userAttachments.get(ii).tick(elapsed);
        }
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray ray, Vector3f result)
    {
        return false;
    }

    @Override // documentation inherited
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        updateFromConfig();
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        // save the names of the nodes to which the user nodes are attached
        String[] userAttachmentNodes = new String[_userAttachments.size()];
        for (int ii = 0; ii < userAttachmentNodes.length; ii++) {
            userAttachmentNodes[ii] =
                ((Node)_userAttachments.get(ii).getParentScope()).getConfig().name;
        }

        // create the node list
        ArrayList<Node> nnodes = new ArrayList<Node>();
        _config.root.getArticulatedNodes(this, _nodes, nnodes, _viewTransform);
        _nodes = nnodes.toArray(new Node[nnodes.size()]);

        // populate the name map
        _nodesByName.clear();
        for (Node node : _nodes) {
            _nodesByName.put(node.getConfig().name, node);
        }

        // set the node transform updaters
        for (NodeTransform transform : _config.nodeTransforms) {
            Node node = _nodesByName.get(transform.node);
            if (node != null) {
                node.setUpdater(transform.createUpdater(node));
            }
        }

        // create the node surfaces
        Map<String, MaterialConfig> materialConfigs = Maps.newHashMap();
        for (Node node : _nodes) {
            node.createSurfaces(_ctx, _config.materialMappings, materialConfigs);
        }

        // create the skinned surfaces
        _surfaces = createSurfaces(
            _ctx, this, _config.skin.visible, _config.materialMappings, materialConfigs);

        // create the animations
        Animation[] oanims = _animations;
        _animations = new Animation[_config.animationMappings.length];
        for (int ii = 0; ii < _animations.length; ii++) {
            Animation anim = (oanims == null || oanims.length <= ii) ?
                new Animation(_ctx, this) : oanims[ii];
            _animations[ii] = anim;
            AnimationMapping mapping = _config.animationMappings[ii];
            anim.setConfig(mapping.name, mapping.animation);
        }

        // populate the animation map
        _animationsByName.clear();
        for (Animation animation : _animations) {
            _animationsByName.put(animation.getName(), animation);
        }

        // create the configured attachments
        Model[] omodels = _configAttachments;
        _configAttachments = new Model[_config.attachments.length];
        for (int ii = 0; ii < _configAttachments.length; ii++) {
            Model model = (omodels == null || omodels.length <= ii) ?
                new Model(_ctx) : omodels[ii];
            _configAttachments[ii] = model;
            Attachment attachment = _config.attachments[ii];
            model.setParentScope(getAttachmentNode(attachment.node));
            model.setConfig(attachment.model);
        }

        // update the user attachments
        ArrayList<Model> oattachments = _userAttachments;
        _userAttachments = new ArrayList<Model>();
        for (int ii = 0; ii < userAttachmentNodes.length; ii++) {
            Model model = oattachments.get(ii);
            Node node = getAttachmentNode(userAttachmentNodes[ii]);
            if (node != null) {
                model.setParentScope(node);
                _userAttachments.add(model);
            }
        }
    }

    /**
     * Notes that an animation has started.
     *
     * @param overrideBlendOut if non-negative, an interval over which to blend out all
     * animations currently playing at the same priority level as this one.
     */
    protected void animationStarted (Animation animation, float overrideBlendOut)
    {
        // make sure it's not in the list already
        _playing.remove(animation);

        // if necessary, stop any animations already playing at the same priority
        // level
        int priority = animation.getPriority();
        if (overrideBlendOut >= 0f) {
            ((Model)_parentScope).stopAnimations(priority, overrideBlendOut);
        }

        // insert into playing list in priority order
        for (int ii = 0, nn = _playing.size(); ii < nn; ii++) {
            if (priority >= _playing.get(ii).getPriority()) {
                _playing.add(ii, animation);
                return;
            }
        }
        _playing.add(animation);

        // notify containing model
        ((Model)_parentScope).animationStarted(animation);
    }

    /**
     * Notes that an animation has stopped.
     */
    protected void animationStopped (Animation animation, boolean completed)
    {
        // remove from playing list *unless* it's completed, in which case we'll remove it
        // after one final update
        if (!completed) {
            _playing.remove(animation);
        }

        // notify containing model
        ((Model)_parentScope).animationStopped(animation, completed);
    }

    /**
     * Returns a reference to the node with the specified name, logging a warning and returning
     * <code>null</code> if no such node exists.
     */
    protected Node getAttachmentNode (String name)
    {
        Node node = _nodesByName.get(name);
        if (node == null) {
            log.warning("Missing node for attachment.", "node", name);
        }
        return node;
    }

    /**
     * Detaches all models attached to the specified node.
     */
    protected void detachAll (Node node)
    {
        for (int ii = _userAttachments.size() - 1; ii >= 0; ii--) {
            if (_userAttachments.get(ii).getParentScope() == node) {
                _userAttachments.remove(ii);
            }
        }
    }

    /**
     * Updates the node transforms based on the current animation state.
     */
    protected void updateTransforms ()
    {
        // handle the special (but likely common) case of a single animation
        int nn = _playing.size();
        if (nn == 1) {
            _playing.get(0).updateTransforms();
            return;
        }

        // increment the update counter so that the tracks know which nodes have been updated
        _update++;

        // process the tracks in order of decreasing priority
        for (int ii = 0; ii < nn; ii++) {
            _playing.get(ii).blendTransforms(_update);
        }
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model configuration. */
    protected ArticulatedConfig _config;

    /** The model nodes, in the order of a preorder depth-first traversal. */
    protected Node[] _nodes;

    /** Maps node names to nodes. */
    protected HashMap<String, Node> _nodesByName = new HashMap<String, Node>();

    /** The skinned surfaces. */
    protected Surface[] _surfaces;

    /** The animations. */
    protected Animation[] _animations;

    /** Maps animation names to animations. */
    protected HashMap<String, Animation> _animationsByName = new HashMap<String, Animation>();

    /** The attachments created from the configuration. */
    protected Model[] _configAttachments;

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

    /** User attachments (their parent scopes are the nodes to which they're attached). */
    protected ArrayList<Model> _userAttachments = new ArrayList<Model>();

    /** The animations currently being played, sorted by decreasing priority. */
    protected ArrayList<Animation> _playing = new ArrayList<Animation>();

    /** Holds the playing animations during the tick. */
    protected Animation[] _playingArray = new Animation[0];

    /** Incremented on each call to {@link #updateTransforms} and used to determine which nodes
     * have been manipulated by animations on the current update. */
    protected int _update;
}
