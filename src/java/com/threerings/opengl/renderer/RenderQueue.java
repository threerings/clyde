//
// $Id$

package com.threerings.opengl.renderer;

import java.util.ArrayList;
import java.util.Comparator;

import com.samskivert.util.QuickSort;

/**
 * Stores a group of {@link Batch}es enqueued for rendering.
 */
public class RenderQueue
{
    /**
     * Adds an opaque batch to the queue.
     */
    public void addOpaque (Batch batch)
    {
        _opaque.add(batch);
    }

    /**
     * Adds a transparent batch to the queue.
     */
    public void addTransparent (Batch batch)
    {
        _transparent.add(batch);
    }

    /**
     * Returns the identified transparency group subqueue.
     */
    public RenderQueue getTransparentQueue (int group)
    {
        for (int size = _groups.size(); size <= group; size++) {
            _groups.add(new QueueBatch());
        }
        return _groups.get(group).queue;
    }

    /**
     * Sorts the contents of the queue in preparation for rendering.
     */
    public void sort ()
    {
        // sort the opaque batches by state
        QuickSort.sort(_opaque, BY_KEY);

        // update and insert the transparency group subqueues
        for (int ii = 0, nn = _groups.size(); ii < nn; ii++) {
            QueueBatch batch = _groups.get(ii);
            if (batch.update()) {
                _transparent.add(batch);
            }
        }

        // sort the transparent batches by depth
        QuickSort.sort(_transparent, BACK_TO_FRONT);
    }

    /**
     * Renders the contents of the queue.
     */
    public void render (Renderer renderer)
    {
        renderer.render(_opaque);
        renderer.render(_transparent);
    }

    /**
     * Clears out the contents of the queue.
     */
    public void clear ()
    {
        _opaque.clear();
        _transparent.clear();
        for (int ii = 0, nn = _groups.size(); ii < nn; ii++) {
            _groups.get(ii).queue.clear();
        }
    }

    /**
     * Compares two packed state keys.
     */
    protected static int compareKeys (int[] k1, int[] k2)
    {
        int l1 = (k1 == null) ? 0 : k1.length;
        int l2 = (k2 == null) ? 0 : k2.length;
        int v1, v2, comp;
        for (int ii = 0, nn = Math.max(l1, l2); ii < nn; ii++) {
            v1 = (ii < l1) ? k1[ii] : 0;
            v2 = (ii < l2) ? k2[ii] : 0;
            if ((comp = v1 - v2) != 0) {
                return comp;
            }
        }
        return 0;
    }

    /**
     * A batch that contains its own {@link RenderQueue}.
     */
    protected static class QueueBatch extends Batch
    {
        /** The render queue for this batch. */
        public RenderQueue queue = new RenderQueue();

        /**
         * Updates the batch's depth value and sorts the batches in the queue.
         *
         * @return true if updated, false if there are no batches in the queue.
         */
        public boolean update ()
        {
            ArrayList<Batch> opaque = queue._opaque;
            ArrayList<Batch> transparent = queue._transparent;
            int osize = opaque.size();
            int tsize = transparent.size();
            if (osize == 0 && tsize == 0) {
                return false;
            }
            float tdepth = 0f;
            for (int ii = 0; ii < osize; ii++) {
                tdepth += opaque.get(ii).depth;
            }
            for (int ii = 0; ii < tsize; ii++) {
                tdepth += transparent.get(ii).depth;
            }
            depth = tdepth / (osize + tsize);
            queue.sort();
            return true;
        }

        @Override // documentation inherited
        public boolean draw (Renderer renderer)
        {
            queue.render(renderer);
            return false;
        }
    }

    /** The set of opaque batches. */
    protected ArrayList<Batch> _opaque = new ArrayList<Batch>();

    /** The set of transparent batches. */
    protected ArrayList<Batch> _transparent = new ArrayList<Batch>();

    /** The transparency group subqueues. */
    protected ArrayList<QueueBatch> _groups = new ArrayList<QueueBatch>();

    /** Sorts batches by state. */
    protected static final Comparator<Batch> BY_KEY = new Comparator<Batch>() {
        public int compare (Batch b1, Batch b2) {
            // if keys are the same, sort front-to-back
            int comp = compareKeys(b1.key, b2.key);
            return (comp == 0) ? Float.compare(b2.depth, b1.depth) : comp;
        }
    };

    /** Sorts batches by depth, back-to-front. */
    protected static final Comparator<Batch> BACK_TO_FRONT = new Comparator<Batch>() {
        public int compare (Batch b1, Batch b2) {
            return Float.compare(b1.depth, b2.depth);
        }
    };
}
