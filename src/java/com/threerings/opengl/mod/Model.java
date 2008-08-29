//
// $Id$

package com.threerings.opengl.mod;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ObserverList;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigManager;
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
import com.threerings.math.Box;
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
import com.threerings.opengl.util.GlContextWrapper;
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
         * Returns a list of all animations currently playing.
         */
        public List<Animation> getPlayingAnimations ()
        {
            return Collections.emptyList();
        }

        /**
         * Retrieves an animation by name.
         */
        public Animation getAnimation (String name)
        {
            return null;
        }

        /**
         * Returns the model's list of animations.
         */
        public Animation[] getAnimations ()
        {
            return Animation.EMPTY_ARRAY;
        }

        /**
         * Determines whether the model (such as a transient effect) has completed.
         */
        public boolean hasCompleted ()
        {
            return false;
        }

        /**
         * Resets the state of this model.
         */
        public void reset ()
        {
            // nothing by default
        }

        /**
         * Returns a reference to the bounds of the model.
         */
        public Box getBounds ()
        {
            return Box.EMPTY;
        }

        /**
         * Updates the bounds of the model.
         */
        public void updateBounds ()
        {
            // nothing by default
        }

        /**
         * Draws the bounds of the model in immediate mode.
         */
        public void drawBounds ()
        {
            // nothing by default
        }

        /**
         * Checks whether the model requires a per-frame call to its {@link #tick} method.
         */
        public boolean requiresTick ()
        {
            return false;
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
                    if (mapping.material == null) {
                        return null;
                    }
                    MaterialConfig config = ctx.getConfigManager().getConfig(
                        MaterialConfig.class, mapping.material);
                    if (config == null) {
                        log.warning("Missing material for mapping.", "material", mapping.material);
                    }
                    return config;
                }
            }
            log.warning("No material mapping found.", "texture", texture, "tag", tag);
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
        _ctx = new GlContextWrapper(ctx) {
            public ConfigManager getConfigManager () {
                return (_config == null) ?
                    _wrapped.getConfigManager() : _config.getConfigManager();
            }
        };
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
     * Starts an animation by name.
     */
    public void startAnimation (String name)
    {
        Animation animation = getAnimation(name);
        if (animation != null) {
            animation.start();
        } else {
            log.warning("Animation not found.", "name", name);
        }
    }

    /**
     * Stops an animation by name.
     */
    public void stopAnimation (String name)
    {
        Animation animation = getAnimation(name);
        if (animation != null) {
            animation.stop();
        }
    }

    /**
     * Stops all animations playing at the specified priority level, blending them out over the
     * specified interval.
     */
    public void stopAnimations (int priority, float blendOut)
    {
        List<Animation> playing = getPlayingAnimations();
        for (int ii = 0, nn = playing.size(); ii < nn; ii++) {
            Animation anim = playing.get(ii);
            if (anim.getPriority() == priority) {
                anim.stop(blendOut);
            }
        }
    }

    /**
     * Stops all animations currently playing.
     */
    public void stopAllAnimations ()
    {
        List<Animation> playing = getPlayingAnimations();
        for (int ii = 0, nn = playing.size(); ii < nn; ii++) {
            playing.get(ii).stop();
        }
    }

    /**
     * Returns a list containing all animations currently playing on this model.
     */
    public List<Animation> getPlayingAnimations ()
    {
        return _impl.getPlayingAnimations();
    }

    /**
     * Checks whether the named animation is playing.
     */
    public boolean isAnimationPlaying (String name)
    {
        Animation animation = getAnimation(name);
        return animation != null && animation.isPlaying();
    }

    /**
     * Retrieves an animation by name.
     */
    public Animation getAnimation (String name)
    {
        return _impl.getAnimation(name);
    }

    /**
     * Returns a reference to this model's list of animations.
     */
    public Animation[] getAnimations ()
    {
        return _impl.getAnimations();
    }

    /**
     * Determines whether this model (such as a transient effect) has completed.
     */
    public boolean hasCompleted ()
    {
        return _impl.hasCompleted();
    }

    /**
     * Adds an observer to this model.
     */
    public void addObserver (ModelObserver observer)
    {
        if (_observers == null) {
            _observers = ObserverList.newFastUnsafe();
        }
        _observers.add(observer);
    }

    /**
     * Removes an observer from this model.
     */
    public void removeObserver (ModelObserver observer)
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
     * Resets the state of this model.
     */
    public void reset ()
    {
        resetEpoch();
        _impl.reset();
    }

    /**
     * Returns a reference to the bounds of the model.
     */
    public Box getBounds ()
    {
        return _impl.getBounds();
    }

    /**
     * Updates the bounds of the model.
     */
    public void updateBounds ()
    {
        _impl.updateBounds();
    }

    /**
     * Draws the bounds of the model in immediate mode.
     */
    public void drawBounds ()
    {
        _impl.drawBounds();
    }

    /**
     * Checks whether the model requires a per-frame call to the {@link #tick} method to
     * advance its state.
     */
    public boolean requiresTick ()
    {
        return _impl.requiresTick();
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

    @Override // documentation inherited
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        resetEpoch();
    }

    @Override // documentation inherited
    public void dispose ()
    {
        super.dispose();
        if (_config != null) {
            _config.removeListener(this);
        }
    }

    /**
     * Notes that the model has completed.  This should only be called by the {@link #_impl}.
     */
    public void completed ()
    {
        if (_observers != null) {
            _completedOp.init(this);
            _observers.apply(_completedOp);
            _completedOp.clear();
        }
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

    /**
     * Notifies the listeners that an animation has started.
     */
    protected void animationStarted (Animation animation)
    {
        Animation.applyStartedOp(_observers, animation);
    }

    /**
     * Notifies the listeners that an animation has stopped.
     */
    protected void animationStopped (Animation animation, boolean completed)
    {
        Animation.applyStoppedOp(_observers, animation, completed);
    }

    /**
     * An {@link ObserverList.ObserverOp} that calls {@link ModelObserver#modelCompleted}.
     */
    protected static class CompletedOp
        implements ObserverList.ObserverOp<ModelObserver>
    {
        /**
         * (Re)initializes this op.
         */
        public void init (Model model)
        {
            _model = model;
        }

        /**
         * Clears out the model reference.
         */
        public void clear ()
        {
            _model = null;
        }

        // documentation inherited from interface ObserverOp
        public boolean apply (ModelObserver observer)
        {
            return observer.modelCompleted(_model);
        }

        /** The completed model. */
        protected Model _model;
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The configuration of this model. */
    protected ModelConfig _config;

    /** The model implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** The lazily-initialized list of model observers. */
    protected ObserverList<ModelObserver> _observers;

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

    /** Completed op to reuse. */
    protected static CompletedOp _completedOp = new CompletedOp();

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
        public void enqueue () { }
    };
}
