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

import java.util.Comparator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.samskivert.util.QuickSort;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.renderer.Batch;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.util.GlContext;

/**
 * The configuration of a render queue.
 */
public class RenderQueueConfig extends ManagedConfig
{
    /** The available sort modes. */
    public enum SortMode
    {
        /** Leaves batches unsorted. */
        NONE(null) {
            public void sort (List<Batch> batches) {
                // no-op
            }
        },

        /** Sorts batches by state, then front-to-back. */
        BY_STATE(new Comparator<Batch>() {
            public int compare (Batch b1, Batch b2) {
                int comp = Batch.compareKeys(b1.key, b2.key);
                return (comp == 0) ? Float.compare(b2.depth, b1.depth) : comp;
            }
        }),

        /** Sorts batches by depth, back-to-front. */
        BACK_TO_FRONT(new Comparator<Batch>() {
            public int compare (Batch b1, Batch b2) {
                return Float.compare(b1.depth, b2.depth);
            }
        }),

        /** Sorts batches by depth, front-to-back. */
        FRONT_TO_BACK(new Comparator<Batch>() {
            public int compare (Batch b1, Batch b2) {
                return Float.compare(b2.depth, b1.depth);
            }
        });

        /**
         * Sorts the supplied list of batches.
         */
        public void sort (List<Batch> batches)
        {
            QuickSort.sort(batches, _comparator);
        }

        SortMode (Comparator<Batch> comparator)
        {
            _comparator = comparator;
        }

        /** The comparator used to sort the batches. */
        protected final Comparator<Batch> _comparator;
    }

    /**
     * Base class for render modes.
     */
    @EditorTypes({ Normal.class, Ortho.class, Skybox.class })
    public static abstract class RenderMode extends DeepObject
        implements Exportable
    {
        /**
         * Renders the batches.
         */
        public void render (GlContext ctx, RenderQueue queue)
        {
            queue.renderLists(ctx.getRenderer());
        }
    }

    /**
     * The normal render mode.
     */
    public static class Normal extends RenderMode
    {
    }

    /**
     * The ortho render mode: switches to an ortho matrix (with pixel scale) before rendering.
     */
    public static class Ortho extends RenderMode
    {
        @Override
        public void render (GlContext ctx, RenderQueue queue) {
            // make sure we have something to render
            if (queue.isEmpty()) {
                return;
            }

            // save the projection parameters
            Renderer renderer = ctx.getRenderer();
            float oleft = renderer.getLeft(), oright = renderer.getRight();
            float obottom = renderer.getBottom(), otop = renderer.getTop();
            float onear = renderer.getNear(), ofar = renderer.getFar();
            _onormal.set(renderer.getNearFarNormal());
            boolean oortho = renderer.isOrtho();

            // set the orthographic projection
            Rectangle viewport = renderer.getViewport();
            renderer.setProjection(
                0f, viewport.width, 0f, viewport.height, -1f, +1f, Vector3f.UNIT_Z, true);

            // render
            super.render(ctx, queue);

            // restore the original projection
            renderer.setProjection(oleft, oright, obottom, otop, onear, ofar, _onormal, oortho);
        }

        /** Stores the near/far clip plane normal. */
        protected Vector3f _onormal = new Vector3f();
    }

    /**
     * The skybox render mode: scales the field of view before rendering and clears the z buffer
     * afterwards.
     */
    public static class Skybox extends RenderMode
    {
        /** The scale to apply to the field of view. */
        @Editable(min=0, step=0.01)
        public float fovScale = 1f;

        @Override
        public void render (GlContext ctx, RenderQueue queue) {
            // make sure we have something to render
            if (queue.isEmpty()) {
                return;
            }

            // as a hack, don't scale the fov if we're subrendering
            Renderer renderer = ctx.getRenderer();
            if (ctx.getCompositor().getSubrenderDepth() > 0) {
                super.render(ctx, queue);
                clearDepth(renderer);
                return;
            }

            // save the projection parameters
            float oleft = renderer.getLeft(), oright = renderer.getRight();
            float obottom = renderer.getBottom(), otop = renderer.getTop();
            float onear = renderer.getNear(), ofar = renderer.getFar();
            _onormal.set(renderer.getNearFarNormal());
            boolean oortho = renderer.isOrtho();

            // apply field of view scale (assumes we're using on-axis perspective projection)
            float tfov = otop / onear;
            float scale = FloatMath.tan(fovScale * FloatMath.atan(tfov)) / tfov;
            renderer.setProjection(
                oleft * scale, oright * scale, obottom * scale,
                otop * scale, onear, ofar, _onormal, false);

            // render
            super.render(ctx, queue);

            // restore the original projection
            renderer.setProjection(oleft, oright, obottom, otop, onear, ofar, _onormal, oortho);

            // clear the z buffer
            clearDepth(renderer);
        }

        /**
         * Clears the depth buffer.
         */
        protected void clearDepth (Renderer renderer)
        {
            renderer.setClearDepth(1f);
            renderer.setState(DepthState.TEST_WRITE);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        }

        /** Stores the near/far clip plane normal. */
        protected Vector3f _onormal = new Vector3f();
    }

    /** The type of the queue. */
    @Editable(hgroup="t")
    public String type = RenderQueue.NORMAL_TYPE;

    /** The priority of the queue. */
    @Editable(hgroup="t")
    public int priority;

    /** The queue's sort mode. */
    @Editable
    public SortMode sortMode = SortMode.BACK_TO_FRONT;

    /** The queue's render mode. */
    @Editable
    public RenderMode renderMode = new Normal();
}
