//
// $Id$

package com.threerings.tudey.data.actor;

import com.threerings.io.Streamable;

import com.threerings.delta.Deltable;
import com.threerings.util.DeepObject;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.ActorSprite;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents an active, stateful element of the scene.
 */
public abstract class Actor extends DeepObject
    implements Streamable, Deltable
{
    /**
     * Creates a new actor.
     */
    public Actor (int id, int created)
    {
        _id = id;
        _created = created;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Actor ()
    {
        // these will be set later
        _id = 0;
        _created = 0;
    }

    /**
     * Returns the actor's unique identifier.
     */
    public int getId ()
    {
        return _id;
    }

    /**
     * Returns the timestamp at which the actor was created.
     */
    public int getCreated ()
    {
        return _created;
    }

    /**
     * Creates a sprite to represent this actor on the client.
     */
    public abstract ActorSprite createSprite (
        TudeyContext ctx, TudeySceneView view, long timestamp);

    /**
     * Interpolates between the state of this actor and that of the specified other, placing the
     * result in the provided object.
     */
    public Actor interpolate (Actor other, float t, Actor result)
    {
        return (Actor)copy(result); // default implementation simply deep-copies
    }

    /**
     * Extrapolates the state of this actor after the specified time interval, in seconds (which
     * may be negative).
     */
    public Actor extrapolate (float elapsed, Actor result)
    {
        return (Actor)copy(result);
    }

    /** The actor's unique identifier. */
    protected final int _id;

    /** The timestamp at which the actor was created. */
    protected final int _created;
}
