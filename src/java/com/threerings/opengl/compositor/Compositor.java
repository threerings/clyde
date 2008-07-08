//
// $Id$

package com.threerings.opengl.compositor;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.Maps;

import com.samskivert.util.ComparableArrayList;

import com.threerings.opengl.compositor.config.RenderQueueConfig;
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
        // if we displace another dependency, merge it in
        Dependency previous = _dependencies.put(dependency, dependency);
        if (previous != null) {
            dependency.merge(previous);
        }
    }

    /**
     * Returns a reference to the default render queue.
     */
    public RenderQueue getRenderQueue ()
    {
        return getRenderQueue("default");
    }

    /**
     * Retrieves a reference to a render queue.
     */
    public RenderQueue getRenderQueue (String name)
    {
        RenderQueue queue = _queuesByName.get(name);
        if (queue == null) {
            RenderQueueConfig config = _ctx.getConfigManager().getConfig(
                RenderQueueConfig.class, name);
            queue = (config == null) ? null : config.createRenderQueue(_ctx);
            queue = (queue == null) ? new RenderQueue(0) : queue;
            _queuesByName.put(name, queue);
            _queues.insertSorted(queue);
        }
        return queue;
    }

    /**
     * Resets the list of render queues.
     */
    public void resetRenderQueues ()
    {
        _queues.clear();
    }

    /**
     * Enqueues all of the scene roots using the present state.
     */
    protected void enqueueRoots ()
    {
        _dependencies.clear();
        for (int ii = 0, nn = _roots.size(); ii < nn; ii++) {
            _roots.get(ii).enqueue();
        }
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The roots of the view. */
    protected ArrayList<Renderable> _roots = new ArrayList<Renderable>();

    /** The current set of dependencies. */
    protected HashMap<Dependency, Dependency> _dependencies = Maps.newHashMap();

    /** Maps render queue names to the created queues. */
    protected HashMap<String, RenderQueue> _queuesByName = Maps.newHashMap();

    /** The set of render queues, sorted by priority. */
    protected ComparableArrayList<RenderQueue> _queues = new ComparableArrayList<RenderQueue>();
}
