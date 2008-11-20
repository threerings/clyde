//
// $Id$

package com.threerings.tudey.util;

import java.util.ArrayList;

import com.threerings.tudey.data.actor.Actor;

/**
 * Records a sequence of actor states and allows finding the interpolated historical state.
 */
public class ActorHistory
{
    /**
     * Creates a new history with the provided initial time and state.
     */
    public ActorHistory (int timestamp, Actor actor, int duration)
    {
        _duration = duration;
        record(timestamp, actor);
    }

    /**
     * Records a state in the stream.
     */
    public void record (int timestamp, Actor actor)
    {
        // add the new entry
        _entries.add(new Entry(timestamp, actor));

        // remove any out-of-date entries
        int oldest = timestamp - _duration;
        while (_entries.get(0).getTimestamp() < oldest) {
            _entries.remove(0);
        }
    }

    /**
     * Determines whether the actor has yet been created at the specified timestamp.
     */
    public boolean isCreated (int timestamp)
    {
        return timestamp >= _entries.get(0).getActor().getCreated();
    }

    /**
     * Determines whether the actor has been destroyed at the specified timestamp.
     */
    public boolean isDestroyed (int timestamp)
    {
        return timestamp >= _entries.get(_entries.size() - 1).getActor().getDestroyed();
    }

    /**
     * Finds the state at the specified timestamp and places it into the result object.
     */
    public Actor get (int timestamp, Actor result)
    {
        // extrapolate if before start or after end
        Entry start = _entries.get(0);
        if (timestamp <= start.getTimestamp()) {
            return start.extrapolate(timestamp, result);
        }
        int eidx = _entries.size() - 1;
        Entry end = _entries.get(eidx);
        if (timestamp >= end.getTimestamp()) {
            return end.extrapolate(timestamp, result);
        }

        // otherwise, use an interpolation search to find the closest two historical positions
        int sidx = 0, dist;
        while ((dist = eidx - sidx) != 1) {
            int midx;
            if (dist == 2) {
                midx = sidx + 1;
            } else {
                midx = sidx +
                    Math.min(Math.max(
                        Math.round(dist * start.getPortion(end, timestamp)),
                    1), dist - 1);
            }
            Entry middle = _entries.get(midx);
            if (timestamp < middle.getTimestamp()) {
                eidx = midx;
                end = middle;
            } else { // time >= middle.getTimestamp()
                sidx = midx;
                start = middle;
            }
        }
        return start.interpolate(end, timestamp, result);
    }

    /**
     * A single historical entry.
     */
    protected static class Entry
    {
        /**
         * Creates a new entry.
         */
        public Entry (int timestamp, Actor actor)
        {
            _timestamp = timestamp;
            _actor = actor;
        }

        /**
         * Returns the timestamp of the entry.
         */
        public int getTimestamp ()
        {
            return _timestamp;
        }

        /**
         * Returns a reference to the actor state.
         */
        public Actor getActor ()
        {
            return _actor;
        }

        /**
         * Extrapolates from this entry to the specified timestamp.
         */
        public Actor extrapolate (int timestamp, Actor result)
        {
            return _actor.extrapolate((timestamp - _timestamp) / 1000f, result);
        }

        /**
         * Interpolates between this entry and another.
         */
        public Actor interpolate (Entry other, int timestamp, Actor result)
        {
            return _actor.interpolate(other.getActor(), getPortion(other, timestamp), result);
        }

        /**
         * Returns the portion of time elapsed between this and the specified other entry.
         */
        public float getPortion (Entry other, int timestamp)
        {
            return (float)(timestamp - _timestamp) / (other.getTimestamp() - _timestamp);
        }

        /** The timestamp of this entry. */
        protected int _timestamp;

        /** The actor state. */
        protected Actor _actor;
    }

    /** The amount of time to retain entries. */
    protected int _duration;

    /** The stored list of entries. */
    protected ArrayList<Entry> _entries = new ArrayList<Entry>();
}
