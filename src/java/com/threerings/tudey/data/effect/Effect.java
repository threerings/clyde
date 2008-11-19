//
// $Id$

package com.threerings.tudey.data.effect;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.math.Rect;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.handler.EffectHandler;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents a stateless event occurring within the scene.
 */
public abstract class Effect extends SimpleStreamableObject
{
    /**
     * Creates a new effect.
     */
    public Effect (int timestamp)
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
    public int getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns the time at which this effect expires (that is, when it should no longer be
     * retransmitted to clients).
     */
    public int getExpiry ()
    {
        return _timestamp + getLifespan();
    }

    /**
     * Returns the effect's area of influence.  Clients are notified of effects when their
     * areas of interest intersect the effect's area of influence.
     */
    public Rect getInfluence ()
    {
        return Rect.MAX_VALUE;
    }

    /**
     * Creates a handler to visualize the effect on the client.
     */
    public abstract EffectHandler createHandler (TudeyContext ctx, TudeySceneView view);

    /**
     * Returns the lifespan of the effect, which is the amount of time to continue retransmitting
     * the effect to clients before discarding it.
     */
    protected int getLifespan ()
    {
        return 3000;
    }

    /** The time at which the effect was fired. */
    protected int _timestamp;
}
