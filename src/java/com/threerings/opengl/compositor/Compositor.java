//
// $Id$

package com.threerings.opengl.compositor;

import java.util.ArrayList;

import com.threerings.util.ArrayHashSet;

import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;

/**
 * Handles the process of compositing the scene from its various elements.
 */
public class Compositor
{
    /**
     * Creates a new compositor.
     */
    public Compositor (GlContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Adds an element to the list of scene roots.
     */
    public void addRoot (Renderable root)
    {
        _roots.add(root);
    }

    /**
     * Removes an element from the list of scene roots.
     */
    public void removeRoot (Renderable root)
    {
        _roots.remove(root);
    }

    /**
     * Renders the composited scene.
     */
    public void renderScene ()
    {
        // first pass: collect dependencies
        enqueueRoots();
    }

    /**
     * Adds an element to the list of render dependencies.
     */
    public void addDependency (Dependency dependency)
    {
        _dependencies.add(dependency);
    }

    /**
     * Enqueues all of the scene roots using the present state.
     */
    protected void enqueueRoots ()
    {
        for (int ii = 0, nn = _roots.size(); ii < nn; ii++) {
            _roots.get(ii).enqueue();
        }
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The roots of the view. */
    protected ArrayList<Renderable> _roots = new ArrayList<Renderable>();

    /** The current set of dependencies. */
    protected ArrayHashSet<Dependency> _dependencies = new ArrayHashSet<Dependency>();
}
