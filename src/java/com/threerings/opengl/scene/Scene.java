//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.expr.DynamicScope;
import com.threerings.math.Ray;
import com.threerings.math.Vector3f;

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

    /** The application context. */
    protected GlContext _ctx;
}
