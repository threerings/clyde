//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.samskivert.util.ObserverList;

import com.threerings.crowd.data.BodyObject;

import com.threerings.config.ConfigReference;
import com.threerings.math.FloatMath;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.opengl.model.config.ModelConfig;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.HandlerConfig;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.HasActor;
import com.threerings.tudey.dobj.ActorDelta;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.ShapeElement;

import static com.threerings.tudey.Log.*;

/**
 * Controls the state of an actor on the server.
 */
public class ActorLogic extends Logic
    implements HasActor
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
        _entityKey = new EntityKey.Actor(id);
        _actor = createActor(ref, id, timestamp, translation, rotation);
        _actor.init(scenemgr.getConfigManager());
        _shape = new ShapeElement(config.shape);
        _shape.setUserObject(this);
        updateShape();

        // if specified, attempt to find a non-colliding spawn point
        if (config.spawnMask != 0 && scenemgr.collides(config.spawnMask, getShape(), timestamp)) {
            adjustSpawnPoint();
        }
        _scenemgr.getActorSpace().add(_shape);

        // create the handlers
        ArrayList<HandlerLogic> handlers = new ArrayList<HandlerLogic>();
        for (HandlerConfig hconfig : config.handlers) {
            HandlerLogic handler = createHandler(hconfig, this);
            if (handler != null) {
                handler.startup(timestamp);
                handlers.add(handler);
            }
        }
        _handlers = handlers.toArray(new HandlerLogic[handlers.size()]);

        // initialize the snapshots
        _previousSnapshot = _snapshot = (Actor)_actor.clone();

        // give subclasses a chance to set up
        didInit();
    }

    /**
     * Checks whether this is a "static" actor.  Static actors are always in clients' areas of
     * interest and must notify the scene manager when their state changes.
     */
    public boolean isStatic ()
    {
        return _config.isStatic;
    }

    /**
     * Sets the reference to the entity that spawned the actor.
     */
    public void setSource (Logic source)
    {
        _source = source;
    }

    /**
     * Returns a reference to the entity that spawned the actor, if known/any.
     */
    public Logic getSource ()
    {
        return _source;
    }

    /**
     * Sets the reference to the entity that caused the actor to be spawned.
     */
    public void setActivator (Logic activator)
    {
        _activator = activator;
    }

    /**
     * Returns a reference to the entity that caused the actor to be spawned, if known/any.
     */
    public Logic getActivator ()
    {
        return _activator;
    }

    /**
     * For player-controlled actors, this notifies the logic that the controlling player
     * is about to enter.
     */
    public void bodyWillEnter (BodyObject body)
    {
        // nothing by default
    }

    /**
     * For player-controlled actors, this notifies the logic that the controlling player
     * is about to leave.
     */
    public void bodyWillLeave (BodyObject body)
    {
        destroy(_scenemgr.getNextTimestamp(), this);
    }

    /**
     * Returns the current tick's snapshot of the actor.
     */
    public Actor getSnapshot ()
    {
        updateSnapshot();
        return _snapshot;
    }

    /**
     * Returns the previous tick's snapshot of the actor.
     */
    public Actor getPreviousSnapshot ()
    {
        updateSnapshot();
        return _previousSnapshot;
    }

    /**
     * Returns the delta between the last snapshot of the actor and the current state, or
     * <code>null</code> if the actor has not changed.
     */
    public ActorDelta getSnapshotDelta ()
    {
        updateSnapshot();
        return _snapshotDelta;
    }

    /**
     * Returns a reference to the actor's shape element.
     */
    public ShapeElement getShapeElement ()
    {
        return _shape;
    }

    /**
     * Determines whether the actor is destroyed as of the current server tick.
     */
    public boolean isDestroyed ()
    {
        return _scenemgr.getTimestamp() >= _actor.getDestroyed();
    }

    /**
     * Determines whether the actor has been removed from the manager.
     */
    public boolean isRemoved ()
    {
        return _shape.getSpace() == null;
    }

    /**
     * Warps the actor.
     */
    public void warp (float x, float y, float rotation)
    {
        warp(x, y, rotation, x, y);
    }

    /**
     * Warps the actor.
     */
    public void warp (float x, float y, float rotation, float tx, float ty)
    {
        warp(x, y, rotation, tx, ty, true);
    }

    /**
     * Warps the actor.
     *
     * @param adjust if true, adjusts the location as when spawning to avoid intersecting other
     * actors.
     */
    public void warp (float x, float y, float rotation, float tx, float ty, boolean adjust)
    {
        // set the warp flag and clear it on the next tick
        _actor.set(Actor.WARP);
        float oldX = _actor.getTranslation().x;
        float oldY = _actor.getTranslation().y;
        float oldR = _actor.getRotation();
        if (tx != x || ty != y) {
            rotation = FloatMath.atan2(ty - y, tx - x);
        }
        move(x, y, rotation);
        if (adjust && _config.spawnMask != 0) {
            _scenemgr.getActorSpace().remove(_shape);
            if (_scenemgr.collides(_config.spawnMask, getShape(), _scenemgr.getTimestamp()) ||
                    _scenemgr.getPathfinder().getPath(
                        this, MAX_ADJUSTMENT_PATH_LENGTH, tx, ty, false, false) == null) {
                adjustSpawnPoint(tx, ty);
                if (_scenemgr.getPathfinder().getPath(
                            this, MAX_ADJUSTMENT_PATH_LENGTH, tx, ty, false, false) == null) {
                    move(oldX, oldY, oldR);
                } else if (tx != x || ty != y) {
                    Vector2f trans = _actor.getTranslation();
                    _actor.setRotation(FloatMath.atan2(ty - trans.y, tx - trans.x));
                    updateShape();
                }
            }

            _scenemgr.getActorSpace().add(_shape);
        }
        _scenemgr.addTickParticipant(new TudeySceneManager.TickParticipant() {
            public boolean tick (int timestamp) {
                _actor.clear(Actor.WARP);
                wasUpdated();
                return false;
            }
        });
    }

    /**
     * Moves the actor and updates its shape.
     */
    public void move (float x, float y, float rotation)
    {
        _actor.getTranslation().set(x, y);
        _actor.setRotation(rotation);
        updateShape();
        wasUpdated();
    }

    /**
     * Destroys the actor.
     */
    public void destroy (int timestamp, Logic activator)
    {
        // make sure we're not already destroyed
        if (isDestroyed()) {
            return;
        }

        // set the destroyed time and remove on the next tick
        _actor.setDestroyed(timestamp);
        wasUpdated();
        _scenemgr.addTickParticipant(new TudeySceneManager.TickParticipant() {
            public boolean tick (int timestamp) {
                remove();
                return false;
            }
        });

        // notify handlers
        for (HandlerLogic handler : _handlers) {
            handler.shutdown(timestamp, activator);
        }

        wasDestroyed();
    }

    /**
     * Removes this actor after it has been destroyed.
     */
    public void remove ()
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

    @Override // documentation inherited
    public Actor getActor ()
    {
        return _actor;
    }

    @Override // documentation inherited
    public String[] getTags ()
    {
        return _config.tags.getTags();
    }

    @Override // documentation inherited
    public boolean isDefaultEntrance ()
    {
        return _config.defaultEntrance;
    }

    @Override // documentation inherited
    public boolean isActive ()
    {
        return !isDestroyed();
    }

    @Override // documentation inherited
    public EntityKey getEntityKey ()
    {
        return _entityKey;
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

    @Override // documentation inherited
    public Shape getShape ()
    {
        return _shape.getWorldShape();
    }

    @Override // documentation inherited
    public void addShapeObserver (ShapeObserver observer)
    {
        _shapeObservers.add(observer);
    }

    @Override // documentation inherited
    public void removeShapeObserver (ShapeObserver observer)
    {
        _shapeObservers.remove(observer);
    }

    @Override // documentation inherited
    public ConfigReference<ModelConfig> getModel ()
    {
        return _config.sprite.model;
    }

    @Override // documentation inherited
    public void signal (int timestamp, Logic source, String name)
    {
        // make sure we're not already destroyed
        if (isDestroyed() || _handlers == null) {
            return;
        }
        for (HandlerLogic handler : _handlers) {
            handler.signal(timestamp, source, name);
        }
    }

    @Override // documentation inherited
    public void setVariable (int timestamp, Logic source, String name, Object value)
    {
        super.setVariable(timestamp, source, name, value);
        // We could be setting variables on startup
        if (_handlers != null) {
            for (HandlerLogic handler : _handlers) {
                handler.variableChanged(timestamp, source, name);
            }
        }
    }

    @Override // documentation inherited
    public void request (int timestamp, PawnLogic source, String name)
    {
        for (HandlerLogic handler : _handlers) {
            handler.request(timestamp, source, name);
        }
    }

    /**
     * Creates the actor object.
     */
    protected Actor createActor (
        ConfigReference<ActorConfig> ref, int id, int timestamp,
        Vector2f translation, float rotation)
    {
        return _config.createActor(ref, id, timestamp, translation, rotation);
    }

    /**
     * Adjusts the initial location of the actor so as to avoid collisions.
     */
    protected void adjustSpawnPoint ()
    {
        boolean warp = _actor.isSet(Actor.WARP);
        _actor.set(Actor.WARP);
        Vector2f translation = _actor.getTranslation();
        adjustSpawnPoint(translation.x, translation.y);
        if (!warp) {
            _actor.clear(Actor.WARP);
        }
    }

    /**
     * Adjusts the initial location of the actor so as to avoid collisions.
     */
    protected void adjustSpawnPoint (float tx, float ty)
    {
        // get the bounds with respect to the position
        Rect bounds = _shape.getBounds();
        float width = bounds.getWidth() + 0.01f, height = bounds.getHeight() + 0.01f;
        Vector2f translation = _actor.getTranslation();
        float ox = translation.x, oy = translation.y;

        // test locations at increasing distances from the original
        for (int dist = 1; dist <= MAX_ADJUSTMENT_DISTANCE; dist++) {
            // loop counterclockwise around the center
            float bottom = oy - dist*height;
            for (int xx = -dist; xx <= +dist; xx++) {
                if (testSpawnPoint(tx, ty, ox + xx*width, bottom)) {
                    return;
                }
            }
            float right = ox + dist*width;
            for (int yy = 1 - dist, yymax = -yy; yy <= yymax; yy++) {
                if (testSpawnPoint(tx, ty, right, oy + yy*height)) {
                    return;
                }
            }
            float top = oy + dist*height;
            for (int xx = +dist; xx >= -dist; xx--) {
                if (testSpawnPoint(tx, ty, ox + xx*width, top)) {
                    return;
                }
            }
            float left = ox - dist*width;
            for (int yy = dist - 1, yymin = -yy; yy >= yymin; yy--) {
                if (testSpawnPoint(tx, ty, left, oy + yy*height)) {
                    return;
                }
            }
        }

        // if we exhaust our search, return to the original location
        _actor.setTranslation(ox, oy);
        updateShape();
    }

    /**
     * Tests the specified location as an adjusted spawn point.
     *
     * @return true if the spawn point is viable (in which case the actor's translation
     * will have been adjusted), false if not.
     */
    protected boolean testSpawnPoint (float ox, float oy, float nx, float ny)
    {
        // update the shape
        _actor.setTranslation(nx, ny);
        updateShape();

        // check for collision
        if (_scenemgr.collides(_config.spawnMask, getShape(), _actor.getCreated())) {
            return false;
        }

        // make sure we can reach the original translation, ignoring actors
        return _scenemgr.getPathfinder().getPath(
            this, MAX_ADJUSTMENT_PATH_LENGTH, ox, oy, false, false) != null;
    }

    /**
     * Updates the shape transform based on the actor's position.
     */
    protected void updateShape ()
    {
        // notify observers that the shape will change
        _shapeObservers.apply(_shapeWillChangeOp);

        // update the shape
        _shape.getTransform().set(_actor.getTranslation(), _actor.getRotation(), 1f);
        _shape.updateBounds();

        // notify observers that the shape has changed
        _shapeObservers.apply(_shapeDidChangeOp);
    }

    /**
     * Updates the snapshot and snapshot delta.
     */
    protected void updateSnapshot ()
    {
        int timestamp = _scenemgr.getTimestamp();
        if (timestamp > _snaptime) {
            _previousSnapshot = _snapshot;
            _snapshotDelta = null;
            if (_actor.isDirty()) {
                _snapshotDelta = new ActorDelta(_snapshot, _actor);
                if (_snapshotDelta.isEmpty()) {
                    _snapshotDelta = null;
                } else {
                    _snapshot = (Actor)_actor.clone();
                }
                _actor.setDirty(false);
            }
            _snaptime = timestamp;
        }
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

    /**
     * Override to perform custom cleanup.
     */
    protected void wasDestroyed ()
    {
        // nothing by default
    }

    /**
     * Sets (or clears) an actor flag and calls {@link #wasUpdated}.
     */
    protected void set (int flag, boolean value)
    {
        _actor.set(flag, value);
        wasUpdated();
    }

    /**
     * Notes that the actor state has changed.  Only static actors need call this method.
     */
    protected void wasUpdated ()
    {
        if (isStatic() && !isRemoved()) {
            _scenemgr.staticActorUpdated(this);
        }
    }

    /** The actor configuration. */
    protected ActorConfig.Original _config;

    /** The entity key. */
    protected EntityKey.Actor _entityKey;

    /** The actor object, which may be manipulated directly. */
    protected Actor _actor;

    /** The actor snapshot, which must not be modified once created. */
    protected Actor _snapshot;

    /** The actor snapshot at the previous tick. */
    protected Actor _previousSnapshot;

    /** The delta between the current and previous snapshots (if any). */
    protected ActorDelta _snapshotDelta;

    /** The timestamp of the actor snapshot. */
    protected int _snaptime;

    /** The actor's shape element. */
    protected ShapeElement _shape;

    /** The actor's event handlers. */
    protected HandlerLogic[] _handlers;

    /** The actor's shape observers. */
    protected ObserverList<ShapeObserver> _shapeObservers = ObserverList.newFastUnsafe();

    /** Optional references to the spawning and activating logic objects for the actor. */
    protected Logic _source, _activator;

    /** Used to notify observers when the shape is about to change. */
    protected ObserverList.ObserverOp<ShapeObserver> _shapeWillChangeOp =
        new ObserverList.ObserverOp<ShapeObserver>() {
        public boolean apply (ShapeObserver observer) {
            observer.shapeWillChange(ActorLogic.this);
            return true;
        }
    };

    /** Used to notify observers when the shape has just changed. */
    protected ObserverList.ObserverOp<ShapeObserver> _shapeDidChangeOp =
        new ObserverList.ObserverOp<ShapeObserver>() {
        public boolean apply (ShapeObserver observer) {
            observer.shapeDidChange(ActorLogic.this);
            return true;
        }
    };

    /** The maximum number of steps away from the spawn point for adjustment. */
    protected static final int MAX_ADJUSTMENT_DISTANCE = 4;

    /** The maximum path length from the origin for adjustment. */
    protected static final float MAX_ADJUSTMENT_PATH_LENGTH = 8f;
}
