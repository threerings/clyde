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

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // tick the elements that we always tick
        for (int ii = _alwaysTick.size() - 1; ii >= 0; ii--) {
            _alwaysTick.get(ii).tick(elapsed);
        }

        // tick the elements that we tick when visible
        Frustum frustum = _ctx.getCompositor().getCamera().getWorldVolume();
        for (int ii = _tickWhenVisible.size() - 1; ii >= 0; ii--) {
            SceneElement element = _tickWhenVisible.get(ii);
            if (frustum.getIntersectionType(element.getBounds()) !=
                    Frustum.IntersectionType.NONE) {
                element.tick(elapsed);
            }
        }
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        Frustum frustum = _ctx.getCompositor().getCamera().getWorldVolume();
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SceneElement element = _elements.get(ii);
            if (frustum.getIntersectionType(element.getBounds()) !=
                    Frustum.IntersectionType.NONE) {
                element.enqueue();
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
        return (policy == TickPolicy.NEVER) ? DISCARD_LIST :
            (policy == TickPolicy.ALWAYS ? _alwaysTick : _tickWhenVisible);
    }

    /** The list of all scene elements. */
    protected ArrayList<SceneElement> _elements = new ArrayList<SceneElement>();

    /** The scene elements that we tick when they're visible. */
    protected ArrayList<SceneElement> _tickWhenVisible = new ArrayList<SceneElement>();

    /** The scene elements that we always tick. */
    protected ArrayList<SceneElement> _alwaysTick = new ArrayList<SceneElement>();

    /** Result vector for intersection testing. */
    protected Vector3f _result = new Vector3f();

    /** A list that discards added elements. */
    protected static final ArrayList<SceneElement> DISCARD_LIST = new ArrayList<SceneElement>() {
        public boolean add (SceneElement element) {
            return false;
        }
    };
}
