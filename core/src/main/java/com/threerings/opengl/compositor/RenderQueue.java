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

package com.threerings.opengl.compositor;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMaps;

import com.threerings.opengl.renderer.Batch;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.compositor.config.RenderQueueConfig;

/**
 * Stores a group of {@link Batch}es enqueued for rendering.
 */
public class RenderQueue
    implements Comparable<RenderQueue>
{
    /** The name of the opaque queue. */
    public static final String OPAQUE = "Opaque";

    /** The name of the transparent queue. */
    public static final String TRANSPARENT = "Transparent";

    /** The name of the overlay queue. */
    public static final String OVERLAY = "Overlay";

    /** The normal render queue type. */
    public static final String NORMAL_TYPE = "normal";

    /**
     * Contains a group of render queues.
     */
    public static class Group
    {
        /**
         * Represents the saved state of a group.
         */
        public static class State
        {
            /**
             * Swaps this state with the of the specified group.
             */
            public void swap (Group group)
            {
                ComparableArrayList<RenderQueue> queues = group._queues;
                int nqueues = queues.size();
                for (int ii = nqueues - _qstates.size(); ii > 0; ii--) {
                    _qstates.add(new RenderQueue.State());
                }
                for (int ii = 0; ii < nqueues; ii++) {
                    _qstates.get(ii).swap(queues.get(ii));
                }
            }

            /** Saved states for each queue. */
            protected ArrayList<RenderQueue.State> _qstates = Lists.newArrayList();
        }

        /**
         * Creates a new group.
         */
        public Group (GlContext ctx)
        {
            _ctx = ctx;
        }

        /**
         * Retrieves a reference to a render queue.
         */
        public RenderQueue getQueue (String name)
        {
            RenderQueue queue = _queuesByName.get(name);
            if (queue == null) {
                _queuesByName.put(name, queue = new RenderQueue(_ctx, name));
                _queues.insertSorted(queue);
            }
            return queue;
        }

        /**
         * Sorts the queues in preparation for rendering.
         */
        public void sortQueues ()
        {
            for (int ii = 0, nn = _queues.size(); ii < nn; ii++) {
                _queues.get(ii).sort();
            }
        }

        /**
         * Renders the contents of the queues.
         */
        public void renderQueues ()
        {
            for (int ii = 0, nn = _queues.size(); ii < nn; ii++) {
                _queues.get(ii).render();
            }
        }

        /**
         * Renders the contents of the queues of the specified type.
         */
        public void renderQueues (String type)
        {
            for (int ii = 0, nn = _queues.size(); ii < nn; ii++) {
                RenderQueue queue = _queues.get(ii);
                if (queue._config.type.equals(type)) {
                    queue.render();
                }
            }
        }

        /**
         * Renders the contents of the queues of the specified type whose priority levels fall
         * within the specified (inclusive) limits.
         */
        public void renderQueues (String type, int minPriority, int maxPriority)
        {
            for (int ii = 0, nn = _queues.size(); ii < nn; ii++) {
                RenderQueue queue = _queues.get(ii);
                RenderQueueConfig config = queue._config;
                if (config.type.equals(type) && config.priority >= minPriority &&
                        config.priority <= maxPriority) {
                    queue.render();
                }
            }
        }

        /**
         * Clears out the contents of the queues.
         */
        public void clearQueues ()
        {
            for (int ii = 0, nn = _queues.size(); ii < nn; ii++) {
                _queues.get(ii).clear();
            }
        }

        /** The application context. */
        protected GlContext _ctx;

        /** Maps render queue names to the created queues. */
        protected HashMap<String, RenderQueue> _queuesByName = Maps.newHashMap();

        /** The set of render queues, sorted by priority. */
        protected ComparableArrayList<RenderQueue> _queues =
            new ComparableArrayList<RenderQueue>();
    }

    /**
     * Creates a new render queue.
     */
    public RenderQueue (GlContext ctx, String name)
    {
        _ctx = ctx;
        if ((_config = ctx.getConfigManager().getConfig(RenderQueueConfig.class, name)) == null) {
            _config = new RenderQueueConfig();
        }
    }

    /**
     * Adds a batch to the queue at the default priority.
     */
    public void add (Batch batch)
    {
        add(batch, 0);
    }

    /**
     * Adds a batch to the queue with the specified priority.
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
     * Returns the identified group within this queue.
     */
    public Group getGroup (int group)
    {
        GroupBatch batch = _groups.get(group);
        if (batch == null) {
            _groups.put(group, batch = new GroupBatch(_ctx));
            _batches.add(batch);
        }
        return batch.group;
    }

    /**
     * Sorts the contents of the queue in preparation for rendering.
     */
    public void sort ()
    {
        // update and insert the group batches
        for (int ii = 0, nn = _batches.size(); ii < nn; ii++) {
            GroupBatch batch = _batches.get(ii);
            if (batch.update()) {
                add(batch);
            }
        }

        // sort each list
        for (int ii = 0, nn = _lists.size(); ii < nn; ii++) {
            _config.sortMode.sort(_lists.get(ii));
        }
    }

    /**
     * Determines whether this queue is empty (after sorting).
     */
    public boolean isEmpty ()
    {
        for (int ii = 0, nn = _lists.size(); ii < nn; ii++) {
            if (!_lists.get(ii).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Renders the contents of the queue.
     */
    public void render ()
    {
        _config.renderMode.render(_ctx, this);
    }

    /**
     * Clears out the contents of the queue.
     */
    public void clear ()
    {
        for (int ii = 0, nn = _lists.size(); ii < nn; ii++) {
            _lists.get(ii).clear();
        }
        for (int ii = 0, nn = _batches.size(); ii < nn; ii++) {
            _batches.get(ii).group.clearQueues();
        }
    }

    /**
     * Renders the contents of the lists.  This is called by the
     * {@link com.threerings.opengl.compositor.config.RenderQueueConfig.RenderMode}.
     */
    public void renderLists (Renderer renderer)
    {
        for (int ii = 0, nn = _lists.size(); ii < nn; ii++) {
            renderer.render(_lists.get(ii));
        }
    }

    // documentation inherited from interface Comparable
    public int compareTo (RenderQueue other)
    {
        return _config.priority - other._config.priority;
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

    /**
     * A batch that contains its own {@link Group}.
     */
    protected static class GroupBatch extends Batch
    {
        /**
         * Represents the saved state of a batch.
         */
        public static class State
        {
            /**
             * Swaps this state with the of the specified batch.
             */
            public void swap (GroupBatch batch)
            {
                // swap depths
                float odepth = _depth;
                _depth = batch.depth;
                batch.depth = odepth;

                // swap keys
                int[] okey = _key;
                _key = batch.key;
                batch.key = okey;

                // swap the group state
                _gstate.swap(batch.group);
            }

            /** The saved depth. */
            protected float _depth;

            /** The saved key. */
            protected int[] _key;

            /** The saved group state. */
            protected Group.State _gstate = new Group.State();
        }

        /** The queue group for this batch. */
        public Group group;

        /**
         * Creates a new group batch.
         */
        public GroupBatch (GlContext ctx)
        {
            group = new Group(ctx);
        }

        /**
         * Updates the batch in preparation for rendering.
         *
         * @return true if the group contains batches for rendering, false if it is empty.
         */
        public boolean update ()
        {
            int total = 0;
            depth = 0f;
            key = null;

            group.sortQueues();
            ComparableArrayList<RenderQueue> queues = group._queues;
            for (int ii = 0, nn = queues.size(); ii < nn; ii++) {
                ComparableArrayList<PriorityList> lists = queues.get(ii)._lists;
                for (int jj = 0, mm = lists.size(); jj < mm; jj++) {
                    PriorityList list = lists.get(jj);
                    for (int kk = 0, ll = list.size(); kk < ll; kk++) {
                        Batch batch = list.get(kk);
                        depth += batch.depth;
                        key = (batch.key == null) ? key : batch.key;
                        total++;
                    }
                }
            }
            if (total > 0) {
                depth /= total;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean draw (Renderer renderer)
        {
            group.renderQueues();
            return false;
        }
    }

    /**
     * Represents the saved state of a queue.
     */
    protected static class State
    {
        /**
         * Swaps this state with the of the specified queue.
         */
        public void swap (RenderQueue queue)
        {
            // swap priority maps
            HashIntMap<PriorityList> opriorities = _priorities;
            _priorities = queue._priorities;
            queue._priorities = opriorities;

            // swap lists
            ComparableArrayList<PriorityList> olists = _lists;
            _lists = queue._lists;
            queue._lists = olists;

            // swap the batch states
            ArrayList<GroupBatch> batches = queue._batches;
            int nbatches = batches.size();
            for (int ii = nbatches - _bstates.size(); ii > 0; ii--) {
                _bstates.add(new GroupBatch.State());
            }
            for (int ii = 0; ii < nbatches; ii++) {
                _bstates.get(ii).swap(batches.get(ii));
            }
        }

        /** The saved priority map. */
        protected HashIntMap<PriorityList> _priorities = IntMaps.newHashIntMap();

        /** The saved set of batch lists. */
        protected ComparableArrayList<PriorityList> _lists =
            new ComparableArrayList<PriorityList>();

        /** Saved states for each batch. */
        protected ArrayList<GroupBatch.State> _bstates = Lists.newArrayList();
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The queue configuration. */
    protected RenderQueueConfig _config;

    /** Maps priorities to batch lists. */
    protected HashIntMap<PriorityList> _priorities = IntMaps.newHashIntMap();

    /** The set of batch lists, sorted by priority. */
    protected ComparableArrayList<PriorityList> _lists = new ComparableArrayList<PriorityList>();

    /** Maps group ids to their batches. */
    protected HashIntMap<GroupBatch> _groups = IntMaps.newHashIntMap();

    /** The set of group batches. */
    protected ArrayList<GroupBatch> _batches = Lists.newArrayList();
}
