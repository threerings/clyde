//
// $Id$

package com.threerings.opengl.scene;

import java.util.ArrayList;

import com.threerings.expr.DynamicScope;
import com.threerings.math.Frustum;
import com.threerings.math.Ray;
import com.threerings.math.Vector3f;

import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.GlContext;

/**
 * A simple, "flat" scene implementation.
 */
public class SimpleScene extends Scene
{
    /**
     * Creates a new simple scene.
     */
    public SimpleScene (GlContext ctx)
    {
        super(ctx);
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // get the frustum and enqueue the first two lists
        Frustum frustum = _ctx.getCompositor().getCamera().getWorldVolume();
        enqueue(frustum, _neverTick);
        enqueue(frustum, _alwaysTick);

        // record the elements rendered for the third
        for (int ii = 0, nn = _tickWhenVisible.size(); ii < nn; ii++) {
            SceneElement element = _tickWhenVisible.get(ii);
            if (frustum.getIntersectionType(element.getBounds()) !=
                    Frustum.IntersectionType.NONE) {
                element.enqueue();
                _visible.add(element);
            }
        }
    }

    @Override // documentation inherited
    public void add (SceneElement element)
    {
        if (element instanceof DynamicScope) {
            ((DynamicScope)element).setParentScope(this);
        }
        _elements.add(element);
        getTickList(element).add(element);
    }

    @Override // documentation inherited
    public void remove (SceneElement element)
    {
        _elements.remove(element);
        getTickList(element).remove(element);
    }

    @Override // documentation inherited
    public SceneElement getIntersection (Ray ray, Vector3f location)
    {
        SceneElement closest = null;
        Vector3f origin = ray.getOrigin();
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SceneElement element = _elements.get(ii);
            if (element.getIntersection(ray, _result) && (closest == null ||
                    origin.distanceSquared(_result) < origin.distanceSquared(location))) {
                closest = element;
                location.set(_result);
            }
        }
        return closest;
    }

    /**
     * Notes that the specified scene element's tick policy is about to change.  Will be followed
     * by a call to {@link #tickPolicyDidChange} when the change has been effected.
     */
    public void tickPolicyWillChange (SceneElement element)
    {
        getTickList(element).remove(element);
    }

    /**
     * Notes that the specified scene element's tick policy has changed.
     */
    public void tickPolicyDidChange (SceneElement element)
    {
        if (_elements.contains(element)) {
            getTickList(element).add(element);
        }
    }

    /**
     * Returns the list corresponding to the specified element's tick policy.
     */
    protected ArrayList<SceneElement> getTickList (SceneElement element)
    {
        TickPolicy policy = element.getTickPolicy();
        return (policy == TickPolicy.NEVER) ? _neverTick :
            (policy == TickPolicy.ALWAYS ? _alwaysTick : _tickWhenVisible);
    }

    /**
     * Enqueues the specified list of elements.
     */
    protected void enqueue (Frustum frustum, ArrayList<SceneElement> list)
    {
        for (int ii = 0, nn = list.size(); ii < nn; ii++) {
            SceneElement element = list.get(ii);
            if (frustum.getIntersectionType(element.getBounds()) !=
                    Frustum.IntersectionType.NONE) {
                element.enqueue();
            }
        }
    }

    /** The list of all scene elements. */
    protected ArrayList<SceneElement> _elements = new ArrayList<SceneElement>();

    /** The scene elements that we never tick. */
    protected ArrayList<SceneElement> _neverTick = new ArrayList<SceneElement>();

    /** The scene elements that we tick when they're visible. */
    protected ArrayList<SceneElement> _tickWhenVisible = new ArrayList<SceneElement>();

    /** Result vector for intersection testing. */
    protected Vector3f _result = new Vector3f();
}
