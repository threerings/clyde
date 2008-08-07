//
// $Id$

package com.threerings.opengl.material.config;

import java.util.ArrayList;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.MutableInteger;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.geom.config.PassDescriptor;
import com.threerings.opengl.renderer.Batch;
import com.threerings.opengl.renderer.CompoundBatch;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;

/**
 * Represents a single technique for rendering a material.
 */
public class TechniqueConfig extends DeepObject
    implements Exportable
{
    /**
     * Represents the manner in which we enqueue the technique's batches.
     */
    @EditorTypes({ NormalEnqueuer.class, GroupedEnqueuer.class })
    public static abstract class Enqueuer
    {
        /** The queue into which we render. */
        @Editable(editor="config", mode="render_queue", nullable=true, hgroup="q")
        public String queue = RenderQueue.OPAQUE;

        /**
         * Determines whether this enqueuer is supported by the hardware.
         */
        public abstract boolean isSupported (GlContext ctx);

        /**
         * Adds the descriptors for this enqueuer's passes (as encountered in a depth-first
         * traversal) to the provided list.
         */
        public abstract void getDescriptors (GlContext ctx, ArrayList<PassDescriptor> list);

        /**
         * Creates the renderable for this enqueuer.
         *
         * @param pidx the index of the current pass in the list returned by
         * {@link #getDescriptors} (updated by callees).
         */
        public abstract Renderable createRenderable (
            GlContext ctx, Scope scope, Geometry geometry,
            RenderQueue.Group group, MutableInteger pidx);
    }

    /**
     * Enqueues a single batch at a configurable priority.
     */
    public static class NormalEnqueuer extends Enqueuer
    {
        /** The priority at which the batch is enqueued. */
        @Editable(hgroup="q")
        public int priority;

        /** The passes to render. */
        @Editable
        public PassConfig[] passes = new PassConfig[0];

        @Override // documentation inherited
        public boolean isSupported (GlContext ctx)
        {
            for (PassConfig pass : passes) {
                if (!pass.isSupported(ctx)) {
                    return false;
                }
            }
            return true;
        }

        @Override // documentation inherited
        public void getDescriptors (GlContext ctx, ArrayList<PassDescriptor> list)
        {
            for (PassConfig pass : passes) {
                list.add(pass.createDescriptor(ctx));
            }
        }

        @Override // documentation inherited
        public Renderable createRenderable (
            GlContext ctx, Scope scope, Geometry geometry,
            RenderQueue.Group group, MutableInteger pidx)
        {
            final RenderQueue queue = group.getQueue(this.queue);
            final Batch batch = (passes.length == 1) ?
                createBatch(ctx, scope, geometry, passes[0], pidx) :
                createBatch(ctx, scope, geometry, pidx);
            return new Renderable() {
                public void enqueue () {
                    queue.add(batch, priority);
                }
            };
        }

        /**
         * Creates a batch to render all of the passes.
         */
        protected CompoundBatch createBatch (
            GlContext ctx, Scope scope, Geometry geometry, MutableInteger pidx)
        {
            CompoundBatch batch = new CompoundBatch();
            for (PassConfig pass : passes) {
                batch.getBatches().add(createBatch(ctx, scope, geometry, pass, pidx));
            }
            return batch;
        }

        /**
         * Creates a batch to render the specified pass.
         */
        protected SimpleBatch createBatch (
            GlContext ctx, Scope scope, Geometry geometry, PassConfig pass, MutableInteger pidx)
        {
            RenderState[] states = pass.createStates(ctx);
            states[RenderState.ARRAY_STATE] = geometry.getArrayState(pidx.value);
            DrawCommand command = geometry.getDrawCommand(pidx.value);
            pidx.value++;
            return new SimpleBatch(states, command);
        }
    }

    /**
     * Invokes some number of sub-enqueuers within a group.
     */
    public static class GroupedEnqueuer extends Enqueuer
    {
        /** The group into which the batches are enqueued. */
        @Editable(hgroup="q")
        public int group;

        /** The enqueuers for the group. */
        @Editable
        public Enqueuer[] enqueuers = new Enqueuer[0];

        @Override // documentation inherited
        public boolean isSupported (GlContext ctx)
        {
            for (Enqueuer enqueuer : enqueuers) {
                if (!enqueuer.isSupported(ctx)) {
                    return false;
                }
            }
            return true;
        }

        @Override // documentation inherited
        public void getDescriptors (GlContext ctx, ArrayList<PassDescriptor> list)
        {
            for (Enqueuer enqueuer : enqueuers) {
                enqueuer.getDescriptors(ctx, list);
            }
        }

        @Override // documentation inherited
        public Renderable createRenderable (
            GlContext ctx, Scope scope, Geometry geometry,
            RenderQueue.Group group, MutableInteger pidx)
        {
            group = group.getQueue(this.queue).getGroup(this.group);
            final Renderable[] renderables = new Renderable[enqueuers.length];
            for (int ii = 0; ii < renderables.length; ii++) {
                renderables[ii] = enqueuers[ii].createRenderable(
                    ctx, scope, geometry, group, pidx);
            }
            return new Renderable() {
                public void enqueue () {
                    for (Renderable renderable : renderables) {
                        renderable.enqueue();
                    }
                }
            };
        }
    }

    /** The render scheme with which this technique is associated. */
    @Editable(editor="config", mode="render_scheme", nullable=true)
    public String scheme;

    /** Determines what we actually enqueue for this technique. */
    @Editable
    public Enqueuer enqueuer = new NormalEnqueuer();

    /**
     * Processes this technique to accommodate the features of the hardware.
     *
     * @return the processed technique, or <code>null</code> if the technique is not supported.
     */
    public TechniqueConfig process (GlContext ctx)
    {
        // for now, we don't do any actual processing; we just check for support
        return isSupported(ctx) ? this : null;
    }

    /**
     * Determines whether this technique is supported.
     */
    public boolean isSupported (GlContext ctx)
    {
        return enqueuer.isSupported(ctx);
    }

    /**
     * Returns the cached configuration for the technique's render scheme.
     */
    public RenderSchemeConfig getSchemeConfig (GlContext ctx)
    {
        if (_schemeConfig == INVALID_SCHEME_CONFIG) {
            return (scheme == null) ? null :
                ctx.getConfigManager().getConfig(RenderSchemeConfig.class, scheme);
        }
        return _schemeConfig;
    }

    /**
     * Returns the cached descriptors for this technique's passes.
     */
    public PassDescriptor[] getDescriptors (GlContext ctx)
    {
        if (_descriptors == null) {
            ArrayList<PassDescriptor> list = new ArrayList<PassDescriptor>();
            enqueuer.getDescriptors(ctx, list);
            _descriptors = list.toArray(new PassDescriptor[list.size()]);
        }
        return _descriptors;
    }

    /**
     * Creates a renderable to render the supplied geometry using this technique.
     */
    public Renderable createRenderable (GlContext ctx, Scope scope, Geometry geometry)
    {
        return enqueuer.createRenderable(
            ctx, scope, geometry, ctx.getCompositor().getGroup(), new MutableInteger(0));
    }

    /**
     * Invalidates any cached state for this config.
     */
    public void invalidate ()
    {
        _schemeConfig = INVALID_SCHEME_CONFIG;
        _descriptors = null;
    }

    /** The cached scheme config. */
    @DeepOmit
    protected transient RenderSchemeConfig _schemeConfig = INVALID_SCHEME_CONFIG;

    /** The cached pass descriptors. */
    @DeepOmit
    protected transient PassDescriptor[] _descriptors;

    /** An invalid render scheme config. */
    protected static final RenderSchemeConfig INVALID_SCHEME_CONFIG = new RenderSchemeConfig();
}
