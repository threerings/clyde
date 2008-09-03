//
// $Id$

package com.threerings.opengl.scene;

import java.lang.ref.SoftReference;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.Maps;

import com.threerings.config.ConfigReference;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.Scoped;
import com.threerings.math.Ray;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.mod.ModelAdapter;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

/**
 * Base class for scenes.
 */
public abstract class Scene extends DynamicScope
    implements Tickable, Renderable
{
    /**
     * Creates a new scene.
     */
    public Scene (GlContext ctx)
    {
        super("scene");
        _ctx = ctx;
    }

    /**
     * Spawns a transient model.
     */
    @Scoped
    public void spawnTransient (ConfigReference<ModelConfig> ref, Transform3D transform)
    {
        Model model = getFromTransientPool(ref);
        model.getLocalTransform().set(transform);
        add(model);
    }

    /**
     * Adds an element to this scene.
     */
    public abstract void add (SceneElement element);

    /**
     * Removes an element from the scene.
     */
    public abstract void remove (SceneElement element);

    /**
     * Checks for an intersection between the provided ray and the contents of the scene.
     *
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first element intersected by the ray, or <code>null</code> for
     * none.
     */
    public abstract SceneElement getIntersection (Ray ray, Vector3f location);

    /**
     * Notes that the specified scene element's tick policy is about to change.  Will be followed
     * by a call to {@link #tickPolicyDidChange} when the change has been effected.
     */
    public void tickPolicyWillChange (SceneElement element)
    {
        // nothing by default
    }

    /**
     * Notes that the specified scene element's tick policy has changed.
     */
    public void tickPolicyDidChange (SceneElement element)
    {
        // nothing by default
    }

    /**
     * Notes that the specified scene element's bounds are about to change.  Will be followed by a
     * call to {@link #boundsDidChange} when the change has been effected.
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
        // nothing by default
    }

    /**
     * Returns an instance of the referenced model from the transient pool.
     */
    protected Model getFromTransientPool (ConfigReference<ModelConfig> ref)
    {
        ArrayList<SoftReference<Model>> list = _transientPool.get(ref);
        if (list != null) {
            for (int ii = list.size() - 1; ii >= 0; ii--) {
                Model model = list.remove(ii).get();
                if (model != null) {
                    model.reset();
                    return model;
                }
            }
            _transientPool.remove(ref);
        }
        Model model = new Model(_ctx, ref);
        model.setParentScope(this);
        model.setUserObject(ref);
        model.addObserver(_transientObserver);
        return model;
    }

    /**
     * Returns a model to the transient pool.
     */
    protected void returnToTransientPool (Model model)
    {
        ConfigReference ref = (ConfigReference)model.getUserObject();
        ArrayList<SoftReference<Model>> list = _transientPool.get(ref);
        if (list == null) {
            _transientPool.put(ref, list = new ArrayList<SoftReference<Model>>());
        }
        list.add(new SoftReference<Model>(model));
    }

    /** The application context. */
    protected GlContext _ctx;

    /** Pooled transient models. */
    protected HashMap<ConfigReference, ArrayList<SoftReference<Model>>> _transientPool =
        Maps.newHashMap();

    /** Removes transient models and returns them to the pool when they complete. */
    protected ModelAdapter _transientObserver = new ModelAdapter() {
        public boolean modelCompleted (Model model) {
            remove(model);
            returnToTransientPool(model);
            return true;
        }
    };
}
