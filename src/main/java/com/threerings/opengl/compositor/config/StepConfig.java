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

package com.threerings.opengl.compositor.config;

import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Executor;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.compositor.Dependency;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.material.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.ColorMaskState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.StencilState;
import com.threerings.opengl.util.GlContext;

/**
 * Represents a single step in the process of updating a target.
 */
@EditorTypes({
    StepConfig.Clear.class,
    StepConfig.RenderQueues.class,
    StepConfig.RenderQuad.class })
public abstract class StepConfig extends DeepObject
    implements Exportable
{
    /**
     * Clears some or all of the buffers.
     */
    public static class Clear extends StepConfig
    {
        /**
         * Color clear parameters.
         */
        public static class Color extends DeepObject
            implements Exportable
        {
            /** Whether or not to clear the color buffer. */
            @Editable(hgroup="c")
            public boolean clear = true;

            /** The color clear value. */
            @Editable(mode="alpha", hgroup="c")
            public Color4f value = new Color4f(0f, 0f, 0f, 0f);
        }

        /**
         * Depth clear parameters.
         */
        public static class Depth extends DeepObject
            implements Exportable
        {
            /** Whether or not to clear the depth buffer. */
            @Editable(hgroup="c")
            public boolean clear = true;

            /** The depth clear value. */
            @Editable(min=0, max=1, step=0.01, hgroup="c")
            public float value = 1f;
        }

        /**
         * Stencil clear parameters.
         */
        public static class Stencil extends DeepObject
            implements Exportable
        {
            /** Whether or not to clear the stencil buffer. */
            @Editable(hgroup="c")
            public boolean clear = true;

            /** The stencil clear value. */
            @Editable(min=0, hgroup="c")
            public int value;
        }

        /** Color buffer clear parameters. */
        @Editable
        public Color color = new Color();

        /** Depth buffer clear parameters. */
        @Editable
        public Depth depth = new Depth();

        /** Stencil buffer clear parameters. */
        @Editable
        public Stencil stencil = new Stencil();

        @Override
        public Executor createExecutor (final GlContext ctx, Scope scope)
        {
            return new Executor() {
                public void execute () {
                    Renderer renderer = ctx.getRenderer();
                    int bits = 0;
                    if (color.clear) {
                        bits |= GL11.GL_COLOR_BUFFER_BIT;
                        renderer.setClearColor(color.value);
                        renderer.setState(ColorMaskState.ALL);
                    }
                    if (depth.clear) {
                        bits |= GL11.GL_DEPTH_BUFFER_BIT;
                        renderer.setClearDepth(depth.value);
                        renderer.setState(DepthState.TEST_WRITE);
                    }
                    if (stencil.clear) {
                        bits |= GL11.GL_STENCIL_BUFFER_BIT;
                        renderer.setClearStencil(stencil.value);
                        renderer.setState(StencilState.DISABLED);
                    }
                    if (bits != 0) {
                        GL11.glClear(bits);
                    }
                }
            };
        }
    }

    /**
     * Renders a set of render queues.
     */
    public static class RenderQueues extends StepConfig
    {
        /** The type of queues to render. */
        @Editable
        public String queueType = RenderQueue.NORMAL_TYPE;

        /** The minimum priority of the queues to render. */
        @Editable(hgroup="p")
        public int minPriority = Integer.MIN_VALUE;

        /** The maximum priority of the queues to render. */
        @Editable(hgroup="p")
        public int maxPriority = Integer.MAX_VALUE;

        @Override
        public Executor createExecutor (final GlContext ctx, Scope scope)
        {
            return new Executor() {
                public void execute () {
                    ctx.getCompositor().getGroup().renderQueues(
                        queueType, minPriority, maxPriority);
                }
            };
        }
    }

    /**
     * Renders a full-screen quad.
     */
    public static class RenderQuad extends StepConfig
    {
        /** The material to use when rendering the quad. */
        @Editable(nullable=true)
        public ConfigReference<MaterialConfig> material;

        /** The level of tesselation in the x direction. */
        @Editable(min=1, hgroup="d")
        public int divisionsX = 1;

        /** The level of tesselation in the y direction. */
        @Editable(min=1, hgroup="d")
        public int divisionsY = 1;

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            MaterialConfig config = ctx.getConfigManager().getConfig(
                MaterialConfig.class, material);
            return config != null && config.getTechnique(ctx, null) != null;
        }

        @Override
        public Executor createExecutor (final GlContext ctx, Scope scope)
        {
            MaterialConfig config = ctx.getConfigManager().getConfig(
                MaterialConfig.class, material);
            final Map<Dependency, Dependency> dependencies = Maps.newHashMap();
            final RenderQueue.Group group = new RenderQueue.Group(ctx);
            final Surface surface = new Surface(
                ctx, scope, GeometryConfig.getQuad(divisionsX, divisionsY), config, group);
            return new Executor() {
                public void execute () {
                    // save the projection matrix and load the identity matrix
                    Renderer renderer = ctx.getRenderer();
                    renderer.setMatrixMode(GL11.GL_PROJECTION);
                    GL11.glPushMatrix();
                    GL11.glLoadIdentity();

                    // save and replace the dependency map
                    Compositor compositor = ctx.getCompositor();
                    Map<Dependency, Dependency> odeps = compositor.getDependencies();
                    compositor.setDependencies(dependencies);

                    // composite, enqueue, sort, render, clear
                    surface.composite();
                    compositor.enqueueEnqueueables();
                    group.sortQueues();
                    group.renderQueues();
                    group.clearQueues();

                    // clear and restore the dependencies
                    compositor.clearDependencies();
                    compositor.setDependencies(odeps);

                    // restore the projection matrix
                    renderer.setMatrixMode(GL11.GL_PROJECTION);
                    GL11.glPopMatrix();
                }
            };
        }
    }

    /**
     * Determines whether this step config is supported by the hardware.
     */
    public boolean isSupported (GlContext ctx, boolean fallback)
    {
        return true;
    }

    /**
     * Creates the executor that will actually perform the step.
     */
    public abstract Executor createExecutor (GlContext ctx, Scope scope);
}
