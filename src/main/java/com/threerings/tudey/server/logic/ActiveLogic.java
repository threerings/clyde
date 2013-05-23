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

package com.threerings.tudey.server.logic;

import java.util.Map;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.tudey.data.actor.Active;

/**
 * Controls the state of an active actor.
 */
public class ActiveLogic extends MobileLogic
{
    /**
     * Returns the amount of time to advance activities to compensate for control latency.
     */
    public int getActivityAdvance ()
    {
        return 0;
    }

    @Override
    public void transfer (Logic source, Map<Object, Object> refs)
    {
        super.transfer(source, refs);

        ActiveLogic asource = (ActiveLogic)source;
        for (IntMap.IntEntry<ActivityLogic> entry : asource._activities.intEntrySet()) {
            if (_activities.get(entry.getIntKey()) != null) {
                _activities.get(entry.getIntKey()).transfer(entry.getValue(), refs);
            }
        }
        _lastActivityStarted = asource._lastActivityStarted;
    }

    @Override
    public boolean tick (int timestamp)
    {
        super.tick(timestamp);

        updateActivities(timestamp);

        // update the activity
        Active active = (Active)_actor;
        ActivityLogic activity = _activities.get(active.getActivity());
        if (_lastTickedActivity != activity && _lastTickedActivity != null) {
            _lastTickedActivity.stop(timestamp);
        }
        _lastTickedActivity = activity;
        if (activity != null) {
            int started = active.getActivityStarted();
            if (started > _lastActivityStarted) {
                activity.start(_lastActivityStarted = started);
            }
            activity.tick(timestamp);
        }

        return true;
    }

    /**
     * Called to update any activities before they are ticked.
     */
    protected void updateActivities (int timestamp)
    {
    }

    /** Activity logic mappings. */
    protected IntMap<ActivityLogic> _activities = IntMaps.newHashIntMap();

    /** The time at which the last activity started. */
    protected int _lastActivityStarted;

    /** The last activity we ticked. */
    protected ActivityLogic _lastTickedActivity;
}
