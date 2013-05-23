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

package com.threerings.tudey.dobj;

import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.net.Transport;

import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.effect.Effect;

/**
 * Represents an update to the dynamic state of the scene.  Each delta represents difference
 * between the current state and the last state (either the baseline state or the result of
 * applying the last acknowledged delta).  These events are published on the client object,
 * rather than the scene object, because they are targeted at specific clients.
 */
public class SceneDeltaEvent extends DEvent
{
    /**
     * Creates a new delta event.
     */
    public SceneDeltaEvent (
        int targetOid, int sceneOid, int acknowledge, short ping, int reference,
        int timestamp, short elapsed, Actor[] addedActors, ActorDelta[] updatedActorDeltas,
        int[] removedActorIds, Effect[] effectsFired)
    {
        super(targetOid);
        _sceneOid = sceneOid;
        _acknowledge = acknowledge;
        _ping = ping;
        _reference = reference;
        _timestamp = timestamp;
        _elapsed = elapsed;
        _addedActors = addedActors;
        _updatedActorDeltas = updatedActorDeltas;
        _removedActorIds = removedActorIds;
        _effectsFired = effectsFired;
    }

    /**
     * Returns the oid of the scene to which this delta applies.
     */
    public int getSceneOid ()
    {
        return _sceneOid;
    }

    /**
     * Returns the timestamp of the last input frame received by the server.
     */
    public int getAcknowledge ()
    {
        return _acknowledge;
    }

    /**
     * Returns the ping time estimate.
     */
    public short getPing ()
    {
        return _ping;
    }

    /**
     * Returns the timestamp of the update that serves as a basis of comparison for this delta
     * (either the last delta known to be acknowledged by the client, or 0 for the baseline).
     */
    public int getReference ()
    {
        return _reference;
    }

    /**
     * Returns the timestamp of the delta.
     */
    public int getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns the time elapsed since the last tick.
     */
    public short getElapsed ()
    {
        return _elapsed;
    }

    /**
     * Returns a reference to the array of actors added to the scene since the last delta, or
     * <code>null</code> for none.
     */
    public Actor[] getAddedActors ()
    {
        return _addedActors;
    }

    /**
     * Returns a reference to the array of deltas for actors updated since the last delta, or
     * <code>null</code> for none.
     */
    public ActorDelta[] getUpdatedActorDeltas ()
    {
        return _updatedActorDeltas;
    }

    /**
     * Returns a reference to the array of ids of actors removed from the scene since the last
     * delta, or <code>null</code> for none.
     */
    public int[] getRemovedActorIds ()
    {
        return _removedActorIds;
    }

    /**
     * Returns the array of effects fired since the last delta, or <code>null</code> for none.
     */
    public Effect[] getEffectsFired ()
    {
        return _effectsFired;
    }

    @Override
    public boolean applyToObject (DObject target)
        throws ObjectAccessException
    {
        return true; // nothing to do here
    }

    @Override
    protected void notifyListener (Object listener)
    {
        if (listener instanceof SceneDeltaListener) {
            ((SceneDeltaListener)listener).sceneDeltaReceived(this);
        }
    }

    @Override
    protected void toString (StringBuilder buf)
    {
        buf.append("DELTA:");
        super.toString(buf);
        buf.append(", sceneOid=").append(_sceneOid);
        buf.append(", acknowledge=").append(_acknowledge);
        buf.append(", ping=").append(_ping);
        buf.append(", reference=").append(_reference);
        buf.append(", timestamp=").append(_timestamp);
        buf.append(", elapsed=").append(_elapsed);
        buf.append(", addedActors=").append(StringUtil.toString(_addedActors));
        buf.append(", updatedActorDeltas=").append(StringUtil.toString(_updatedActorDeltas));
        buf.append(", removedActorIds=").append(StringUtil.toString(_removedActorIds));
        buf.append(", effectsFired=").append(StringUtil.toString(_effectsFired));
    }

    /** The oid of the scene to which this event applies. */
    protected int _sceneOid;

    /** The timestamp of the latest input frame received by the server. */
    protected int _acknowledge;

    /** The estimated ping time. */
    protected short _ping;

    /** The timestamp of the update that serves as a basis of comparison for this delta (either
     * the last delta known to be acknowledged by the client, or 0 for the baseline). */
    protected int _reference;

    /** The timestamp of the delta. */
    protected int _timestamp;

    /** The amount of time elapsed since the previous tick. */
    protected short _elapsed;

    /** The actors added to the scene since the referenced update (or <code>null</code>). */
    protected Actor[] _addedActors;

    /** The deltas of the actors updated since the referenced update (or <code>null</code). */
    protected ActorDelta[] _updatedActorDeltas;

    /** The ids of the actors removed since the referenced update (or <code>null</code>). */
    protected int[] _removedActorIds;

    /** The effects fired since the last delta (or <code>null</code>). */
    protected Effect[] _effectsFired;
}
