//
// $Id$

package com.threerings.tudey.data.actor;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.client.TudeySceneController;
import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.PawnAdvancer;
import com.threerings.tudey.util.TudeyContext;

/**
 * An actor controlled by a player.
 */
public class Pawn extends Mobile
{
    /**
     * Creates a new pawn.
     */
    public Pawn (
        ConfigReference<ActorConfig> config, int id, int created,
        Vector2f translation, float rotation)
    {
        super(config, id, created, translation, rotation);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Pawn ()
    {
    }

    @Override // documentation inherited
    public ActorAdvancer maybeCreateAdvancer (TudeyContext ctx, TudeySceneView view, int timestamp)
    {
        TudeySceneController ctrl = view.getController();
        return (ctrl.getTargetId() == _id && ctrl.isTargetControlled()) ?
            new PawnAdvancer(this, timestamp) : null;
    }
}
