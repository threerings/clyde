//
// $Id$

package com.threerings.tudey.dobj;

import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.net.Transport;

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
        int targetOid, int sceneOid, long acknowledge, long timestamp, AddedActor[] addedActors,
        ActorDelta[] updatedActors, RemovedActor[] removedActors, Effect[] effects)
    {
        this(targetOid, sceneOid, acknowledge, timestamp, addedActors, updatedActors,
            removedActors, effects, Transport.DEFAULT);
    }

    /**
     * Creates a new delta event.
     */
    public SceneDeltaEvent (
        int targetOid, int sceneOid, long acknowledge, long timestamp, AddedActor[] addedActors,
        ActorDelta[] updatedActors, RemovedActor[] removedActors, Effect[] effects,
        Transport transport)
    {
        super(targetOid, transport);
        _sceneOid = sceneOid;
        _acknowledge = acknowledge;
        _timestamp = timestamp;
        _addedActors = addedActors;
        _updatedActors = updatedActors;
        _removedActors = removedActors;
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
     * Returns the timestamp of the last input frame received by the server.
     */
    public long getAcknowledge ()
    {
        return _acknowledge;
    }

    /**
     * Returns the timestamp of the delta.
     */
    public long getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns a reference to the array of actors added to the scene since the last delta, or
     * <code>null</code> for none.
     */
    public AddedActor[] getAddedActors ()
    {
        return _addedActors;
    }

    /**
     * Returns a reference to the array of actors updated since the last delta, or
     * <code>null</code> for none.
     */
    public ActorDelta[] getUpdatedActors ()
    {
        return _updatedActors;
    }

    /**
     * Returns a reference to the array of actors removed from the scene since the last delta, or
     * <code>null</code> for none.
     */
    public RemovedActor[] getRemovedActors ()
    {
        return _removedActors;
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
        buf.append(", acknowledge=").append(_acknowledge);
        buf.append(", timestamp=").append(_timestamp);
        buf.append(", addedActors=").append(StringUtil.toString(_addedActors));
        buf.append(", updatedActors=").append(StringUtil.toString(_updatedActors));
        buf.append(", removedActors=").append(StringUtil.toString(_removedActors));
        buf.append(", effects=").append(StringUtil.toString(_effects));
    }

    /** The oid of the scene to which this event applies. */
    protected int _sceneOid;

    /** The timestamp of the latest input frame received by the server. */
    protected long _acknowledge;

    /** The timestamp of the delta. */
    protected long _timestamp;

    /** The actors added to the scene since the last delta (or <code>null</code>). */
    protected AddedActor[] _addedActors;

    /** The actors updated since the last delta (or <code>null</code). */
    protected ActorDelta[] _updatedActors;

    /** The actors removed since the last delta (or <code>null</code>). */
    protected RemovedActor[] _removedActors;

    /** The effects fired since the last delta (or <code>null</code>). */
    protected Effect[] _effects;
}
