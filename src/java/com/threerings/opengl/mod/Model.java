//
// $Id$

package com.threerings.opengl.mod;

import java.util.Map;

import com.samskivert.util.ObjectUtil;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.ScopeUpdateListener;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Ray;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.model.config.ModelConfig.Imported.MaterialMapping;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Intersectable;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

import static com.threerings.opengl.Log.*;

/**
 * A 3D model.
 */
public class Model extends DynamicScope
    implements Tickable, Intersectable, Renderable,
        ConfigUpdateListener<ModelConfig>
{
    /**
     * The actual model implementation.
     */
    public static abstract class Implementation extends SimpleScope
        implements Tickable, Intersectable, Renderable
    {
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Attaches the specified model at the given point.
         *
         * @param replace if true, replace any existing attachments at the point.
         */
        public void attach (String point, Model model, boolean replace)
        {
            log.warning("Attachment not supported.", "point", point, "model", model);
        }

        /**
         * Detaches an attached model.
         */
        public void detach (Model model)
        {
            log.warning("Model not attached.", "model", model);
        }

        /**
         * Detaches any models attached to the specified point.
         */
        public void detachAll (String point)
        {
            // nothing by default
        }

        /**
         * Returns the model's list of animations.
         */
        public Animation[] getAnimations ()
        {
            return null;
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

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "impl";
        }

        /**
         * Creates a set of surfaces.
         */
        protected Surface[] createSurfaces (
            GlContext ctx, Scope scope, VisibleMesh[] meshes, MaterialMapping[] materialMappings,
            Map<String, MaterialConfig> materialConfigs)
        {
            Surface[] surfaces = new Surface[meshes.length];
            for (int ii = 0; ii < meshes.length; ii++) {
                surfaces[ii] = createSurface(
                    ctx, scope, meshes[ii], materialMappings, materialConfigs);
            }
            return surfaces;
        }

        /**
         * Creates a single surface.
         */
        protected static Surface createSurface (
            GlContext ctx, Scope scope, VisibleMesh mesh, MaterialMapping[] materialMappings,
            Map<String, MaterialConfig> materialConfigs)
        {
            return new Surface(ctx, scope, mesh.geometry,
                getMaterialConfig(ctx, mesh.texture, mesh.tag, materialMappings, materialConfigs));
        }

        /**
         * Resolves a material config through a cache.
         */
        protected static MaterialConfig getMaterialConfig (
            GlContext ctx, String texture, String tag, MaterialMapping[] materialMappings,
            Map<String, MaterialConfig> materialConfigs)
        {
            String key = texture + "|" + tag;
            MaterialConfig config = materialConfigs.get(key);
            if (config == null) {
                materialConfigs.put(
                    key, config = getMaterialConfig(ctx, texture, tag, materialMappings));
            }
            return config;
        }

        /**
         * Resolves a material config.
         */
        protected static MaterialConfig getMaterialConfig (
            GlContext ctx, String texture, String tag, MaterialMapping[] materialMappings)
        {
            for (MaterialMapping mapping : materialMappings) {
                if (ObjectUtil.equals(texture, mapping.texture) && tag.equals(mapping.tag)) {
                    return (mapping.material == null) ? null :
                        ctx.getConfigManager().getConfig(MaterialConfig.class, mapping.material);
                }
            }
            return null;
        }
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
        super("model");
        _ctx = ctx;
        setConfig(config);
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
    public void setConfig (ConfigReference<ModelConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(ModelConfig.class, ref));
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
     * Resets the configuration of this model to the null configuration.
     */
    public void clearConfig ()
    {
        setConfig((ModelConfig)null);
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
     * Sets the model's fog state.
     */
    public void setFogState (FogState state)
    {
        if (_fogState != state) {
            _fogState = state;
            wasUpdated();
        }
    }

    /**
     * Returns a reference to the model's fog state.
     */
    public FogState getFogState ()
    {
        return _fogState;
    }

    /**
     * Sets the model's light state.
     */
    public void setLightState (LightState state)
    {
        if (_lightState != state) {
            _lightState = state;
            wasUpdated();
        }
    }

    /**
     * Returns a reference to the model's light state.
     */
    public LightState getLightState ()
    {
        return _lightState;
    }

    /**
     * Attaches the specified model at the given point.
     */
    public void attach (String point, Model model)
    {
        attach(point, model, true);
    }

    /**
     * Attaches the specified model at the given point.
     *
     * @param replace if true, replace any existing attachments at the point.
     */
    public void attach (String point, Model model, boolean replace)
    {
        _impl.attach(point, model, replace);
    }

    /**
     * Detaches an attached model.
     */
    public void detach (Model model)
    {
        _impl.detach(model);
    }

    /**
     * Detaches any models attached to the specified point.
     */
    public void detachAll (String point)
    {
        _impl.detachAll(point);
    }

    /**
     * Returns a reference to this model's list of animations (or <code>null</code> if the
     * model does not support animation).
     */
    public Animation[] getAnimations ()
    {
        return _impl.getAnimations();
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
            this, Scope.NOW, new MutableLong(System.currentTimeMillis()));
        _epoch.value = now.value;
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getModelImplementation(_ctx, this, _impl);
        _impl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
    }

    /** The application context. */
    protected GlContext _ctx;

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

    /** The model's fog state. */
    @Scoped
    protected FogState _fogState;

    /** The model's light state. */
    @Scoped
    protected LightState _lightState;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
        public void enqueue () { }
    };
}
