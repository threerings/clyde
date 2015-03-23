//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.material.config;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.Reference;
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

import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.compositor.Dependency;
import com.threerings.opengl.compositor.Enqueueable;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.compositor.config.RenderEffectConfig;
import com.threerings.opengl.compositor.config.RenderQueueConfig;
import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.geometry.Geometry;
import com.threerings.opengl.geometry.config.DeformerConfig;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.renderer.Batch;
import com.threerings.opengl.renderer.CompoundBatch;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.config.CoordSpace;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

/**
 * Represents a single technique for rendering a material.
 */
public class TechniqueConfig extends DeepObject
    implements Exportable, Preloadable.LoadableConfig
{
    /**
     * Represents a dependency that must be resolved when rendering.
     */
    @EditorTypes({
        StencilReflectionDependency.class, StencilRefractionDependency.class,
        RenderEffectDependency.class, SkipColorClearDependency.class })
    public static abstract class TechniqueDependency extends DeepObject
        implements Exportable, Preloadable.LoadableConfig
    {
        /**
         * Determines whether the dependency is supported.
         */
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return true;
        }

        @Override
        public void preload (GlContext ctx)
        {
            // Do nothing
        }

        /**
         * Creates the adder for this dependency.
         */
        public abstract Dependency.Adder createAdder (GlContext ctx, Scope scope);
    }

    /**
     * A dependency on a stencil reflection.
     */
    public static class StencilReflectionDependency extends TechniqueDependency
    {
        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return ctx.getRenderer().getStencilBits() > 0;
        }

        @Override
        public Dependency.Adder createAdder (final GlContext ctx, Scope scope)
        {
            final Dependency.StencilReflection dependency = new Dependency.StencilReflection(ctx);
            return new Dependency.Adder() {
                public boolean add () {
                    ctx.getCompositor().addDependency(dependency);
                    return true;
                }
            };
        }
    }

    /**
     * A dependency on a stencil refraction.
     */
    public static class StencilRefractionDependency extends TechniqueDependency
    {
        /** The refraction ratio (index of refraction below the surface over index of refraction
         * above the surface). */
        @Editable(step=0.01)
        public float ratio = 1f;

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return ctx.getRenderer().getStencilBits() > 0;
        }

        @Override
        public Dependency.Adder createAdder (final GlContext ctx, Scope scope)
        {
            final Dependency.StencilRefraction dependency = new Dependency.StencilRefraction(ctx);
            dependency.ratio = ratio;
            return new Dependency.Adder() {
                public boolean add () {
                    ctx.getCompositor().addDependency(dependency);
                    return true;
                }
            };
        }
    }

    /**
     * A dependency on a render effect.
     */
    public static class RenderEffectDependency extends TechniqueDependency
        implements Preloadable.LoadableConfig
    {
        /** The effect reference. */
        @Editable(nullable=true)
        public ConfigReference<RenderEffectConfig> renderEffect;

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Config(RenderEffectConfig.class, renderEffect);
        }

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            RenderEffectConfig config = ctx.getConfigManager().getConfig(
                RenderEffectConfig.class, renderEffect);
            return config != null && config.getTechnique(ctx, null) != null;
        }

        @Override
        public Dependency.Adder createAdder (final GlContext ctx, Scope scope)
        {
            final Dependency.RenderEffect dependency = new Dependency.RenderEffect(ctx);
            dependency.config = ctx.getConfigManager().getConfig(
                RenderEffectConfig.class, renderEffect);
            return new Dependency.Adder() {
                public boolean add () {
                    ctx.getCompositor().addDependency(dependency);
                    return true;
                }
            };
        }
    }

    /**
     * A "dependency" that lets the compositor know that it can skip the color clear, because
     * if we render something using this technique, then we know that we will be writing over
     * all pixels.  Used for skyboxes.
     */
    public static class SkipColorClearDependency extends TechniqueDependency
    {
        @Override
        public Dependency.Adder createAdder (final GlContext ctx, Scope scope)
        {
            return new Dependency.Adder() {
                public boolean add () {
                    ctx.getCompositor().setSkipColorClear();
                    return true;
                }
            };
        }
    }

    /**
     * Represents the manner in which we enqueue the technique's batches.
     */
    @EditorTypes({ NormalEnqueuer.class, CompoundEnqueuer.class, GroupedEnqueuer.class })
    public static abstract class Enqueuer extends DeepObject
        implements Exportable, Preloadable.LoadableConfig
    {
        @Override
        public void preload (GlContext ctx)
        {
            // Do nothing
        }

        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
        }

        /**
         * Determines whether this enqueuer is supported by the hardware.
         */
        public abstract boolean isSupported (GlContext ctx, boolean fallback);

        /**
         * Calls the appropriate corresponding rewrite method of {@link MaterialRewriter}.
         */
        public abstract Enqueuer rewrite (MaterialRewriter rewriter);

        /**
         * Adds the descriptors for this enqueuer's passes (as encountered in a depth-first
         * traversal) to the provided list.
         */
        public abstract void getDescriptors (GlContext ctx, List<PassDescriptor> list);

        /**
         * Creates the enqueueable for this enqueuer.
         *
         * @param update if true, update the geometry before enqueuing it.
         * @param adders a list to populate with adders to run before compositing.
         * @param pidx the index of the current pass in the list returned by
         * {@link #getDescriptors} (updated by callees).
         */
        public abstract Enqueueable createEnqueueable (
            GlContext ctx, Scope scope, Geometry geometry, boolean update,
            RenderQueue.Group group, List<Dependency.Adder> adders, MutableInteger pidx);

        /**
         * Invalidates any cached data.
         */
        public abstract void invalidate ();
    }

    /**
     * Enqueues a single batch at a configurable priority.
     */
    public static class NormalEnqueuer extends Enqueuer
    {
        /** The queue into which we render. */
        @Editable(nullable=true, hgroup="q")
        @Reference(RenderQueueConfig.class)
        public String queue = RenderQueue.OPAQUE;

        /** The priority at which the batch is enqueued. */
        @Editable(hgroup="q")
        public int priority;

        /** The passes to render. */
        @Editable
        public PassConfig[] passes = new PassConfig[0];

        @Override
        public void preload (GlContext ctx)
        {
            for (PassConfig pass : passes) {
                pass.preload(ctx);
            }
        }

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            for (PassConfig pass : passes) {
                if (!pass.isSupported(ctx, fallback)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Enqueuer rewrite (MaterialRewriter rewriter)
        {
            return rewriter.rewrite(this);
        }

        @Override
        public void getDescriptors (GlContext ctx, List<PassDescriptor> list)
        {
            for (PassConfig pass : passes) {
                list.add(pass.createDescriptor(ctx));
            }
        }

        @Override
        public Enqueueable createEnqueueable (
            GlContext ctx, Scope scope, final Geometry geometry, boolean update,
            RenderQueue.Group group, List<Dependency.Adder> adders, MutableInteger pidx)
        {
            final RenderQueue queue = group.getQueue(this.queue);
            List<Updater> updaters = Lists.newArrayList();
            final Batch batch = (passes.length == 1) ?
                createBatch(ctx, scope, geometry, passes[0], adders, updaters, pidx) :
                createBatch(ctx, scope, geometry, adders, updaters, pidx);
            TransformState transformState = ScopeUtil.resolve(
                scope, "transformState", TransformState.IDENTITY, TransformState.class);
            final Transform3D modelview = transformState.getModelview();
            final Vector3f center = geometry.getCenter();
            if (update) {
                if (updaters.isEmpty()) {
                    return new Enqueueable() {
                        public void enqueue () {
                            geometry.update();
                            batch.depth = modelview.transformPointZ(center);
                            queue.add(batch, priority);
                        }
                    };
                } else {
                    final Updater[] updaterArray = updaters.toArray(new Updater[updaters.size()]);
                    return new Enqueueable() {
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
                    return new Enqueueable() {
                        public void enqueue () {
                            batch.depth = modelview.transformPointZ(center);
                            queue.add(batch, priority);
                        }
                    };
                } else {
                    final Updater[] updaterArray = updaters.toArray(new Updater[updaters.size()]);
                    return new Enqueueable() {
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

        @Override
        public void invalidate ()
        {
            for (PassConfig pass : passes) {
                pass.invalidate();
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
                List<Batch> batches = ((CompoundBatch)batch).getBatches();
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
            GlContext ctx, Scope scope, Geometry geometry, List<Dependency.Adder> adders,
            List<Updater> updaters, MutableInteger pidx)
        {
            CompoundBatch batch = new CompoundBatch();
            for (PassConfig pass : passes) {
                SimpleBatch simple = createBatch(
                    ctx, scope, geometry, pass, adders, updaters, pidx);
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
            List<Dependency.Adder> adders, List<Updater> updaters, MutableInteger pidx)
        {
            // initialize the states and draw command
            RenderState[] states = pass.createStates(ctx, scope, adders, updaters);
            states[RenderState.ARRAY_STATE] = geometry.getArrayState(pidx.value);
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
                binding.createUpdater(ctx.getConfigManager(), scope, container).update();
            }
            for (ExpressionBinding binding : pass.dynamicBindings) {
                updaters.add(binding.createUpdater(ctx.getConfigManager(), scope, container));
            }

            return new SimpleBatch(states, command);
        }
    }

    /**
     * Invokes some number of sub-enqueuers.
     */
    public static class CompoundEnqueuer extends Enqueuer
    {
        /** The sub-enqueuers. */
        @Editable
        public Enqueuer[] enqueuers = new Enqueuer[0];

        @Override
        public void preload (GlContext ctx)
        {
            for (Enqueuer enqueuer : enqueuers) {
                enqueuer.preload(ctx);
            }
        }

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            for (Enqueuer enqueuer : enqueuers) {
                if (!enqueuer.isSupported(ctx, fallback)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Enqueuer rewrite (MaterialRewriter rewriter)
        {
            return rewriter.rewrite(this);
        }

        @Override
        public void getDescriptors (GlContext ctx, List<PassDescriptor> list)
        {
            for (Enqueuer enqueuer : enqueuers) {
                enqueuer.getDescriptors(ctx, list);
            }
        }

        @Override
        public Enqueueable createEnqueueable (
            GlContext ctx, Scope scope, final Geometry geometry, boolean update,
            RenderQueue.Group group, List<Dependency.Adder> adders, MutableInteger pidx)
        {
            final Enqueueable[] enqueueables = new Enqueueable[enqueuers.length];
            for (int ii = 0; ii < enqueueables.length; ii++) {
                enqueueables[ii] = enqueuers[ii].createEnqueueable(
                    ctx, scope, geometry, false, group, adders, pidx);
            }
            if (update) {
                return new Enqueueable() {
                    public void enqueue () {
                        geometry.update();
                        for (Enqueueable enqueueable : enqueueables) {
                            enqueueable.enqueue();
                        }
                    }
                };
            } else {
                return new Enqueueable() {
                    public void enqueue () {
                        for (Enqueueable enqueueable : enqueueables) {
                            enqueueable.enqueue();
                        }
                    }
                };
            }
        }

        @Override
        public void invalidate ()
        {
            for (Enqueuer enqueuer : enqueuers) {
                enqueuer.invalidate();
            }
        }
    }

    /**
     * Invokes some number of sub-enqueuers within a group.
     */
    public static class GroupedEnqueuer extends CompoundEnqueuer
    {
        /** The queue into which we render. */
        @Editable(nullable=true, weight=-1, hgroup="q")
        @Reference(RenderQueueConfig.class)
        public String queue = RenderQueue.OPAQUE;

        /** The group into which the batches are enqueued. */
        @Editable(weight=-1, hgroup="q")
        public int group;

        @Override
        public Enqueuer rewrite (MaterialRewriter rewriter)
        {
            return rewriter.rewrite(this);
        }

        @Override
        public Enqueueable createEnqueueable (
            GlContext ctx, Scope scope, Geometry geometry, boolean update,
            RenderQueue.Group group, List<Dependency.Adder> adders, MutableInteger pidx)
        {
            return super.createEnqueueable(ctx, scope, geometry, update,
                group.getQueue(this.queue).getGroup(this.group), adders, pidx);
        }
    }

    /**
     * Wraps another enqueuer.
     */
    public static class EnqueuerWrapper extends Enqueuer
    {
        /**
         * Creates a wrapped enqueuer.
         */
        public EnqueuerWrapper (Enqueuer wrapped)
        {
            _wrapped = wrapped;
        }

        @Override
        public void preload (GlContext ctx)
        {
            _wrapped.preload(ctx);
        }

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return _wrapped.isSupported(ctx, fallback);
        }

        @Override
        public Enqueuer rewrite (MaterialRewriter rewriter)
        {
            return _wrapped.rewrite(rewriter);
        }

        @Override
        public void getDescriptors (GlContext ctx, List<PassDescriptor> list)
        {
            _wrapped.getDescriptors(ctx, list);
        }

        @Override
        public Enqueueable createEnqueueable (
            GlContext ctx, Scope scope, Geometry geometry, boolean update, RenderQueue.Group group,
            List<Dependency.Adder> adders, MutableInteger pidx)
        {
            return _wrapped.createEnqueueable(
                ctx, scope, geometry, update, group, adders, pidx);
        }

        @Override
        public void invalidate ()
        {
            _wrapped.invalidate();
        }

        /** The wrapped enqueuer. */
        protected Enqueuer _wrapped;
    }

    /**
     * A simple container for a set of states.
     */
    public static class StateContainer
    {
        /** The contained states. */
        public RenderState[] states;

        /**
         * Creates a new container.
         */
        public StateContainer (RenderState[] states)
        {
            this.states = states;
        }
    }

    /** The render scheme with which this technique is associated. */
    @Editable(nullable=true, hgroup="s")
    @Reference(RenderSchemeConfig.class)
    public String scheme;

    /** Whether or not this technique receives projections. */
    @Editable(hgroup="s")
    public boolean receivesProjections;

    /** Basic material dependencies. */
    @Editable
    public TechniqueDependency[] dependencies = new TechniqueDependency[0];

    /** The deformer to apply to the geometry. */
    @Editable(nullable=true)
    public DeformerConfig deformer;

    /** Determines what we actually enqueue for this technique. */
    @Editable
    public Enqueuer enqueuer = new NormalEnqueuer();

    @Override
    public void preload (GlContext ctx)
    {
        for (TechniqueDependency dependency : dependencies) {
            dependency.preload(ctx);
        }
        enqueuer.preload(ctx);
    }

    @Deprecated
    public void getUpdateReferences (ConfigReferenceSet refs)
    {
    }

    /**
     * Processes this technique to accommodate the features of the hardware.
     *
     * @return the processed technique, or <code>null</code> if the technique is not supported.
     */
    public TechniqueConfig process (GlContext ctx, boolean fallback)
    {
        // for now, we don't do any actual processing; we just check for support
        return isSupported(ctx, fallback) ? this : null;
    }

    /**
     * Determines whether this technique is supported.
     */
    public boolean isSupported (GlContext ctx, boolean fallback)
    {
        for (TechniqueDependency dependency : dependencies) {
            if (!dependency.isSupported(ctx, fallback)) {
                return false;
            }
        }
        return enqueuer.isSupported(ctx, fallback);
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
            List<PassDescriptor> list = Lists.newArrayList();
            enqueuer.getDescriptors(ctx, list);
            _descriptors = list.toArray(new PassDescriptor[list.size()]);
        }
        return _descriptors;
    }

    /**
     * Creates a compositable to render the supplied geometry using this technique.
     */
    public Compositable createCompositable (GlContext ctx, Scope scope, Geometry geometry)
    {
        return createCompositable(ctx, scope, geometry, ctx.getCompositor().getGroup());
    }

    /**
     * Creates a compositable to render the supplied geometry using this technique.
     */
    public Compositable createCompositable (
        final GlContext ctx, Scope scope, Geometry geometry, RenderQueue.Group group)
    {
        List<Dependency.Adder> adders = Lists.newArrayList();
        for (TechniqueDependency dependency : dependencies) {
            adders.add(dependency.createAdder(ctx, scope));
        }
        final Enqueueable enqueueable = enqueuer.createEnqueueable(
            ctx, scope, geometry, geometry.requiresUpdate(),
            group, adders, new MutableInteger(0));
        if (adders.isEmpty()) {
            return new Compositable() {
                public void composite () {
                    ctx.getCompositor().addEnqueueable(enqueueable);
                }
            };
        }
        final Dependency.Adder[] aarray = adders.toArray(new Dependency.Adder[adders.size()]);
        return new Compositable() {
            public void composite () {
                for (Dependency.Adder adder : aarray) {
                    if (!adder.add()) {
                        return;
                    }
                }
                ctx.getCompositor().addEnqueueable(enqueueable);
            }
        };
    }

    /**
     * Invalidates any cached state for this config.
     */
    public void invalidate ()
    {
        enqueuer.invalidate();
        _schemeConfig = RenderSchemeConfig.INVALID;
        _descriptors = null;
    }

    /** The cached scheme config. */
    @DeepOmit
    protected transient RenderSchemeConfig _schemeConfig = RenderSchemeConfig.INVALID;

    /** The cached pass descriptors. */
    @DeepOmit
    protected transient PassDescriptor[] _descriptors;
}
