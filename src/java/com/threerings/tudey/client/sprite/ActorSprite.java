//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.opengl.mod.Model;
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

        // create the history and play head actor
        _history = new ActorHistory(timestamp, actor, view.getBufferDelay() * 2);
        int delayedTime = view.getDelayedTime();
        _actor = _history.get(delayedTime, (Actor)actor.clone());

        // create the model
        _model = new Model(ctx);

        // if the actor is created, add it immediately
        _created = _history.isCreated(delayedTime);
        if (_created) {
            wasAdded();
        }

        // register as tick participant
        view.addTickParticipant(this);
    }

    /**
     * Returns a reference to the "play head" actor containing interpolated state.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Returns a reference to the sprite's model.
     */
    public Model getModel ()
    {
        return _model;
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
        _removed = timestamp;
    }

    // documentation inherited from interface TudeySceneView.TickParticipant
    public boolean tick (int delayedTime)
    {
        // update the interpolated state
        _history.get(delayedTime, _actor);

        // handle pre-creation state
        if (!_created) {
            if (_history.isCreated(delayedTime)) {
                _created = true;
                wasCreated();
                wasAdded();
            } else {
                return true; // chill until actually created
            }
        }

        // see if we're destroyed/removed
        if (_history.isDestroyed(delayedTime)) {
            wasRemoved();
            wasDestroyed();
            return false;

        } else if (delayedTime >= _removed) {
            wasRemoved();
            return false;

        } else {
            update();
            return true;
        }
    }

    /**
     * Notes that the actor has been created.  This is only called if the client actually witnesses
     * the actor's creation; otherwise, it simply gets a call to {@link #wasAdded} (which is called
     * in any case).
     */
    protected void wasCreated ()
    {
    }

    /**
     * Notes that the actor has been destroyed.  This is only called if the client actually
     * witnesses the actor's destruction; otherwise, it simply gets a call to {@link #wasRemoved}.
     */
    protected void wasDestroyed ()
    {
    }

    /**
     * Notes that the actor has been added.
     */
    protected void wasAdded ()
    {
        // add the model to the scene
        _view.getScene().add(_model);
    }

    /**
     * Notes that the actor has been removed.
     */
    protected void wasRemoved ()
    {
        // remove the model from the scene
        _view.getScene().remove(_model);
    }

    /**
     * Called to update the sprite for the current tick.
     */
    protected void update ()
    {
    }

    /** The history that we use to find interpolated actor state. */
    protected ActorHistory _history;

    /** The "play head" actor with interpolated state. */
    protected Actor _actor;

    /** Whether or not the actor has been created. */
    protected boolean _created;

    /** The timestamp at which the actor was removed, if any. */
    protected int _removed = Integer.MAX_VALUE;

    /** The actor model. */
    protected Model _model;
}
