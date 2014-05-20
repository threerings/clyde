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

package com.threerings.tudey.util;

import java.util.ArrayList;

import com.threerings.math.FloatMath;

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
        init(timestamp, actor);
    }

    /**
     * (Re)initializes the history.
     */
    public void init (int timestamp, Actor actor)
    {
        _entries.clear();
        record(timestamp, actor, true);
    }

    /**
     * Records a state in the stream.
     */
    public void record (int timestamp, Actor actor, boolean updated)
    {
        // add the new entry
        _entries.add(new Entry(timestamp, actor));
        if (updated) {
            _lastUpdate = timestamp;
            _seenLast = false;
        }

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
     * Returns the actor state from the last recieved entry.
     */
    public Actor getLastKnownActor ()
    {
        return _entries.get(_entries.size() - 1).getActor();
    }

    /**
     * Finds the state at the specified timestamp and places it into the result object.
     */
    public boolean get (int timestamp, Actor result, boolean isStatic)
    {
        if (_seenLast && isStatic) {
            return false;
        }
        _seenLast = timestamp >= _lastUpdate;
        // extrapolate if before start or after end
        Entry start = _entries.get(0);
        if (timestamp <= start.getTimestamp()) {
            start.extrapolate(timestamp, result);
            return true;
        }
        int eidx = _entries.size() - 1;
        Entry end = _entries.get(eidx);
        if (timestamp >= end.getTimestamp()) {
            end.extrapolate(timestamp, result);
            return true;
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
                        FloatMath.round(dist * start.getPortion(end, timestamp)),
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
        start.interpolate(end, timestamp, result);
        return true;
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
            return _actor.extrapolate((timestamp - _timestamp) / 1000f, timestamp, result);
        }

        /**
         * Interpolates between this entry and another.
         */
        public Actor interpolate (Entry other, int timestamp, Actor result)
        {
            return _actor.interpolate(
                other.getActor(), _timestamp, other.getTimestamp(), timestamp, result);
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

    /** If the last entry has been seen by the actor sprite. */
    protected boolean _seenLast;

    /** The timestamp of the last update. */
    protected int _lastUpdate;
}
