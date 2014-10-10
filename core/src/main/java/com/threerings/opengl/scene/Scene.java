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

package com.threerings.opengl.scene;

import java.lang.ref.SoftReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

import com.threerings.config.ConfigReference;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.Scoped;
import com.threerings.expr.Updater;
import com.threerings.math.Box;
import com.threerings.math.Frustum;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.openal.SoundGroup;
import com.threerings.openal.SoundClipManager;
import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.ModelAdapter;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Tickable;

import static com.threerings.opengl.Log.log;

/**
 * Base class for scenes.
 */
public abstract class Scene extends DynamicScope
    implements Tickable, Compositable
{
    /**
     * Extends the normal model class to include an optional pre-tick updater.
     */
    public static class Transient extends Model
    {
        /**
         * Creates a new transient model.
         */
        public Transient (GlContext ctx, ConfigReference<ModelConfig> ref)
        {
            super(ctx, ref);
        }

        /**
         * Installs an updater that will be called before each tick.
         */
        public void setUpdater (Updater updater)
        {
            _updater = updater;
        }

        @Override
        public void tick (float elapsed)
        {
            if (_updater != null) {
                _updater.update();
            }
            super.tick(elapsed);
        }

        /** The updater to call before each tick. */
        protected Updater _updater;
    }

    /**
     * Creates a new scene.
     */
    public Scene (GlContext ctx)
    {
        this(ctx, DEFAULT_SOURCES);
    }

    /**
     * Creates a new scene.
     *
     * @param sources the number of simultaneous sound sources to allow.
     */
    public Scene (GlContext ctx, int sources)
    {
        super("scene");
        _ctx = ctx;
        _soundGroup = ctx.getSoundManager().createGroup(ctx.getClipProvider(), sources);
        _clipmgr = new SoundClipManager(ctx);
    }

    /**
     * Spawns a transient model.
     */
    @Scoped
    public Transient spawnTransient (ConfigReference<ModelConfig> ref, Transform3D transform)
    {
        Transient model = getFromTransientPool(ref);
        model.setLocalTransform(transform);
        boolean add = !_transientPolicy;
        if (_transientPolicy) {
            switch (model.getTransientPolicy()) {
            case BOUNDS:
                add = _ctx.getCompositor().getCamera().getWorldVolume().getBounds().intersects(
                            model.getBounds());
                break;
            case FRUSTUM:
                add = _ctx.getCompositor().getCamera().getWorldVolume().getIntersectionType(
                        model.getBounds()) != Frustum.IntersectionType.NONE;
                break;
            case ALWAYS:
            case DEFAULT:
                add = true;
            }
        }
        if (add) {
            add(model);
        } else {
            returnToTransientPool(model);
            model = null;
        }
        return model;
    }

    /**
     * Returns an instance of the referenced model from the transient pool.
     */
    @Scoped
    public Transient getFromTransientPool (ConfigReference<ModelConfig> ref)
    {
        ArrayList<SoftReference<Transient>> list = _transientPool.get(ref);
        if (list != null) {
            for (int ii = list.size() - 1; ii >= 0; ii--) {
                Transient model = list.remove(ii).get();
                if (model != null) {
                    model.reset();
                    return model;
                }
            }
            _transientPool.remove(ref);
        }
        Transient model = new Transient(_ctx, ref);
        model.setParentScope(this);
        model.setUserObject(ref);
        model.addObserver(_transientObserver);
        return model;
    }

    /**
     * Returns a model to the transient pool.
     */
    @Scoped
    public void returnToTransientPool (Transient model)
    {
        // clear out the model's updater, if any
        model.setUpdater(null);

        ConfigReference<?> ref = (ConfigReference<?>)model.getUserObject();
        ArrayList<SoftReference<Transient>> list = _transientPool.get(ref);
        if (list == null) {
            _transientPool.put(ref, list = new ArrayList<SoftReference<Transient>>());
        }
        list.add(new SoftReference<Transient>(model));
    }

    /**
     * Adds all of the specified elements to the scene.
     */
    public void addAll (SceneElement[] elements)
    {
        for (SceneElement element : elements) {
            add(element);
        }
    }

    /**
     * Adds an element to this scene.
     */
    public void add (SceneElement element)
    {
        // set the parent scope if appropriate
        if (element instanceof DynamicScope) {
            ((DynamicScope)element).setParentScope(this);
        }

        // add to data structures
        addToTick(element);
        addToSpatial(element);


        // add to influence update list
        _updateInfluences.add(element);
        dumpInfluence(element, "add", 1);

        // notify the element
        element.wasAdded(this);
    }

    /**
     * Removes all of the specified elements from the scene.
     */
    public void removeAll (SceneElement[] elements)
    {
        removeAll(elements, true);
    }

    /**
     * Removes all of the specified elements from the scene.
     *
     * @param clearParentScopes if true and the element is an instance of {@link DynamicScope},
     * set the element's parent scope to <code>null</code>.
     */
    public void removeAll (SceneElement[] elements, boolean clearParentScopes)
    {
        for (SceneElement element : elements) {
            remove(element, clearParentScopes);
        }
    }

    /**
     * Removes an element from the scene.
     */
    public void remove (SceneElement element)
    {
        remove(element, true);
    }

    /**
     * Removes an element from the scene.
     *
     * @param clearParentScope if true and the element is an instance of {@link DynamicScope},
     * set the element's parent scope to <code>null</code>.
     */
    public void remove (SceneElement element, boolean clearParentScope)
    {
        if (_disposed) {
            return; // don't bother with the extra computation
        }

        // notify element
        element.willBeRemoved();

        // remove from visible, influence update lists
        _visible.remove(element);
        _updateInfluences.remove(element);
        dumpInfluence(element, "remove", -1);

        // remove from data structures
        removeFromTick(element);
        removeFromSpatial(element);

        // clear the scope if appropriate
        if (element instanceof DynamicScope && clearParentScope) {
            ((DynamicScope)element).setParentScope(null);
        }
    }

    /**
     * Adds an influence to this scene.
     */
    public void add (SceneInfluence influence)
    {
        // add to spatial data structure
        addToSpatial(influence);

        // add any intersecting elements to the update list
        int count = _updateInfluences.size();
        getElements(influence.getBounds(), _updateInfluences);
        dumpInfluence(influence, "add Influence", _updateInfluences.size() - count);
    }

    /**
     * Removes an influence from this scene.
     */
    public void remove (SceneInfluence influence)
    {
        if (_disposed) {
            return; // don't bother with the extra computation
        }

        // add any intersecting elements to the update list
        int count = _updateInfluences.size();
        getElements(influence.getBounds(), _updateInfluences);
        dumpInfluence(influence, "remove influence", _updateInfluences.size() - count);

        // remove from spatial data structure
        removeFromSpatial(influence);
    }

    /**
     * Adds a viewer effect to this scene.
     */
    public void add (ViewerEffect effect)
    {
        // add to spatial data structure
        addToSpatial(effect);
    }

    /**
     * Removes a viewer effect from this scene.
     */
    public void remove (ViewerEffect effect)
    {
        // remove from spatial data structure
        if (!_disposed) {
            removeFromSpatial(effect);
        }
    }

    /**
     * Checks for an intersection between the provided ray and the contents of the scene.
     *
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first element intersected by the ray, or <code>null</code> for
     * none.
     */
    public SceneElement getIntersection (Ray3D ray, Vector3f location)
    {
        Predicate<SceneElement> filter = Predicates.alwaysTrue();
        return getIntersection(ray, location, filter);
    }

    /**
     * Checks for an intersection between the provided ray and the contents of the scene.
     *
     * @param filter a predicate to use in filtering the results of the test.
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first element intersected by the ray, or <code>null</code> for
     * none.
     */
    public abstract SceneElement getIntersection (
        Ray3D ray, Vector3f location, Predicate<? super SceneElement> filter);

    /**
     * Retrieves all scene elements whose bounds intersect the provided region.
     *
     * @param results a list to hold the results of the search.
     */
    public abstract void getElements (Box bounds, Collection<SceneElement> results);

    /**
     * Retrieves all scene influences whose bounds intersect the provided region.
     */
    public abstract void getInfluences (Box bounds, Collection<SceneInfluence> results);

    /**
     * Retrieves all viewer effects whose bounds intersect the provided region.
     */
    public abstract void getEffects (Box bounds, Collection<ViewerEffect> results);

    /**
     * Returns the size of the list of elements that we tick on every frame.
     */
    public int getAlwaysTickCount ()
    {
        return _alwaysTick.size();
    }

    /**
     * Returns the time elapsed to process always tickers.
     */
    public long getAlwaysTickTime ()
    {
        return _alwaysTickTime;
    }

    /**
     * Returns the size of the set of elements that we are going to tick because they're visible.
     */
    public int getVisibleTickCount ()
    {
        return _visible.size();
    }

    /**
     * Returns the time elapsed to process visible tickers.
     */
    public long getVisibleTickTime ()
    {
        return _visibleTickTime;
    }

    /**
     * Returns the size of the set of elements whose influences must be updated.
     */
    public int getUpdateInfluencesCount ()
    {
        return _updateInfluencesCount;
    }

    /**
     * Returns the time to process influences.
     */
    public long getUpdateInfluencesTime ()
    {
        return _updateInfluencesTime;
    }

    /**
     * Returns the number of effects acting upon the viewer.
     */
    public int getViewerEffectCount ()
    {
        return _effects.size();
    }

    /**
     * Returns the time up update viewer effects.
     */
    public long getViewerEffectTime ()
    {
        return _viewerEffectTime;
    }

    /**
     * Writes the set of active viewer effects out to the log.
     */
    public void dumpViewerEffects ()
    {
        log.info("Viewer effects: " + _effects);
    }

    /**
     * Notes that the specified scene element's tick policy is about to change.  Will be followed
     * by a call to {@link #tickPolicyDidChange} when the change has been effected.
     */
    public void tickPolicyWillChange (SceneElement element)
    {
        removeFromTick(element);
    }

    /**
     * Notes that the specified scene element's tick policy has changed.
     */
    public void tickPolicyDidChange (SceneElement element)
    {
        addToTick(element);
    }

    /**
     * Notes that the specified scene element's bounds are about to change.  Will be followed by a
     * call to {@link #boundsDidChange(SceneElement)} when the change has been effected.
     */
    public void boundsWillChange (SceneElement element)
    {
        // nothing by default
    }

    /**
     * Notes that the specified scene element's bounds have changed.
     */
    public void boundsDidChange (SceneElement element)
    {
        // add to update list
        _updateInfluences.add(element);
        dumpInfluence(element, "bounds did change", 1);
    }

    /**
     * Notes that the specified influence's bounds are about to change.  Will be followed by a call
     * to {@link #boundsDidChange(SceneInfluence)} when the change has been effected.
     */
    public void boundsWillChange (SceneInfluence influence)
    {
        // add any intersecting elements to the update list
        int count = _updateInfluences.size();
        getElements(influence.getBounds(), _updateInfluences);
        dumpInfluence(influence, "influence bounds will change", _updateInfluences.size() - count);
    }

    /**
     * Notes that the specified influence's bounds have changed.
     */
    public void boundsDidChange (SceneInfluence influence)
    {
        // add any intersecting elements to the update list
        int count = _updateInfluences.size();
        getElements(influence.getBounds(), _updateInfluences);
        dumpInfluence(influence, "influence bounds did change", _updateInfluences.size() - count);
    }

    /**
     * Notes that the specified effect's bounds are about to change.  Will be followed by a call
     * to {@link #boundsDidChange(ViewerEffect)} when the change has been effected.
     */
    public void boundsWillChange (ViewerEffect effect)
    {
    }

    /**
     * Notes that the specified effect's bounds have changed.
     */
    public void boundsDidChange (ViewerEffect effect)
    {
    }

    /**
     * Clears any viewer effects (when called outside the tick method).
     */
    public void clearEffects ()
    {
        setEffects(_neffects);
    }

    /**
     * Sets the transient policy state.
     */
    public void setTransientPolicy (boolean enabled)
    {
        _transientPolicy = enabled;
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        if (_dumpInfluences) {
            log.info("INFLUENCES!!!");
        }
        // tick the elements that we always tick (in reverse order,
        // so that they can remove themselves)
        long tick = System.nanoTime();
        for (int ii = _alwaysTick.size() - 1; ii >= 0; ii--) {
            _alwaysTick.get(ii).tick(elapsed);
        }
        long tock = System.nanoTime();
        _alwaysTickTime = tock - tick;

        // tick the visible tick-when-visible elements
        if (!_visible.isEmpty()) {
            for (SceneElement element : _visible.toArray(new SceneElement[_visible.size()])) {
                element.tick(elapsed);
            }
            _visible.clear();
        }
        tick = System.nanoTime();
        _visibleTickTime = tick - tock;

        // find the effects acting on the viewer
        Vector3f location = _ctx.getCameraHandler().getViewerTranslation();
        getEffects(_viewer.set(location, location), _neffects);
        setEffects(_neffects);
        _neffects.clear();

        // update the active effects
        for (ViewerEffect effect : _effects) {
            effect.update();
        }
        tock = System.nanoTime();
        _viewerEffectTime = tock - tick;

        // update the influences of any flagged elements
        _updateInfluencesCount = _updateInfluences.size();
        if (_updateInfluencesCount > 0) {
            _updateArray = _updateInfluences.toArray(_updateArray);
            _updateInfluences.clear();
            for (int ii = 0; ii < _updateInfluencesCount; ii++) {
                SceneElement element = _updateArray[ii];
                boolean contains = _updateInfluences.contains(element);
                if (element.isInfluenceable()) {
                    getInfluences(element.getBounds(), _influences);
                }
                dumpInfluence(element, "set influences", 0);
                element.setInfluences(_influences);
                _influences.clear();
                if (_dumpInfluences && !contains && _updateInfluences.contains(element)) {
                    dumpInfluence(element, "SELF REFERENTIAL!!", 0);
                }
            }
            // make sure we don't retain any references
            Arrays.fill(_updateArray, 0, _updateInfluencesCount, null);
        }
        _dumpInfluences = false;
        tick = System.nanoTime();
        _updateInfluencesTime = tick - tock;

        _clipmgr.tick(elapsed);
    }

    /**
     * Will log all influenced and influencing elements on the next tick.
     */
    public void dumpInfluences ()
    {
        _dumpInfluences = true;
    }

    @Override
    public void dispose ()
    {
        super.dispose();
        clearEffects();
        _soundGroup.dispose();
        _disposed = true;
    }

    /**
     * Sets the effects acting on the viewer.
     */
    protected void setEffects (ViewerEffectSet effects)
    {
        if (_effects.equals(effects)) {
            return;
        }
        // deactivate any effects no longer in the set
        for (ViewerEffect effect : _effects) {
            if (!effects.contains(effect)) {
                effect.deactivate();
            }
        }
        // activate any new effects
        for (ViewerEffect effect : effects) {
            if (!_effects.contains(effect)) {
                effect.activate(this);
            }
        }
        _effects.clear();
        _effects.addAll(effects);

        // update the background color
        _ctx.getCompositor().setBackgroundColor(_effects.getBackgroundColor(_viewer));
    }

    /**
     * Adds an element to the scene's tick data structure.
     */
    protected void addToTick (SceneElement element)
    {
        if (element.getTickPolicy() == TickPolicy.ALWAYS) {
            _alwaysTick.add(element);
        }
    }

    /**
     * Removes an element from the scene's tick data structure.
     */
    protected void removeFromTick (SceneElement element)
    {
        if (element.getTickPolicy() == TickPolicy.ALWAYS) {
            _alwaysTick.remove(element);
        }
    }

    /**
     * Adds an element to the scene's spatial data structure.
     */
    protected abstract void addToSpatial (SceneElement element);

    /**
     * Removes an element from the scene's spatial data structure.
     */
    protected abstract void removeFromSpatial (SceneElement element);

    /**
     * Adds an influence to the scene's spatial data structure.
     */
    protected abstract void addToSpatial (SceneInfluence influence);

    /**
     * Removes an influence from the scene's spatial data structure.
     */
    protected abstract void removeFromSpatial (SceneInfluence influence);

    /**
     * Adds an effect to the scene's spatial data structure.
     */
    protected abstract void addToSpatial (ViewerEffect effect);

    /**
     * Removes an effect from the scene's spatial data structure.
     */
    protected abstract void removeFromSpatial (ViewerEffect effect);

    /**
     * Composites a list of elements for rendering.
     */
    protected void composite (ArrayList<SceneElement> elements, Frustum frustum)
    {
        for (int ii = 0, nn = elements.size(); ii < nn; ii++) {
            SceneElement element = elements.get(ii);
            if (frustum.getIntersectionType(element.getBounds()) !=
                    Frustum.IntersectionType.NONE) {
                composite(element);
            }
        }
    }

    /**
     * Composites an element for rendering.
     */
    protected final void composite (SceneElement element)
    {
        element.composite();
        if (element.getTickPolicy() == TickPolicy.WHEN_VISIBLE) {
            _visible.add(element);
        }
    }

    /**
     * Searches for an intersection with the supplied elements.
     */
    protected SceneElement getIntersection (
        ArrayList<SceneElement> elements, Ray3D ray, Vector3f location,
        Predicate<? super SceneElement> filter)
    {
        SceneElement closest = null;
        Vector3f origin = ray.getOrigin();
        for (int ii = 0, nn = elements.size(); ii < nn; ii++) {
            SceneElement element = elements.get(ii);
            if (filter.apply(element) && element.getIntersection(ray, _result) &&
                    (closest == null || origin.distanceSquared(_result) <
                        origin.distanceSquared(location))) {
                closest = element;
                location.set(_result);
            }
        }
        return closest;
    }

    /**
     * Adds all objects from the provided list that intersect the given bounds to the specified
     * results list.
     */
    protected static <T extends SceneObject> void getIntersecting (
        ArrayList<T> objects, Box bounds, Collection<T> results)
    {
        for (int ii = 0, nn = objects.size(); ii < nn; ii++) {
            T object = objects.get(ii);
            if (object.getBounds().intersects(bounds)) {
                results.add(object);
            }
        }
    }

    /**
     * Log influenced scene elements if enabled.
     */
    protected void dumpInfluence (SceneElement element, String msg, int diff)
    {
        if (_dumpInfluences) {
            log.info(msg, "diff", diff, "element", element.getUserObject());
        }
    }

    /**
     * Log scene influences if enabled.
     */
    protected void dumpInfluence (SceneInfluence influence, String msg, int diff)
    {
        if (_dumpInfluences) {
            log.info(msg, "diff", diff, "influence", influence);
        }
    }


    /** The application context. */
    protected GlContext _ctx;

    /** The scene elements that we always tick. */
    protected ArrayList<SceneElement> _alwaysTick = new ArrayList<SceneElement>();

    /** The visible elements to tick. */
    protected HashSet<SceneElement> _visible = new HashSet<SceneElement>();

    /** The elements whose influence sets must be updated. */
    protected HashSet<SceneElement> _updateInfluences = new HashSet<SceneElement>();

    /** The last count of the influences set. */
    protected int _updateInfluencesCount;

    /** Holds the scene elements while we're updating their influences. */
    protected SceneElement[] _updateArray = new SceneElement[0];

    /** The effects currently acting on the viewer. */
    protected ViewerEffectSet _effects = new ViewerEffectSet();

    /** Pooled transient models. */
    protected HashMap<ConfigReference<?>, ArrayList<SoftReference<Transient>>> _transientPool =
        Maps.newHashMap();

    /** Removes transient models and returns them to the pool when they complete. */
    protected ModelAdapter _transientObserver = new ModelAdapter() {
        public boolean modelCompleted (Model model) {
            remove(model, false);
            returnToTransientPool((Transient)model);
            return true;
        }
    };

    /** A sound group for the scene. */
    @Scoped
    protected SoundGroup _soundGroup;

    /** A sound clip manager for the scene. */
    @Scoped
    protected SoundClipManager _clipmgr;

    /** Set when we've been disposed. */
    protected boolean _disposed;

    /** The viewer volume. */
    protected Box _viewer = new Box();

    /** Holds the influences affecting an element. */
    protected SceneInfluenceSet _influences = new SceneInfluenceSet();

    /** Result vector for intersection testing. */
    protected Vector3f _result = new Vector3f();

    /** Holds the new set of effects acting on the viewer. */
    protected ViewerEffectSet _neffects = new ViewerEffectSet();

    /** If we dump influences on the next tick. */
    protected boolean _dumpInfluences;

    /** The time delta's during the tick. */
    protected long _alwaysTickTime, _visibleTickTime, _updateInfluencesTime, _viewerEffectTime;

    /** If transient policies are enabled. */
    protected boolean _transientPolicy;

    /** The default number of sound sources to allow. */
    protected static final int DEFAULT_SOURCES = 10;
}
