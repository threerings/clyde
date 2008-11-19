//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.actor.Actor;

/**
 * Represents an active element of the scene.
 */
public abstract class ActorSprite extends Sprite
    implements TudeySceneView.TickParticipant
{
    /**
     * Creates a new actor sprite.
     */
    public ActorSprite (GlContext ctx, TudeySceneView view, long timestamp, Actor actor)
    {
        super(ctx, view);

        // register as tick participant
        view.addTickParticipant(this);
    }

    /**
     * Updates this sprite with new state.
     */
    public void update (long timestamp, Actor actor)
    {
    }

    /**
     * Notes that the actor has been removed.
     */
    public void remove (long timestamp)
    {
    }

    // documentation inherited from interface TudeySceneView.TickParticipant
    public boolean tick (long delayedTime)
    {
        return true;
    }
}
