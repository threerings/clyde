//
// $Id$

package com.threerings.tudey.dobj;

import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.net.Transport;

import com.threerings.tudey.data.Actor;
import com.threerings.tudey.data.Effect;

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
        int targetOid, int sceneOid, int timestamp, Actor[] addedActors,
        ActorDelta[] updatedActorDeltas, int[] removedActorIds, Effect[] effects)
    {
        this(targetOid, sceneOid, timestamp, addedActors, updatedActorDeltas,
            removedActorIds, effects, Transport.DEFAULT);
    }

    /**
     * Creates a new delta event.
     */
    public SceneDeltaEvent (
        int targetOid, int sceneOid, int timestamp, Actor[] addedActors,
        ActorDelta[] updatedActorDeltas, int[] removedActorIds, Effect[] effects,
        Transport transport)
    {
        super(targetOid, transport);
        _sceneOid = sceneOid;
        _timestamp = timestamp;
        _addedActors = addedActors;
        _updatedActorDeltas = updatedActorDeltas;
        _removedActorIds = removedActorIds;
        _effects = effects;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public SceneDeltaEvent ()
    {
    }

    /**
     * Returns the oid of the scene to which this delta applies.
     */
    public int getSceneOid ()
    {
        return _sceneOid;
    }

    /**
     * Returns the timestamp of the delta.
     */
    public long getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns the array of actors added to the scene since the last delta, or <code>null</code>
     * for none.
     */
    public Actor[] getAddedActors ()
    {
        return _addedActors;
    }

    /**
     * Returns the array of deltas for actors updated since the last delta, or <code>null</code>
     * for none.
     */
    public ActorDelta[] getUpdatedActorDeltas ()
    {
        return _updatedActorDeltas;
    }

    /**
     * Returns the array of actor ids representing the actors removed from the scene since the last
     * delta, or <code>null</code> for none.
     */
    public int[] getRemovedActorIds ()
    {
        return _removedActorIds;
    }

    /**
     * Returns the array of effects fired since the last delta, or <code>null</code> for none.
     */
    public Effect[] getEffects ()
    {
        return _effects;
    }

    @Override // documentation inherited
    public boolean applyToObject (DObject target)
        throws ObjectAccessException
    {
        return true; // nothing to do here
    }

    @Override // documentation inherited
    protected void notifyListener (Object listener)
    {
        if (listener instanceof SceneDeltaListener) {
            ((SceneDeltaListener)listener).sceneDeltaReceived(this);
        }
    }

    @Override // documentation inherited
    protected void toString (StringBuilder buf)
    {
        buf.append("DELTA:");
        super.toString(buf);
        buf.append(", sceneOid=").append(_sceneOid);
        buf.append(", timestamp=").append(_timestamp);
        buf.append(", addedActors=").append(StringUtil.toString(_addedActors));
        buf.append(", updatedActorDeltas=").append(StringUtil.toString(_updatedActorDeltas));
        buf.append(", removedActorIds=").append(StringUtil.toString(_removedActorIds));
        buf.append(", effects=").append(StringUtil.toString(_effects));
    }

    /** The oid of the scene to which this event applies. */
    protected int _sceneOid;

    /** The timestamp of the delta. */
    protected long _timestamp;

    /** The actors added to the scene since the last delta (or <code>null</code>). */
    protected Actor[] _addedActors;

    /** The deltas of any actors updated since the last delta (or <code>null</code). */
    protected ActorDelta[] _updatedActorDeltas;

    /** The ids of the actors removed since the last delta (or <code>null</code>). */
    protected int[] _removedActorIds;

    /** The effects fired since the last delta (or <code>null</code>). */
    protected Effect[] _effects;
}
