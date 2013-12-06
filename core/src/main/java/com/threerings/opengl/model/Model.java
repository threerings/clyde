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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.google.common.base.Objects;

import com.samskivert.util.ObserverList;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeUpdateListener;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.material.Projection;
import com.threerings.opengl.material.Surface;
import com.threerings.opengl.material.config.GeometryMaterial;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.config.AnimationConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.model.config.ModelConfig.Imported.MaterialMapping;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.scene.SceneElement;
import com.threerings.opengl.scene.SceneInfluenceSet;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.GlContextWrapper;
import com.threerings.opengl.util.Intersectable;
import com.threerings.opengl.util.Tickable;

import static com.threerings.opengl.Log.log;

/**
 * A 3D model.
 */
public class Model extends DynamicScope
    implements SceneElement, ConfigUpdateListener<ModelConfig>
{
    @Override
    public void addListener (ScopeUpdateListener listener)
    {
        if (listener instanceof Animation.Procedural) {
            // NOTE: This relies on the fact that DynamicScope will run
            // through its listeners in reverse order, thereby making
            // this the last operation run. This is important because
            // procedural animations have a reliance on their parents'
            // timestamps being updated prior to their own.
            super.addListener(0, listener);
        } else {
            super.addListener(listener);
        }
    }
    /**
    * The actual model implementation.
    */
    public static abstract class Implementation extends SimpleScope
        implements Tickable, Intersectable, Compositable
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Gets a list of children from this implementation.
         */
        public List<Model> getChildren ()
        {
            return Collections.<Model>emptyList();
        }

        /**
         * Returns a reference to the world transform of the given point.
         */
        public Transform3D getPointWorldTransform (String point)
        {
            return null;
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
         * Creates an animation for the model or returns <code>null</code> if not supported.
         */
        public Animation createAnimation ()
        {
            return null;
        }

        /**
         * Determines whether the model (such as a transient effect) has completed.
         */
        public boolean hasCompleted ()
        {
            return false;
        }

        /**
         * Set this model's visibility.
         */
        public void setVisible (boolean visible)
        {
            // Model will not composite() us if it is not visible, so typically nothing
            // is needed here. Certain subclasses need this method, however.
        }

        /**
         * Handles events where visibility of a parent has been set.
         */
        public void visibilityWasSet ()
        {
            // nothing by default
        }

        /**
         * Resets the state of this model.
         */
        public void reset ()
        {
            // nothing by default
        }

        /**
         * Returns a set of flags indicating the types of influences that affect the model.
         */
        public int getInfluenceFlags ()
        {
            return 0;
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
         * Dumps some information about the model to the standard output.
         */
        public void dumpInfo (String prefix)
        {
            // nothing by default
        }

        /**
         * Sets the tick policy of the model.
         */
        public void setTickPolicy (TickPolicy policy)
        {
            log.warning("Setting tick policy not supported.", "policy", policy);
        }

        /**
         * Returns the transient policy of the model.
         */
        public ModelConfig.TransientPolicy getTransientPolicy ()
        {
            return ModelConfig.TransientPolicy.ALWAYS;
        }

        /**
         * Returns the tick policy of the model.
         */
        public TickPolicy getTickPolicy ()
        {
            return TickPolicy.NEVER;
        }

        /**
         * Notes that the model was added to a scene.
         */
        public void wasAdded ()
        {
            // nothing by default
        }

        /**
         * Notes that the model will be removed from the scene.
         */
        public void willBeRemoved ()
        {
            // nothing by default
        }

        /**
         * Returns true if this is the implementation.
         */
        public boolean isImplementation (Model.Implementation impl)
        {
            return this == impl;
        }

        // documentation inherited from interface Tickable
        public void tick (float elapsed)
        {
            updateBounds();
        }

        // documentation inherited from interface Intersectable
        public boolean getIntersection (Ray3D ray, Vector3f result)
        {
            return false;
        }

        // documentation inherited from interface Compositable
        public void composite ()
        {
            // nothing by default
        }

        @Override
        public String getScopeName ()
        {
            return "impl";
        }

        /**
         * Creates a set of surfaces.
         */
        protected static Surface[] createSurfaces (
            GlContext ctx, Scope scope, GeometryMaterial[] gmats)
        {
            Surface[] surfaces = new Surface[gmats.length];
            for (int ii = 0; ii < gmats.length; ii++) {
                GeometryMaterial gmat = gmats[ii];
                surfaces[ii] = new Surface(ctx, scope, gmat.geometry, gmat.material);
            }
            return surfaces;
        }

        /**
         * Creates a set of surfaces.
         */
        protected static Surface[] createSurfaces (
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
    }

    /** A flag indicating that the model implementation relies on the fog state. */
    public static final int FOG_INFLUENCE = (1 << 0);

    /** A flag indicating that the model implementation relies on the light state. */
    public static final int LIGHT_INFLUENCE = (1 << 1);

    /** A flag indicating that the model implementation relies on the projections. */
    public static final int PROJECTION_INFLUENCE = (1 << 2);

    /** A flag indicating that the model implementation relies on the definitions. */
    public static final int DEFINITION_INFLUENCE = (1 << 3);

    /**
     * Resolves a material config through a cache.
     */
    public static MaterialConfig getMaterialConfig (
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
     * Creates a new model with the referenced configuration.
     */
    public Model (GlContext ctx, ConfigReference<ModelConfig> ref)
    {
        this(ctx, ctx.getConfigManager().getConfig(ModelConfig.class, ref));
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
     * Gets the children for this model.
     */
    public List<Model> getChildren ()
    {
        return _impl.getChildren();
    }

    /**
     * Sets the local transform to the specified value and promotes it to
     * {@link Transform3D#UNIFORM}, then updates the bounds of the model.
     */
    public void setLocalTransform (Transform3D transform)
    {
        _localTransform.set(transform);
        _localTransform.promote(Transform3D.UNIFORM);
        updateBounds();
    }

    /**
     * Returns a reference to the model's local transform.
     */
    public Transform3D getLocalTransform ()
    {
        return _localTransform;
    }

    /**
     * Returns a reference to the world transform of the model point.
     */
    public Transform3D getPointWorldTransform (String point)
    {
        return _impl.getPointWorldTransform(point);
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
        if (_config == config) {
            return;
        }
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
     * Sets the model's render scheme.
     */
    public void setRenderScheme (String scheme)
    {
        if (!Objects.equal(_renderScheme, scheme)) {
            _renderScheme = scheme;
            wasUpdated();
        }
    }

    /**
     * Returns the model's render scheme.
     */
    public String getRenderScheme ()
    {
        return _renderScheme;
    }

    /**
     * Sets the model's visibility flag.
     */
    public void setVisible (boolean visible)
    {
        boolean oldVis = _visible;
        _visible = visible;
        _impl.setVisible(visible);
        if (oldVis != visible) {
            visibilityWasSet();
        }
    }

    /**
     * Notifies children that visibility was set.
     */
    public void visibilityWasSet ()
    {
        _impl.visibilityWasSet();

        List<Model> children = getChildren();
        for (Model m : children) {
            m.visibilityWasSet();
        }
    }

    /**
     * Returns the value of the model's visibility flag.
     */
    public boolean isVisible ()
    {
        return _visible;
    }

    /**
     * Check if we're 'showing' - us visible && parents showing.
     */
    public boolean isShowing ()
    {
        // Manually short-circuit.
        if (!_visible) {
            return _visible;
        }

        Scope curScope = _parentScope;
        boolean parentVis = true;
        while (curScope != null && !(curScope instanceof Model)) {
            curScope = curScope.getParentScope();
        }
        if (curScope instanceof Model) {
            parentVis = ((Model)curScope).isShowing();
        }

        return parentVis;
    }

    /**
     * Sets the model's color state.
     */
    public void setColorState (ColorState state)
    {
        if (_colorState != state) {
            _colorState = state;
            wasUpdated();
        }
    }

    /**
     * Returns a reference to the model's color state.
     */
    public ColorState getColorState ()
    {
        return _colorState;
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
     * Creates an animation for this model.
     */
    public Animation createAnimation (String name)
    {
        Animation anim = createAnimation();
        if (anim != null) {
            anim.setConfig(null, name);
        }
        return anim;
    }

    /**
     * Creates an animation for this model.
     */
    public Animation createAnimation (ConfigReference<AnimationConfig> ref)
    {
        Animation anim = createAnimation();
        if (anim != null) {
            anim.setConfig(null, ref);
        }
        return anim;
    }

    /**
     * Creates an animation for this model.
     */
    public Animation createAnimation (
        String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        Animation anim = createAnimation();
        if (anim != null) {
            anim.setConfig(null, name, firstKey, firstValue, otherArgs);
        }
        return anim;
    }

    /**
     * Creates an unconfigured animation for the model.
     */
    public Animation createAnimation ()
    {
        return _impl.createAnimation();
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
     * Dumps some information about the model to the standard output.
     */
    public void dumpInfo (String prefix)
    {
        System.out.println(prefix + (_config == null ? null : _config.getReference()));
        _impl.dumpInfo(prefix);
    }

    /**
     * Sets the tick policy of the model.
     */
    public void setTickPolicy (TickPolicy policy)
    {
        _impl.setTickPolicy(policy);
    }

    /**
     * Sets the model's user object reference.
     */
    public void setUserObject (Object object)
    {
        _userObject = object;
    }

    /**
     * Returns a reference to the scene to which this model was added, or null for none.
     */
    public Scene getScene ()
    {
        return _scene;
    }

    /**
     * Returns the transient creation policy.
     */
    public ModelConfig.TransientPolicy getTransientPolicy ()
    {
        return _impl.getTransientPolicy();
    }

    // documentation inherited from interface SceneElement
    public TickPolicy getTickPolicy ()
    {
        return _impl.getTickPolicy();
    }

    // documentation inherited from interface SceneElement
    public Object getUserObject ()
    {
        return _userObject;
    }

    // documentation inherited from interface SceneElement
    public Box getBounds ()
    {
        return _impl.getBounds();
    }

    // documentation inherited from interface SceneElement
    public void wasAdded (Scene scene)
    {
        _scene = scene;
        _impl.wasAdded();
    }

    // documentation inherited from interface SceneElement
    public void willBeRemoved ()
    {
        _impl.willBeRemoved();
        _scene = null;
    }

    // documentation inherited from interface SceneElement
    public void setInfluences (SceneInfluenceSet influences)
    {
        int flags = _impl.getInfluenceFlags();
        boolean influenceable = (flags != 0);
        if (influenceable ? _influences.equals(influences) : _influences.isEmpty()) {
            return;
        }
        _influences.clear();
        if (influenceable) {
            _influences.addAll(influences);
        }

        // process the influences
        Box bounds = getBounds();
        boolean updated = false;
        FogState fogState = ((flags & FOG_INFLUENCE) == 0) ?
            null : _influences.getFogState(bounds, _fogState);
        if (_fogState != fogState) {
            _fogState = fogState;
            updated = true;
        }
        LightState lightState = ((flags & LIGHT_INFLUENCE) == 0) ?
            null : _influences.getLightState(bounds, _lightState);
        if (_lightState != lightState) {
            _lightState = lightState;
            updated = true;
        }
        Projection[] projections = ((flags & PROJECTION_INFLUENCE) == 0) ?
            null : _influences.getProjections(_projections);
        if (_projections != projections) {
            _projections = projections;
            updated = true;
        }
        Map<String, Object> definitions = ((flags & DEFINITION_INFLUENCE) == 0) ?
            null : _influences.getDefinitions(_definitions);
        if (_definitions != definitions) {
            _definitions = definitions;
            updated = true;
        }

        if (updated) {
            wasUpdated();
        }
    }

    // documentation inherited from interface SceneElement
    public boolean isInfluenceable ()
    {
        return _impl.getInfluenceFlags() != 0;
    }

    // documentation inherited from interface SceneElement
    public boolean updateLastVisit (int visit)
    {
        if (_lastVisit == visit) {
            return false;
        }
        _lastVisit = visit;
        return true;
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        _impl.tick(elapsed);
    }

    // documentation inherited from interface Intersectable
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        return _impl.getIntersection(ray, result);
    }

    // documentation inherited from interface Compositable
    public void composite ()
    {
        if (_visible) {
            _impl.composite();
        }
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ModelConfig> event)
    {
        updateFromConfig();
    }

    @Override
    public <T> T get (String name, Class<T> clazz)
    {
        // first dynamic, then reflective, then scene-defined
        T result = super.get(name, clazz);
        if (result != null || _definitions == null) {
            return result;
        }
        Object value = _definitions.get(name);
        return clazz.isInstance(value) ? clazz.cast(value) : null;
    }

    @Override
    public void wasUpdated ()
    {
        super.wasUpdated();
        if (_impl != null && _compoundDepth == 0) {
            updateFromConfig();
        }
    }

    @Override
    public void dispose ()
    {
        super.dispose();
        _impl.dispose();
        if (_config != null) {
            _config.removeListener(this);
        }
    }

    /**
     * Returns a reference to the scene containing the model, if any.  This should only be called
     * by the {@link #_impl}.
     */
    public Scene getScene (Implementation impl)
    {
        return (_impl.isImplementation(impl)) ? _scene : null;
    }

    /**
     * Notes that the model has completed.  This should only be called by the {@link #_impl}.
     */
    public void completed (Implementation impl)
    {
        if (_observers != null && _impl.isImplementation(impl)) {
            _completedOp.init(this);
            _observers.apply(_completedOp);
            _completedOp.clear();
        }
    }

    /**
     * Notes that the tick policy will change.  Should only be called by the {@link #_impl}.
     */
    public void tickPolicyWillChange (Implementation impl)
    {
        if (_scene != null && _parentScope == _scene && _impl.isImplementation(impl)) {
            _scene.tickPolicyWillChange(this);
        }
    }

    /**
     * Notes that the tick policy has changed.  Should only be called by the {@link #_impl}.
     */
    public void tickPolicyDidChange (Implementation impl)
    {
        if (_scene != null && _parentScope == _scene && _impl.isImplementation(impl)) {
            _scene.tickPolicyDidChange(this);
        }
    }

    /**
     * Notes that the bounds will change.  Should only be called by the {@link #_impl}.
     */
    public void boundsWillChange (Implementation impl)
    {
        if (_scene != null && _parentScope == _scene && _impl.isImplementation(impl)) {
            _scene.boundsWillChange(this);
        }
    }

    /**
     * Notes that the bounds have changed.  Should only be called by the {@link #_impl}.
     */
    public void boundsDidChange (Implementation impl)
    {
        if (_scene != null && _parentScope == _scene && _impl.isImplementation(impl)) {
            _scene.boundsDidChange(this);
        }
    }

    /**
     * Resets the model epoch to the current time.
     */
    protected void resetEpoch ()
    {
        _epoch.value = ScopeUtil.resolveTimestamp(this, Scope.NOW).value;
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getModelImplementation(_ctx, this, _impl);
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl == nimpl) {
            return;
        }
        boolean tickPolicyChanging = (_impl.getTickPolicy() != nimpl.getTickPolicy());
        boolean boundsChanging = !_impl.getBounds().equals(nimpl.getBounds());
        if (tickPolicyChanging) {
            tickPolicyWillChange(_impl);
        }
        if (boundsChanging) {
            boundsWillChange(_impl);
        }
        if (_scene != null) {
            _impl.willBeRemoved();
        }
        _impl.dispose();
        _impl = nimpl;
        if (tickPolicyChanging) {
            tickPolicyDidChange(_impl);
        }
        if (boundsChanging) {
            boundsDidChange(_impl);
        }
        if (_scene != null) {
            _impl.wasAdded();
        }
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
     * Resolves a material config.
     */
    protected static MaterialConfig getMaterialConfig (
        GlContext ctx, String texture, String tag, MaterialMapping[] materialMappings)
    {
        for (MaterialMapping mapping : materialMappings) {
            if (Objects.equal(texture, mapping.texture) && tag.equals(mapping.tag)) {
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

    /**
     * An {@link com.samskivert.util.ObserverList.ObserverOp} that calls
     * {@link ModelObserver#modelCompleted}.
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

    /** The scene containing the model, if any. */
    protected Scene _scene;

    /** The influences affecting this model. */
    protected SceneInfluenceSet _influences = new SceneInfluenceSet();

    /** The model's render scheme. */
    @Scoped
    protected String _renderScheme;

    /** Visibility flag. */
    protected boolean _visible = true;

    /** The model's color state. */
    @Scoped
    protected ColorState _colorState;

    /** The model's fog state. */
    @Scoped
    protected FogState _fogState;

    /** The model's light state. */
    @Scoped
    protected LightState _lightState;

    /** The projections affecting the model. */
    @Scoped
    protected Projection[] _projections;

    /** The definitions affecting the model. */
    protected Map<String, Object> _definitions;

    /** The model's user object. */
    protected Object _userObject;

    /** The visitation id of the last visit. */
    protected int _lastVisit;

    /** Completed op to reuse. */
    protected static CompletedOp _completedOp = new CompletedOp();

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) { };
}
