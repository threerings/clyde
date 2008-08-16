//
// $Id$

package com.threerings.opengl.mod;

import java.util.ArrayList;

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
import com.threerings.opengl.util.Tickable;

/**
 * An animation for an {@link Articulated} model.
 */
public class Animation extends SimpleScope
    implements Tickable, ConfigUpdateListener<AnimationConfig>
{
    /**
     * The actual animation implementation.
     */
    public static abstract class Implementation extends SimpleScope
        implements Tickable
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
            // nothing by default
        }

        /**
         * Stops the animation.
         */
        public void stop ()
        {
            // nothing by default
        }

        // documentation inherited from interface Tickable
        public void tick (float elapsed)
        {
            // nothing by default
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "impl";
        }
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
            _config = config;

            // resolve the targets
            _targets = new Articulated.Node[config.targets.length];
            Function getNode = ScopeUtil.resolve(_parentScope, "getNode", Function.NULL);
            for (int ii = 0; ii < _targets.length; ii++) {
                _targets[ii] = (Articulated.Node)getNode.call(config.targets[ii]);
            }

        }

        /** The implementation configuration. */
        protected AnimationConfig.Imported _config;

        /** The targets of the animation. */
        protected Articulated.Node[] _targets;
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
            _config = config;

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

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        _impl.tick(elapsed);
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

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getAnimationImplementation(_ctx, this, _impl);
        _impl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The name of the animation. */
    protected String _name;

    /** The configuration of this animation. */
    protected AnimationConfig _config;

    /** The animation implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
