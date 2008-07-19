//
// $Id$

package com.threerings.opengl.mod;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.ScopeUpdateListener;
import com.threerings.expr.Scoped;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Ray;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Intersectable;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

/**
 * A 3D model.
 */
public class Model
    implements Tickable, Intersectable, Renderable,
        ConfigUpdateListener<ModelConfig>, ScopeUpdateListener
{
    /**
     * The actual model implementation.
     */
    public static abstract class Implementation
        implements Scope, Tickable, Intersectable, Renderable
    {
        public Implementation (Scope parentScope)
        {
            _parentScope = parentScope;
        }

        // documentation inherited from interface Scope
        public String getScopeName ()
        {
            return "impl";
        }

        // documentation inherited from interface Scope
        public Scope getParentScope ()
        {
            return _parentScope;
        }

        // documentation inherited from interface Scope
        public <T> T get (String name, Class<T> clazz)
        {
            return ScopeUtil.get(this, name, clazz);
        }

        // documentation inherited from interface Scope
        public void addListener (ScopeUpdateListener listener)
        {
            _parentScope.addListener(listener);
        }

        // documentation inherited from interface Scope
        public void removeListener (ScopeUpdateListener listener)
        {
            _parentScope.removeListener(listener);
        }

        // documentation inherited from interface Tickable
        public void tick (float elapsed)
        {
            // nothing by default
        }

        // documentation inherited from interface Intersectable
        public boolean getIntersection (Ray ray, Vector3f result)
        {
            return false;
        }

        /** A reference to the parent scope. */
        protected Scope _parentScope;
    }

    /**
     * Creates a new model with a null configuration.
     */
    public Model (GlContext ctx)
    {
        this(ctx, (ModelConfig)null);
    }

    /**
     * Creates a new model with the named configuration.
     */
    public Model (GlContext ctx, String name)
    {
        this(ctx, ctx.getConfigManager().getConfig(ModelConfig.class, name));
    }

    /**
     * Creates a new model with the named configuration and arguments.
     */
    public Model (
        GlContext ctx, String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        this(ctx, ctx.getConfigManager().getConfig(
            ModelConfig.class, name, firstKey, firstValue, otherArgs));
    }

    /**
     * Creates a new model with the given configuration.
     */
    public Model (GlContext ctx, ModelConfig config)
    {
        _ctx = ctx;
        _scope.addListener(this);
        setConfig(config);
    }

    /**
     * Returns a reference to the model scope.
     */
    public DynamicScope getScope ()
    {
        return _scope;
    }

    /**
     * Returns a reference to the model's local transform.
     */
    public Transform3D getLocalTransform ()
    {
        return _localTransform;
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (String name)
    {
        setConfig(_ctx.getConfigManager().getConfig(ModelConfig.class, name));
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        setConfig(_ctx.getConfigManager().getConfig(
            ModelConfig.class, name, firstKey, firstValue, otherArgs));
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (ModelConfig config)
    {
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    /**
     * Returns a reference to this model's configuration.
     */
    public ModelConfig getConfig ()
    {
        return _config;
    }

    /**
     * Resets the state of this model.
     */
    public void reset ()
    {
        resetEpoch();
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        _impl.tick(elapsed);
    }

    // documentation inherited from interface Intersectable
    public boolean getIntersection (Ray ray, Vector3f result)
    {
        return _impl.getIntersection(ray, result);
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        _impl.enqueue();
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ModelConfig> event)
    {
        updateFromConfig();
    }

    // documentation inherited from interface ScopeUpdateListener
    public void scopeUpdated (ScopeEvent event)
    {
        resetEpoch();
    }

    /**
     * Resets the model epoch to the current time.
     */
    protected void resetEpoch ()
    {
        MutableLong now = ScopeUtil.resolve(
            _scope, Scope.NOW, new MutableLong(System.currentTimeMillis()));
        _epoch.value = now.value;
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getModelImplementation(_ctx, _scope, _impl);
        _impl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The expression scope. */
    protected DynamicScope _scope = new DynamicScope(this, "model");

    /** The configuration of this model. */
    protected ModelConfig _config;

    /** The model implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** A container for the model epoch. */
    @Scoped
    protected MutableLong _epoch = new MutableLong(System.currentTimeMillis());

    /** The model's local transform. */
    @Scoped
    protected Transform3D _localTransform = new Transform3D(Transform3D.UNIFORM);

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
        public void enqueue () { }
    };
}
