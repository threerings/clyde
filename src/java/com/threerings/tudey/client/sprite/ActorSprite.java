//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.util.ActorHistory;

/**
 * Represents an active element of the scene.
 */
public abstract class ActorSprite extends Sprite
    implements TudeySceneView.TickParticipant
{
    /**
     * Creates a new actor sprite.
     */
    public ActorSprite (GlContext ctx, TudeySceneView view, int timestamp, Actor actor)
    {
        super(ctx, view);

        // create the history
        _history = new ActorHistory(timestamp, actor, view.getBufferDelay() * 2);

        // register as tick participant
        view.addTickParticipant(this);
    }

    /**
     * Updates this sprite with new state.
     */
    public void update (int timestamp, Actor actor)
    {
        _history.record(timestamp, actor);
    }

    /**
     * Notes that the actor has been removed.
     */
    public void remove (int timestamp)
    {
    }

    // documentation inherited from interface TudeySceneView.TickParticipant
    public boolean tick (int delayedTime)
    {
        return true;
    }

    /** The history that we use to find interpolated actor state. */
    protected ActorHistory _history;
}
