//
// $Id$

package com.threerings.opengl.compositor;

import java.lang.ref.SoftReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;

import com.samskivert.util.QuickSort;

import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.compositor.config.RenderEffectConfig;
import com.threerings.opengl.compositor.config.RenderQueueConfig;
import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.ColorMaskState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.StencilState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;

/**
 * Handles the process of compositing the view from its various elements.
 */
public class Compositor
{
    /**
     * Creates a new compositor.
     */
    public Compositor (GlContext ctx)
    {
        _ctx = ctx;
        _group = new RenderQueue.Group(ctx);
    }

    /**
     * Sets the camera reference.
     */
    public void setCamera (Camera camera)
    {
        _camera = camera;
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
     * Adds an element to the list of view roots.
     */
    public void addRoot (Renderable root)
    {
        _roots.add(root);
    }

    /**
     * Removes an element from the list of view roots.
     */
    public void removeRoot (Renderable root)
    {
        _roots.remove(root);
    }

    /**
     * Adds an effect to apply.
     */
    public void addEffect (RenderEffect effect)
    {
        _effects.add(effect);
    }

    /**
     * Removes an effect.
     */
    public void removeEffect (RenderEffect effect)
    {
        _effects.remove(effect);
    }

    /**
     * Renders the composited view.
     */
    public void renderView ()
    {
        // start by requesting that the roots enqueue themselves and register their dependencies
        for (int ii = 0, nn = _roots.size(); ii < nn; ii++) {
            _roots.get(ii).enqueue();
        }
        // reset the renderer stats
        Renderer renderer = _ctx.getRenderer();
        renderer.resetStats();

        // add the in-built effects
        _combinedEffects.addAll(_effects);

        // resolve and clear the set of dependencies
        for (Dependency dependency : _dependencies.values()) {
            dependency.resolve(this);
        }
        _dependencies.clear();

        // sort the queues in preparation for rendering
        _group.sortQueues();

        // apply the camera state
        _camera.apply(renderer);

        // process the effects in reverse order
        QuickSort.sort(_combinedEffects);
        renderPrevious(_combinedEffects.size());

        // clean up
        _skipColorClear = false;
        _group.clearQueues();
        _combinedEffects.clear();
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
     * Sets the flag indicating that we need not clear the color buffer before rendering the
     * frame.
     */
    public void setSkipColorClear ()
    {
        _skipColorClear = true;
    }

    /**
     * Adds an effect associated with a dependency.
     */
    public void addDependencyEffect (RenderEffectConfig config)
    {
        SoftReference<RenderEffect> ref = _cachedEffects.get(config);
        RenderEffect effect = (ref == null) ? null : ref.get();
        if (effect == null) {
            _cachedEffects.put(config, new SoftReference<RenderEffect>(
                effect = new RenderEffect(_ctx, _ctx.getScope(), config)));
        }
        _combinedEffects.add(effect);
    }

    /**
     * For the specified index within the list of combined effects, renders the previous
     * contents.
     */
    public void renderPrevious (int idx)
    {
        int minPriority = Integer.MIN_VALUE, maxPriority = Integer.MAX_VALUE;
        if (idx > 0) {
            int pidx = idx - 1;
            RenderEffect peffect = _combinedEffects.get(pidx);
            peffect.render(pidx);
            minPriority = peffect.getPriority() + 1;
        }
        if (idx < _combinedEffects.size()) {
            maxPriority = _combinedEffects.get(idx).getPriority();
        }
        renderQueues(minPriority, maxPriority);
    }

    /**
     * Retrieves a reference to a render queue.
     */
    public RenderQueue getQueue (String name)
    {
        return _group.getQueue(name);
    }

    /**
     * Resets the list of render queues.
     */
    public void resetQueues ()
    {
        _group = new RenderQueue.Group(_ctx);
    }

    /**
     * Sets the base render queue group reference.
     */
    public void setGroup (RenderQueue.Group group)
    {
        _group = group;
    }

    /**
     * Returns a reference to the base render queue group.
     */
    public RenderQueue.Group getGroup ()
    {
        return _group;
    }

    /**
     * Renders the contents of the queues within the specified priority range.
     */
    protected void renderQueues (int minPriority, int maxPriority)
    {
        // if the range includes the lower bound, perform the clear
        if (minPriority == Integer.MIN_VALUE) {
            Renderer renderer = _ctx.getRenderer();
            int bits = GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT;
            if (!_skipColorClear) {
                bits |= GL11.GL_COLOR_BUFFER_BIT;
                renderer.setClearColor(_backgroundColor);
                renderer.setState(ColorMaskState.ALL);
            }
            renderer.setClearDepth(1f);
            renderer.setState(DepthState.TEST_WRITE);
            renderer.setClearStencil(0);
            renderer.setState(StencilState.DISABLED);
            GL11.glClear(bits);
        }
        _group.renderQueues(RenderQueue.NORMAL_TYPE, minPriority, maxPriority);
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The camera. */
    protected Camera _camera = new Camera();

    /** The background color. */
    protected Color4f _backgroundColor = new Color4f(0f, 0f, 0f, 0f);

    /** The roots of the view. */
    protected ArrayList<Renderable> _roots = new ArrayList<Renderable>();

    /** The non-dependency render effects. */
    protected ArrayList<RenderEffect> _effects = new ArrayList<RenderEffect>();

    /** The current set of dependencies. */
    protected HashMap<Dependency, Dependency> _dependencies = Maps.newHashMap();

    /** The combined list of render effects. */
    protected ArrayList<RenderEffect> _combinedEffects = new ArrayList<RenderEffect>();

    /** When set, indicates that we need not clear the color buffer. */
    protected boolean _skipColorClear;

    /** The base render queue group. */
    protected RenderQueue.Group _group;

    /** Cached render effects. */
    protected IdentityHashMap<RenderEffectConfig, SoftReference<RenderEffect>> _cachedEffects =
        Maps.newIdentityHashMap();
}
