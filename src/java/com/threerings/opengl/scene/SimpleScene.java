//
// $Id$

package com.threerings.opengl.scene;

import java.util.ArrayList;

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
        for (int ii = 0, nn = _alwaysTick.size(); ii < nn; ii++) {
            _alwaysTick.get(ii).tick(elapsed);
        }

        // tick the elements that we tick when visible
        Frustum frustum = _ctx.getCompositor().getCamera().getWorldVolume();
        for (int ii = 0, nn = _tickWhenVisible.size(); ii < nn; ii++) {
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
        TickPolicy policy = element.getTickPolicy();
        if (policy != TickPolicy.NEVER) {
            (policy == TickPolicy.ALWAYS ? _alwaysTick : _tickWhenVisible).add(element);
        }
        _elements.add(element);
    }

    @Override // documentation inherited
    public void remove (SceneElement element)
    {
        TickPolicy policy = element.getTickPolicy();
        if (policy != TickPolicy.NEVER) {
            (policy == TickPolicy.ALWAYS ? _alwaysTick : _tickWhenVisible).remove(element);
        }
        _elements.remove(element);
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

    /** The list of all scene elements. */
    protected ArrayList<SceneElement> _elements = new ArrayList<SceneElement>();

    /** The scene elements that we tick when they're visible. */
    protected ArrayList<SceneElement> _tickWhenVisible = new ArrayList<SceneElement>();

    /** The scene elements that we always tick. */
    protected ArrayList<SceneElement> _alwaysTick = new ArrayList<SceneElement>();

    /** Result vector for intersection testing. */
    protected Vector3f _result = new Vector3f();
}
