//
// $Id$

package com.threerings.opengl.mod;

import java.util.ArrayList;

import com.samskivert.util.ObserverList;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Function;
import com.threerings.expr.ObjectExpression.Evaluator;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;

import com.threerings.opengl.model.config.AnimationConfig;
import com.threerings.opengl.util.GlContext;

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
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Starts the animation.
         */
        public void start ()
        {
            // blend in
            blendToWeight(_config.weight, _config.blendIn);

            // notify containing animation
            ((Animation)_parentScope).started();
        }

        /**
         * Stops the animation.
         */
        public void stop ()
        {
            // blend out
            blendToWeight(0f, _config.blendOut);
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

        @Override // documentation inherited
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
        public Imported (Scope parentScope, AnimationConfig.Imported config)
        {
            super(parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (AnimationConfig.Imported config)
        {
            super.setConfig(_config = config);

            // resolve the targets
            _targets = new Articulated.Node[config.targets.length];
            Function getNode = ScopeUtil.resolve(_parentScope, "getNode", Function.NULL);
            for (int ii = 0; ii < _targets.length; ii++) {
                _targets[ii] = (Articulated.Node)getNode.call(config.targets[ii]);
            }

        }

        @Override // documentation inherited
        public void start ()
        {
            _fidx = 0;
            _accum = 0f;
            _completed = false;

            super.start();
        }

        @Override // documentation inherited
        public void stop ()
        {
            super.stop();
        }



        /** The implementation configuration. */
        protected AnimationConfig.Imported _config;

        /** The targets of the animation. */
        protected Articulated.Node[] _targets;

        /** A snapshot of the original transforms of the targets, for transitioning. */
        protected Transform3D[] _snapshot;

        /** The time remaining until we have to start blending the animation out. */
        protected float _countdown;

        /** The index of the current animation frame. */
        protected int _fidx;

        /** The progress towards the next frame. */
        protected float _accum;

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
        public Procedural (Scope parentScope, AnimationConfig.Procedural config)
        {
            super(parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (AnimationConfig.Procedural config)
        {
            super.setConfig(_config = config);

            // create the target transforms
            Function getNode = ScopeUtil.resolve(_parentScope, "getNode", Function.NULL);
            _transforms = new TargetTransform[config.transforms.length];
            for (int ii = 0; ii < _transforms.length; ii++) {
                AnimationConfig.TargetTransform transform = config.transforms[ii];
                ArrayList<Articulated.Node> targets = new ArrayList<Articulated.Node>();
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
        }

        /** The implementation configuration. */
        protected AnimationConfig.Procedural _config;

        /** The target transforms. */
        protected TargetTransform[] _transforms;
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
    public void setConfig (String name, ConfigReference<AnimationConfig> ref)
    {
        setConfig(name, _ctx.getConfigManager().getConfig(AnimationConfig.class, ref));
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
     * Starts playing this animation.
     */
    public void start ()
    {
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
     *
     * @return true if the animation has completed.
     */
    public boolean tick (float elapsed)
    {
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

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<AnimationConfig> event)
    {
        updateFromConfig();
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "animation";
    }

    @Override // documentation inherited
    public String toString ()
    {
        return _name;
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getAnimationImplementation(_ctx, this, _impl);
        _impl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
    }

    /**
     * Notes that the animation started.
     */
    protected void started ()
    {
        // notify the containing implementation
        ((Articulated)_parentScope).animationStarted(this);

        // notify observers
        applyStartedOp(_observers, this);
    }

    /**
     * Notes that the animation stopped.
     */
    protected void stopped (boolean completed)
    {
        // notify the containing implementation
        ((Articulated)_parentScope).animationStopped(this, completed);

        // notify observers
        applyStoppedOp(_observers, this, completed);
    }

    /**
     * Applies the {@link #_startedOp} to the supplied list of observers.
     */
    protected static void applyStartedOp (
        ObserverList<AnimationObserver> observers, Animation animation)
    {
        if (observers != null) {
            _startedOp.init(animation);
            observers.apply(_startedOp);
        }
    }

    /**
     * Applies the {@link #_stoppedOp} to the supplied list of observers.
     */
    protected static void applyStoppedOp (
        ObserverList<AnimationObserver> observers, Animation animation, boolean completed)
    {
        if (observers != null) {
            _stoppedOp.init(animation, completed);
            observers.apply(_stoppedOp);
        }
    }

    /**
     * An {@link ObserverList.ObserverOp} that calls {@link AnimationObserver#animationStarted}.
     */
    protected static class StartedOp
        implements ObserverList.ObserverOp<AnimationObserver>
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

        /** The started animation. */
        protected Animation _animation;
    }

    /**
     * An {@link ObserverList.ObserverOp} that calls {@link AnimationObserver#animationStopped}.
     */
    protected static class StoppedOp
        implements ObserverList.ObserverOp<AnimationObserver>
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

        /** The started animation. */
        protected Animation _animation;

        /** Whether or not the animation completed. */
        protected boolean _completed;
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The name of the animation. */
    protected String _name;

    /** The configuration of this animation. */
    protected AnimationConfig _config;

    /** The animation implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** The lazily-initialized list of animation observers. */
    protected ObserverList<AnimationObserver> _observers;

    /** Started op to reuse. */
    protected static StartedOp _startedOp = new StartedOp();

    /** Stopped op to reuse. */
    protected static StoppedOp _stoppedOp = new StoppedOp();

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
