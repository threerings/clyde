//
// $Id$

package com.threerings.opengl.mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
import com.threerings.math.Box;
import com.threerings.math.Matrix4f;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.config.ArticulatedConfig;
import com.threerings.opengl.model.config.ArticulatedConfig.AnimationMapping;
import com.threerings.opengl.model.config.ArticulatedConfig.Attachment;
import com.threerings.opengl.model.config.ArticulatedConfig.NodeTransform;
import com.threerings.opengl.model.config.ModelConfig.Imported.MaterialMapping;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.DebugBounds;
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
            Scope parentScope, ArticulatedConfig.Node config,
            Transform3D parentWorldTransform, Transform3D parentViewTransform)
        {
            super(parentScope);
            _viewTransform = new Transform3D(Transform3D.UNIFORM);
            setConfig(config, parentWorldTransform, parentViewTransform);
        }

        /**
         * Sets the configuration of this node.
         */
        public void setConfig (
            ArticulatedConfig.Node config, Transform3D parentWorldTransform,
            Transform3D parentViewTransform)
        {
            _config = config;
            _parentWorldTransform = parentWorldTransform;
            _parentViewTransform = parentViewTransform;
            if (_localTransform == null) {
                // only set the local transform on initialization
                _localTransform = new Transform3D(config.transform);
            }
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
         * Returns a reference to this node's world transform.
         */
        public Transform3D getWorldTransform ()
        {
            return _worldTransform;
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
         * Updates the node for the current frame.
         */
        public void update ()
        {
            // compose parent world transform with local transform
            _parentWorldTransform.compose(_localTransform, _worldTransform);
        }

        /**
         * Checks for an intersection between the mesh in this node (if any) and the supplied ray.
         */
        public boolean getIntersection (Ray3D ray, Vector3f result)
        {
            return false;
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

        /** A reference to the parent world transform. */
        protected Transform3D _parentWorldTransform;

        /** A reference to the parent view transform. */
        protected Transform3D _parentViewTransform;

        /** The node's local transform. */
        protected Transform3D _localTransform;

        /** The node's world transform. */
        @Scoped
        protected Transform3D _worldTransform = new Transform3D();

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
            Scope parentScope, ArticulatedConfig.MeshNode config,
            Transform3D parentWorldTransform, Transform3D parentViewTransform)
        {
            super(parentScope);
            _viewTransform = _transformState.getModelview();
            setConfig(config, parentWorldTransform, parentViewTransform);
        }

        @Override // documentation inherited
        public void createSurfaces (
            GlContext ctx, MaterialMapping[] materialMappings,
            Map<String, MaterialConfig> materialConfigs)
        {
            if (_surface != null) {
                _surface.dispose();
            }
            VisibleMesh mesh = ((ArticulatedConfig.MeshNode)_config).visible;
            _surface = (mesh == null) ? null :
                createSurface(ctx, this, mesh, materialMappings, materialConfigs);
        }

        @Override // documentation inherited
        public void update ()
        {
            super.update();

            // transform the bounds and add them to the parent bounds
            _parentBounds.addLocal(
                _config.getBounds().transform(_worldTransform, _bounds));
        }

        @Override // documentation inherited
        public boolean getIntersection (Ray3D ray, Vector3f result)
        {
            // transform the ray into model space before checking against the collision mesh
            CollisionMesh collision = ((ArticulatedConfig.MeshNode)_config).collision;
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
            super.enqueue();
            if (_surface != null) {
                _transformState.setDirty(true);
                _surface.enqueue();
            }
        }

        @Override // documentation inherited
        public void dispose ()
        {
            super.dispose();
            if (_surface != null) {
                _surface.dispose();
            }
        }

        /** The bounds of the parent. */
        @Bound("nbounds")
        protected Box _parentBounds;

        /** The bounds of the mesh. */
        protected Box _bounds = new Box();

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
        _viewTransform = _transformState.getModelview();
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
        Scene scene = ((Model)_parentScope).getScene();
        if (scene != null) {
            model.wasAdded(scene);
        }
    }

    @Override // documentation inherited
    public void detach (Model model)
    {
        for (int ii = 0, nn = _userAttachments.size(); ii < nn; ii++) {
            if (_userAttachments.get(ii) == model) {
                if (model.getScene() != null) {
                    model.willBeRemoved();
                }
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
    public void reset ()
    {
        for (Model model : _configAttachments) {
            model.reset();
        }
        for (int ii = 0, nn = _userAttachments.size(); ii < nn; ii++) {
            _userAttachments.get(ii).reset();
        }
        for (int ii = 0; ii < _animations.length; ii++) {
            if (_config.animationMappings[ii].startAutomatically) {
                _animations[ii].start();
            }
        }
    }

    @Override // documentation inherited
    public Box getBounds ()
    {
        return _bounds;
    }

    @Override // documentation inherited
    public void drawBounds ()
    {
        DebugBounds.draw(_bounds, Color4f.WHITE);
        for (Model model : _configAttachments) {
            model.drawBounds();
        }
        for (int ii = 0, nn = _userAttachments.size(); ii < nn; ii++) {
            _userAttachments.get(ii).drawBounds();
        }
    }

    @Override // documentation inherited
    public void setTickPolicy (TickPolicy policy)
    {
        if (_tickPolicy != policy) {
            ((Model)_parentScope).tickPolicyWillChange();
            _tickPolicy = policy;
            ((Model)_parentScope).tickPolicyDidChange();
        }
    }

    @Override // documentation inherited
    public TickPolicy getTickPolicy ()
    {
        return _tickPolicy;
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        // notify configured attachments
        Scene scene = ((Model)_parentScope).getScene();
        for (Model model : _configAttachments) {
            model.wasAdded(scene);
        }

        // and user attachments
        for (int ii = 0, nn = _userAttachments.size(); ii < nn; ii++) {
            _userAttachments.get(ii).wasAdded(scene);
        }
    }

    @Override // documentation inherited
    public void willBeRemoved ()
    {
        // notify configured attachments
        for (Model model : _configAttachments) {
            model.willBeRemoved();
        }

        // and user attachments
        for (int ii = 0, nn = _userAttachments.size(); ii < nn; ii++) {
            _userAttachments.get(ii).willBeRemoved();
        }
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        // update the world transform
        _parentWorldTransform.compose(_localTransform, _worldTransform);

        // initialize the bounds
        _config.skin.bounds.transform(_worldTransform, _nbounds);

        // copy the tracks to an array so that callbacks can manipulate the list;
        // note if any tracks have completed
        boolean completed = false;
        _playingArray = _playing.toArray(_playingArray);
        for (int ii = 0, nn = _playing.size(); ii < nn; ii++) {
            completed |= _playingArray[ii].tick(elapsed);
        }

        // update the local node transforms
        updateTransforms();

        // if any tracks have completed, remove them
        if (completed) {
            for (int ii = _playing.size() - 1; ii >= 0; ii--) {
                Animation animation = _playing.get(ii);
                if (animation.hasCompleted()) {
                    _playing.remove(ii);
                }
            }
        }

        // update the nodes and expand the bounds
        for (Node node : _nodes) {
            node.update();
        }

        // tick the configured attachments
        for (Model model : _configAttachments) {
            model.tick(elapsed);
            model.updateBounds();
            _nbounds.addLocal(model.getBounds());
        }

        // and the user attachments
        for (int ii = 0, nn = _userAttachments.size(); ii < nn; ii++) {
            Model model = _userAttachments.get(ii);
            model.tick(elapsed);
            model.updateBounds();
            _nbounds.addLocal(model.getBounds());
        }

        // update the bounds if necessary
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange();
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange();
        }
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        // exit early if there's no bounds intersection
        if (!_bounds.intersects(ray)) {
            return false;
        }
        // first check against the skin
        Vector3f closest = result;
        if (getSkinIntersection(ray, result)) {
            result = updateClosest(ray.getOrigin(), result, closest);
        }
        // then the nodes
        for (Node node : _nodes) {
            if (node.getIntersection(ray, result)) {
                result = updateClosest(ray.getOrigin(), result, closest);
            }
        }
        // then the configured attachments
        for (Model model : _configAttachments) {
            if (model.getIntersection(ray, result)) {
                result = updateClosest(ray.getOrigin(), result, closest);
            }
        }
        // then the user attachments
        for (int ii = 0, nn = _userAttachments.size(); ii < nn; ii++) {
            if (_userAttachments.get(ii).getIntersection(ray, result)) {
                result = updateClosest(ray.getOrigin(), result, closest);
            }
        }
        // if we ever changed the result reference, that means we hit something
        return (result != closest);
    }

    @Override // documentation inherited
    public void enqueue ()
    {
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

        // save the old nodes (if any)
        IdentityHashMap<ArticulatedConfig.Node, Node> onodes = Maps.newIdentityHashMap();
        if (_nodes != null) {
            for (Node node : _nodes) {
                onodes.put(node.getConfig(), node);
            }
        }

        // create the node list
        ArrayList<Node> nnodes = new ArrayList<Node>();
        _config.root.getArticulatedNodes(this, onodes, nnodes, _worldTransform, _viewTransform);
        _nodes = nnodes.toArray(new Node[nnodes.size()]);
        for (Node node : onodes.values()) {
            node.dispose(); // dispose of the unrecycled old nodes
        }

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
        if (_surfaces != null) {
            for (Surface surface : _surfaces) {
                surface.dispose();
            }
        }
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
            if (mapping.startAutomatically) {
                anim.start();
            }
        }
        if (oanims != null) {
            for (int ii = _animations.length; ii < oanims.length; ii++) {
                oanims[ii].dispose();
            }
        }

        // populate the animation map
        _animationsByName.clear();
        for (Animation animation : _animations) {
            _animationsByName.put(animation.getName(), animation);
        }

        // create the configured attachments
        Scene scene = ((Model)_parentScope).getScene();
        Model[] omodels = _configAttachments;
        _configAttachments = new Model[_config.attachments.length];
        for (int ii = 0; ii < _configAttachments.length; ii++) {
            Model model = (omodels == null || omodels.length <= ii) ?
                new Model(_ctx) : omodels[ii];
            _configAttachments[ii] = model;
            Attachment attachment = _config.attachments[ii];
            model.setParentScope(getAttachmentNode(attachment.node));
            model.setConfig(attachment.model);
            model.getLocalTransform().set(attachment.transform);
            if (model.getScene() == null && scene != null) {
                model.wasAdded(scene);
            }
        }
        if (omodels != null) {
            for (int ii = _configAttachments.length; ii < omodels.length; ii++) {
                Model model = omodels[ii];
                if (scene != null) {
                    model.willBeRemoved();
                }
                model.dispose();
            }
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

            } else if (scene != null) {
                model.willBeRemoved();
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
            Model model = _userAttachments.get(ii);
            if (model.getParentScope() == node) {
                if (model.getScene() != null) {
                    model.willBeRemoved();
                }
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

    /**
     * Checks for an intersection with the skin mesh.
     */
    protected boolean getSkinIntersection (Ray3D ray, Vector3f result)
    {
        // we must transform the ray into model space before checking against the collision mesh
        CollisionMesh collision = _config.skin.collision;
        if (collision == null || !collision.getIntersection(
                ray.transform(_worldTransform.invert()), result)) {
            return false;
        }
        // then transform it back if we get a hit
        _worldTransform.transformPointLocal(result);
        return true;
    }

    /**
     * Updates the value of the closest point and returns a new result vector reference.
     */
    protected static Vector3f updateClosest (Vector3f origin, Vector3f result, Vector3f closest)
    {
        if (result == closest) {
            return new Vector3f();
        }
        if (origin.distanceSquared(result) < origin.distanceSquared(closest)) {
            closest.set(result);
        }
        return result;
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

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The parent implementation of {@link #getBoneMatrix}. */
    @Bound("getBoneMatrix")
    protected Function _parentGetBoneMatrix = Function.NULL;

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** The view transform. */
    @Scoped
    protected Transform3D _viewTransform;

    /** The shared transform state. */
    @Scoped
    protected TransformState _transformState = new TransformState();

    /** The bounds of the model. */
    protected Box _bounds = new Box();

    /** Holds the bounds of the model when updating. */
    @Scoped
    protected Box _nbounds = new Box();

    /** The model's tick policy. */
    protected TickPolicy _tickPolicy = TickPolicy.ALWAYS;

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
