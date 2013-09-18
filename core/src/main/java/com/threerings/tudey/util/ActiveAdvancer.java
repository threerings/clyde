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

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.tudey.data.actor.Active;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;

/**
 * Advancer for active actors.
 */
public class ActiveAdvancer extends MobileAdvancer
{
    /**
     * Creates a new advancer for the supplied active.
     */
    public ActiveAdvancer (Environment environment, Active active, int timestamp)
    {
        super(environment, active, timestamp);
    }

    /**
     * Determines whether the actor can move.
     */
    public boolean canMove ()
    {
        Activity activity = getActivity();
        return activity == null || activity.allowsMovement();
    }

    /**
     * Determines whether the actor can rotate.
     */
    public boolean canRotate ()
    {
        Activity activity = getActivity();
        return activity == null || activity.allowsRotation();
    }

    @Override
    public void init (Actor actor, int timestamp)
    {
        super.init(actor, timestamp);
        _active = (Active)actor;
    }

    @Override
    protected void step (float elapsed)
    {
        // update the current activity
        Activity activity = getActivity();
        if (activity != null) {
            activity.step(elapsed);
        }
        super.step(elapsed);
    }

    @Override
    protected void mobileStep (float elapsed, int timestamp)
    {
        if (!canMove()) {
            _active.clear(Mobile.MOVING);
        }
        super.mobileStep(elapsed, timestamp);
    }

    /**
     * Returns a reference to the mapping for the current activity, or <code>null</code> for none.
     */
    protected Activity getActivity ()
    {
        return _activities.get(_active.getActivity());
    }

    /**
     * The mapping for an activity.
     */
    protected class Activity
    {
        /**
         * Creates a new activity that does not allow movement or rotation.
         *
         * @param clear the interval after which to clear the activity.
         */
        public Activity (int clear)
        {
            this(false, clear);
        }

        /**
         * Creates a new activity.
         *
         * @param movement whether or not to allow movement and/or rotation during the activity.
         * @param clear the interval after which to clear the activity.
         */
        public Activity (boolean movement, int clear)
        {
            this(movement, movement, clear);
        }

        /**
         * Creates a new activity.
         *
         * @param movement whether or not to allow movement during the activity.
         * @param rotation whether or not to allow rotation during the activity.
         * @param clear the interval after which to clear the activity.
         */
        public Activity (boolean movement, boolean rotation, int clear)
        {
            _movement = movement;
            _rotation = rotation;
            _clear = clear;
        }

        /**
         * Checks whether the activity allows movement.
         */
        public boolean allowsMovement ()
        {
            return _movement;
        }

        /**
         * Checks whether the activity allows rotation.
         */
        public boolean allowsRotation ()
        {
            return _rotation;
        }

        /**
         * Updates the activity for the current timestamp.
         */
        public void step (float elapsed)
        {
            int started = _active.getActivityStarted();
            if (_timestamp - started >= _clear) {
                _active.setActivity(Active.NONE, started + _clear);
            } else if (!canMove()) {
                _active.clear(Mobile.MOVING);
            }
        }

        /**
         * Allows the activity to respond to input.
         */
        public void updateInput ()
        {
            // nothing by default
        }

        /** Whether or not the activity allows movement. */
        protected boolean _movement;

        /** Whether or not the activity allows rotation. */
        protected boolean _rotation;

        /** The interval after which to clear the activity. */
        protected int _clear;
    }

    /** A casted reference to the active. */
    protected Active _active;

    /** The mappings for the various activities. */
    protected IntMap<Activity> _activities = IntMaps.newHashIntMap();
}
