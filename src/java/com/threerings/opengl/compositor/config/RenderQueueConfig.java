//
// $Id$

package com.threerings.opengl.compositor.config;

import java.util.Comparator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.samskivert.util.QuickSort;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.renderer.Batch;
import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.compositor.RenderQueue;

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
        protected Comparator<Batch> _comparator;
    }

    /** The available render modes. */
    public enum RenderMode
    {
        /** Renders the batches normally. */
        NORMAL(),

        /** Renders the batches in window space using an orthographic projection. */
        ORTHO() {
            public void render (Renderer renderer, RenderQueue queue) {
                // make sure we have something to render
                if (queue.isEmpty()) {
                    return;
                }

                // save the projection matrix and replace it with the ortho matrix
                renderer.setMatrixMode(GL11.GL_PROJECTION);
                GL11.glPushMatrix();
                GL11.glLoadIdentity();
                Rectangle viewport = renderer.getViewport();
                GL11.glOrtho(0f, viewport.width, 0f, viewport.height, -1f, +1f);

                super.render(renderer, queue);

                // restore the projection matrix
                renderer.setMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
            }
        };

        /**
         * Renders the batches.
         */
        public void render (Renderer renderer, RenderQueue queue)
        {
            queue.renderLists(renderer);
        }
    }

    /** The priority of the queue. */
    @Editable
    public int priority;

    /** The queue's sort mode. */
    @Editable(hgroup="m")
    public SortMode sortMode = SortMode.BACK_TO_FRONT;

    /** The queue's render mode. */
    @Editable(hgroup="m")
    public RenderMode renderMode = RenderMode.NORMAL;
}
