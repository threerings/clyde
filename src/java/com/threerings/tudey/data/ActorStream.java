//
// $Id$

package com.threerings.tudey.data;

import java.util.ArrayList;

/**
 * Contains a stream of actor states, allowing the state at any given time to be determined by
 * interpolation or extrapolation.
 */
public class ActorStream
{
    /**
     * Creates a new stream with the given initial frame.
     *
     * @param length the length of the stream (determines the number of frames to retain).
     */
    public ActorStream (long timestamp, Actor state, long length)
    {
        _actor = (Actor)state.clone();
        _length = length;
        updated(timestamp, state);
    }

    /**
     * Returns the "play head" actor.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Notes that the actor has been removed.
     */
    public void removed (long timestamp)
    {
        Frame last = _frames.get(_frames.size() - 1);
        Actor state = (Actor)last.state.clone();
        state.pstate = Actor.PresenceState.REMOVED;
        updated(timestamp, state);
    }

    /**
     * Notes that the actor has been updated.
     */
    public void updated (long timestamp, Actor state)
    {
        _frames.add(new Frame(timestamp, state));
        prune(timestamp - _length);
    }

    /**
     * Sets the state of the "play head" actor by interpolating between or extrapolating from the
     * frames in the stream.
     */
    public void seek (long timestamp)
    {
        int size = _frames.size();
        Frame first = _frames.get(0), last = _frames.get(size - 1);
        if (first == last || timestamp <= first.timestamp) {
            first.state.extrapolate((timestamp - first.timestamp) / 1000f, _actor);
            return;
        } else if (timestamp >= last.timestamp) {
            last.state.extrapolate((timestamp - last.timestamp) / 1000f, _actor);
            return;
        }
        for (int ii = size - 2; ii >= 0; ii--) {
            Frame frame = _frames.get(ii);
            if (timestamp >= frame.timestamp) {
                Frame next = _frames.get(ii + 1);
                float t = (float)(timestamp - frame.timestamp) /
                    (next.timestamp - frame.timestamp);
                frame.state.interpolate(next.state, t, _actor);
                return;
            }
        }
    }

    /**
     * Removes any states old enough not to affect the state at the given timestamp.
     */
    protected void prune (long timestamp)
    {
        for (int ii = _frames.size() - 1; ii >= 1; ii--) {
            Frame frame = _frames.get(ii);
            if (timestamp >= frame.timestamp) {
                _frames.subList(0, ii).clear();
                return;
            }
        }
    }

    /**
     * The actor's state at a single point in time.
     */
    protected static class Frame
    {
        /** The frame timestamp. */
        public long timestamp;

        /** The state of the actor. */
        public Actor state;

        public Frame (long timestamp, Actor state)
        {
            this.timestamp = timestamp;
            this.state = state;
        }

        @Override // documentation inherited
        public String toString ()
        {
            return "[timestamp=" + timestamp + ", state=" + state + "]";
        }
    }

    /** The recorded frames. */
    protected ArrayList<Frame> _frames = new ArrayList<Frame>();

    /** The "play head" actor. */
    protected Actor _actor;

    /** The length of the stream. */
    protected long _length;
}
