//
// $Id$

package com.threerings.opengl.renderer;

import java.util.ArrayList;
import java.util.Comparator;

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.QuickSort;

/**
 * Stores a group of {@link Batch}es enqueued for rendering.
 */
public class RenderQueue
    implements Comparable<RenderQueue>
{
    /**
     * Creates a new render queue for the specified layer.
     */
    public RenderQueue (int layer)
    {
        _layer = layer;
    }

    /**
     * Adds an opaque batch to the queue.
     */
    public void addOpaque (Batch batch)
    {
        _opaque.add(batch, 0);
    }

    /**
     * Adds an opaque batch to the queue with the specified priority.
     */
    public void addOpaque (Batch batch, int priority)
    {
        _opaque.add(batch, priority);
    }

    /**
     * Adds a transparent batch to the queue.
     */
    public void addTransparent (Batch batch)
    {
        _transparent.add(batch, 0);
    }

    /**
     * Adds a transparent batch to the queue with the specified priority.
     */
    public void addTransparent (Batch batch, int priority)
    {
        _transparent.add(batch, priority);
    }

    /**
     * Returns the identified transparency group subqueue.
     */
    public RenderQueue getTransparentQueue (int group)
    {
        QueueBatch batch = _groups.get(group);
        if (batch == null) {
            _groups.put(group, batch = new QueueBatch());
            _batches.add(batch);
        }
        return batch.queue;
    }

    /**
     * Sorts the contents of the queue in preparation for rendering.
     */
    public void sort ()
    {
        // sort the opaque batches by state
        _opaque.sort(BY_KEY);

        // update and insert the transparency group subqueues
        for (int ii = 0, nn = _batches.size(); ii < nn; ii++) {
            QueueBatch batch = _batches.get(ii);
            int priority = batch.update();
            if (priority != Integer.MIN_VALUE) {
                _transparent.add(batch, priority);
            }
        }

        // sort the transparent batches by depth
        _transparent.sort(BACK_TO_FRONT);
    }

    /**
     * Renders the contents of the queue.
     */
    public void render (Renderer renderer)
    {
        _opaque.render(renderer);
        _transparent.render(renderer);
    }

    /**
     * Clears out the contents of the queue.
     */
    public void clear ()
    {
        _opaque.clear();
        _transparent.clear();
        for (int ii = 0, nn = _batches.size(); ii < nn; ii++) {
            _batches.get(ii).queue.clear();
        }
    }

    // documentation inherited from interface Comparable
    public int compareTo (RenderQueue other)
    {
        return _layer - other._layer;
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
        public RenderQueue queue = new RenderQueue(0);

        /**
         * Updates the batch in preparation for rendering.
         *
         * @return the highest priority of any batch in the queue, or {@link Integer#MIN_VALUE}
         * if the queue is empty.
         */
        public int update ()
        {
            depth = 0f;
            _total = 0;
            _priority = Integer.MIN_VALUE;
            process(queue._opaque);
            process(queue._transparent);
            if (_total > 0) {
                depth /= _total;
                queue.sort();
            }
            return _priority;
        }

        @Override // documentation inherited
        public boolean draw (Renderer renderer)
        {
            queue.render(renderer);
            return false;
        }

        /**
         * Processes the specified list of batches to obtain the batch count, highest priority,
         * and depth total.
         */
        protected void process (BatchList batches)
        {
            ComparableArrayList<PriorityList> lists = batches._lists;
            for (int ii = 0, nn = lists.size(); ii < nn; ii++) {
                PriorityList list = lists.get(ii);
                int size = list.size();
                if (size > 0) {
                    _total += size;
                    _priority = Math.max(_priority, list._priority);
                }
                for (int jj = 0; jj < size; jj++) {
                    Batch batch = list.get(jj);
                    depth += batch.depth;
                }
            }
        }

        /** The total number of batches. */
        protected int _total;

        /** The highest priority of any batch. */
        protected int _priority;
    }

    /**
     * Contains a list of batches.
     */
    protected static class BatchList
    {
        /**
         * Adds a batch to the list.
         */
        public void add (Batch batch, int priority)
        {
            PriorityList list = _priorities.get(priority);
            if (list == null) {
                _priorities.put(priority, list = new PriorityList(priority));
                _lists.insertSorted(list);
            }
            list.add(batch);
        }

        /**
         * Sorts the batches in the list.
         */
        public void sort (Comparator<Batch> comparator)
        {
            for (int ii = 0, nn = _lists.size(); ii < nn; ii++) {
                QuickSort.sort(_lists.get(ii), comparator);
            }
        }

        /**
         * Renders the batches in the list.
         */
        public void render (Renderer renderer)
        {
            for (int ii = 0, nn = _lists.size(); ii < nn; ii++) {
                renderer.render(_lists.get(ii));
            }
        }

        /**
         * Clears the batches from the list.
         */
        public void clear ()
        {
            for (int ii = 0, nn = _lists.size(); ii < nn; ii++) {
                _lists.get(ii).clear();
            }
        }

        /** Maps priorities to batch lists. */
        protected HashIntMap<PriorityList> _priorities = new HashIntMap<PriorityList>();

        /** The set of batch lists, sorted by priority. */
        protected ComparableArrayList<PriorityList> _lists =
            new ComparableArrayList<PriorityList>();
    }

    /**
     * A list of batches at the same priority level.
     */
    protected static class PriorityList extends ArrayList<Batch>
        implements Comparable<PriorityList>
    {
        /**
         * Creates a new list with the supplied priority.
         */
        public PriorityList (int priority)
        {
            _priority = priority;
        }

        // documentation inherited from interface Comparable
        public int compareTo (PriorityList other)
        {
            return _priority - other._priority;
        }

        /** The priority level of this list. */
        protected int _priority;
    }

    /** The layer that this queue represents. */
    protected int _layer;

    /** The set of opaque batches. */
    protected BatchList _opaque = new BatchList();

    /** The set of transparent batches. */
    protected BatchList _transparent = new BatchList();

    /** Maps transparency groups to their batches. */
    protected HashIntMap<QueueBatch> _groups = new HashIntMap<QueueBatch>();

    /** The set of transparency group batches. */
    protected ArrayList<QueueBatch> _batches = new ArrayList<QueueBatch>();

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
