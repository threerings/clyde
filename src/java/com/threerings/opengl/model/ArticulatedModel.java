//
// $Id$

package com.threerings.opengl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ObserverList;

import com.threerings.math.Box;
import com.threerings.math.Matrix4f;
import com.threerings.math.Ray;
import com.threerings.math.Transform;
import com.threerings.math.Vector3f;

import com.threerings.export.Exportable;

import com.threerings.opengl.material.Material;
import com.threerings.opengl.material.SkinHost;
import com.threerings.opengl.material.Surface;
import com.threerings.opengl.material.SurfaceHost;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.MaterialState;
import com.threerings.opengl.renderer.state.TransformState;

import com.threerings.opengl.model.Animation.Frame;

import static com.threerings.opengl.Log.*;

/**
 * A model whose meshes are contained in a transformation hierarchy.
 */
public class ArticulatedModel extends Model
    implements SkinHost
{
    /**
     * A node in the transformation hierarchy.
     */
    public class Node
        implements Exportable
    {
        public Node (String name, boolean bone, Transform transform, Node[] children)
        {
            _name = name;
            _bone = bone;
            _localTransform = transform;
            _children = children;
        }

        public Node ()
        {
        }

        /**
         * Initializes the node using the provided material map.
         */
        public void init (Transform parentModelview)
        {
            _nodes.put(_name, this);
            _modelview = parentModelview.compose(_localTransform);
            if (_bone) {
                _invRefTransform = _modelview.invert();
                _boneTransform = _modelview.compose(_invRefTransform);
                _boneTransform.update(Transform.AFFINE);
            }
            for (Node child : _children) {
                child.init(_modelview);
            }
        }

        /**
         * Returns a reference to the node's local transform.
         */
        public Transform getLocalTransform ()
        {
            return _localTransform;
        }

        /**
         * Returns a reference to the node's modelview transform.
         */
        public Transform getModelview ()
        {
            return _modelview;
        }

        /**
         * Returns a reference to the node's bone transform.
         */
        public Transform getBoneTransform ()
        {
            return _boneTransform;
        }

        /**
         * Finds the intersection (if any) between a ray and the contents of this node.
         */
        public boolean getIntersection (Ray ray, Vector3f result)
        {
            float rdist = Float.MAX_VALUE;
            Vector3f cresult = new Vector3f();
            for (Node child : _children) {
                if (child.getIntersection(ray, cresult)) {
                    float dist = ray.getOrigin().distanceSquared(cresult);
                    if (dist < rdist) {
                        rdist = dist;
                        result.set(cresult);
                    }
                }
            }
            return (rdist < Float.MAX_VALUE);
        }

        /**
         * Enqueues the node for rendering.
         */
        public void enqueue (Transform parentModelview)
        {
            // compose parent modelview with local transform
            parentModelview.compose(_localTransform, _modelview);

            // update bone transform if necessary
            if (_bone) {
                _modelview.compose(_invRefTransform, _boneTransform);
                _boneTransform.update(Transform.AFFINE);
            }

            // enqueue children
            for (Node child : _children) {
                child.enqueue(_modelview);
            }
        }

        /**
         * (Re)creates the surfaces under this node.
         */
        public void createSurfaces (String variant, HashMap<String, Material> materials)
        {
            for (Node child : _children) {
                child.createSurfaces(variant, materials);
            }
        }

        /**
         * Updates the surfaces under this node.
         */
        public void updateSurfaces ()
        {
            // update the children
            for (Node child : _children) {
                child.updateSurfaces();
            }
        }

        /**
         * Creates a clone of this node for the specified new model.
         */
        public Node clone (ArticulatedModel omodel)
        {
            Node onode = omodel.createNode(
                _name, _bone, new Transform(_localTransform), cloneChildren(omodel));
            omodel._nodes.put(_name, onode);
            onode._modelview = new Transform(_modelview);
            onode._invRefTransform = _invRefTransform;
            if (_bone) {
                onode._boneTransform = new Transform(_boneTransform);
                onode._boneTransform.update(Transform.AFFINE);
            }
            return onode;
        }

        /**
         * Creates clones of this node's children for the specified new model.
         */
        protected Node[] cloneChildren (ArticulatedModel omodel)
        {
            Node[] ochildren = new Node[_children.length];
            for (int ii = 0; ii < _children.length; ii++) {
                ochildren[ii] = _children[ii].clone(omodel);
            }
            return ochildren;
        }

        /** The name of this node. */
        protected String _name;

        /** Whether or not this node is used as a bone. */
        protected boolean _bone;

        /** The node's transformation relative to its parent. */
        protected Transform _localTransform;

        /** The children of this node. */
        protected Node[] _children;

        /** The node's modelview transformation. */
        protected transient Transform _modelview;

        /** For nodes used as bones, the inverse of the reference pose transform. */
        protected transient Transform _invRefTransform;

        /** For nodes used as bones, the bone transform. */
        protected transient Transform _boneTransform;

        /** The value of the update counter at the last update. */
        protected transient int _lastUpdate;

        /** The total weight of the animation tracks applied to this node. */
        protected transient float _totalWeight;
    }

    /**
     * A node that contains a (visible and/or collision) mesh.
     */
    public class MeshNode extends Node
        implements SurfaceHost
    {
        public MeshNode (
            String name, boolean bone, Transform transform, Node[] children,
            VisibleMesh vmesh, CollisionMesh cmesh)
        {
            super(name, bone, transform, children);
            _vmesh = vmesh;
            _cmesh = cmesh;
        }

        public MeshNode ()
        {
        }

        // documentation inherited from interface SurfaceHost
        public Transform getModelview ()
        {
            return _modelview;
        }

        // documentation inherited from interface SurfaceHost
        public ColorState getColorState ()
        {
            return _cstate;
        }

        // documentation inherited from interface SurfaceHost
        public FogState getFogState ()
        {
            return _fstate;
        }

        // documentation inherited from interface SurfaceHost
        public LightState getLightState ()
        {
            return _lstate;
        }

        // documentation inherited from interface SurfaceHost
        public MaterialState getMaterialState ()
        {
            return _mstate;
        }

        // documentation inherited from interface SurfaceHost
        public TransformState getTransformState ()
        {
            return _tstate;
        }

        @Override // documentation inherited
        public void init (Transform parentTransform)
        {
            super.init(parentTransform);
            if (_vmesh == null) {
                return;
            }
            // create the transform state
            _tstate = new TransformState();
            _modelview = _tstate.getModelview();
        }

        @Override // documentation inherited
        public boolean getIntersection (Ray ray, Vector3f result)
        {
            // we must transform the ray from view space to model space before checking against
            // the collision mesh
            if (_cmesh == null || !_cmesh.getIntersection(
                    ray.transform(_modelview.invert()), result)) {
                return super.getIntersection(ray, result);
            }
            // then transform it back if we get a hit
            _modelview.transformPointLocal(result);

            // compare to child intersection, if any
            Vector3f cresult = new Vector3f(), origin = ray.getOrigin();
            if (super.getIntersection(ray, cresult) &&
                    origin.distanceSquared(cresult) < origin.distanceSquared(result)) {
                result.set(cresult);
            }
            return true;
        }

        @Override // documentation inherited
        public void enqueue (Transform parentModelview)
        {
            super.enqueue(parentModelview);
            if (_surface == null) {
                return;
            }
            // update transform state
            _tstate.setDirty(true);

            // enqueue surface for rendering
            _surface.enqueue();
        }

        @Override // documentation inherited
        public void createSurfaces (String variant, HashMap<String, Material> materials)
        {
            super.createSurfaces(variant, materials);
            String texture = _vmesh.getTexture();
            Material material = materials.get(texture);
            if (material == null) {
                material = getMaterial(variant, texture);
            }
            _surface = material.createSurface(_vmesh);
            _surface.setHost(this);
        }

        @Override // documentation inherited
        public void updateSurfaces ()
        {
            super.updateSurfaces();
            _surface.update();
        }

        @Override // documentation inherited
        public Node clone (ArticulatedModel omodel)
        {
            MeshNode onode = omodel.createNode(
                _name, _bone, new Transform(_localTransform),
                cloneChildren(omodel), _vmesh, _cmesh);
            omodel._nodes.put(_name, onode);
            onode._tstate = new TransformState(_modelview);
            onode._modelview = onode._tstate.getModelview();
            onode._surface = (Surface)_surface.clone();
            onode._surface.setHost(onode);
            return onode;
        }

        /** The node's visible mesh (if any). */
        protected VisibleMesh _vmesh;

        /** The node's collision mesh (if any). */
        protected CollisionMesh _cmesh;

        /** The mesh's transform state. */
        protected transient TransformState _tstate;

        /** The material surface. */
        protected transient Surface _surface;
    }

    /**
     * Handles a single animation on this model.
     */
    public class AnimationTrack
    {
        /**
         * Creates an animation track to run the specified animation on this model.
         */
        public AnimationTrack (Animation anim)
        {
            _anim = anim;

            // find the list of affected nodes
            String[] tnames = anim.getTargets();
            _targets = new Node[tnames.length];
            for (int ii = 0; ii < _targets.length; ii++) {
                _targets[ii] = _nodes.get(tnames[ii]);
            }

            // create an array to hold the snapshot pose
            _ttransforms = new Transform[_targets.length];
            for (int ii = 0; ii < _targets.length; ii++) {
                if (_targets[ii] != null) {
                    _ttransforms[ii] = new Transform();
                }
            }
        }

        /**
         * Returns a reference to the model that created this track.
         */
        public ArticulatedModel getModel ()
        {
            return ArticulatedModel.this;
        }

        /**
         * Returns a reference to the animation being played on this track.
         */
        public Animation getAnimation ()
        {
            return _anim;
        }

        /**
         * Adds an observer to be notified when the animation is cancelled or completed.
         */
        public void addObserver (AnimationObserver observer)
        {
            _observers.add(observer);
        }

        /**
         * Removes an observer from this track.
         */
        public void removeObserver (AnimationObserver observer)
        {
            _observers.remove(observer);
        }

        /**
         * Sets the priority level of this track.  Tracks are blended together in order of
         * decreasing priority, and blending stops for each node when the combined weight
         * of all tracks affecting it is greater than or equal to one.
         */
        public void setPriority (int priority)
        {
            _priority = priority;

            // remove, reinsert in sorted order
            if (_animtracks.remove(this)) {
                insertAnimationTrack(this);
            }
        }

        /**
         * Returns the priority level of this track.
         */
        public int getPriority ()
        {
            return _priority;
        }

        /**
         * Determines whether playing this track at full weight stops any other tracks playing at
         * the same priority level.
         */
        public void setOverride (boolean override)
        {
            _override = override;
        }

        /**
         * Checks whether playing this track at full weight stops any other tracks playing at the
         * same priority level.
         */
        public boolean getOverride ()
        {
            return _override;
        }

        /**
         * Sets the animation speed multiplier.
         */
        public void setSpeed (float speed)
        {
            _speed = speed;
        }

        /**
         * Retrieves the animation speed multiplier.
         */
        public float getSpeed ()
        {
            return _speed;
        }

        /**
         * Plays this animation track.
         */
        public void play ()
        {
            play(0f, 1f, 0f, 0f, false);
        }

        /**
         * Plays this animation track.
         *
         * @param transitionFrames the interval over which to interpolate from the current pose to
         * the first frame of the animation (in terms of the animation frame rate).
         */
        public void play (int transitionFrames)
        {
            play((float)transitionFrames / _anim.getFrameRate(), 1f, 0f, 0f, false);
        }

        /**
         * Plays this animation track.
         *
         * @param transition the interval over which to interpolate from the current pose to the
         * first frame of the animation.
         */
        public void play (float transition)
        {
            play(transition, 1f, 0f, 0f, false);
        }

        /**
         * Plays this animation track.
         *
         * @param transition the interval over which to interpolate from the current pose to the
         * first frame of the animation.
         * @param weight the weight of the animation.
         */
        public void play (float transition, float weight)
        {
            play(transition, weight, 0f, 0f, false);
        }

        /**
         * Plays this animation track.
         *
         * @param transition the interval over which to interpolate from the current pose to the
         * first frame of the animation.
         * @param weight the weight of the animation.
         * @param blend the interval over which to blend to and from the target weight.
         */
        public void play (float transition, float weight, float blend)
        {
            play(transition, weight, blend, blend, false);
        }

        /**
         * Plays this animation track.
         *
         * @param transition the interval over which to interpolate from the current pose to the
         * first frame of the animation.
         * @param weight the target weight of the animation.
         * @param blendIn the interval over which to blend to the target weight.
         * @param blendOut the interval over which to blend back to zero.
         */
        public void play (float transition, float weight, float blendIn, float blendOut)
        {
            play(transition, weight, blendIn, blendOut, false);
        }

        /**
         * Plays this animation track in a loop.
         */
        public void loop ()
        {
            play(0f, 1f, 0f, 0f, true);
        }

        /**
         * Plays this animation track in a loop.
         */
        public void loop (int transitionFrames)
        {
            play((float)transitionFrames / _anim.getFrameRate(), 1f, 0f, 0f, true);
        }

        /**
         * Plays this animation track in a loop.
         *
         * @param transition the interval over which to interpolate from the current pose to the
         * first frame of the animation.
         */
        public void loop (float transition)
        {
            play(transition, 1f, 0f, 0f, true);
        }

        /**
         * Plays this animation track in a loop.
         *
         * @param transition the interval over which to interpolate from the current pose to the
         * first frame of the animation.
         * @param weight the weight of the animation.
         */
        public void loop (float transition, float weight)
        {
            play(transition, weight, 0f, 0f, true);
        }

        /**
         * Plays this animation track in a loop.
         *
         * @param transition the interval over which to interpolate from the current pose to the
         * first frame of the animation.
         * @param weight the weight of the animation.
         */
        public void loop (float transition, float weight, float blend)
        {
            play(transition, weight, blend, 0f, true);
        }

        /**
         * Sets the position within the animation.
         */
        public void seek (float frame)
        {
            _fidx = (int)frame;
            _accum = frame - _fidx;
        }

        /**
         * Stops the animation on this track immediately.
         */
        public void stop ()
        {
            stop(0f);
        }

        /**
         * Stops the animation on this track, blending its weight to zero over the specified
         * interval.
         */
        public void stop (float blend)
        {
            blendToWeight(0f, blend);
        }

        /**
         * Sets the weight of this track immediately, cancelling any blend in progress.
         */
        public void setWeight (float weight)
        {
            blendToWeight(weight, 0f);
        }

        /**
         * Blends to a target weight over the specified interval.
         */
        public void blendToWeight (float weight, float blend)
        {
            _tweight = weight;
            if (blend > 0f) {
                _wrate = (_tweight - _weight) / blend;
            } else {
                _weight = _tweight;
            }
            if (weight == 0f) {
                _blendOut = 0f; // cancel any plans to blend out
            }
        }

        /**
         * Returns the current weight of this track.
         */
        public float getWeight ()
        {
            return _weight;
        }

        /**
         * Sets whether or not to loop the current animation.
         */
        public void setLooping (boolean looping)
        {
            if (_looping = looping) {
                _blendOut = 0f; // cancel any plans to blend out
            }
        }

        /**
         * Checks whether or not the current animation is looping.
         */
        public boolean isLooping ()
        {
            return _looping;
        }

        /**
         * Checks whether this track has completed and should be applied one last time before
         * being removed.
         */
        public boolean hasCompleted ()
        {
            return _completed;
        }

        /**
         * Updates the current animation position based on the elapsed time in seconds.
         *
         * @return true if the track has completed.
         */
        public boolean tick (float elapsed)
        {
            // see if we need to start blending out
            elapsed *= _speed;
            if (_blendOut > 0f && (_countdown -= elapsed) <= 0f) {
                blendToWeight(0f, _blendOut);
                _blendOut = 0f;
            }
            // update the weight
            if (_weight < _tweight) {
                _weight = Math.min(_weight + elapsed*_wrate, _tweight);
            } else if (_weight > _tweight) {
                _weight = Math.max(_weight + elapsed*_wrate, _tweight);
            }
            // if the weight is zero, we're done
            if (_weight == 0f && _tweight == 0f) {
                cancelled();
                return false;
            }
            // if we're transitioning, update the accumulated portion based on transition time
            if (_transition > 0f) {
                _accum += (elapsed / _transition);
                if (_accum < 1f) {
                    return false; // still transitioning
                }
                // done transitioning; fix accumulated frames and clear transition interval
                _accum = (_accum - 1f) * _transition * _anim.getFrameRate();
                _transition = 0f;

            // otherwise, based on frame rate
            } else {
                _accum += (elapsed * _anim.getFrameRate());
            }
            int frames = (int)_accum;
            _accum -= frames;

            // advance the frame index
            int fcount = _anim.getFrames().length;
            if (_looping) {
                _fidx = (_fidx + frames) % fcount;

            } else if ((_fidx += frames) >= fcount - 1) {
                _fidx = fcount - 1;
                _accum = 0f;
                completed();
            }
            return _completed;
        }

        /**
         * Updates the node transforms based on the current animation state.
         */
        public void updateTransforms ()
        {
            Frame[] frames = _anim.getFrames();
            Transform[] t1, t2;
            if (_transition > 0f) {
                t1 = _ttransforms;
                t2 = frames[0].getTransforms();
            } else {
                t1 = frames[_fidx].getTransforms();
                t2 = frames[(_fidx + 1) % frames.length].getTransforms();
            }
            for (int ii = 0; ii < _targets.length; ii++) {
                // lerp into the target transform
                Node target = _targets[ii];
                if (target != null) {
                    t1[ii].lerp(t2[ii], _accum, target.getLocalTransform());
                }
            }
        }

        /**
         * Blends in the node transforms based on the current animation state.
         */
        public void blendTransforms ()
        {
            Frame[] frames = _anim.getFrames();
            Transform[] t1, t2;
            if (_transition > 0f) {
                t1 = _ttransforms;
                t2 = frames[0].getTransforms();
            } else {
                t1 = frames[_fidx].getTransforms();
                t2 = frames[(_fidx + 1) % frames.length].getTransforms();
            }
            for (int ii = 0; ii < _targets.length; ii++) {
                // first make sure the target exists
                Node target = _targets[ii];
                if (target == null) {
                    continue;
                }
                // then see if we're the first to touch it, in which case we can lerp directly
                if (target._lastUpdate != _update) {
                    t1[ii].lerp(t2[ii], _accum, target.getLocalTransform());
                    target._lastUpdate = _update;
                    target._totalWeight = _weight;
                    continue;
                }
                // if the total weight is less than one, we can add our contribution
                if (target._totalWeight >= 1f) {
                    continue;
                }
                float weight = Math.min(_weight, 1f - target._totalWeight);
                t1[ii].lerp(t2[ii], _accum, _xform);
                target.getLocalTransform().lerpLocal(
                    _xform, weight / (target._totalWeight += weight));
            }
        }

        /**
         * Plays this animation track.
         *
         * @param transition the interval over which to interpolate from the current pose to the
         * first frame of the animation.
         * @param weight the target weight of the animation.
         * @param blendIn the interval over which to blend to the target weight.
         * @param blendOut the interval over which to blend back to zero (if not looping).
         * @param looping if true, play the animation in a continuous loop.
         */
        protected void play (
            float transition, float weight, float blendIn, float blendOut, boolean looping)
        {
            _transition = transition;
            _fidx = 0;
            _accum = 0f;
            _looping = looping;
            _completed = false;

            // initialize the blend parameters
            _weight = 0f;
            blendToWeight(weight, blendIn);

            // if blending out, store countdown time and blend interval
            if (!looping && blendOut > 0f) {
                float duration = transition + (_anim.getFrames().length - 1f)/_anim.getFrameRate();
                _countdown = duration - (_blendOut = blendOut);
            } else {
                _blendOut = 0f;
            }

            // if transitioning, take a snapshot of the nodes' transforms
            if (transition > 0f) {
                for (int ii = 0; ii < _targets.length; ii++) {
                    Node target = _targets[ii];
                    if (target != null) {
                        _ttransforms[ii].set(target.getLocalTransform());
                    }
                }
            }

            // stop any animations already playing, if appropriate
            if (_override && _weight >= 1f) {
                stopAnimation(_priority, blendIn);
            }

            // add ourself to the list of animations playing
            if (!_animtracks.contains(this)) {
                insertAnimationTrack(this);
            }
        }

        /**
         * Creates a clone of this track for the specified new model.
         */
        public AnimationTrack clone (ArticulatedModel omodel)
        {
            AnimationTrack otrack = omodel.createAnimationTrack(_anim);
            return otrack;
        }

        /**
         * Called when the animation has completed.
         */
        protected void completed ()
        {
            // set the completed flag
            _completed = true;

            // notify track and model observers
            _animCompletedOp.init(this);
            _observers.apply(_animCompletedOp);
            _animobs.apply(_animCompletedOp);
        }

        /**
         * Called when the animation has been cancelled.
         */
        protected void cancelled ()
        {
            // remove from the list
            _animtracks.remove(this);

            // notify track and model observers
            _animCancelledOp.init(this);
            _observers.apply(_animCancelledOp);
            _animobs.apply(_animCancelledOp);
        }

        /** The animation being played on this track. */
        protected Animation _anim;

        /** The target nodes of the animation. */
        protected Node[] _targets;

        /** The list of animation observers. */
        protected ObserverList<AnimationObserver> _observers =
            new ObserverList<AnimationObserver>(ObserverList.FAST_UNSAFE_NOTIFY);

        /** The priority level of this track. */
        protected int _priority;

        /** A snapshot of the original transforms of the targets, for transitioning. */
        protected Transform[] _ttransforms;

        /** The interval over which to transition into the first frame. */
        protected float _transition;

        /** The current weight of the track. */
        protected float _weight;

        /** The target weight of the track. */
        protected float _tweight;

        /** The rate of change for the weight. */
        protected float _wrate;

        /** The time remaining until we have to start blending the animation out. */
        protected float _countdown;

        /** The interval over which to blend out the animation. */
        protected float _blendOut;

        /** The index of the current animation frame. */
        protected int _fidx;

        /** The progress towards the next frame. */
        protected float _accum;

        /** The animation speed. */
        protected float _speed = 1f;

        /** Whether or not playing this animation at full weight stops any other animations playing
         * at the same priority level. */
        protected boolean _override = true;

        /** Whether or not the current animation is looping. */
        protected boolean _looping;

        /** Set when the animation has completed. */
        protected boolean _completed;

        /** A temporary transform for interpolation. */
        protected Transform _xform = new Transform();
    }

    /**
     * Creates a new articulated model.
     */
    public ArticulatedModel (Properties props)
    {
        super(props);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ArticulatedModel ()
    {
    }

    /**
     * Creates a non-mesh node for this model.  This method should only be called when forming a
     * model from an XML definition.
     */
    public Node createNode (String name, boolean bone, Transform transform, Node[] children)
    {
        return new Node(name, bone, transform, children);
    }

    /**
     * Creates a node containing a (visible and/or collision) mesh for this model.  This method
     * should only be called when forming a model from an XML definition.
     */
    public MeshNode createNode (
        String name, boolean bone, Transform transform, Node[] children, VisibleMesh vmesh,
        CollisionMesh cmesh)
    {
        return new MeshNode(name, bone, transform, children, vmesh, cmesh);
    }

    /**
     * Configures the root and skin meshes of this model.  This method should only be called when
     * forming a model from an XML definition.
     */
    public void setData (Node root, SkinMesh[] smeshes, CollisionMesh cmesh)
    {
        _root = root;
        _smeshes = smeshes;
        _cmesh = cmesh;
    }

    /**
     * Attaches a model to the specified attachment point, replacing any existing attachment(s).
     *
     * @param point the name of the point to which the model should be attached.
     */
    public void attach (String point, Model model)
    {
        attach(point, model, true);
    }

    /**
     * Attaches a model to the specified attachment point.
     *
     * @param point the name of the point to which the model should be attached.
     * @param replace if true, replace any existing attachments at the point.
     */
    public void attach (String point, Model model, boolean replace)
    {
        Node node = getAttachmentNode(point);
        if (node == null) {
            return;
        }
        if (replace) {
            detachAll(node);
        }
        _attachments.add(new Attachment(node, model));
        updateAttached(model);
    }

    /**
     * Detaches any models attached to the specified point.
     */
    public void detachAll (String point)
    {
        detachAll(getAttachmentNode(point));
    }

    /**
     * Detaches an attached model.
     */
    public void detach (Model model)
    {
        for (int ii = 0, nn = _attachments.size(); ii < nn; ii++) {
            if (_attachments.get(ii).model == model) {
                _attachments.remove(ii);
                return;
            }
        }
        log.warning("Missing attachment to remove [model=" + model + "].");
    }

    /**
     * Retrieves the translation of the specified node.
     */
    public Vector3f getTranslation (String point)
    {
        Node node = _nodes.get(point);
        if (node != null) {
            Transform world = _ctx.getRenderer().getCamera().getWorldTransform();
            return world.transformPoint(node.getModelview().getTranslation());
        } else {
            log.warning("Invalid node [name=" + point + "].");
            return new Vector3f();
        }
    }

    /**
     * Adds an observer to notify when animations are completed or cancelled.
     */
    public void addAnimationObserver (AnimationObserver observer)
    {
        _animobs.add(observer);
    }

    /**
     * Removes an animation observer.
     */
    public void removeAnimationObserver (AnimationObserver observer)
    {
        _animobs.remove(observer);
    }

    /**
     * Goes to the first frame of an animation and stops.
     */
    public void setAnimationFrame (String name)
    {
        setAnimationFrame(name, 0);
    }

    /**
     * Goes to a single frame of an animation and stops.
     */
    public void setAnimationFrame (String name, int fidx)
    {
        // create the track, seek the position, update the transforms
        AnimationTrack track = createAnimationTrack(name);
        track.play();
        track.seek(fidx);
        track.updateTransforms();

        // now stop all animation, including the track we created
        stopAnimation();
    }

    /**
     * Creates and returns an animation track to run the named animation.
     */
    public AnimationTrack createAnimationTrack (String name)
    {
        return createAnimationTrack(_ctx.getModelCache().getAnimation(name));
    }

    /**
     * Creates and returns an animation track to run the specified animation.
     */
    public AnimationTrack createAnimationTrack (Animation anim)
    {
        return new AnimationTrack(anim);
    }

    /**
     * Returns a reference to the list of animation tracks currently playing.
     */
    public ArrayList<AnimationTrack> getAnimationTracks ()
    {
        return _animtracks;
    }

    /**
     * Stops all animations.
     */
    public void stopAnimation ()
    {
        for (int ii = 0, nn = _animtracks.size(); ii < nn; ii++) {
            _animtracks.get(ii).stop();
        }
    }

    /**
     * Stops all animations at the specified priority level.
     */
    public void stopAnimation (int priority)
    {
        stopAnimation(priority, 0f);
    }

    /**
     * Stops all animations at the specified priority level, blending their weights to zero over
     * the specified interval.
     */
    public void stopAnimation (int priority, float blend)
    {
        for (int ii = 0, nn = _animtracks.size(); ii < nn; ii++) {
            AnimationTrack track = _animtracks.get(ii);
            if (track.getPriority() == priority) {
                track.stop(blend);
            }
        }
    }

    // documentation inherited from interface SurfaceHost
    public Transform getModelview ()
    {
        return _modelview;
    }

    // documentation inherited from interface SurfaceHost
    public TransformState getTransformState ()
    {
        return TransformState.IDENTITY;
    }

    // documentation inherited from interface SkinHost
    public Matrix4f getBoneMatrix (String bone)
    {
        return _nodes.get(bone).getBoneTransform().getMatrix();
    }

    // documentation inherited from interface Intersectable
    public boolean getIntersection (Ray ray, Vector3f result)
    {
        // we must transform the ray into model space before checking against the collision mesh
        Transform view = _ctx.getRenderer().getCamera().getViewTransform();
        if (_cmesh == null || !_worldBounds.intersects(ray) ||
            !_cmesh.getIntersection(ray.transform(_transform.invert()), result)) {
            return _root.getIntersection(ray.transform(view), result);
        }
        // then transform it back if we get a hit
        _transform.transformPointLocal(result);

        // compare to node intersection, if any
        Vector3f rresult = new Vector3f(), origin = ray.getOrigin();
        if (_root.getIntersection(ray.transform(view), rresult) &&
                origin.distanceSquared(rresult) < origin.distanceSquared(result)) {
            result.set(rresult);
        }
        return true;
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // update the local transforms based on the animation
        updateTransforms();

        // update the modelview transform and the transform hierarchy
        _ctx.getRenderer().getCamera().getViewTransform().compose(_transform, _modelview);
        _root.enqueue(_modelview);

        // enqueue the skinned surfaces
        for (Surface surface : _ssurfaces) {
            surface.enqueue();
        }

        // enqueue the attached models
        for (int ii = 0, nn = _attachments.size(); ii < nn; ii++) {
            Attachment attachment = _attachments.get(ii);
            attachment.model.enqueue(attachment.node._modelview);
        }
    }

    @Override // documentation inherited
    public void createSurfaces (String variant)
    {
        // create the static mesh surfaces
        HashMap<String, Material> materials = new HashMap<String, Material>();
        _root.createSurfaces(variant, materials);

        // create the skin surfaces
        _ssurfaces = new Surface[_smeshes.length];
        for (int ii = 0; ii < _smeshes.length; ii++) {
            SkinMesh mesh = _smeshes[ii];
            String texture = mesh.getTexture();
            Material material = materials.get(texture);
            if (material == null) {
                material = getMaterial(variant, texture);
            }
            Surface surface = material.createSurface(mesh);
            surface.setHost(this);
            _ssurfaces[ii] = surface;
        }
    }

    @Override // documentation inherited
    public void updateSurfaces ()
    {
        super.updateSurfaces();
        _root.updateSurfaces();
        for (Surface surface : _ssurfaces) {
            surface.update();
        }
        for (int ii = 0, nn = _attachments.size(); ii < nn; ii++) {
            updateAttached(_attachments.get(ii).model);
        }
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
        _trackarray = _animtracks.toArray(_trackarray);
        for (int ii = 0, nn = _animtracks.size(); ii < nn; ii++) {
            completed |= _trackarray[ii].tick(elapsed);
        }

        // if any tracks have completed, issue a final update and remove them
        if (completed) {
            updateTransforms();
            for (int ii = _animtracks.size() - 1; ii >= 0; ii--) {
                AnimationTrack track = _animtracks.get(ii);
                if (track.hasCompleted()) {
                    _animtracks.remove(ii);
                }
            }
        }

        // tick the attached models
        for (int ii = 0, nn = _attachments.size(); ii < nn; ii++) {
            _attachments.get(ii).model.tick(elapsed);
        }
    }

    @Override // documentation inherited
    public Object clone ()
    {
        ArticulatedModel omodel = (ArticulatedModel)super.clone();
        omodel.initTransientFields();
        omodel._root = _root.clone(omodel);
        omodel._ssurfaces = new Surface[_ssurfaces.length];
        for (int ii = 0; ii < _ssurfaces.length; ii++) {
            omodel._ssurfaces[ii] = (Surface)_ssurfaces[ii].clone();
            omodel._ssurfaces[ii].setHost(omodel);
        }
        omodel._attachments = new ArrayList<Attachment>();
        for (int ii = 0, nn = _attachments.size(); ii < nn; ii++) {
            Attachment attachment = _attachments.get(ii);
            omodel._attachments.add(new Attachment(
                omodel._nodes.get(attachment.node._name),
                (Model)attachment.model.clone()));
        }
        omodel._animtracks = new ArrayList<AnimationTrack>();
        for (int ii = 0, nn = _animtracks.size(); ii < nn; ii++) {
            omodel._animtracks.add(_animtracks.get(ii).clone(omodel));
        }
        return omodel;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        initTransientFields();

        // initialize the transformation hierarchy
        _root.init(_transform);

        // add the skin mesh bounds
        updateLocalBounds();

        // create the surfaces for the default variant
        createSurfaces(null);
    }

    /**
     * Initializes the model's transient fields after loading or cloning.
     */
    protected void initTransientFields ()
    {
        _modelview = new Transform();
        _nodes = new HashMap<String, Node>();
        _animobs = new ObserverList<AnimationObserver>(ObserverList.FAST_UNSAFE_NOTIFY);
        _trackarray = new AnimationTrack[0];
    }

    @Override // documentation inherited
    protected void enqueue (Transform modelview)
    {
        // update the local transforms based on the animation
        updateTransforms();

        // update the world transform and the transform hierarchy
        _ctx.getRenderer().getCamera().getWorldTransform().compose(modelview, _transform);
        _root.enqueue(modelview);

        // enqueue the skinned surfaces
        for (Surface surface : _ssurfaces) {
            surface.enqueue();
        }

        // enqueue the attached models
        for (int ii = 0, nn = _attachments.size(); ii < nn; ii++) {
            Attachment attachment = _attachments.get(ii);
            attachment.model.enqueue(attachment.node._modelview);
        }
    }

    /**
     * Updates the node transforms based on the current animation state.
     */
    protected void updateTransforms ()
    {
        // handle the special (but likely common) case of a single track
        int nn = _animtracks.size();
        if (nn == 1) {
            _animtracks.get(0).updateTransforms();
            return;
        }

        // increment the update counter so that the tracks know which nodes have been updated
        _update++;

        // process the tracks in order of decreasing priority
        for (int ii = 0; ii < nn; ii++) {
            _animtracks.get(ii).blendTransforms();
        }
    }

    /**
     * Updates the local bounds of this model.
     */
    protected void updateLocalBounds ()
    {
        _localBounds = new Box(Vector3f.MAX_VALUE, Vector3f.MIN_VALUE);
        for (SkinMesh mesh : _smeshes) {
            _localBounds.addLocal(mesh.getBounds());
        }
    }

    /**
     * Inserts an animation track in sorted order.  Higher priority tracks come before (and
     * take precendence over) lower priority tracks, and more recently added tracks come
     * before less recently added ones.
     */
    protected void insertAnimationTrack (AnimationTrack track)
    {
        int priority = track.getPriority();
        for (int ii = 0, nn = _animtracks.size(); ii < nn; ii++) {
            if (priority >= _animtracks.get(ii).getPriority()) {
                _animtracks.add(ii, track);
                return;
            }
        }
        _animtracks.add(track);
    }

    /**
     * Fetches the named attachment point, logging a warning and returning <code>null</code> if
     * the point doesn't exist.
     */
    protected Node getAttachmentNode (String point)
    {
        Node node = (point != null) ? _nodes.get(point) : _root;
        if (node == null) {
            log.warning("Invalid attachment point [name=" + point + "].");
        }
        return node;
    }

    /**
     * Detaches all models attached to the specified node.
     */
    protected void detachAll (Node node)
    {
        for (int ii = _attachments.size() - 1; ii >= 0; ii--) {
            if (_attachments.get(ii).node == node) {
                _attachments.remove(ii);
            }
        }
    }

    /**
     * Updates the states of the attached model.
     */
    protected void updateAttached (Model model)
    {
        model.setFogState(_fstate);
        model.setLightState(_lstate);
        model.getColor().set(_cstate.getColor());
        model.updateSurfaces();
    }

    /**
     * Represents an attachment.
     */
    protected static final class Attachment
    {
        /** The node to which the model is attached. */
        public Node node;

        /** The attached model. */
        public Model model;

        public Attachment (Node node, Model model)
        {
            this.node = node;
            this.model = model;
        }
    }

    /**
     * Superclass of animation observer ops.
     */
    protected static abstract class AnimationObserverOp
        implements ObserverList.ObserverOp<AnimationObserver>
    {
        /**
         * (Re)initializes the op.
         */
        public void init (AnimationTrack track)
        {
            _track = track;
        }

        /** The animation track. */
        protected AnimationTrack _track;
    }

    /** The root node of the transformation hierarchy. */
    protected Node _root;

    /** The model's skinned meshes. */
    protected SkinMesh[] _smeshes;

    /** The skin collision mesh. */
    protected CollisionMesh _cmesh;

    /** The skinned surfaces. */
    protected transient Surface[] _ssurfaces;

    /** The modelview transformation. */
    protected transient Transform _modelview;

    /** Maps names to nodes. */
    protected transient HashMap<String, Node> _nodes;

    /** Attached models. */
    protected transient ArrayList<Attachment> _attachments = new ArrayList<Attachment>();

    /** The list of animation observers. */
    protected transient ObserverList<AnimationObserver> _animobs;

    /** The active animation tracks, sorted by decreasing priority. */
    protected transient ArrayList<AnimationTrack> _animtracks = new ArrayList<AnimationTrack>();

    /** Holds the animation tracks during the tick. */
    protected transient AnimationTrack[] _trackarray;

    /** Incremented on each call to {@link #updateTransforms} and used to determine which nodes
     * have been manipulated by animation tracks on the current update. */
    protected transient int _update;

    /** Used to notify observers when animations complete. */
    protected static AnimationObserverOp _animCompletedOp = new AnimationObserverOp() {
        public boolean apply (AnimationObserver observer) {
            return observer.animationCompleted(_track);
        }
    };

    /** Used to notify observers when animations are cancelled. */
    protected static AnimationObserverOp _animCancelledOp = new AnimationObserverOp() {
        public boolean apply (AnimationObserver observer) {
            return observer.animationCancelled(_track);
        }
    };
}
