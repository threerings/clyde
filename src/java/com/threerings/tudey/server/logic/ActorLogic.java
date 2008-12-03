//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.HandlerConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.ShapeElement;

/**
 * Controls the state of an actor on the server.
 */
public class ActorLogic extends Logic
{
    /**
     * Initializes the actor.
     */
    public void init (
        TudeySceneManager scenemgr, ConfigReference<ActorConfig> ref,
        ActorConfig.Original config, int id, int timestamp, Vector2f translation, float rotation)
    {
        super.init(scenemgr);
        _config = config;
        _actor = createActor(ref, id, timestamp, translation, rotation);
        _actor.init(scenemgr.getConfigManager());
        _shape = new ShapeElement(config.shape);
        _shape.setUserObject(this);
        updateShape();
        _scenemgr.getActorSpace().add(_shape);

        // create the handlers
        ArrayList<HandlerLogic> handlers = new ArrayList<HandlerLogic>();
        for (HandlerConfig hconfig : config.handlers) {
            HandlerLogic handler = createHandler(hconfig, this);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        _handlers = handlers.toArray(new HandlerLogic[handlers.size()]);

        // give subclasses a chance to set up
        didInit();
    }

    /**
     * Returns a reference to the actor object.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Returns the current tick's snapshot of the actor.
     */
    public Actor getSnapshot ()
    {
        int timestamp = _scenemgr.getTimestamp();
        if (_snaptime < timestamp) {
            _snapshot = (Actor)_actor.clone();
            _snaptime = timestamp;
        }
        return _snapshot;
    }

    /**
     * Moves the actor and updates its shape.
     */
    public void move (float x, float y, float rotation)
    {
        _actor.getTranslation().set(x, y);
        _actor.setRotation(rotation);
        updateShape();
    }

    /**
     * Destroys the actor.
     */
    public void destroy (int timestamp)
    {
        // set the destroyed time and remove on the next tick
        _actor.setDestroyed(timestamp);
        _scenemgr.addTickParticipant(new TudeySceneManager.TickParticipant() {
            public boolean tick (int timestamp) {
                remove();
                return false;
            }
        });
    }

    @Override // documentation inherited
    public String getTag ()
    {
        return _config.tag;
    }

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _actor.getTranslation();
    }

    @Override // documentation inherited
    public float getRotation ()
    {
        return _actor.getRotation();
    }

    /**
     * Creates the actor object.
     */
    protected Actor createActor (
        ConfigReference<ActorConfig> ref, int id, int timestamp,
        Vector2f translation, float rotation)
    {
        return new Actor(ref, id, timestamp, translation, rotation);
    }

    /**
     * Updates the shape transform based on the actor's position.
     */
    protected void updateShape ()
    {
        _shape.getTransform().set(_actor.getTranslation(), _actor.getRotation(), 1f);
        _shape.updateBounds();
    }

    /**
     * Removes this actor after it has been destroyed.
     */
    protected void remove ()
    {
        // remove from space and logic mapping
        _scenemgr.getActorSpace().remove(_shape);
        _scenemgr.removeActorLogic(_actor.getId());

        // notify the handlers
        for (HandlerLogic handler : _handlers) {
            handler.removed();
        }

        // give subclasses a chance to cleanup
        wasRemoved();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /**
     * Override to perform custom cleanup.
     */
    protected void wasRemoved ()
    {
        // nothing by default
    }

    /** The actor configuration. */
    protected ActorConfig.Original _config;

    /** The actor object, which may be manipulated directly. */
    protected Actor _actor;

    /** The actor snapshot, which must not be modified once created. */
    protected Actor _snapshot;

    /** The timestamp of the actor snapshot. */
    protected int _snaptime;

    /** The actor's shape element. */
    protected ShapeElement _shape;

    /** The actor's event handlers. */
    protected HandlerLogic[] _handlers;
}
