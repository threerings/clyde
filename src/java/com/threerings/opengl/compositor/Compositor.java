//
// $Id$

package com.threerings.opengl.compositor;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;

import com.samskivert.util.ComparableArrayList;

import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.compositor.config.RenderQueueConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.ColorMaskState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.StencilState;
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
     * Initializes the compositor once the renderer has been initialized.
     */
    public void init ()
    {
        _camera.getViewport().set(_ctx.getRenderer().getViewport());
    }

    /**
     * Returns a reference to the camera.
     */
    public Camera getCamera ()
    {
        return _camera;
    }

    /**
     * Returns a reference to the background color.
     */
    public Color4f getBackgroundColor ()
    {
        return _backgroundColor;
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
        enqueueRoots();
        sortQueues();

        // reset the renderer stats
        Renderer renderer = _ctx.getRenderer();
        renderer.resetStats();

        // clear the depth and stencil buffers (and the color buffer, provided that none of the
        // queues do it for us)
        int bits = GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT;
        if (!queueClearsColor()) {
            bits |= GL11.GL_COLOR_BUFFER_BIT;
            renderer.setClearColor(_backgroundColor);
            renderer.setState(ColorMaskState.ALL);
        }
        renderer.setClearDepth(1f);
        renderer.setState(DepthState.TEST_WRITE);
        renderer.setClearStencil(0);
        renderer.setState(StencilState.DISABLED);
        GL11.glClear(bits);

        _camera.apply(renderer);
        renderQueues();
        clearQueues();

        // allow the renderer to clean up
        renderer.cleanup();
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
    public RenderQueue getQueue ()
    {
        return getQueue(RenderQueue.DEFAULT);
    }

    /**
     * Retrieves a reference to a render queue.
     */
    public RenderQueue getQueue (String name)
    {
        RenderQueue queue = _queuesByName.get(name);
        if (queue == null) {
            RenderQueueConfig config = _ctx.getConfigManager().getConfig(
                RenderQueueConfig.class, name);
            queue = (config == null) ? null : config.createQueue(_ctx);
            queue = (queue == null) ? new RenderQueue(0) : queue;
            _queuesByName.put(name, queue);
            _queues.insertSorted(queue);
        }
        return queue;
    }

    /**
     * Resets the list of render queues.
     */
    public void resetQueues ()
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

    /**
     * Sorts the queues in preparation for rendering.
     */
    protected void sortQueues ()
    {
        for (int ii = 0, nn = _queues.size(); ii < nn; ii++) {
            _queues.get(ii).sort();
        }
    }

    /**
     * Determines whether any of the populated render queues are flagged as clearing the color
     * buffer.
     */
    protected boolean queueClearsColor ()
    {
        for (int ii = 0, nn = _queues.size(); ii < nn; ii++) {
            RenderQueue queue = _queues.get(ii);
            if (queue.clearsColor() && queue.size() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Renders the contents of the queues.
     */
    protected void renderQueues ()
    {
        Renderer renderer = _ctx.getRenderer();
        for (int ii = 0, nn = _queues.size(); ii < nn; ii++) {
            _queues.get(ii).render(renderer);
        }
    }

    /**
     * Clears out the contents of the queues.
     */
    protected void clearQueues ()
    {
        for (int ii = 0, nn = _queues.size(); ii < nn; ii++) {
            _queues.get(ii).clear();
        }
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The camera. */
    protected Camera _camera = new Camera();

    /** The background color. */
    protected Color4f _backgroundColor = new Color4f(0f, 0f, 0f, 0f);

    /** The roots of the view. */
    protected ArrayList<Renderable> _roots = new ArrayList<Renderable>();

    /** The current set of dependencies. */
    protected HashMap<Dependency, Dependency> _dependencies = Maps.newHashMap();

    /** Maps render queue names to the created queues. */
    protected HashMap<String, RenderQueue> _queuesByName = Maps.newHashMap();

    /** The set of render queues, sorted by priority. */
    protected ComparableArrayList<RenderQueue> _queues = new ComparableArrayList<RenderQueue>();
}
