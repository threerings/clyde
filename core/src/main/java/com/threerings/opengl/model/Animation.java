//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.model;

import java.util.ArrayList;

import com.samskivert.util.ObserverList;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.Executor;
import com.threerings.expr.Function;
import com.threerings.expr.MutableLong;
import com.threerings.expr.ObjectExpression.Evaluator;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;

import com.threerings.opengl.model.config.AnimationConfig;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.log;

/**
 * An animation for an {@link Articulated} model.
 */
public class Animation extends SimpleScope
    implements ConfigUpdateListener<AnimationConfig>
{
    /**
     * The actual animation implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (GlContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
        }

        /**
         * Starts the animation.
         */
        public void start ()
        {
            // blend in
            blendToWeight(_config.weight, _config.blendIn);

            // notify containing animation
            ((Animation)_parentScope).started(
                (_config.override && _config.weight == 1f) ? _config.blendIn : -1f);
        }

        /**
         * Stops the animation.
         */
        public void stop ()
        {
            stop(_config.blendOut);
        }

        /**
         * Stops the animation, blending out over the specified interval.
         */
        public void stop (float blendOut)
        {
            // blend out
            blendToWeight(0f, blendOut);
        }

        /**
         * Determines whether the animation is currently playing.
         */
        public boolean isPlaying ()
        {
            return _weight > 0f || _targetWeight > 0f;
        }

        /**
         * Returns the priority of this animation.
         */
        public int getPriority ()
        {
            return _config.priority;
        }

        /**
         * Sets a modifier to the speed at which to play the animation.
         */
        public void setSpeedModifier (float speedModifier)
        {
            // nothing by default
        }

        /**
         * Updates this animation based on the elapsed time in seconds.
         *
         * @return true if the animation has completed.
         */
        public boolean tick (float elapsed)
        {
            // update the weight
            if (_weight < _targetWeight) {
                _weight = Math.min(_weight + elapsed*_weightRate, _targetWeight);
            } else if (_weight > _targetWeight) {
                _weight = Math.max(_weight + elapsed*_weightRate, _targetWeight);
            }
            // if the weight is zero, we're done
            if (_weight == 0f && _targetWeight == 0f) {
                ((Animation)_parentScope).stopped(false);
            }
            return false;
        }

        /**
         * Checks whether this animation has completed.
         */
        public boolean hasCompleted ()
        {
            return false;
        }

        /**
         * Updates the transforms directly from this animation.
         */
        public void updateTransforms ()
        {
            // nothing by default
        }

        /**
         * Blends in the influence of this animation.
         *
         * @param update the current value of the update counter (used to determine which nodes
         * have been touched on this update).
         */
        public void blendTransforms (int update)
        {
            // nothing by default
        }

        /**
         * Dumps some information about the animation to the standard output.
         */
        public void dumpInfo (String prefix)
        {
            // nothing by default
        }

        @Override
        public String getScopeName ()
        {
            return "impl";
        }

        /**
         * (Re)configures the implementation.
         */
        protected void setConfig (AnimationConfig.Original config)
        {
            _config = config;
        }

        /**
         * Blends to a target weight over an interval specified in seconds.
         */
        protected void blendToWeight (float weight, float interval)
        {
            _targetWeight = weight;
            if (interval > 0f) {
                _weightRate = (_targetWeight - _weight) / interval;
            } else {
                _weight = _targetWeight;
            }
        }

        /** The application context. */
        protected GlContext _ctx;

        /** The implementation configuration. */
        protected AnimationConfig.Original _config;

        /** The current weight of the animation. */
        protected float _weight;

        /** The target weight of the animation. */
        protected float _targetWeight;

        /** The weight's current rate of change. */
        protected float _weightRate;
    }

    /**
     * An imported implementation.
     */
    public static class Imported extends Implementation
    {
        /**
         * Creates a new imported implementation.
         */
        public Imported (GlContext ctx, Scope parentScope, AnimationConfig.Imported config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (AnimationConfig.Imported config)
        {
            super.setConfig(_config = config);

            // resolve the targets and initialize the snapshot array
            _targets = new Articulated.Node[config.targets.length];
            if (_snapshot == null || _snapshot.length != _targets.length) {
                _snapshot = new Transform3D[_targets.length];
            }
            Function getNode = ScopeUtil.resolve(_parentScope, "getNode", Function.NULL);
            for (int ii = 0; ii < _targets.length; ii++) {
                _targets[ii] = (Articulated.Node)getNode.call(config.targets[ii]);
                if (_targets[ii] == null) {
                    _snapshot[ii] = null;
                } else if (_snapshot[ii] == null) {
                    _snapshot[ii] = new Transform3D();
                }
            }
            if (config.modifiers.length == 0) {
                _transforms = config.transforms;
            } else {
                Transform3D[] nodeDefaults = new Transform3D[_targets.length];
                for (int ii = 0; ii < _targets.length; ii++) {
                    nodeDefaults[ii] = (_targets[ii] == null ?
                        new Transform3D() : _targets[ii].getConfig().transform)
                        .promote(Transform3D.UNIFORM);
                }
                _transforms = config.getModifiedTransforms(nodeDefaults);
            }

            // create the executors
            _executors = new FrameExecutor[config.actions.length];
            for (int ii = 0; ii < _executors.length; ii++) {
                AnimationConfig.FrameAction action = config.actions[ii];
                _executors[ii] = new FrameExecutor(
                    action.frame, action.action.createExecutor(_ctx, this));
            }

            if (_fidx > _transforms.length) {
                _fidx = 0;
                _eidx = 0;
            }
        }

        @Override
        public void start ()
        {
            // initialize frame counter
            int offset = Math.round(_config.offset.getValue() * getFrameRate());
            _fidx = _eidx = Math.max(0, offset) % _transforms.length;
            _accum = 0f;
            _completed = false;

            // if transitioning, take a snapshot of the current transforms
            if (_transitioning = (_config.transition > 0f)) {
                for (int ii = 0; ii < _targets.length; ii++) {
                    Articulated.Node target = _targets[ii];
                    if (target != null) {
                        _snapshot[ii].set(target.getLocalTransform());
                    }
                }
            }

            // if blending out, store countdown time
            if (_counting = (!_config.loop && _config.blendOut > 0f)) {
                float foff = (_fidx == 0) ? 0f : (_fidx / getFrameRate());
                _countdown = _config.getDuration() - foff - _config.blendOut;
            }

            // blend in
            super.start();
        }

        @Override
        public boolean isPlaying ()
        {
            return super.isPlaying() && !hasCompleted();
        }

        @Override
        public boolean tick (float elapsed)
        {
            // see if we need to start blending out
            if (_counting && (_countdown -= elapsed) <= 0f) {
                blendToWeight(0f, _config.blendOut);
            }

            // update the weight
            super.tick(elapsed);
            if (!isPlaying()) {
                return false;
            }

            // if we're transitioning, update the accumulated portion based on transition time
            if (_transitioning) {
                _accum += (elapsed / _config.transition);
                if (_accum < 1f) {
                    return false; // still transitioning
                }
                // done transitioning; fix accumulated frames and clear transition flag
                _accum = (_accum - 1f) * _config.transition * getFrameRate();
                _transitioning = false;

            // otherwise, based on frame rate
            } else {
                _accum += (elapsed * getFrameRate());
            }

            // advance the frame index and execute any actions
            int frames = (int)_accum;
            _accum -= frames;
            _fidx += frames;
            if (_fidx < 0) { // sanity check
                log.warning("Frame index went negative!", "anim",
                    ((Animation)_parentScope)._name, "fidx", _fidx, "accum", _accum,
                    "elapsed", elapsed, "frames", frames);
                _fidx = 0;
            }
            executeActions();

            // check for loop or completion
            int fcount = _transforms.length;
            if (_config.loop) {
                if (_fidx >= fcount) {
                    _fidx %= fcount;
                    _eidx = 0;
                    executeActions();
                }
            } else if (_fidx >= fcount - 1) {
                _fidx = fcount - 1;
                _accum = 0f;
                _completed = true;
                ((Animation)_parentScope).stopped(true);
                return true;
            }
            return false;
        }

        @Override
        public boolean hasCompleted ()
        {
            return _completed;
        }

        @Override
        public void updateTransforms ()
        {
            Transform3D[] t1, t2;
            if (_transitioning) {
                t1 = _snapshot;
                t2 = _transforms[_fidx];
            } else {
                t1 = _transforms[_fidx];
                t2 = _transforms[(_fidx + 1) % _transforms.length];
            }
            for (int ii = 0; ii < _targets.length; ii++) {
                // lerp into the target transform
                Articulated.Node target = _targets[ii];
                if (target != null) {
                    t1[ii].lerp(t2[ii], _accum, target.getLocalTransform());
                }
            }
        }

        @Override
        public void blendTransforms (int update)
        {
            Transform3D[] t1, t2;
            if (_transitioning) {
                t1 = _snapshot;
                t2 = _transforms[_fidx];
            } else {
                t1 = _transforms[_fidx];
                t2 = _transforms[(_fidx + 1) % _transforms.length];
            }
            for (int ii = 0; ii < _targets.length; ii++) {
                // first make sure the target exists
                Articulated.Node target = _targets[ii];
                if (target == null) {
                    continue;
                }
                // then see if we're the first to touch it, in which case we can lerp directly
                if (target.lastUpdate != update) {
                    t1[ii].lerp(t2[ii], _accum, target.getLocalTransform());
                    target.lastUpdate = update;
                    target.totalWeight = _weight;
                    continue;
                }
                // if our weight is greater than zero and the total weight is
                // less than one, we can add our contribution
                if (_weight <= 0f || target.totalWeight >= 1f) {
                    continue;
                }
                float mweight = Math.min(_weight, 1f - target.totalWeight);
                t1[ii].lerp(t2[ii], _accum, _xform);
                target.getLocalTransform().lerpLocal(
                    _xform, mweight / (target.totalWeight += mweight));
            }
        }

        @Override
        protected void blendToWeight (float weight, float interval)
        {
            super.blendToWeight(weight, interval);
            if (weight == 0f) {
                _counting = false; // cancel any plans to blend out
            }
        }

        /**
         * Returns the animation's frame rate.
         */
        protected float getFrameRate ()
        {
            return _config.getScaledRate() * ((Animation)_parentScope).getSpeed();
        }

        /**
         * Executes all actions scheduled before or at the current frame.
         */
        protected void executeActions ()
        {
            float frame = _fidx + _accum;
            for (; _eidx < _executors.length && _executors[_eidx].frame < frame; _eidx++) {
                _executors[_eidx].executor.execute();
            }
        }

        /** The implementation configuration. */
        protected AnimationConfig.Imported _config;

        /** The targets of the animation. */
        protected Articulated.Node[] _targets;

        /** The animation transforms after modifications are applied. */
        protected Transform3D[][] _transforms;

        /** A snapshot of the original transforms of the targets, for transitioning. */
        protected Transform3D[] _snapshot;

        /** Executors for frame actions. */
        protected FrameExecutor[] _executors;

        /** Whether we are currently transitioning into the first frame. */
        protected boolean _transitioning;

        /** Whether we are counting down until we must blend out. */
        protected boolean _counting;

        /** The time remaining until we have to start blending the animation out. */
        protected float _countdown;

        /** The index of the current animation frame. */
        protected int _fidx;

        /** The progress towards the next frame. */
        protected float _accum;

        /** The index of the next executor. */
        protected int _eidx;

        /** Set when the animation has completed. */
        protected boolean _completed;

        /** A temporary transform for interpolation. */
        protected Transform3D _xform = new Transform3D();
    }

    /**
     * A procedural implementation.
     */
    public static class Procedural extends Implementation
    {
        /**
         * Creates a new procedural implementation.
         */
        public Procedural (GlContext ctx, Scope parentScope, AnimationConfig.Procedural config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (AnimationConfig.Procedural config)
        {
            super.setConfig(_config = config);
            updateFromConfig();
        }

        @Override
        public void start ()
        {
            _accum = _config.offset.getValue();
            _completed = false;

            // if we have a finite duration, adjust the offset appropriately
            if (_config.duration > 0f && (_accum %= _config.duration) < 0f) {
                _accum += _config.duration;
            }

            // if blending out, store countdown time
            if (_counting = (_config.duration > 0f && _config.blendOut > 0f)) {
                _countdown = _config.duration - _accum - _config.blendOut;
            }

            // set the offset epoch
            resetEpoch();

            // blend in
            super.start();
        }

        @Override
        public boolean isPlaying ()
        {
            return super.isPlaying() && !hasCompleted();
        }

        @Override
        public boolean tick (float elapsed)
        {
            // see if we need to start blending out
            if (_counting && (_countdown -= elapsed) <= 0f) {
                blendToWeight(0f, _config.blendOut);
            }

            // update the weight
            super.tick(elapsed);
            if (!isPlaying()) {
                return false;
            }
            _accum += elapsed;

            // check for completion
            if (_config.duration > 0f && _accum >= _config.duration) {
                _completed = true;
                ((Animation)_parentScope).stopped(true);
                return true;
            }
            return false;
        }

        @Override
        public boolean hasCompleted ()
        {
            return _completed;
        }

        @Override
        public void updateTransforms ()
        {
            for (TargetTransform transform : _transforms) {
                transform.update();
            }
        }

        @Override
        public void blendTransforms (int update)
        {
            for (TargetTransform transform : _transforms) {
                transform.blend(update, _weight);
            }
        }

        @Override
        protected void blendToWeight (float weight, float interval)
        {
            super.blendToWeight(weight, interval);
            if (weight == 0f) {
                _counting = false; // cancel any plans to blend out
            }
        }

        @Override
        public void scopeUpdated (ScopeEvent event)
        {
            super.scopeUpdated(event);
            resetEpoch();
            updateFromConfig();
        }

        /**
         * Updates the animation from its configuration and scope.
         */
        protected void updateFromConfig ()
        {
            // create the target transforms
            Function getNode = ScopeUtil.resolve(_parentScope, "getNode", Function.NULL);
            _transforms = new TargetTransform[_config.transforms.length];
            for (int ii = 0; ii < _transforms.length; ii++) {
                AnimationConfig.TargetTransform transform = _config.transforms[ii];
                ArrayList<Articulated.Node> targets =
                    new ArrayList<Articulated.Node>(transform.targets.length);
                for (String target : transform.targets) {
                    Articulated.Node node = (Articulated.Node)getNode.call(target);
                    if (node != null) {
                        targets.add(node);
                    }
                }
                _transforms[ii] = new TargetTransform(
                    targets.toArray(new Articulated.Node[targets.size()]),
                    transform.expression.createEvaluator(this));
            }
        }

        /**
         * Resets the epoch value to the current time minus the offset.
         */
        protected void resetEpoch ()
        {
            _epoch.value = _parentEpoch.value - (long)(_accum * 1000f);
        }

        /**
         * Pairs a node with its transform evaluator.
         */
        protected static class TargetTransform
        {
            /** The nodes to update. */
            public Articulated.Node[] targets;

            /** The expression evaluator for the transform. */
            public Evaluator<Transform3D> evaluator;

            public TargetTransform (Articulated.Node[] targets, Evaluator<Transform3D> evaluator)
            {
                this.targets = targets;
                this.evaluator = evaluator;
            }

            /**
             * Updates the transforms directly from this target.
             */
            public void update ()
            {
                Transform3D transform = evaluator.evaluate();
                for (Articulated.Node target : targets) {
                    target.getLocalTransform().set(transform);
                }
            }

            /**
             * Blends in the influence of this target.
             *
             * @param update the current value of the update counter (used to determine which nodes
             * have been touched on this update).
             * @param weight the weight of our contribution.
             */
            public void blend (int update, float weight)
            {
                Transform3D transform = evaluator.evaluate();
                for (Articulated.Node target : targets) {
                    // if we're the first to touch it, we can set the transform directly
                    if (target.lastUpdate != update) {
                        target.getLocalTransform().set(transform);
                        target.lastUpdate = update;
                        target.totalWeight = weight;
                        continue;
                    }
                    // if the total weight is less than one, we can add our contribution
                    if (target.totalWeight >= 1f) {
                        continue;
                    }
                    float mweight = Math.min(weight, 1f - target.totalWeight);
                    target.getLocalTransform().lerpLocal(
                        transform, mweight / (target.totalWeight += mweight));
                }
            }
        }

        /** The implementation configuration. */
        protected AnimationConfig.Procedural _config;

        /** The target transforms. */
        protected TargetTransform[] _transforms;

        /** Whether we are counting down until we must blend out. */
        protected boolean _counting;

        /** The time remaining until we have to start blending the animation out. */
        protected float _countdown;

        /** The accumulated time. */
        protected float _accum;

        /** Set when the animation has completed. */
        protected boolean _completed;

        /** The parent epoch. */
        @Bound("epoch")
        protected MutableLong _parentEpoch;

        /** The offset epoch. */
        @Scoped
        protected MutableLong _epoch = new MutableLong(System.currentTimeMillis());
    }

    /**
     * A sequential implementation.
     */
    public static class Sequential extends Implementation
    {
        /**
         * Creates a new sequential implementation.
         */
        public Sequential (GlContext ctx, Scope parentScope, AnimationConfig.Sequential config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (AnimationConfig.Sequential config)
        {
            super.setConfig(_config = config);

            // (re)create the component animations
            Animation[] oanims = _animations;
            _animations = new Animation[config.animations.length];
            for (int ii = 0; ii < _animations.length; ii++) {
                Animation anim = (oanims == null || oanims.length <= ii) ?
                    new Animation(_ctx, this) : oanims[ii];
                _animations[ii] = anim;
                AnimationConfig.ComponentAnimation comp = config.animations[ii];
                anim.setConfig(null, comp.animation);
                anim.setSpeed(comp.speed);
                anim.setSpeedModifier(((Animation)_parentScope).getSpeedModifier());
            }
            if (oanims != null) {
                if (_aidx >= _animations.length) {
                    _aidx = 0;
                }
                for (int ii = _animations.length; ii < oanims.length; ii++) {
                    oanims[ii].dispose();
                }
            }
        }

        @Override
        public void setSpeedModifier (float speedModifier)
        {
            for (Animation anim : _animations) {
                anim.setSpeedModifier(speedModifier);
            }
        }

        @Override
        public void start ()
        {
            // initialize animation counter, start the first animation
            _animations[_aidx = 0].start();

            // blend in
            super.start();
        }

        @Override
        public boolean isPlaying ()
        {
            return super.isPlaying() && !hasCompleted();
        }

        @Override
        public boolean tick (float elapsed)
        {
            // update the weight
            super.tick(elapsed);
            if (!isPlaying()) {
                return false;
            }

            // tick the active component animation
            Animation anim = _animations[_aidx];
            anim.tick(elapsed);
            if (anim.isPlaying()) {
                return false;
            }
            _aidx++;

            // check for loop or completion
            int acount = _animations.length;
            if (_aidx >= acount) {
                if (_config.loop) {
                    _aidx %= acount;
                } else {
                    _aidx = acount - 1;
                    ((Animation)_parentScope).stopped(true);
                    return true;
                }
            }
            _animations[_aidx].start();
            return false;
        }

        @Override
        public boolean hasCompleted ()
        {
            return _animations[_aidx].hasCompleted();
        }

        @Override
        public void updateTransforms ()
        {
            _animations[_aidx].updateTransforms();
        }

        @Override
        public void blendTransforms (int update)
        {
            _animations[_aidx].blendTransforms(update);
        }

        /** The implementation configuration. */
        protected AnimationConfig.Sequential _config;

        /** The component animations. */
        protected Animation[] _animations;

        /** The index of the current animation. */
        protected int _aidx;
    }

    /** An empty array of animations. */
    public static final Animation[] EMPTY_ARRAY = new Animation[0];

    /**
     * Creates a new animation.
     */
    public Animation (GlContext ctx, Scope parentScope)
    {
        super(parentScope);
        _ctx = ctx;
    }

    /**
     * Sets the configuration of this animation.
     */
    public void setConfig (String name, String config)
    {
        setConfig(name, _ctx.getConfigManager().getConfig(AnimationConfig.class, config));
    }

    /**
     * Sets the configuration of this animation.
     */
    public void setConfig (String name, ConfigReference<AnimationConfig> ref)
    {
        setConfig(name, _ctx.getConfigManager().getConfig(AnimationConfig.class, ref));
    }

    /**
     * Sets the configuration of this animation.
     */
    public void setConfig (
        String name, String config, String firstKey, Object firstValue, Object... otherArgs)
    {
        setConfig(name, _ctx.getConfigManager().getConfig(
            AnimationConfig.class, config, firstKey, firstValue, otherArgs));
    }

    /**
     * Sets the configuration of this animation.
     */
    public void setConfig (String name, AnimationConfig config)
    {
        _name = name;
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    /**
     * Returns the name of the animation.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Sets the speed at which to play the animation.
     */
    public void setSpeed (float speed)
    {
        _speed = speed;
    }

    /**
     * Returns the modified speed at which the animation is being played.
     */
    public float getSpeed ()
    {
        return _speed * _speedModifier;
    }

    /**
     * Sets a modifier to the speed at which to play the animation.
     */
    public void setSpeedModifier (float speedModifier)
    {
        _speedModifier = speedModifier;
        _impl.setSpeedModifier(speedModifier);
    }

    /**
     * Returns the speed modifier.
     */
    @Scoped
    public float getSpeedModifier ()
    {
        return _speedModifier;
    }

    /**
     * Starts playing this animation.
     */
    public void start ()
    {
        resetEpoch();
        _lastTickTimestamp = ScopeUtil.resolveTimestamp(this, Scope.NOW).value;
        _impl.start();
    }

    /**
     * Stops playing this animation.
     */
    public void stop ()
    {
        _impl.stop();
    }

    /**
     * Stops playing this animation, blending it out over the specified interval
     * (as opposed to its default interval).
     */
    public void stop (float blendOut)
    {
        _impl.stop(blendOut);
    }

    /**
     * Determines whether this animation is playing.
     */
    public boolean isPlaying ()
    {
        return _impl.isPlaying();
    }

    /**
     * Adds an observer to this animation.
     */
    public void addObserver (AnimationObserver observer)
    {
        if (_observers == null) {
            _observers = ObserverList.newFastUnsafe();
        }
        _observers.add(observer);
    }

    /**
     * Removes an observer from this animation.
     */
    public void removeObserver (AnimationObserver observer)
    {
        if (_observers == null) {
            return;
        }
        _observers.remove(observer);
        if (_observers.isEmpty()) {
            _observers = null;
        }
    }

    /**
     * Returns the priority of this animation.
     */
    public int getPriority ()
    {
        return _impl.getPriority();
    }

    /**
     * Updates this animation based on the elapsed time in seconds.
     * NOTE: This is DIFFERENT from the typical tickable interface!
     * In our case, true = 'done animating', false = 'still animating'.
     * (Default tickable, true = 'keep ticking', false = 'stop ticking'.)
     *
     * @return true if the animation has completed.
     */
    public boolean tick (float elapsed)
    {
        // Since we may have been taken off the tickable queue, elapsed is invalid.
        // Fix elapsed based on our own internal timestamping.
        long nnow = ScopeUtil.resolveTimestamp(this, Scope.NOW).value;
        float telapsed = (nnow - _lastTickTimestamp) / 1000f;
        _lastTickTimestamp = nnow;

        // NOTE: Measuring the timestamp here is actually off by one
        // (Since elapsed measures things at the end of the frame). Because of this,
        // the potential for quirks due to animations finishing too quickly arose.
        // We fix this by buffering the measured time here to match the elapsed time.
        elapsed = _lastElapsed;
        _lastElapsed = telapsed;

        return _impl.tick(elapsed);
    }

    /**
     * Checks whether the animation has just completed, and thus should be removed from the
     * playing list after a final update has been performed.
     */
    public boolean hasCompleted ()
    {
        return _impl.hasCompleted();
    }

    /**
     * Updates the transforms directly from this animation.
     */
    public void updateTransforms ()
    {
        _impl.updateTransforms();
    }

    /**
     * Blends in the influence of this animation.
     *
     * @param update the current value of the update counter (used to determine which nodes have
     * been touched on this update).
     */
    public void blendTransforms (int update)
    {
        _impl.blendTransforms(update);
    }

    /**
     * Dumps some information about the animation to the standard output.
     */
    public void dumpInfo (String prefix)
    {
        System.out.println(prefix + (_config == null ? null : _config.getReference()));
        _impl.dumpInfo(prefix);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<AnimationConfig> event)
    {
        updateFromConfig();
    }

    @Override
    public String getScopeName ()
    {
        return "animation";
    }

    @Override
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        resetEpoch();
    }

    @Override
    public void dispose ()
    {
        super.dispose();
        if (_config != null) {
            _config.removeListener(this);
        }
    }

    @Override
    public String toString ()
    {
        return _name;
    }

    /**
     * Updates the animation to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getAnimationImplementation(_ctx, this, _impl);
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl != nimpl) {
            _impl.dispose();
            _impl = nimpl;
        }
    }

    /**
     * Notes that the animation started.
     *
     * @param overrideBlendOut if non-negative, an interval over which to blend out all
     * animations currently playing at the same priority level as this one.
     */
    protected void started (float overrideBlendOut)
    {
        // notify the containing implementation
        if (_parentScope instanceof Articulated) {
            ((Articulated)_parentScope).animationStarted(this, overrideBlendOut);
        }

        // notify observers
        applyStartedOp(_observers, this);
    }

    /**
     * Notes that the animation stopped.
     */
    protected void stopped (boolean completed)
    {
        // notify the containing implementation
        if (_parentScope instanceof Articulated) {
            ((Articulated)_parentScope).animationStopped(this, completed);
        }

        // notify observers
        applyStoppedOp(_observers, this, completed);
    }

    /**
     * Resets the epoch value to the current time.
     */
    protected void resetEpoch ()
    {
        _epoch.value = ScopeUtil.resolveTimestamp(this, Scope.NOW).value;
    }

    /**
     * Applies the {@link #_startedOp} to the supplied list of observers.
     */
    protected static void applyStartedOp (
        ObserverList<? extends AnimationObserver> observers, Animation animation)
    {
        if (observers != null) {
            _startedOp.init(animation);
            @SuppressWarnings("unchecked") ObserverList<AnimationObserver> obs =
                (ObserverList<AnimationObserver>)observers;
            obs.apply(_startedOp);
            _startedOp.clear();
        }
    }

    /**
     * Applies the {@link #_stoppedOp} to the supplied list of observers.
     */
    protected static void applyStoppedOp (
        ObserverList<? extends AnimationObserver> observers,
        Animation animation, boolean completed)
    {
        if (observers != null) {
            _stoppedOp.init(animation, completed);
            @SuppressWarnings("unchecked") ObserverList<AnimationObserver> obs =
                (ObserverList<AnimationObserver>)observers;
            obs.apply(_stoppedOp);
            _stoppedOp.clear();
        }
    }

    /**
     * Contains an executor to activate at a specific frame.
     */
    protected static class FrameExecutor
    {
        /** The frame at which to execute the action. */
        public float frame;

        /** The action executor. */
        public Executor executor;

        public FrameExecutor (float frame, Executor executor)
        {
            this.frame = frame;
            this.executor = executor;
        }
    }

    /**
     * Animation op base class.
     */
    protected static abstract class AnimationOp
        implements ObserverList.ObserverOp<AnimationObserver>
    {
        /**
         * Clears out the animation reference.
         */
        public void clear ()
        {
            _animation = null;
        }

        /** The relevant animation. */
        protected Animation _animation;
    }

    /**
     * An {@link com.samskivert.util.ObserverList.ObserverOp} that calls
     * {@link AnimationObserver#animationStarted}.
     */
    protected static class StartedOp extends AnimationOp
    {
        /**
         * (Re)initializes this op.
         */
        public void init (Animation animation)
        {
            _animation = animation;
        }

        // documentation inherited from interface ObserverOp
        public boolean apply (AnimationObserver observer)
        {
            return observer.animationStarted(_animation);
        }
    }

    /**
     * An {@link com.samskivert.util.ObserverList.ObserverOp} that calls
     * {@link AnimationObserver#animationStopped}.
     */
    protected static class StoppedOp extends AnimationOp
    {
        /**
         * (Re)initializes this op.
         */
        public void init (Animation animation, boolean completed)
        {
            _animation = animation;
            _completed = completed;
        }

        // documentation inherited from interface ObserverOp
        public boolean apply (AnimationObserver observer)
        {
            return observer.animationStopped(_animation, _completed);
        }

        /** Whether or not the animation completed. */
        protected boolean _completed;
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The name of the animation. */
    protected String _name;

    /** The configuration of this animation. */
    protected AnimationConfig _config;

    /** The speed at which to play the animation. */
    protected float _speed = 1f;

    /** A speed modifier. */
    protected float _speedModifier = 1f;

    /** The animation implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** The lazily-initialized list of animation observers. */
    protected ObserverList<AnimationObserver> _observers;

    /** The last timestamp for our tick. */
    protected long _lastTickTimestamp;

    /** The last measured elapsed time, to buffer our measurements. */
    protected float _lastElapsed;

    /** A container for the animation epoch. */
    @Scoped
    protected MutableLong _epoch = new MutableLong(System.currentTimeMillis());

    /** Started op to reuse. */
    protected static StartedOp _startedOp = new StartedOp();

    /** Stopped op to reuse. */
    protected static StoppedOp _stoppedOp = new StoppedOp();

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null, null) {
    };
}
