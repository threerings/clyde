//
// $Id$

package com.threerings.opengl.scene;

import java.util.ArrayList;

import com.samskivert.util.Predicate;

import com.threerings.math.Box;
import com.threerings.math.Frustum;
import com.threerings.math.Ray;
import com.threerings.math.Vector3f;

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
        Frustum frustum = _ctx.getCompositor().getCamera().getWorldVolume();
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SceneElement element = _elements.get(ii);
            if (frustum.getIntersectionType(element.getBounds()) !=
                    Frustum.IntersectionType.NONE) {
                enqueue(element);
            }
        }
    }

    @Override // documentation inherited
    public SceneElement getIntersection (
        Ray ray, Vector3f location, Predicate<SceneElement> filter)
    {
        SceneElement closest = null;
        Vector3f origin = ray.getOrigin();
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SceneElement element = _elements.get(ii);
            if (filter.isMatch(element) && element.getIntersection(ray, _result) &&
                    (closest == null || origin.distanceSquared(_result) <
                        origin.distanceSquared(location))) {
                closest = element;
                location.set(_result);
            }
        }
        return closest;
    }

    @Override // documentation inherited
    public void getElements (Box bounds, ArrayList<SceneElement> results)
    {
        getIntersecting(_elements, bounds, results);
    }

    @Override // documentation inherited
    public void getInfluences (Box bounds, ArrayList<SceneInfluence> results)
    {
        getIntersecting(_influences, bounds, results);
    }

    @Override // documentation inherited
    protected void addToSpatial (SceneElement element)
    {
        _elements.add(element);
    }

    @Override // documentation inherited
    protected void removeFromSpatial (SceneElement element)
    {
        _elements.remove(element);
    }

    @Override // documentation inherited
    protected void addToSpatial (SceneInfluence influence)
    {
        _influences.add(influence);
    }

    @Override // documentation inherited
    protected void removeFromSpatial (SceneInfluence influence)
    {
        _influences.remove(influence);
    }

    /**
     * Adds all objects from the provided list that intersect the given bounds to the specified
     * results list.
     */
    protected static <T extends SceneObject> void getIntersecting (
        ArrayList<T> objects, Box bounds, ArrayList<T> results)
    {
        for (int ii = 0, nn = objects.size(); ii < nn; ii++) {
            T object = objects.get(ii);
            if (object.getBounds().intersects(bounds)) {
                results.add(object);
            }
        }
    }

    /** The list of all scene elements. */
    protected ArrayList<SceneElement> _elements = new ArrayList<SceneElement>();

    /** The list of all scene influences. */
    protected ArrayList<SceneInfluence> _influences = new ArrayList<SceneInfluence>();
}
