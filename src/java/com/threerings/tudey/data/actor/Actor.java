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
     * Returns the actor's unique identifier.
     */
    public int getId ()
    {
        return _id;
    }

    /**
     * Creates a sprite to represent this actor on the client.
     */
    public abstract ActorSprite createSprite (
        TudeyContext ctx, TudeySceneView view, long timestamp);

    /** The actor's unique identifier. */
    protected int _id;
}
