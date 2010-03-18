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

package com.threerings.tudey.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap.IntEntry;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.client.OccupantObserver;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.config.ConfigManager;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.math.FloatMath;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;

import com.threerings.opengl.GlView;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.gui.StretchWindow;
import com.threerings.opengl.gui.Window;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.scene.HashScene;
import com.threerings.opengl.scene.SceneElement;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;
import com.threerings.opengl.util.Tickable;

import com.samskivert.util.Predicate;

import com.threerings.tudey.client.sprite.ActorSprite;
import com.threerings.tudey.client.sprite.EffectSprite;
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.Sprite;
import com.threerings.tudey.client.sprite.PlaceableSprite;
import com.threerings.tudey.client.sprite.TileSprite;
import com.threerings.tudey.client.util.TimeSmoother;
import com.threerings.tudey.data.TudeyCodes;
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.TudeySceneConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.dobj.ActorDelta;
import com.threerings.tudey.dobj.SceneDeltaEvent;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.ShapeElement;
import com.threerings.tudey.space.HashSpace;
import com.threerings.tudey.space.SpaceElement;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.TruncatedAverage;
import com.threerings.tudey.util.TudeyContext;
import com.threerings.tudey.util.TudeySceneMetrics;
import com.threerings.tudey.util.TudeyUtil;

import static com.threerings.tudey.Log.*;

/**
 * Displays a view of a Tudey scene.
 */
public class TudeySceneView extends SimpleScope
    implements GlView, PlaceView, TudeySceneModel.Observer, OccupantObserver,
        ChatDisplay, ActorAdvancer.Environment, TudeyCodes
{
    /**
     * An interface for objects (such as sprites and observers) that require per-tick updates.
     */
    public interface TickParticipant
    {
        /**
         * Ticks the participant.
         *
         * @param delayedTime the current delayed client time.
         * @return true to continue ticking the participant, false to remove it from the list.
         */
        public boolean tick (int delayedTime);
    }

    /**
     * Creates a new scene view for use in the editor.
     */
    public TudeySceneView (TudeyContext ctx)
    {
        this(ctx, null);
    }

    /**
     * Creates a new scene view.
     */
    public TudeySceneView (TudeyContext ctx, TudeySceneController ctrl)
    {
        super(ctx.getScope());
        _ctx = ctx;
        _ctrl = ctrl;
        _placeConfig = (ctrl == null) ?
            new TudeySceneConfig() : (TudeySceneConfig)ctrl.getPlaceConfig();
        _scene = new HashScene(ctx, 64f, 6);
        _scene.setParentScope(this);

        // create and initialize the camera handler
        _camhand = createCameraHandler();
        TudeySceneMetrics.initCameraHandler(_camhand);

        // create the input window
        _inputWindow = new StretchWindow(ctx, null) {
            public boolean shouldShadeBehind () {
                return false;
            }
        };
        _inputWindow.setModal(true);

        // insert the baseline (empty) update record
        _records.add(new UpdateRecord(0, new HashIntMap<Actor>()));
    }

    /**
     * Returns a reference to the scene controller.
     */
    public TudeySceneController getController ()
    {
        return _ctrl;
    }

    /**
     * Returns a reference to the camera handler.
     */
    public OrbitCameraHandler getCameraHandler ()
    {
        return _camhand;
    }

    /**
     * Returns a reference to the window used to gather input events.
     */
    public Window getInputWindow ()
    {
        return _inputWindow;
    }

    /**
     * Returns a reference to the view scene.
     */
    public HashScene getScene ()
    {
        return _scene;
    }

    /**
     * Returns a reference to the actor space.
     */
    public HashSpace getActorSpace ()
    {
        return _actorSpace;
    }

    /**
     * Returns the delayed client time, which is the smoothed time minus a delay that compensates
     * for network jitter and dropped packets.
     */
    public int getDelayedTime ()
    {
        return _delayedTime;
    }

    /**
     * Returns the delay with which to display information received from the server in order to
     * compensate for network jitter and dropped packets.
     */
    public int getBufferDelay ()
    {
        return TudeyUtil.getBufferDelay(_elapsedAverage.value());
    }

    /**
     * Returns the advanced time, which is the smoothed time plus an interval that compensates for
     * buffering and latency.
     */
    public int getAdvancedTime ()
    {
        return _advancedTime;
    }

    /**
     * Returns the interval ahead of the smoothed server time (which estimates the server time
     * minus one-way latency) at which we schedule input events.  This should be at least the
     * transmit interval (which represents the maximum amount of time that events may be delayed)
     * plus the two-way latency.
     */
    public int getInputAdvance ()
    {
        return _placeConfig.getInputAdvance(_pingAverage.value());
    }

    /**
     * Returns the smoothed estimate of the server time (plus network latency) calculated at
     * the start of each tick.
     */
    public int getSmoothedTime ()
    {
        return _smoothedTime;
    }

    /**
     * Sets the scene model for this view.
     */
    public void setSceneModel (TudeySceneModel model)
    {
        // clear out the existing sprites
        if (_sceneModel != null) {
            _sceneModel.removeObserver(this);
        }
        for (EntrySprite sprite : _entrySprites.values()) {
            sprite.dispose();
        }
        _entrySprites.clear();

        // suggest garbage collection/finalization here, because we may have OpenGL objects hanging
        // around that should be reclaimed
        System.gc();
        System.runFinalization();

        // create the new sprites
        (_sceneModel = model).addObserver(this);
        for (Entry entry : _sceneModel.getEntries()) {
            addEntrySprite(entry);
        }
    }

    /**
     * Returns the sprite corresponding to the entry with the given key.
     */
    public EntrySprite getEntrySprite (Object key)
    {
        return _entrySprites.get(key);
    }

    /**
     * Returns a reference to the actor sprite with the supplied id, or <code>null</code> if it
     * doesn't exist.
     */
    public ActorSprite getActorSprite (int id)
    {
        return _actorSprites.get(id);
    }

    /**
     * Returns a reference to the target sprite.
     */
    public ActorSprite getTargetSprite ()
    {
        return _targetSprite;
    }

    /**
     * Checks for an intersection between the provided ray and the sprites in the scene.
     *
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first sprite intersected by the ray, or <code>null</code> for
     * none.
     */
    public Sprite getIntersection (Ray3D ray, Vector3f location)
    {
        Predicate<Sprite> filter = Predicate.trueInstance();
        return getIntersection(ray, location, filter);
    }

    /**
     * Checks for an intersection between the provided ray and the sprites in the scene.
     *
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first sprite intersected by the ray, or <code>null</code> for
     * none.
     */
    public Sprite getIntersection (Ray3D ray, Vector3f location, final Predicate<Sprite> filter)
    {
        SceneElement el = _scene.getIntersection(ray, location, new Predicate<SceneElement>() {
            public boolean isMatch (SceneElement element) {
                Object userObject = element.getUserObject();
                return userObject instanceof Sprite && filter.isMatch((Sprite)userObject);
            }
        });
        return (el == null) ? null : (Sprite)el.getUserObject();
    }

    /**
     * Gets the transform of an object on the floor with the provided coordinates.
     *
     * @param mask the floor mask to use for the query.
     */
    public Transform3D getFloorTransform (float x, float y, float rotation, int mask)
    {
        return getFloorTransform(x, y, rotation, _floorMaskFilter.init(mask));
    }

    /**
     * Gets the transform of an object on the floor with the provided coordinates.
     *
     * @param filter the floor filter to use for the query.
     */
    public Transform3D getFloorTransform (
        float x, float y, float rotation, Predicate<SceneElement> filter)
    {
        return getFloorTransform(x, y, rotation, filter, new Transform3D(Transform3D.UNIFORM));
    }

    /**
     * Gets the transform of an object on the floor with the provided coordinates.
     *
     * @param mask the floor mask to use for the query.
     */
    public Transform3D getFloorTransform (
        float x, float y, float rotation, int mask, Transform3D result)
    {
        return getFloorTransform(x, y, rotation, _floorMaskFilter.init(mask), result);
    }

    /**
     * Gets the transform of an object on the floor with the provided coordinates.
     *
     * @param filter the floor filter to use for the query.
     */
    public Transform3D getFloorTransform (
        float x, float y, float rotation, Predicate<SceneElement> filter, Transform3D result)
    {
        Vector3f translation = result.getTranslation();
        translation.set(x, y, getFloorZ(x, y, filter, translation.z));
        result.getRotation().fromAngleAxis(FloatMath.HALF_PI + rotation, Vector3f.UNIT_Z);
        return result;
    }

    /**
     * Returns the z coordinate of the floor at the provided coordinates, or the provided default
     * if no floor is found.
     *
     * @param mask the floor mask to use for the query.
     */
    public float getFloorZ (float x, float y, int mask, float defvalue)
    {
        return getFloorZ(x, y, _floorMaskFilter.init(mask), defvalue);
    }

    /**
     * Returns the z coordinate of the floor at the provided coordinates, or the provided default
     * if no floor is found.
     *
     * @param filter the floor filter to use for the query.
     */
    public float getFloorZ (float x, float y, Predicate<SceneElement> filter, float defvalue)
    {
        _ray.getOrigin().set(x, y, 10000f);
        return (_scene.getIntersection(_ray, _isect, filter) == null) ? defvalue : _isect.z;
    }

    /**
     * Processes a scene delta received from the server.
     *
     * @return true if the scene delta was processed, false if we have not yet received the
     * reference delta.
     */
    public boolean processSceneDelta (SceneDeltaEvent event)
    {
        // update the ping estimate (used to compute the input advance)
        _pingAverage.record(_ping = event.getPing());

        // update the interval estimate (used to compute the buffer delay)
        _elapsedAverage.record(event.getElapsed());

        // create/update the time smoothers
        int timestamp = event.getTimestamp();
        int delayed = timestamp - getBufferDelay();
        int advanced = timestamp + getInputAdvance();
        if (_smoother == null) {
            _smoother = new TimeSmoother(_smoothedTime = timestamp);
            _delayedSmoother = new TimeSmoother(_delayedTime = delayed);
            _advancedSmoother = new TimeSmoother(_advancedTime = advanced);
        } else {
            _smoother.update(timestamp);
            _delayedSmoother.update(delayed);
            _advancedSmoother.update(advanced);
        }

        // find the reference and remove all records before it
        if (!pruneRecords(event.getReference())) {
            return false;
        }
        HashIntMap<Actor> oactors = _records.get(0).getActors();
        HashIntMap<Actor> actors = new HashIntMap<Actor>();

        // start with all the old actors
        actors.putAll(oactors);

        // add any new actors
        Actor[] added = event.getAddedActors();
        if (added != null) {
            for (Actor actor : added) {
                actor.init(_ctx.getConfigManager());
                Actor oactor = actors.put(actor.getId(), actor);
                if (oactor != null) {
                    log.warning("Replacing existing actor.", "oactor", oactor, "nactor", actor);
                }
            }
        }

        // update any updated actors
        ActorDelta[] updated = event.getUpdatedActorDeltas();
        if (updated != null) {
            for (ActorDelta delta : updated) {
                int id = delta.getId();
                Actor oactor = actors.get(id);
                if (oactor != null) {
                    Actor nactor = (Actor)delta.apply(oactor);
                    nactor.init(_ctx.getConfigManager());
                    actors.put(id, nactor);
                } else {
                    log.warning("Missing actor for delta.", "delta", delta);
                }
            }
        }

        // remove any removed actors
        int[] removed = event.getRemovedActorIds();
        if (removed != null) {
            for (int id : removed) {
                actors.remove(id);
            }
        }

        // record the update
        _records.add(new UpdateRecord(timestamp, actors));

        // at this point, if we are to preload, we have enough information to begin
        if (_loadingWindow != null && _preloads == null) {
            ((TudeySceneModel)_ctx.getSceneDirector().getScene().getSceneModel()).getPreloads(
                _preloads = new PreloadableSet(_ctx));
            ConfigManager cfgmgr = _ctx.getConfigManager();
            for (Actor actor : actors.values()) {
                actor.getPreloads(cfgmgr, _preloads);
            }
            _loadingActors = Lists.newArrayList(actors.values());
            return true;
        }

        // update loading actors, create/update the sprites for actors in the set
    OUTER:
        for (Actor actor : actors.values()) {
            int id = actor.getId();
            if (_loadingActors != null) {
                for (int ii = 0, nn = _loadingActors.size(); ii < nn; ii++) {
                    if (_loadingActors.get(ii).getId() == id) {
                        _loadingActors.set(ii, actor);
                        continue OUTER;
                    }
                }
            }
            ActorSprite sprite = _actorSprites.get(id);
            if (sprite != null) {
                if (_ctrl.isControlledTargetId(id)) {
                    _ctrl.controlledTargetUpdated(timestamp, actor);
                } else {
                    sprite.update(timestamp, actor);
                }
                continue;
            }
            addActorSprite(actor);
        }

        // remove sprites for actors no longer in the set
        for (Iterator<IntEntry<ActorSprite>> it = _actorSprites.intEntrySet().iterator();
                it.hasNext(); ) {
            IntEntry<ActorSprite> entry = it.next();
            if (!actors.containsKey(entry.getIntKey())) {
                ActorSprite sprite = entry.getValue();
                sprite.remove(timestamp);
                if (_targetSprite == sprite) {
                    _targetSprite = null;
                    if (_ctrl.isTargetControlled()) {
                        _ctrl.controlledTargetRemoved(timestamp);
                    }
                }
                it.remove();
            }
        }

        // same deal with loading actors
        if (_loadingActors != null) {
            for (int ii = _loadingActors.size() - 1; ii >= 0; ii--) {
                if (!actors.containsKey(_loadingActors.get(ii).getId())) {
                    _loadingActors.remove(ii);
                }
            }
        }

        // create handlers for any effects fired since the last update
        Effect[] fired = event.getEffectsFired();
        if (fired != null) {
            int last = _records.get(_records.size() - 2).getTimestamp();
            for (Effect effect : fired) {
                if (effect.getTimestamp() > last) {
                    effect.init(_ctx.getConfigManager());
                    new EffectSprite(_ctx, this, effect);
                }
            }
        }

        return true;
    }

    /**
     * Adds a participant to tick at each frame.
     */
    public void addTickParticipant (TickParticipant participant)
    {
        _tickParticipants.add(participant);
    }

    /**
     * Removes a participant from the tick list.
     */
    public void removeTickParticipant (TickParticipant participant)
    {
        _tickParticipants.remove(participant);
    }

    /**
     * Updates the target sprite based on the target id.
     */
    public void updateTargetSprite ()
    {
        _targetSprite = _actorSprites.get(_ctrl.getTargetId());
    }

    // documentation inherited from interface GlView
    public void wasAdded ()
    {
        _ctx.setCameraHandler(_camhand);
        _ctx.getRoot().addWindow(_inputWindow);
        if (_ctrl != null) {
            _ctrl.wasAdded();
        }
    }

    // documentation inherited from interface GlView
    public void wasRemoved ()
    {
        _ctx.getRoot().removeWindow(_inputWindow);
        if (_loadingWindow != null) {
            _ctx.getRoot().removeWindow(_loadingWindow);
            _loadingWindow = null;
        }
        if (_ctrl != null) {
            _ctrl.wasRemoved();
        }
        _scene.dispose();
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // if we are loading, preload the next batch of resources or
        // create the next batch of sprites
        if (_loadingWindow != null && _preloads != null) {
            float ppct, epct, apct;
            if ((ppct = _preloads.preloadBatch()) == 1f) {
                if ((epct = createEntrySpriteBatch()) == 1f) {
                    apct = createActorSpriteBatch();
                } else {
                    apct = 0f;
                }
            } else {
                epct = apct = 0f;
            }
            updateLoadingWindow(
                ppct*PRELOAD_PERCENT + epct*ENTRY_LOAD_PERCENT + apct*ACTOR_LOAD_PERCENT);
            if (apct == 1f) {
                _loadingWindow = null;
                _loadingEntries = null;
                _loadingActors = null;
            }
        }

        // update the smoothed time, if possible
        if (_smoother != null) {
            _smoothedTime = _smoother.getTime();
            _delayedTime = _delayedSmoother.getTime();
            _advancedTime = _advancedSmoother.getTime();
        }

        // tick the controller, if present
        if (_ctrl != null) {
            _ctrl.tick(elapsed);
        }

        // tick the participants in reverse order, to allow removal
        int delayedTime = getDelayedTime();
        for (int ii = _tickParticipants.size() - 1; ii >= 0; ii--) {
            if (!_tickParticipants.get(ii).tick(delayedTime)) {
                _tickParticipants.remove(ii);
            }
        }

        // track the target sprite, if any
        if (_targetSprite != null) {
            Vector3f translation = _targetSprite.getModel().getLocalTransform().getTranslation();
            _camhand.getTarget().set(translation).addLocal(TudeySceneMetrics.getTargetOffset());
            _camhand.updatePosition();
        }

        // tick the scene
        _scene.tick(_loadingWindow == null ? elapsed : 0f);
    }

    // documentation inherited from interface Compositable
    public void composite ()
    {
        _scene.composite();
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _tsobj = (TudeySceneObject)plobj;
        _ctx.getOccupantDirector().addOccupantObserver(this);
        _ctx.getChatDirector().addChatDisplay(this);

        // if we don't need to preload, set the scene model immediately; otherwise, create the
        // loading screen and wait for the first scene delta to start preloading
        TudeySceneModel model =
            (TudeySceneModel)_ctx.getSceneDirector().getScene().getSceneModel();
        _loadingWindow = maybeCreateLoadingWindow(model);
        if (_loadingWindow == null) {
            setSceneModel(model);
            return;
        }
        _ctx.getRoot().addWindow(_loadingWindow);
        updateLoadingWindow(0f);

        // suggest garbage collection/finalization here, with the idea that we'll note unused soft
        // references here and dispose of them in setSceneModel's gc call
        System.gc();
        System.runFinalization();
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        if (_sceneModel != null) {
            _sceneModel.removeObserver(this);
        }
        _ctx.getOccupantDirector().removeOccupantObserver(this);
        _ctx.getChatDirector().removeChatDisplay(this);
        _tsobj = null;
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (Entry entry)
    {
        addEntrySprite(entry);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        addPreloads(nentry);
        Object key = nentry.getKey();
        EntrySprite sprite = _entrySprites.get(key);
        if (sprite != null) {
            sprite.update(nentry);
            return;
        }
        if (_loadingEntries != null) {
            // search the entries we have yet to load
            for (int ii = 0, nn = _loadingEntries.size(); ii < nn; ii++) {
                Entry entry = _loadingEntries.get(ii);
                if (entry.getKey().equals(key)) {
                    _loadingEntries.set(ii, nentry);
                    return;
                }
            }
        }
        log.warning("Missing sprite to update.", "entry", nentry);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
        Object key = oentry.getKey();
        EntrySprite sprite = _entrySprites.remove(key);
        if (sprite != null) {
            sprite.dispose();
            return;
        }
        if (_loadingEntries != null) {
            // search the entries we have yet to load
            for (int ii = 0, nn = _loadingEntries.size(); ii < nn; ii++) {
                Entry entry = _loadingEntries.get(ii);
                if (entry.getKey().equals(key)) {
                    _loadingEntries.remove(ii);
                    return;
                }
            }
        }
        log.warning("Missing entry sprite to remove.", "entry", oentry);
    }

    // documentation inherited from interface OccupantObserver
    public void occupantEntered (OccupantInfo info)
    {
        TudeyOccupantInfo toi = (TudeyOccupantInfo)info;
        ActorSprite sprite = _actorSprites.get(toi.pawnId);
        if (sprite != null) {
            sprite.occupantEntered(toi);
        }
    }

    // documentation inherited from interface OccupantObserver
    public void occupantLeft (OccupantInfo info)
    {
        TudeyOccupantInfo toi = (TudeyOccupantInfo)info;
        ActorSprite sprite = _actorSprites.get(toi.pawnId);
        if (sprite != null) {
            sprite.occupantLeft(toi);
        }
    }

    // documentation inherited from interface OccupantObserver
    public void occupantUpdated (OccupantInfo oinfo, OccupantInfo ninfo)
    {
        TudeyOccupantInfo otoi = (TudeyOccupantInfo)oinfo;
        ActorSprite sprite = _actorSprites.get(otoi.pawnId);
        if (sprite != null) {
            sprite.occupantUpdated(otoi, (TudeyOccupantInfo)ninfo);
        }
    }

    // documentation inherited from interface ChatDisplay
    public boolean displayMessage (ChatMessage msg, boolean alreadyDisplayed)
    {
        if (!(msg instanceof UserMessage && chatType().equals(msg.localtype)) ||
                msg instanceof TellFeedbackMessage) {
            return false;
        }
        UserMessage umsg = (UserMessage)msg;
        TudeyOccupantInfo info =
            (TudeyOccupantInfo)_ctx.getOccupantDirector().getOccupantInfo(umsg.speaker);
        if (info == null) {
            return false;
        }
        ActorSprite sprite = _actorSprites.get(info.pawnId);
        return sprite != null && sprite.displayMessage(umsg, alreadyDisplayed);
    }

    // documentation inherited from interface ChatDisplay
    public void clear ()
    {
        for (ActorSprite sprite : _actorSprites.values()) {
            sprite.clearMessages();
        }
    }

    // documentation inherited from interface ActorAdvancer.Environment
    public boolean getPenetration (Actor actor, Shape shape, Vector2f result)
    {
        // start with zero penetration
        result.set(Vector2f.ZERO);

        // check the scene model
        _sceneModel.getPenetration(actor, shape, result);

        // get the intersecting elements
        _actorSpace.getIntersecting(shape, _elements);
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SpaceElement element = _elements.get(ii);
            Actor oactor = ((ActorSprite)element.getUserObject()).getActor();
            if (actor.canCollide(oactor)) {
                ((ShapeElement)element).getWorldShape().getPenetration(shape, _penetration);
                if (_penetration.lengthSquared() > result.lengthSquared()) {
                    result.set(_penetration);
                }
            }
        }
        _elements.clear();

        // if our vector is non-zero, we penetrated
        return !result.equals(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "view";
    }

    /**
     * Creates the camera handler for the view.
     */
    protected OrbitCameraHandler createCameraHandler ()
    {
        return new OrbitCameraHandler(_ctx);
    }

    /**
     * Creates the loading window, or returns <code>null</code> to skip preloading.
     */
    protected Window maybeCreateLoadingWindow (TudeySceneModel model)
    {
        return null;
    }

    /**
     * Creates a batch of entry sprites as part of the loading process.
     *
     * @return the completion percentage.
     */
    protected float createEntrySpriteBatch ()
    {
        if (_loadingEntries != null && _loadingEntries.isEmpty()) {
            return 1f;
        }
        TudeySceneModel model =
            (TudeySceneModel)_ctx.getSceneDirector().getScene().getSceneModel();
        Collection<Entry> entries = model.getEntries();
        if (_loadingEntries == null) {
            _loadingEntries = Lists.newArrayList(entries);
            (_sceneModel = model).addObserver(this);
        }
        for (int ii = _loadingEntries.size() - 1, ll = Math.max(_loadingEntries.size() - 50, 0);
                ii >= ll; ii--) {
            addEntrySprite(_loadingEntries.remove(ii));
        }
        if (_loadingEntries.isEmpty()) {
            return 1f;
        }
        return (float)_entrySprites.size() / entries.size();
    }

    /**
     * Creates a batch of actor sprites as part of the loading process.
     *
     * @return the completion percentage.
     */
    protected float createActorSpriteBatch ()
    {
        if (_loadingActors != null && _loadingActors.isEmpty()) {
            return 1f;
        }
        HashIntMap<Actor> actors = _records.get(_records.size() - 1).getActors();
        if (_loadingActors == null) {
            _loadingActors = Lists.newArrayList(actors.values());
        }
        for (int ii = _loadingActors.size() - 1, ll = Math.max(_loadingActors.size() - 5, 0);
                ii >= ll; ii--) {
            addActorSprite(_loadingActors.remove(ii));
        }
        if (_loadingActors.isEmpty()) {
            System.gc();
            System.runFinalization();
            return 1f;
        }
        return (float)_actorSprites.size() / actors.size();
    }

    /**
     * Updates the loading window with the current percentage of resources loaded.  If
     * <code>pct</code> is equal to 1.0, this method should remove the loading window (or start
     * fading it out).
     */
    protected void updateLoadingWindow (float pct)
    {
        if (pct == 1f) {
            _ctx.getRoot().removeWindow(_loadingWindow);
        }
    }

    /**
     * Adds a sprite for the specified entry.
     */
    protected void addEntrySprite (Entry entry)
    {
        addPreloads(entry);
        _entrySprites.put(entry.getKey(), entry.createSprite(_ctx, this));
    }

    /**
     * Adds a sprite for the specified actor.
     */
    protected void addActorSprite (Actor actor)
    {
        addPreloads(actor);
        int id = actor.getId();
        int timestamp = _records.get(_records.size() - 1).getTimestamp();
        ActorSprite sprite = new ActorSprite(_ctx, this, timestamp, actor);
        _actorSprites.put(id, sprite);
        if (id == _ctrl.getTargetId()) {
            _targetSprite = sprite;
            if (_ctrl.isTargetControlled()) {
                _ctrl.controlledTargetAdded(timestamp, actor);
            }
        }
    }

    /**
     * Adds the specified entry's preloads to the set if appropriate.
     */
    protected void addPreloads (Entry entry)
    {
        if (_preloads != null) {
            entry.getPreloads(_ctx.getConfigManager(), _preloads);
        }
    }

    /**
     * Adds the specified actor's preloads to the set if appropriate.
     */
    protected void addPreloads (Actor actor)
    {
        if (_preloads != null) {
            actor.getPreloads(_ctx.getConfigManager(), _preloads);
        }
    }

    /**
     * Adds the specified effect's preloads to the set if appropriate.
     */
    protected void addPreloads (Effect effect)
    {
        if (_preloads != null) {
            effect.getPreloads(_ctx.getConfigManager(), _preloads);
        }
    }

    /**
     * Prunes all records before the supplied reference time, if found.
     *
     * @return true if the reference time was found, false if not.
     */
    protected boolean pruneRecords (int reference)
    {
        for (int ii = _records.size() - 1; ii >= 0; ii--) {
            if (_records.get(ii).getTimestamp() == reference) {
                _records.subList(0, ii).clear();
                return true;
            }
        }
        return false;
    }

    /**
     * The valid chat type for this view.
     */
    protected String chatType ()
    {
        return ChatCodes.PLACE_CHAT_TYPE;
    }

    /**
     * Contains the state at a single update.
     */
    protected static class UpdateRecord
    {
        /**
         * Creates a new update record.
         */
        public UpdateRecord (int timestamp, HashIntMap<Actor> actors)
        {
            _timestamp = timestamp;
            _actors = actors;
        }

        /**
         * Returns the timestamp of this update.
         */
        public int getTimestamp ()
        {
            return _timestamp;
        }

        /**
         * Returns the map of actors.
         */
        public HashIntMap<Actor> getActors ()
        {
            return _actors;
        }

        /** The timestamp of the update. */
        protected int _timestamp;

        /** The states of the actors. */
        protected HashIntMap<Actor> _actors;
    }

    /**
     * Used to select sprites according to their floor flags.
     */
    protected static class FloorMaskFilter extends Predicate<SceneElement>
    {
        /**
         * (Re)initializes the filter with its mask.
         *
         * @return a reference to the filter, for chaining.
         */
        public FloorMaskFilter init (int mask)
        {
            _mask = mask;
            return this;
        }

        @Override // documentation inherited
        public boolean isMatch (SceneElement element)
        {
            Object obj = element.getUserObject();
            return obj instanceof Sprite && (((Sprite)obj).getFloorFlags() & _mask) != 0;
        }

        /** The floor mask. */
        protected int _mask;
    }

    /** The application context. */
    protected TudeyContext _ctx;

    /** The controller that created this view. */
    protected TudeySceneController _ctrl;

    /** The place configuration. */
    protected TudeySceneConfig _placeConfig;

    /** A casted reference to the scene object. */
    protected TudeySceneObject _tsobj;

    /** The view's camera handler. */
    protected OrbitCameraHandler _camhand;

    /** A window used to gather input events. */
    protected Window _inputWindow;

    /** The loading window, if any. */
    protected Window _loadingWindow;

    /** The set of resources to preload. */
    protected PreloadableSet _preloads;

    /** The remaining entries to add during loading. */
    protected List<Entry> _loadingEntries;

    /** The remaining actors to add during loading. */
    protected List<Actor> _loadingActors;

    /** The OpenGL scene. */
    @Scoped
    protected HashScene _scene;

    /** The scene model. */
    protected TudeySceneModel _sceneModel;

    /** Smoother used to provide a smoothed time estimate. */
    protected TimeSmoother _smoother;

    /** The smoothed time. */
    protected int _smoothedTime;

    /** Smooths the delayed time. */
    protected TimeSmoother _delayedSmoother;

    /** The delayed time. */
    protected int _delayedTime;

    /** Smooths the advanced time. */
    protected TimeSmoother _advancedSmoother;

    /** The advanced time. */
    protected int _advancedTime;

    /** The last estimated ping time. */
    protected int _ping;

    /** The trailing average of the ping times. */
    protected TruncatedAverage _pingAverage = new TruncatedAverage();

    /** The trailing average of the elapsed times. */
    protected TruncatedAverage _elapsedAverage = new TruncatedAverage();

    /** Records of each update received from the server. */
    protected List<UpdateRecord> _records = Lists.newArrayList();

    /** Sprites corresponding to the scene entries. */
    protected HashMap<Object, EntrySprite> _entrySprites = new HashMap<Object, EntrySprite>();

    /** Sprites corresponding to the actors in the scene. */
    protected HashIntMap<ActorSprite> _actorSprites = new HashIntMap<ActorSprite>();

    /** The actor space (used for client-side collision detection). */
    protected HashSpace _actorSpace = new HashSpace(64f, 6);

    /** The list of participants in the tick. */
    protected List<TickParticipant> _tickParticipants = Lists.newArrayList();

    /** The sprite that the camera is tracking. */
    protected ActorSprite _targetSprite;

    /** The offset of the camera target from the target sprite's translation. */
    protected Vector3f _targetOffset = new Vector3f();

    /** Used to find the floor. */
    protected Ray3D _ray = new Ray3D(Vector3f.ZERO, new Vector3f(0f, 0f, -1f));

    /** Used to find the floor. */
    protected Vector3f _isect = new Vector3f();

    /** Used to find the floor. */
    protected FloorMaskFilter _floorMaskFilter = new FloorMaskFilter();

    /** Holds collected elements during queries. */
    protected List<SpaceElement> _elements = Lists.newArrayList();

    /** Stores penetration vector during queries. */
    protected Vector2f _penetration = new Vector2f();

    /** The percentage of load progress devoted to preloading. */
    protected static final float PRELOAD_PERCENT = 0.6f;

    /** The percentage of load progress devoted to loading entries. */
    protected static final float ENTRY_LOAD_PERCENT = 0.3f;

    /** The percentage of load progress devoted to loading actors. */
    protected static final float ACTOR_LOAD_PERCENT = 0.1f;
}
