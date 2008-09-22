//
// $Id$

package com.threerings.opengl.material.config;

import java.util.ArrayList;

import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.ExpressionBinding;
import com.threerings.expr.MutableInteger;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.geometry.config.DeformerConfig;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.renderer.Batch;
import com.threerings.opengl.renderer.CompoundBatch;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.config.CoordSpace;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.TransformState;
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
    public static abstract class Enqueuer extends DeepObject
        implements Exportable
    {
        /** The queue into which we render. */
        @Editable(editor="config", mode="render_queue", nullable=true, hgroup="q")
        public String queue = RenderQueue.OPAQUE;

        /**
         * Adds the enqueuer's update references to the provided set.
         */
        public abstract void getUpdateReferences (ConfigReferenceSet refs);

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
         * @param update if true, update the geometry before enqueuing it.
         * @param pidx the index of the current pass in the list returned by
         * {@link #getDescriptors} (updated by callees).
         */
        public abstract Renderable createRenderable (
            GlContext ctx, Scope scope, Geometry geometry,
            boolean update, RenderQueue.Group group, MutableInteger pidx);
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
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            for (PassConfig pass : passes) {
                pass.getUpdateReferences(refs);
            }
        }

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
            GlContext ctx, Scope scope, final Geometry geometry,
            boolean update, RenderQueue.Group group, MutableInteger pidx)
        {
            final RenderQueue queue = group.getQueue(this.queue);
            ArrayList<Updater> updaters = new ArrayList<Updater>();
            final Batch batch = (passes.length == 1) ?
                createBatch(ctx, scope, geometry, passes[0], updaters, pidx) :
                createBatch(ctx, scope, geometry, updaters, pidx);
            final Transform3D modelview = getTransformState(batch).getModelview();
            final Vector3f center = geometry.getCenter();
            if (update) {
                if (updaters.isEmpty()) {
                    return new Renderable() {
                        public void enqueue () {
                            geometry.update();
                            batch.depth = modelview.transformPointZ(center);
                            queue.add(batch, priority);
                        }
                    };
                } else {
                    final Updater[] updaterArray = updaters.toArray(new Updater[updaters.size()]);
                    return new Renderable() {
                        public void enqueue () {
                            geometry.update();
                            for (Updater updater : updaterArray) {
                                updater.update();
                            }
                            batch.depth = modelview.transformPointZ(center);
                            queue.add(batch, priority);
                        }
                    };
                }
            } else {
                if (updaters.isEmpty()) {
                    return new Renderable() {
                        public void enqueue () {
                            batch.depth = modelview.transformPointZ(center);
                            queue.add(batch, priority);
                        }
                    };
                } else {
                    final Updater[] updaterArray = updaters.toArray(new Updater[updaters.size()]);
                    return new Renderable() {
                        public void enqueue () {
                            for (Updater updater : updaterArray) {
                                updater.update();
                            }
                            batch.depth = modelview.transformPointZ(center);
                            queue.add(batch, priority);
                        }
                    };
                }
            }
        }

        /**
         * Extracts the transform state from the specified batch.
         */
        protected TransformState getTransformState (Batch batch)
        {
            if (batch instanceof SimpleBatch) {
                TransformState state =
                    (TransformState)((SimpleBatch)batch).getStates()[RenderState.TRANSFORM_STATE];
                if (state != null) {
                    return state;
                }
            } else if (batch instanceof CompoundBatch) {
                ArrayList<Batch> batches = ((CompoundBatch)batch).getBatches();
                if (!batches.isEmpty()) {
                    return getTransformState(batches.get(0));
                }
            }
            return TransformState.IDENTITY;
        }

        /**
         * Creates a batch to render all of the passes.
         */
        protected CompoundBatch createBatch (
            GlContext ctx, Scope scope, Geometry geometry,
            ArrayList<Updater> updaters, MutableInteger pidx)
        {
            CompoundBatch batch = new CompoundBatch();
            for (PassConfig pass : passes) {
                SimpleBatch simple = createBatch(ctx, scope, geometry, pass, updaters, pidx);
                batch.getBatches().add(simple);
                if (batch.key == null) {
                    batch.key = simple.key;
                }
            }
            return batch;
        }

        /**
         * Creates a batch to render the specified pass.
         */
        protected SimpleBatch createBatch (
            GlContext ctx, Scope scope, Geometry geometry, PassConfig pass,
            ArrayList<Updater> updaters, MutableInteger pidx)
        {
            // initialize the states and draw command
            RenderState[] states = pass.createStates(ctx, scope, updaters);
            states[RenderState.ARRAY_STATE] = geometry.getArrayState(pidx.value);
            if (states[RenderState.FOG_STATE] == null) {
                states[RenderState.FOG_STATE] = ScopeUtil.resolve(
                    scope, "fogState", FogState.DISABLED, FogState.class);
            }
            if (states[RenderState.LIGHT_STATE] == null) {
                states[RenderState.LIGHT_STATE] = ScopeUtil.resolve(
                    scope, "lightState", LightState.DISABLED, LightState.class);
            }
            CoordSpace space = geometry.getCoordSpace(pidx.value);
            if (space == CoordSpace.EYE) {
                states[RenderState.TRANSFORM_STATE] = TransformState.IDENTITY;
            } else {
                String name = (space == CoordSpace.OBJECT) ?
                    "transformState" : "viewTransformState";
                states[RenderState.TRANSFORM_STATE] = ScopeUtil.resolve(
                    scope, name, TransformState.IDENTITY, TransformState.class);
            }
            DrawCommand command = geometry.getDrawCommand(pidx.value);
            pidx.value++;

            // update the static bindings and add the dynamic updaters to the list
            StateContainer container = new StateContainer(states);
            for (ExpressionBinding binding : pass.staticBindings) {
                binding.createUpdater(scope, container).update();
            }
            for (ExpressionBinding binding : pass.dynamicBindings) {
                updaters.add(binding.createUpdater(scope, container));
            }

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
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            for (Enqueuer enqueuer : enqueuers) {
                enqueuer.getUpdateReferences(refs);
            }
        }

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
            GlContext ctx, Scope scope, final Geometry geometry,
            boolean update, RenderQueue.Group group, MutableInteger pidx)
        {
            group = group.getQueue(this.queue).getGroup(this.group);
            final Renderable[] renderables = new Renderable[enqueuers.length];
            for (int ii = 0; ii < renderables.length; ii++) {
                renderables[ii] = enqueuers[ii].createRenderable(
                    ctx, scope, geometry, false, group, pidx);
            }
            if (update) {
                return new Renderable() {
                    public void enqueue () {
                        geometry.update();
                        for (Renderable renderable : renderables) {
                            renderable.enqueue();
                        }
                    }
                };
            } else {
                return new Renderable() {
                    public void enqueue () {
                        for (Renderable renderable : renderables) {
                            renderable.enqueue();
                        }
                    }
                };
            }
        }
    }

    /** The render scheme with which this technique is associated. */
    @Editable(editor="config", mode="render_scheme", nullable=true)
    public String scheme;

    /** The deformer to apply to the geometry. */
    @Editable(nullable=true)
    public DeformerConfig deformer;

    /** Determines what we actually enqueue for this technique. */
    @Editable
    public Enqueuer enqueuer = new NormalEnqueuer();

    /**
     * Adds the technique's update references to the provided set.
     */
    public void getUpdateReferences (ConfigReferenceSet refs)
    {
        enqueuer.getUpdateReferences(refs);
    }

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
        if (_schemeConfig == RenderSchemeConfig.INVALID) {
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
            ctx, scope, geometry, geometry.requiresUpdate(),
            ctx.getCompositor().getGroup(), new MutableInteger(0));
    }

    /**
     * Invalidates any cached state for this config.
     */
    public void invalidate ()
    {
        _schemeConfig = RenderSchemeConfig.INVALID;
        _descriptors = null;
    }

    /**
     * A simple container for a set of states.
     */
    protected static class StateContainer
    {
        /** The contained states. */
        public RenderState[] states;

        public StateContainer (RenderState[] states)
        {
            this.states = states;
        }
    }

    /** The cached scheme config. */
    @DeepOmit
    protected transient RenderSchemeConfig _schemeConfig = RenderSchemeConfig.INVALID;

    /** The cached pass descriptors. */
    @DeepOmit
    protected transient PassDescriptor[] _descriptors;
}
