//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.handler.EffectHandler;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents a stateless event occurring within the scene.
 */
public abstract class Effect extends SimpleStreamableObject
{
    /**
     * Returns the time at which the effect was fired.  Because important effects may be
     * retransmitted until acknowledged, this will not necessarily be equal to the timestamp of the
     * delta in which the effect was received.
     */
    public long getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Creates a handler to visualize this effect on the client.
     */
    public abstract EffectHandler createHandler (TudeyContext ctx, TudeySceneView view);

    /** The time at which the effect was fired. */
    protected long _timestamp;
}
