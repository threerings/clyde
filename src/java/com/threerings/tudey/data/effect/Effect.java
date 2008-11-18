//
// $Id$

package com.threerings.tudey.data.effect;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents a stateless event occurring within the scene.
 */
public abstract class Effect extends SimpleStreamableObject
{
    /**
     * Creates a new effect.
     */
    public Effect (long timestamp)
    {
        _timestamp = timestamp;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Effect ()
    {
    }

    /**
     * Returns the time at which the effect was fired.
     */
    public long getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Handles the effect on the client.
     */
    public abstract void handle (TudeyContext ctx, TudeySceneView view);

    /** The time at which the effect was fired. */
    protected long _timestamp;
}
