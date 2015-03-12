//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeBasedTable;

import com.samskivert.util.ArrayUtil;
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
import com.threerings.config.ConfigReference;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.Scoped;
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;

import com.threerings.opengl.GlView;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.effect.Easing;
import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.StretchWindow;
import com.threerings.opengl.gui.Window;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.CompoundConfig.ComponentModel;
import com.threerings.opengl.model.config.MergedStaticConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.scene.HashScene;
import com.threerings.opengl.scene.SceneElement;
import com.threerings.opengl.scene.ViewerEffect;
import com.threerings.opengl.util.PreloadableSet;
import com.threerings.opengl.util.Tickable;

import com.samskivert.util.RunAnywhere;

import com.threerings.tudey.client.sprite.ActorSprite;
import com.threerings.tudey.client.sprite.EffectSprite;
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.Sprite;
import com.threerings.tudey.client.util.TimeSmoother;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.CameraConfig;
import com.threerings.tudey.config.EffectConfig;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.data.TudeyCodes;
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.TudeySceneConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Prespawnable;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.data.effect.Prefireable;
import com.threerings.tudey.dobj.ActorDelta;
import com.threerings.tudey.dobj.SceneDeltaEvent;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.ShapeElement;
import com.threerings.tudey.space.HashSpace;
import com.threerings.tudey.space.SpaceElement;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.Coord;
import com.threerings.tudey.util.TruncatedAverage;
import com.threerings.tudey.util.TudeyContext;
import com.threerings.tudey.util.TudeySceneMetrics;
import com.threerings.tudey.util.TudeyUtil;

import static com.threerings.tudey.Log.log;

/**
 * Displays a view of a Tudey scene.
 */
public class TudeySceneView extends DynamicScope
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
        super("view", ctx.getScope());
        _ctx = ctx;
        _ctrl = ctrl;
        _placeConfig = (ctrl == null) ?
            new TudeySceneConfig() : (TudeySceneConfig)ctrl.getPlaceConfig();
        _scene = new HashScene(ctx, 64f, 6) {
            @Override public void getEffects (Box bounds, Collection<ViewerEffect> results) {
                // remove any effects that desire to be omitted when loading
                super.getEffects(bounds, results);
                if (_loadingWindow != null) {
                    for (Iterator<ViewerEffect> it = results.iterator(); it.hasNext(); ) {
                        if (it.next().omitWhileLoading()) {
                            it.remove();
                        }
                    }
                }
            }
            @Override protected void dumpInfluence (SceneElement element, String msg, int diff)
            {
                if (_dumpInfluences) {
                    Object user = element.getUserObject();
                    if (user instanceof EntrySprite) {
                        log.info(msg, "diff", diff,
                                "entry", ((EntrySprite)user).getEntry().getReference());
                    } else if (user instanceof ActorSprite) {
                        log.info(msg, "diff", diff,
                                "actor", ((ActorSprite)user).getActor().getConfig());
                    } else {
                        super.dumpInfluence(element, msg, diff);
                    }
                }
            }
        };
        _scene.setParentScope(this);

        // create and initialize the camera handler
        _camhand = createCameraHandler();
        _camcfg.apply(_camhand);

        // create the input window
        if (_ctrl != null) {
            _inputWindow = new StretchWindow(ctx, null) {
                @Override public boolean shouldShadeBehind () {
                    return false;
                }
                @Override public String getTooltipText () {
                    Sprite sprite = _ctrl.getHoverSprite();
                    return (sprite == null) ? super.getTooltipText() : sprite.getTooltipText();
                }
                @Override public float getTooltipTimeout () {
                    Sprite sprite = _ctrl.getHoverSprite();
                    return (sprite == null) ?
                        super.getTooltipTimeout() : sprite.getTooltipTimeout();
                }
                @Override public String getTooltipWindowStyle () {
                    Sprite sprite = _ctrl.getHoverSprite();
                    return (sprite == null) ?
                        super.getTooltipWindowStyle() : sprite.getTooltipWindowStyle();
                }
                @Override protected Component createTooltipComponent (String tiptext) {
                    Sprite sprite = _ctrl.getHoverSprite();
                    return (sprite == null) ? super.createTooltipComponent(tiptext) :
                        sprite.createTooltipComponent(tiptext);
                }
            };
        } else {
            _inputWindow = new StretchWindow(ctx, null) {
                @Override public boolean shouldShadeBehind () {
                    return false;
                }
            };
        }

        _inputWindow.setTooltipRelativeToMouse(true);
        _inputWindow.setModal(true);

        // insert the baseline (empty) update record
        _records.add(new UpdateRecord(
            0, RunAnywhere.currentTimeMillis(), new HashIntMap<Actor>()));
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
     * Returns the client control delta (the difference between the advanced and delayed times).
     */
    public int getControlDelta ()
    {
        return _advancedTime - _delayedTime;
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
     * Returns the average of the ping times.
     */
    public int getPing ()
    {
        return _pingAverage.value();
    }

    /**
     * Returns the average of the elapsed times.
     */
    public int getElapsed ()
    {
        return _elapsedAverage.value();
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
     * Returns the time taken to process the controller on the previous tick.
     */
    public long getControllerTime ()
    {
        return _controllerTime;
    }

    /**
     * Returns the time taken to process tick participants on the previous tick.
     */
    public long getTickerTime ()
    {
        return _tickerTime;
    }

    /**
     * Returns the number of tick participants on the previous tick.
     */
    public int getTickerCount ()
    {
        return _tickerCount;
    }

    public void dumpTickers ()
    {
        _dumpTickers = true;
    }

    /**
     * Returns the time taken to process the scene on the previous tick.
     */
    public long getSceneTime ()
    {
        return _sceneTime;
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
        _suppressMergeUpdates = true;
        for (Entry entry : _sceneModel.getEntries()) {
            addEntrySprite(entry);
        }

        // init merged sprites
        _suppressMergeUpdates = false;
        for (Sprite sprite : _mergedSprites.values()) {
            sprite.getModel().getConfig().wasUpdated();
        }
    }

    /**
     * Returns the sprite corresponding to the entity with the given key.
     */
    public Sprite getSprite (EntityKey key)
    {
        if (key instanceof EntityKey.Entry) {
            return getEntrySprite(((EntityKey.Entry)key).getKey());
        } else if (key instanceof EntityKey.Actor) {
            return getActorSprite(((EntityKey.Actor)key).getId());
        } else {
            return null;
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
     * Returns a reference to the controlled sprite.
     */
    public ActorSprite getControlledSprite ()
    {
        return _controlledSprite;
    }

    /**
     * Checks whether we should attempt to merge static models.
     */
    public boolean canMerge ()
    {
        return getMergeGranularity() > 0;
    }

    /**
     * Attempts to merge a static model.
     *
     * @return a reference to the merged model, or <code>null</code> if the model cannot be merged.
     */
    public Model maybeMerge (
        int x, int y, ConfigReference<ModelConfig> ref,
        Transform3D transform, final int floorFlags)
    {
        int granularity = getMergeGranularity();
        Coord key = new Coord(x >> granularity, y >> granularity);
        Sprite sprite = _mergedSprites.get(key);
        Model model;
        if (sprite == null) {
            final Model fmodel = model = new Model(_ctx) {
                @Override protected void updateFromConfig () {
                    if (!_suppressMergeUpdates) {
                        super.updateFromConfig();
                    }
                }
            };
            _mergedSprites.put(key, sprite = new Sprite(_ctx, this) {
                @Override public int getFloorFlags () {
                    return floorFlags;
                }
                @Override public Model getModel () {
                    return fmodel;
                }
            });
            model.setUserObject(sprite);
            model.setConfig(new ModelConfig(new MergedStaticConfig(
                new ComponentModel[] { new ComponentModel(ref, transform) })) { {
                   _cfgmgr = _configs = _ctx.getConfigManager();
                }
                @Override protected void maybeFireOnConfigManager () {
                    // no-op
                }
                @Override protected void addUpdateDependencies () {
                    // no-op
                }
            });
            _scene.add(model);

        } else {
            if (sprite.getFloorFlags() != floorFlags) {
                return null;
            }
            model = sprite.getModel();
            ModelConfig mconfig = model.getConfig();
            MergedStaticConfig impl = (MergedStaticConfig)mconfig.implementation;
            impl.models = ArrayUtil.append(impl.models, new ComponentModel(ref, transform));
            mconfig.wasUpdated();
        }
        return model;
    }

    /**
     * Unmerges a model.
     *
     * @return whether or not the model was found and unmerged.
     */
    public boolean unmerge (
        int x, int y, ConfigReference<ModelConfig> ref, Transform3D transform)
    {
        if (_disposed) {
            return true; // don't bother with the computation if we're being removed
        }
        int granularity = getMergeGranularity();
        Coord key = new Coord(x >> granularity, y >> granularity);
        Sprite sprite = _mergedSprites.get(key);
        if (sprite == null) {
            return false;
        }
        Model model = sprite.getModel();
        ModelConfig mconfig = sprite.getModel().getConfig();
        MergedStaticConfig impl = (MergedStaticConfig)mconfig.implementation;
        for (int ii = 0; ii < impl.models.length; ii++) {
            ComponentModel cmodel = impl.models[ii];
            if (Objects.equal(cmodel.model, ref) && cmodel.transform.equals(transform)) {
                if (impl.models.length == 1) {
                    _scene.remove(model);
                    _mergedSprites.remove(key);
                    if (_loadingMerged != null) {
                        _loadingMerged.remove(sprite);
                    }
                } else {
                    impl.models = ArrayUtil.splice(impl.models, ii, 1);
                    mconfig.wasUpdated();
                }
                return true;
            }
        }
        return false;
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
        Predicate<Sprite> filter = Predicates.alwaysTrue();
        return getIntersection(ray, location, filter);
    }

    /**
     * Checks for an intersection between the provided ray and the sprites in the scene.
     *
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first sprite intersected by the ray, or <code>null</code> for
     * none.
     */
    public Sprite getIntersection (
        Ray3D ray, Vector3f location, final Predicate<? super Sprite> filter)
    {
        SceneElement el = _scene.getIntersection(ray, location, new Predicate<SceneElement>() {
            public boolean apply (SceneElement element) {
                Object userObject = element.getUserObject();
                return userObject instanceof Sprite && filter.apply((Sprite)userObject);
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
        float x, float y, float rotation, Predicate<? super SceneElement> filter)
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
        float x, float y, float rotation, Predicate<? super SceneElement> filter,
        Transform3D result)
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
    public float getFloorZ (
        float x, float y, Predicate<? super SceneElement> filter, float defvalue)
    {
        _ray.getOrigin().set(x, y, 10000f);
        return (_scene.getIntersection(_ray, _isect, filter) == null) ? defvalue : _isect.z;
    }

    /**
     * Requests to prespawn an actor.  Only one actor may be prespawned at any given timestamp;
     * if one already exists for the specified timestamp, this method will return null.
     */
    public ActorSprite prespawnActor (
        int timestamp, ActorSprite source, Vector2f translation,
        float rotation, ConfigReference<ActorConfig> ref)
    {
        // make sure we haven't already prespawned an actor at that timestamp
        int id = -timestamp;
        ActorSprite osprite = _actorSprites.get(id);
        if (osprite != null) {
            return null;
        }

        // attempt to resolve the implementation
        ConfigManager cfgmgr = _ctx.getConfigManager();
        ActorConfig config = cfgmgr.getConfig(ActorConfig.class, ref);
        ActorConfig.Original original = (config == null) ? null : config.getOriginal(cfgmgr);
        if (original == null) {
            log.warning("Failed to resolve actor config.", "actor", ref);
            return null;
        }
        Actor actor = original.createActor(ref, id, timestamp, translation, rotation);
        actor.init(cfgmgr);
        if (actor instanceof Prespawnable && source != null) {
            ((Prespawnable)actor).noteSource(source.getActor());
        }
        ActorSprite sprite = new ActorSprite(_ctx, this, timestamp, actor);
        _actorSprites.put(id, sprite);
        return sprite;
    }

    /**
     * Requests to prefire an effect.
     *
     * @param translation an offset from the target's translation, or null, or the absolute
     * translation if there's no target.
     * @param rotation an offset from the target's rotation, or the absolute rotation if there's
     * no target.
     */
    public EffectSprite prefireEffect (
        int timestamp, EntityKey target, Vector2f translation, float rotation,
        ConfigReference<EffectConfig> ref)
    {
        // attempt to resolve the implementation
        ConfigManager cfgmgr = _ctx.getConfigManager();
        EffectConfig config = cfgmgr.getConfig(EffectConfig.class, ref);
        EffectConfig.Original original = (config == null) ? null : config.getOriginal(cfgmgr);
        if (original == null) {
            log.warning("Failed to resolve effect config.", "effect", ref);
            return null;
        }
        if (target != null) {
            Sprite sprite = getSprite(target);
            if (sprite instanceof ActorSprite) {
                Actor a = ((ActorSprite)sprite).getActor();
                translation = (translation == null)
                    ? a.getTranslation()
                    : translation.add(a.getTranslation());
                rotation = FloatMath.normalizeAngle(rotation + a.getRotation());
            }
        }

        Effect effect = original.createEffect(ref, timestamp, target, translation, rotation);
        effect.init(cfgmgr);
        return new EffectSprite(_ctx, this, effect);
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
        int elapsed = event.getElapsed();
        _elapsedAverage.record(elapsed);

        // look for the previous update in order to compute the jitter
        int timestamp = event.getTimestamp();
        long now = RunAnywhere.currentTimeMillis();
        UpdateRecord lrecord = _records.get(_records.size() - 1);
        if (lrecord.getTimestamp() + elapsed == timestamp) {
            noteJitter((int)(now - lrecord.getReceived()) - elapsed);
        }

        // create/update the time smoothers
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
        // start with all the old actors
        HashIntMap<Actor> actors = oactors.clone();
        Set<Integer> uids = Sets.newHashSet();

        // add any new actors
        Actor[] added = event.getAddedActors();
        if (added != null) {
            for (Actor actor : added) {
                actor.init(_ctx.getConfigManager());
                Actor oactor = actors.put(actor.getId(), actor);
                uids.add(actor.getId());
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
                    uids.add(id);
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
        _records.add(new UpdateRecord(timestamp, now, actors));

        // at this point, if we are to preload, we have enough information to begin
        if (_loadingWindow != null && _preloads == null) {
            ((TudeySceneModel)_ctx.getSceneDirector().getScene().getSceneModel()).getPreloads(
                _preloads = new PreloadableSet(_ctx));
            ConfigManager cfgmgr = _ctx.getConfigManager();
            for (Actor actor : actors.values()) {
                actor.getPreloads(cfgmgr, _preloads);
            }
            _loadingActors = actors.clone();
            addExtraPreloads();
            return true;
        }

        // update loading actors, create/update the sprites for actors in the set
        for (Actor actor : actors.values()) {
            int id = actor.getId();
            ActorSprite sprite = _actorSprites.get(id);
            if (sprite != null) {
                if (_ctrl.isControlledId(id)) {
                    _ctrl.controlledActorUpdated(timestamp, actor);
                } else {
                    sprite.update(timestamp, actor, uids.contains(id));
                }

            } else if (_loadingActors != null && _loadingActors.containsKey(id)) {
                _loadingActors.put(id, actor);

            } else {
                addActorSprite(actor);
            }
        }

        // remove sprites for actors no longer in the set
        for (Iterator<IntEntry<ActorSprite>> it = _actorSprites.intEntrySet().iterator();
                it.hasNext(); ) {
            IntEntry<ActorSprite> entry = it.next();
            int id = entry.getIntKey();
            if (id < 0) {
                ActorSprite sprite = entry.getValue();
                if (sprite.getActor().getCreated() <= timestamp) {
                    sprite.remove(timestamp);
                    it.remove();
                }
            } else if (!actors.containsKey(id)) {
                ActorSprite sprite = entry.getValue();
                sprite.remove(timestamp);
                if (_controlledSprite == sprite) {
                    _controlledSprite = null;
                    _ctrl.controlledSpriteRemoved(timestamp);
                }
                if (_targetSprite == sprite) {
                    _targetSprite = _controlledSprite;
                }
                it.remove();
            }
        }

        // same deal with loading actors
        if (_loadingActors != null) {
            _loadingActors.keySet().retainAll(actors.keySet());
        }

        // create handlers for any effects fired since the last update
        Effect[] fired = event.getEffectsFired();
        if (fired != null) {
            int last = _records.get(_records.size() - 2).getTimestamp();
            for (Effect effect : fired) {
                if (effect.getTimestamp() > last && !(effect instanceof Prefireable &&
                        ((Prefireable)effect).getClientOid() == _ctx.getClient().getClientOid())) {
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
     * Adds a participant to tick at each frame.
     *
     * @param prepend if true, prepend the participant so that it is ticked last (participants
     * are usually ticked in reverse order of addition).
     */
    public void addTickParticipant (TickParticipant participant, boolean prepend)
    {
        _tickParticipants.add(prepend ? 0 : _tickParticipants.size(), participant);
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

    /**
     * Updates the controlled sprite base on the controlled id.
     */
    public void updateControlledSprite ()
    {
        _controlledSprite = _actorSprites.get(_ctrl.getControlledId());
    }

    /**
     * Adds a camera config to the stack with no transition.
     */
    public void addCameraConfig (CameraConfig camcfg)
    {
        addCameraConfig(camcfg, 0f, null);
    }

    /**
     * Adds a camera config to the stack with an option transition.
     */
    public void addCameraConfig (CameraConfig camcfg, float transition, Easing easing)
    {
        List<CameraConfig> cfgs = _camcfgs.get(camcfg.priority, camcfg.zoom);
        if (cfgs == null) {
            cfgs = Lists.newArrayList();
            _camcfgs.put(camcfg.priority, camcfg.zoom, cfgs);
        }
        cfgs.add(camcfg);
        if (getTopCameraConfig() == camcfg) {
            setCameraConfig(camcfg, transition, easing);
        }
    }

    /**
     * Removes a camera config from the stack with no transition.
     */
    public void removeCameraConfig (CameraConfig camcfg)
    {
        removeCameraConfig(camcfg, 0f, null);
    }

    /**
     * Removes a camera config from the stack with an optional transition.
     */
    public void removeCameraConfig (CameraConfig camcfg, float transition, Easing easing)
    {
        CameraConfig topcfg = getTopCameraConfig();
        List<CameraConfig> cfgs = _camcfgs.get(camcfg.priority, camcfg.zoom);
        if (cfgs != null && cfgs.remove(camcfg)) {
            if (cfgs.size() == 0) {
                _camcfgs.remove(camcfg.priority, camcfg.zoom);
            }
            if (camcfg == topcfg) {
                setCameraConfig(getTopCameraConfig(), transition, easing);
            }
        }
    }

    /**
     * Sets the preferred zoom level.
     */
    public void setPreferredZoom (int zoom)
    {
        setPreferredZoom(zoom, 0f, null);
    }

    /**
     * Sets the preferred zoom level.
     */
    public void setPreferredZoom (int zoom, float transition, Easing easing)
    {
        CameraConfig topcfg = getTopCameraConfig();
        _zoom = zoom;
        CameraConfig cfg = getTopCameraConfig();
        if (topcfg != cfg) {
            setCameraConfig(cfg, transition, easing);
        }
    }

    /**
     * Dump to the logs scene influences on the next tick.
     */
    public void dumpInfluences ()
    {
        _scene.dumpInfluences();
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
        dispose();
        _disposed = true;
        _scene.dispose();
        _actorSpace.dispose();
        for (EntrySprite sprite : _entrySprites.values()) {
            sprite.dispose();
        }
        for (ActorSprite sprite : _actorSprites.values()) {
            sprite.dispose();
        }
        _entrySprites.clear();
        _actorSprites.clear();
        _mergedSprites.clear();
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // if we are loading, preload the next batch of resources or
        // create the next batch of sprites
        if (doLoading()) {
            float ppct = 0f, epct = 0f, mpct = 0f, apct = 0f;
            if ((ppct = _preloads.preloadBatch(BATCH_LOAD_DURATION)) == 1f) {
                if ((epct = createEntrySpriteBatch()) == 1f) {
                    if ((mpct = initMergedSpriteBatch()) == 1f) {
                        apct = createActorSpriteBatch();
                    }
                }
            }
            updateLoadingWindow(
                ppct*PRELOAD_PERCENT + epct*ENTRY_LOAD_PERCENT +
                mpct*ENTRY_MERGE_PERCENT + apct*ACTOR_LOAD_PERCENT);
            if (apct == 1f) {
                _loadingWindow = null;
                _loadingEntries = null;
                _loadingMerged = null;
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
        long tick = System.nanoTime();
        if (_ctrl != null) {
            _ctrl.tick(elapsed);
        }
        long tock = System.nanoTime();
        _controllerTime = tock - tick;

        // tick the participants in reverse order, to allow removal
        int delayedTime = getDelayedTime();
        _tickerCount = _tickParticipants.size();
        long start = 0;
        if (_dumpTickers) {
            start = System.nanoTime();
            log.info("TICKERS!!!");
        }
        for (int ii = _tickParticipants.size() - 1; ii >= 0; ii--) {
            TickParticipant tp = _tickParticipants.get(ii);
            if (!tp.tick(delayedTime)) {
                _tickParticipants.remove(ii);
            }
            if (_dumpTickers) {
                dumpTicker(tp, System.nanoTime() - start);
                start = System.nanoTime();
            }
        }
        _dumpTickers = false;
        tick = System.nanoTime();
        _tickerTime = tick - tock;

        // tick the camera transition, if any
        if (_camtrans != null) {
            _camtrans.tick(elapsed);
        }

        // track the target sprite, if any
        if (_targetSprite != null) {
            Vector3f translation = _targetSprite.getModel().getLocalTransform().getTranslation();
            _camhand.getTarget().set(translation).addLocal(_camcfg.offset);
            _camhand.updatePosition();
        }

        // tick the scene
        tick = System.nanoTime();
        _scene.tick(_loadingWindow == null ? elapsed : 0f);
        _sceneTime = System.nanoTime() - tick;
    }

    // documentation inherited from interface Compositable
    public void composite ()
    {
        if (_loadingWindow == null) {
            _scene.composite();
        }
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
        if (!(msg instanceof UserMessage) || msg instanceof TellFeedbackMessage ||
                !(ChatCodes.PLACE_CHAT_TYPE.equals(msg.localtype) ||
                    getChatType().equals(msg.localtype))) {
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
    public TudeySceneModel getSceneModel ()
    {
        return _sceneModel;
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

    // documentation inherited from interface ActorAdvancer.Environment
    public boolean collides (Actor actor, Shape shape)
    {
        // check the scene model
        if (_sceneModel.collides(actor, shape)) {
            return true;
        }

        // look for intersecting elements
        _actorSpace.getIntersecting(shape, _elements);
        try {
            for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
                SpaceElement element = _elements.get(ii);
                Actor oactor = ((ActorSprite)element.getUserObject()).getActor();
                if (actor.canCollide(oactor)) {
                    return true;
                }
            }
        } finally {
            _elements.clear();
        }
        return false;
    }

    // documentation inherited from interface ActorAdvancer.Environment
    public int getDirections (Actor actor, Shape shape)
    {
        return _sceneModel.getDirections(actor, shape);
    }

    /**
     * Checks for collision against a mask.
     */
    public boolean collides (int mask, Shape shape)
    {
        // check the scene model
        if (_sceneModel.collides(mask, shape)) {
            return true;
        }
        // look for intersecting elements
        _actorSpace.getIntersecting(shape, _elements);
        try {
            for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
                SpaceElement element = _elements.get(ii);
                Actor oactor = ((ActorSprite)element.getUserObject()).getActor();
                if ((oactor.getCollisionFlags() & mask) != 0) {
                    return true;
                }
            }
        } finally {
            _elements.clear();
        }
        return false;
    }

    /**
     * Creates the camera handler for the view.
     */
    protected OrbitCameraHandler createCameraHandler ()
    {
        return new OrbitCameraHandler(_ctx);
    }

    /**
     * Sets the camera configuration (optionally transitioning to it over time).
     */
    protected void setCameraConfig (
        CameraConfig camcfg, final float transition, final Easing easing)
    {
        if (transition <= 0f) {
            _camcfg.set(camcfg).apply(_camhand);
            _camtrans = null;
            return;
        }
        final CameraConfig ocamcfg = new CameraConfig(_camcfg);
        final CameraConfig ncamcfg = new CameraConfig(camcfg);
        _camtrans = new Tickable() {
            public void tick (float elapsed) {
                if ((_total += elapsed) >= transition) {
                    setCameraConfig(ncamcfg, 0f, null);
                } else {
                    ocamcfg.lerp(ncamcfg, easing.getTime(_total / transition),
                        _camcfg).apply(_camhand);
                }
            }
            protected float _total;
        };
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
            _suppressMergeUpdates = true;
        }
        long end = System.currentTimeMillis() + BATCH_LOAD_DURATION;
        for (int ii = _loadingEntries.size() - 1;
                ii >= 0 && System.currentTimeMillis() < end; ii--) {
            addEntrySprite(_loadingEntries.remove(ii));
        }
        if (_loadingEntries.isEmpty()) {
            return 1f;
        }
        return (float)_entrySprites.size() / entries.size();
    }

    /**
     * Initializes a batch of merged sprites as part of the loading process.
     *
     * @return the completion percentage.
     */
    protected float initMergedSpriteBatch ()
    {
        if (_loadingMerged != null && _loadingMerged.isEmpty()) {
            return 1f;
        }
        if (_loadingMerged == null) {
            _loadingMerged = Lists.newArrayList(_mergedSprites.values());
            _suppressMergeUpdates = false;
        }
        long end = System.currentTimeMillis() + BATCH_LOAD_DURATION;
        for (int ii = _loadingMerged.size() - 1;
                ii >= 0 && System.currentTimeMillis() < end; ii--) {
            _loadingMerged.remove(ii).getModel().getConfig().wasUpdated();
        }
        int size = _loadingMerged.size();
        if (size == 0) {
            return 1f;
        }
        return 1f - (float)size / _mergedSprites.size();
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
            _loadingActors = actors.clone();
        }
        long end = System.currentTimeMillis() + BATCH_LOAD_DURATION;
        for (Iterator<Actor> it = _loadingActors.values().iterator();
                it.hasNext() && (System.currentTimeMillis() < end); ) {
            addActorSprite(it.next());
            it.remove();
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
        if (actor instanceof Prespawnable &&
                ((Prespawnable)actor).getClientOid() == _ctx.getClient().getClientOid()) {
            ActorSprite sprite = _actorSprites.remove(-actor.getCreated());
            if (sprite != null) {
                _actorSprites.put(id, sprite);
                sprite.reinit(timestamp, actor);
                return;
            }
        }
        ActorSprite sprite = new ActorSprite(_ctx, this, timestamp, actor);
        _actorSprites.put(id, sprite);
        if (id == _ctrl.getControlledId()) {
            _controlledSprite = sprite;
            _ctrl.controlledActorAdded(timestamp, actor);
        }
        if (id == _ctrl.getTargetId()) {
            _targetSprite = sprite;
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
     * Gives the view a chance to add any required additional preloads to the set.
     */
    protected void addExtraPreloads ()
    {
        // nothing by default
    }

    /**
     * Returns the merge granularity to use for static tile models.  This is expressed as a power
     * of two: tiles with coordinates in the same 2^granularity square block will be merged
     * together if possible.  A value of zero allows no merging.
     */
    protected int getMergeGranularity ()
    {
        return 2;
    }

    /**
     * Notes a jitter value (difference between elapsed time on server and elapsed time on client
     * between two successive updates).
     */
    protected void noteJitter (int value)
    {
        // nothing by default
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
     * Returns the valid chat type for this view.
     */
    protected String getChatType ()
    {
        return ChatCodes.PLACE_CHAT_TYPE;
    }

    /**
     * Returns true if we should perform loading operations.
     */
    protected boolean doLoading ()
    {
        return _loadingWindow != null && _preloads != null && _ctx.getSceneDirector() != null &&
                _ctx.getSceneDirector().getScene() != null;
    }

    /**
     * Returns the camera config that should be currently active.
     */
    protected CameraConfig getTopCameraConfig ()
    {
        if (_camcfgs.isEmpty()) {
            return TudeySceneMetrics.getDefaultCameraConfig();
        }
        Integer priority = _camcfgs.rowKeySet().last();
        SortedMap<Integer, List<CameraConfig>> map = _camcfgs.row(priority);
        Integer zoom = _zoom;
        List<CameraConfig> cfgs = map.get(_zoom);
        if (cfgs == null) {
            SortedMap<Integer, List<CameraConfig>> submap = map.headMap(_zoom);
            zoom = submap.isEmpty() ? null : submap.lastKey();
            if (zoom != null) {
                cfgs = map.get(zoom);
            } else {
                zoom = map.tailMap(_zoom).firstKey();
                cfgs = map.get(zoom);
            }
        }
        return cfgs.get(cfgs.size() - 1);
    }

    protected void dumpTicker (TickParticipant tp, long time)
    {
        time /= 1000L;
        if (tp instanceof ActorSprite) {
            ActorSprite sprite = (ActorSprite)tp;
            log.info("  ", "time", time, "actor", sprite.getActor().getConfig());
        } else if (tp instanceof EffectSprite) {
            EffectSprite sprite = (EffectSprite)tp;
            log.info("  ", "time", time, "effect", sprite.getEffect().getConfig());
        } else {
            log.info("  ", "time", time, "ticker", tp);
        }
    }

    /**
     * Contains the state at a single update.
     */
    protected static class UpdateRecord
    {
        /**
         * Creates a new update record.
         */
        public UpdateRecord (int timestamp, long received, HashIntMap<Actor> actors)
        {
            _timestamp = timestamp;
            _received = received;
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
         * Returns the time at which the update was received.
         */
        public long getReceived ()
        {
            return _received;
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

        /** The wall clock time at which the update was received. */
        protected long _received;

        /** The states of the actors. */
        protected HashIntMap<Actor> _actors;
    }

    /**
     * Used to select sprites according to their floor flags.
     */
    protected static class FloorMaskFilter
        implements Predicate<SceneElement>
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

        // from Predicate
        public boolean apply (SceneElement element)
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

    /** The remaining merged sprites to be initialized during loading. */
    protected List<Sprite> _loadingMerged;

    /** The remaining actors to add during loading. */
    protected HashIntMap<Actor> _loadingActors;

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

    /** Sprites for merged static models mapped by coordinates. */
    protected Map<Coord, Sprite> _mergedSprites = Maps.newHashMap();

    /** When set, indicates that we should hold off on updating merged sprites. */
    protected boolean _suppressMergeUpdates;

    /** The sprite that the camera is tracking. */
    protected ActorSprite _targetSprite;

    /** The sprite that the user is controlling. */
    protected ActorSprite _controlledSprite;

    /** Thre preferred zoom level. */
    protected int _zoom = 0;

    /** A table of ordered camera configs. */
    protected TreeBasedTable<Integer, Integer, List<CameraConfig>> _camcfgs =
        TreeBasedTable.create();

    /** The current camera config. */
    protected CameraConfig _camcfg = new CameraConfig(
        TudeySceneMetrics.getDefaultCameraConfig());

    /** The active camera transition, if any. */
    protected Tickable _camtrans;

    /** Set when we've been disposed. */
    protected boolean _disposed;

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

    /** The time taken to process the ticks. */
    protected long _controllerTime, _tickerTime, _sceneTime;

    /** The number of tick participants on the previous tick. */
    protected int _tickerCount;
    protected boolean _dumpTickers;

    /** The amount of time to spend on each batch when loading. */
    protected static final long BATCH_LOAD_DURATION = 50L;

    /** The percentage of load progress devoted to preloading. */
    protected static final float PRELOAD_PERCENT = 0.4f;

    /** The percentage of load progress devoted to loading entries. */
    protected static final float ENTRY_LOAD_PERCENT = 0.3f;

    /** The percentage of load progress devoted to merging entries. */
    protected static final float ENTRY_MERGE_PERCENT = 0.2f;

    /** The percentage of load progress devoted to loading actors. */
    protected static final float ACTOR_LOAD_PERCENT = 0.1f;
}
