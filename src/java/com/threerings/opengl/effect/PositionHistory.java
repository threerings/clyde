//
// $Id$

package com.threerings.opengl.effect;

import java.util.ArrayList;

import com.threerings.math.Vector3f;

/**
 * Stores all of the positions occupied by a particle up to a maximum interval and provides
 * a means of retrieving an interpolated historical position.
 */
public final class PositionHistory
{
    /**
     * (Re)initializes the history.
     *
     * @param initial the initial position.
     */
    public void init (Vector3f initial)
    {
        // throw all entries back in the pool
        _pool.addAll(_entries);
        _entries.clear();

        // add the initial entry
        addEntry(initial, _time = 0f);
    }

    /**
     * Records a position occupied by the particle.
     */
    public void record (Vector3f position, float elapsed, float length)
    {
        if (elapsed <= 0f) {
            return; // make sure we have actually advanced
        }
        addEntry(position, _time += elapsed);

        // we can remove everything up to the first entry past the cutoff (which we will
        // need for interpolation)
        float cutoff = _time - (_length = length);
        while (_entries.get(1).time <= cutoff) {
            _pool.add(_entries.remove(0));
        }
    }

    /**
     * Computes the position at some point in the past.
     *
     * @param t the time parameter: zero for the oldest location, one for the newest.
     */
    public Vector3f get (float t, Vector3f result)
    {
        // use an interpolation search to find the closest two historical positions
        float time = _time - (1f - t)*_length;
        Entry start = _entries.get(0);
        if (time <= start.time) {
            return result.set(start.position);
        }
        int eidx = _entries.size() - 1;
        Entry end = _entries.get(eidx);
        if (time >= end.time) {
            return result.set(end.position);
        }
        int sidx = 0, dist;
        while ((dist = eidx - sidx) != 1) {
            int midx;
            if (dist == 2) {
                midx = sidx + 1;
            } else {
                midx = sidx +
                    Math.min(Math.max(
                        Math.round(dist * (time - start.time) / (end.time - start.time)),
                    1), dist - 1);
            }
            Entry middle = _entries.get(midx);
            if (time <= middle.time) {
                eidx = midx;
                end = middle;
            } else { // time > middle.time
                sidx = midx;
                start = middle;
            }
        }
        return start.position.lerp(end.position,
            (time - start.time) / (end.time - start.time), result);
    }

    /**
     * Adds an entry for the specified values, fetching one from the pool if possible.
     */
    protected void addEntry (Vector3f position, float time)
    {
        int size = _pool.size();
        _entries.add(size == 0 ?
            new Entry(position, time) : _pool.remove(size - 1).set(position, time));
    }

    /**
     * An entry in the history.
     */
    protected static final class Entry
    {
        /** The stored position. */
        public Vector3f position = new Vector3f();

        /** The associated time. */
        public float time;

        public Entry (Vector3f position, float time)
        {
            set(position, time);
        }

        /**
         * Sets the contents of this entry.
         *
         * @return a reference to this entry, for chaining.
         */
        public Entry set (Vector3f position, float time)
        {
            this.position.set(position);
            this.time = time;
            return this;
        }
    }

    /** The last time value recorded. */
    protected float _time;

    /** The current length. */
    protected float _length;

    /** The entries in the history. */
    protected ArrayList<Entry> _entries = new ArrayList<Entry>();

    /** The pool of entries to reuse. */
    protected ArrayList<Entry> _pool = new ArrayList<Entry>();
}
