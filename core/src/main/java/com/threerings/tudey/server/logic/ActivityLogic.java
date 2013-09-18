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

import com.threerings.math.Vector2f;

import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;

/**
 * Logic classes for activities.
 */
public abstract class ActivityLogic extends Logic
{
    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, ActiveLogic source)
    {
        super.init(scenemgr);
        _source = source;
    }

    /**
     * Starts the activity.
     */
    public void start (int timestamp)
    {
        _started = timestamp - (shouldAdvance() ? _source.getActivityAdvance() : 0);
    }

    /**
     * Stops the activity.
     */
    public void stop (int timestamp)
    {
    }

    /**
     * Updates the activity.
     */
    public void tick (int timestamp)
    {
        // nothing by default
    }

    @Override
    public boolean isActive ()
    {
        return _source.isActive();
    }

    @Override
    public EntityKey getEntityKey ()
    {
        return _source.getEntityKey();
    }

    @Override
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override
    public float getRotation ()
    {
        return _source.getRotation();
    }

    @Override
    public Shape getShape ()
    {
        return _source.getShape();
    }

    @Override
    public void addShapeObserver (ShapeObserver observer)
    {
        _source.addShapeObserver(observer);
    }

    @Override
    public void removeShapeObserver (ShapeObserver observer)
    {
        _source.removeShapeObserver(observer);
    }

    @Override
    public void transfer (Logic source, Map<Object, Object> refs)
    {
        super.transfer(source, refs);
        _started = ((ActivityLogic)source)._started;
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /**
     * Checks whether we should advance the activity to compensate for control latency, if
     * relevant.
     */
    protected boolean shouldAdvance ()
    {
        return true;
    }

    /** The activity source. */
    protected ActiveLogic _source;

    /** The time at which the activity started. */
    protected int _started;
}
