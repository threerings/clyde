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

package com.threerings.tudey.data.actor;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;
import com.threerings.util.DeepOmit;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.util.ActiveAdvancer;
import com.threerings.tudey.util.ActorAdvancer;

/**
 * An actor capable of performing activities.
 */
public class Active extends Mobile
{
    /** Indicates that no activity is being performed. */
    public static final int NONE = 0;

    /** The last activity defined in this class. */
    public static final int LAST_ACTIVITY = NONE;

    /**
     * Creates a new active actor.
     */
    public Active (
        ConfigReference<ActorConfig> config, int id, int created,
        Vector2f translation, float rotation)
    {
        super(config, id, created, translation, rotation);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Active ()
    {
    }

    /**
     * Sets both the activity and the start timestamp.
     */
    public void setActivity (int activity, int started)
    {
        setActivity(activity);
        setActivityStarted(started);
    }

    /**
     * Sets the activity identifier.
     */
    public void setActivity (int activity)
    {
        _activity = activity;
        setDirty(true);
    }

    /**
     * Returns the activity identifier.
     */
    public int getActivity ()
    {
        return _activity;
    }

    /**
     * Sets the activity timestamp.
     */
    public void setActivityStarted (int started)
    {
        _activityStarted = started;
        setDirty(true);
    }

    /**
     * Returns the activity timestamp.
     */
    public int getActivityStarted ()
    {
        return _activityStarted;
    }

    @Override
    public Actor interpolate (Actor other, int start, int end, int timestamp, Actor result)
    {
        // apply the activity if the time has come
        Active aresult = (Active)super.interpolate(other, start, end, timestamp, result);
        Active oactive = (Active)other;
        int activityStarted = oactive.getActivityStarted();
        if (timestamp >= activityStarted) {
            aresult.setActivity(oactive.getActivity(), activityStarted);
        }
        return aresult;
    }

    @Override
    public ActorAdvancer createAdvancer (ActorAdvancer.Environment environment, int timestamp)
    {
        return new ActiveAdvancer(environment, this, timestamp);
    }

    @Override
    public Object copy (Object dest)
    {
        Active result = (Active)super.copy(dest);
        result._activity = _activity;
        result._activityStarted = _activityStarted;
        return result;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!super.equals(other)) {
            return false;
        }
        Active oactive = (Active)other;
        return _activity == oactive._activity &&
            _activityStarted == oactive._activityStarted;
    }

    @Override
    public int hashCode ()
    {
        int hash = super.hashCode();
        hash = 31*hash + _activity;
        hash = 31*hash + _activityStarted;
        return hash;
    }

    /** Identifies the activity being performed by the actor. */
    @DeepOmit
    protected int _activity;

    /** The time at which the current activity started. */
    @DeepOmit
    protected int _activityStarted;
}
