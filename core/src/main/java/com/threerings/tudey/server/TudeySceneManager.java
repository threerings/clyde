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

package com.threerings.tudey.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.inject.Inject;
import com.google.inject.Injector;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Histogram;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Interval;
import com.samskivert.util.ObserverList;
import com.samskivert.util.Randoms;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.PresentsSession;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.CrowdSession;

import com.threerings.whirled.server.SceneManager;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.CameraConfig;
import com.threerings.tudey.config.EffectConfig;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.data.TudeyBodyObject;
import com.threerings.tudey.data.TudeyCodes;
import com.threerings.tudey.data.TudeySceneConfig;
import com.threerings.tudey.data.TudeySceneMarshaller;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.server.logic.ActorLogic;
import com.threerings.tudey.server.logic.EffectLogic;
import com.threerings.tudey.server.logic.EntryLogic;
import com.threerings.tudey.server.logic.Logic;
import com.threerings.tudey.server.logic.PawnLogic;
import com.threerings.tudey.server.util.Pathfinder;
import com.threerings.tudey.server.util.SceneTicker;
import com.threerings.tudey.shape.Segment;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.ShapeElement;
import com.threerings.tudey.space.HashSpace;
import com.threerings.tudey.space.SpaceElement;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.TudeySceneMetrics;
import com.threerings.tudey.util.TudeyUtil;

import static com.threerings.tudey.Log.log;

/**
 * Manager for Tudey scenes.
 */
public class TudeySceneManager extends SceneManager
    implements TudeySceneProvider, TudeySceneModel.Observer,
        ActorAdvancer.Environment, RunQueue, TudeyCodes
{
    /**
     * An interface for objects that take part in the server tick.
     */
    public interface TickParticipant
    {
        /**
         * Ticks the participant.
         *
         * @param timestamp the timestamp of the current tick.
         * @return true to continue ticking the participant, false to remove it from the list.
         */
        public boolean tick (int timestamp);
    }

    /**
     * An interface for objects to notify when actors are added or removed.
     */
    public interface ActorObserver
    {
        /**
         * Notes that an actor has been added.
         */
        public void actorAdded (ActorLogic logic);

        /**
         * Notes that an actor has been removed.
         */
        public void actorRemoved (ActorLogic logic);
    }

    /**
     * Base interface for sensors.
     */
    public interface Sensor
    {
        /**
         * Returns the sensor's bitmask.  Only triggers whose flags intersect the mask will
         * activate the sensor.
         */
        public int getMask ();

        /**
         * Triggers the sensor.
         *
         * @param timestamp the timestamp of the intersection.
         * @param actor the logic object of the actor that triggered the sensor.
         */
        public void trigger (int timestamp, ActorLogic actor);
    }

    /**
     * An interface for objects to notify when we shutdown.
     */
    public interface ShutdownObserver
    {
        /**
         * Notes that we're shutting down.
         */
        public void didShutdown ();
    }

    /**
     * An interface for objects that should be notified when actors intersect them.
     */
    public interface IntersectionSensor extends Sensor
    {
    }

    /**
     * Enables or disables tick participant profiling.
     */
    public static void setTickProfEnabled (boolean enabled)
    {
        _tickProfEnabled = enabled;
    }

    /**
     * Checks whether tick profiling is enabled.
     */
    public static boolean isTickProfEnabled ()
    {
        return _tickProfEnabled;
    }

    /**
     * Sets the frequency at which we sample tick participants.
     */
    public static void setTickProfInterval (int interval)
    {
        _tickProfInterval = interval;
    }

    /**
     * Returns the tick profile interval.
     */
    public static int getTickProfInterval ()
    {
        return _tickProfInterval;
    }

    /**
     * Dumps the current set of tick profiles to the log.
     */
    public static void dumpTickProfiles ()
    {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, TickProfile> entry : _profiles.entrySet()) {
            buf.append(entry.getKey()).append(" => ").append(entry.getValue()).append('\n');
        }
        log.info(buf.toString());
    }

    /**
     * Clears the current set of tick profiles.
     */
    public static void clearTickProfiles ()
    {
        _profiles.clear();
    }

    /**
     * Returns the delay with which the clients display information received from the server in
     * order to compensate for network jitter and dropped packets.
     */
    public int getBufferDelay ()
    {
        return TudeyUtil.getBufferDelay(getTickInterval());
    }

    /**
     * Returns the number of ticks per second.
     */
    public int getTicksPerSecond ()
    {
        return 1000 / getTickInterval();
    }

    /**
     * Returns the interval at which we call the {@link #tick} method.
     */
    public int getTickInterval ()
    {
        return (_ticker == null) ? DEFAULT_TICK_INTERVAL : _ticker.getActualInterval();
    }

    /**
     * Returns the interval at which clients transmit their input frames.
     */
    public int getTransmitInterval ()
    {
        return ((TudeySceneConfig)_config).getTransmitInterval();
    }

    /**
     * Returns a reference to the configuration manager for the scene.
     */
    public ConfigManager getConfigManager ()
    {
        return _cfgmgr;
    }

    /**
     * Adds a participant to notify at each tick.
     */
    public void addTickParticipant (TickParticipant participant)
    {
        addTickParticipant(participant, false);
    }

    /**
     * Adds a participant to notify at each tick.
     *
     * @param withinTick if true and we are not currently in the process of ticking, adds the
     * participant in the next tick.
     */
    public void addTickParticipant (final TickParticipant participant, boolean withinTick)
    {
        if (withinTick && !_ticking) {
            _tickParticipants.add(new TickParticipant() {
                public boolean tick (int timestamp) {
                    _tickParticipants.add(participant);
                    return false;
                }
            });
        } else {
            _tickParticipants.add(participant);
        }
    }

    /**
     * Removes a participant from the tick list.
     */
    public void removeTickParticipant (TickParticipant participant)
    {
        _tickParticipants.remove(participant);
    }

    /**
     * Adds an observer for actor events.
     */
    public void addActorObserver (ActorObserver observer)
    {
        _actorObservers.add(observer);
    }

    /**
     * Removes an actor observer.
     */
    public void removeActorObserver (ActorObserver observer)
    {
        _actorObservers.remove(observer);
    }

    /**
     * Adds a shutdown observer.
     */
    public void addShutdownObserver (ShutdownObserver observer)
    {
        _shutdownObservers.add(observer);
    }

    /**
     * Removes a shutdown observer.
     */
    public void removeShutdownObserver (ShutdownObserver observer)
    {
        _shutdownObservers.remove(observer);
    }

    /**
     * Returns the timestamp of the current tick.
     */
    public int getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns the timestamp of the last tick.
     */
    public int getPreviousTimestamp ()
    {
        return _previousTimestamp;
    }

    /**
     * Returns the approximate timestamp of the next tick.
     */
    public int getNextTimestamp ()
    {
        return _timestamp + getTickInterval();
    }

    /**
     * Returns the amount of time spent processing the last tick.
     */
    public long getTickDuration ()
    {
        return _tickDuration;
    }

    /**
     * Returns the list of logic objects with the supplied tag.
     */
    public List<Logic> getTagged (String tag)
    {
        List<Logic> list = _tagged.get(tag);
        return (list == null) ? ImmutableList.<Logic>of() : list;
    }

    /**
     * Returns the list of logic objects that are instances of the supplied class.
     */
    public <L extends Logic> List<L> getInstances (Class<L> clazz)
    {
        @SuppressWarnings("unchecked") // we ensure that only the right type are added
        List<L> list = (List<L>)_instances.get(clazz);
        return (list == null) ? ImmutableList.<L>of() : list;
    }

    /**
     * Returns a reference to the actor space.
     */
    public HashSpace getActorSpace ()
    {
        return _actorSpace;
    }

    /**
     * Returns a reference to the sensor space.
     */
    public HashSpace getSensorSpace ()
    {
        return _sensorSpace;
    }

    /**
     * Returns a reference to the pathfinder object.
     */
    public Pathfinder getPathfinder ()
    {
        return _pathfinder;
    }

    /**
     * Sets the default untransformed area of interest region for clients.
     */
    public void setDefaultLocalInterest (Rect interest)
    {
        _defaultLocalInterest = interest;
    }

    /**
     * Returns the default untransformed area of interest region for clients.
     */
    public Rect getDefaultLocalInterest ()
    {
        return _defaultLocalInterest;
    }

    /**
     * Checks whether we should show region debug effects.
     */
    public boolean getDebugRegions ()
    {
        return false;
    }

    /**
     * Spawns an actor with the named configuration.
     */
    public final ActorLogic spawnActor (
        int timestamp, Vector2f translation, float rotation, String name)
    {
        return spawnActor(timestamp, translation, rotation,
            new ConfigReference<ActorConfig>(name));
    }

    /**
     * Spawns an actor with the supplied name and arguments.
     */
    public final ActorLogic spawnActor (
        int timestamp, Vector2f translation, float rotation, String name,
        String firstKey, Object firstValue, Object... otherArgs)
    {
        return spawnActor(timestamp, translation, rotation,
            new ConfigReference<ActorConfig>(name, firstKey, firstValue, otherArgs));
    }

    /**
     * Spawns an actor with the referenced configuration.
     */
    public final ActorLogic spawnActor (
        int timestamp, Vector2f translation, float rotation, ConfigReference<ActorConfig> ref)
    {
        return spawnActor(timestamp, translation, rotation, ref, null);
    }

    /**
     * Spawns an actor with the referenced configuration.
     *
     * @param actor if non-null, the already-created actor object.
     */
    public ActorLogic spawnActor (
        int timestamp, Vector2f translation, float rotation,
        ConfigReference<ActorConfig> ref, Actor actor)
    {
        // return immediately if the place has shut down
        if (!_plobj.isActive()) {
            return null;
        }

        // attempt to resolve the implementation
        ActorConfig config = _cfgmgr.getConfig(ActorConfig.class, ref);
        ActorConfig.Original original = (config == null) ? null : config.getOriginal(_cfgmgr);
        if (original == null) {
            log.warning("Failed to resolve actor config.", "actor", ref, "where", where());
            return null;
        }

        // create the logic object
        final ActorLogic logic = (ActorLogic)createLogic(original.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // initialize the logic and add it to the map
        int id = (actor == null) ? ++_lastActorId : actor.getId();
        logic.init(this, ref, original, id, timestamp, translation, rotation, actor);
        _actors.put(id, logic);
        addMappings(logic);

        // special processing for static actors
        if (logic.isStatic()) {
            _staticActors.add(logic);
            _staticActorsAdded.add(logic);
        }

        // notify observers
        _actorObservers.apply(new ObserverList.ObserverOp<ActorObserver>() {
            public boolean apply (ActorObserver observer) {
                observer.actorAdded(logic);
                return true;
            }
        });

        return logic;
    }

    /**
     * Fires off an effect at the with the named configuration.
     */
    public final EffectLogic fireEffect (
        int timestamp, Logic target, Vector2f translation, float rotation, String name)
    {
        return fireEffect(timestamp, target, translation, rotation,
            new ConfigReference<EffectConfig>(name));
    }

    /**
     * Fires off an effect with the supplied name and arguments.
     */
    public final EffectLogic fireEffect (
        int timestamp, Logic target, Vector2f translation, float rotation, String name,
        String firstKey, Object firstValue, Object... otherArgs)
    {
        return fireEffect(timestamp, target, translation, rotation,
            new ConfigReference<EffectConfig>(name, firstKey, firstValue, otherArgs));
    }

    /**
     * Fires off an effect with the referenced configuration.
     *
     * @param translation an offset from the target's translation, or null, or the absolute
     * translation if there's no target.
     * @param rotation an offset from the target's rotation, or the absolute rotation if there's
     * no target.
     */
    public EffectLogic fireEffect (
        int timestamp, Logic target, Vector2f translation, float rotation,
        ConfigReference<EffectConfig> ref)
    {
        // return immediately if the place has shut down
        if (!_plobj.isActive()) {
            return null;
        }

        // attempt to resolve the implementation
        EffectConfig config = _cfgmgr.getConfig(EffectConfig.class, ref);
        EffectConfig.Original original = (config == null) ? null : config.getOriginal(_cfgmgr);
        if (original == null) {
            log.warning("Failed to resolve effect config.", "effect", ref, "where", where());
            return null;
        }

        // create the logic class
        EffectLogic logic = (EffectLogic)createLogic(original.getLogicClassName());
        if (logic == null) {
            return null;
        }

        // clamp the timestamp to the acceptable range
        timestamp = Math.max(timestamp, _previousTimestamp + 1);

        // initialize the logic and add it to the list
        logic.init(this, ref, original, timestamp, target, translation, rotation);
        _effectsFired.add(logic);

        return logic;
    }

    /**
     * Creates an instance of the logic object with the specified class name using the injector,
     * logging a warning and returning <code>null</code> on error.
     */
    public Logic createLogic (String cname)
    {
        try {
            return (Logic)_injector.getInstance(Class.forName(cname));
        } catch (Exception e) {
            log.warning("Failed to instantiate logic.", "class", cname, e);
            return null;
        }
    }

    /**
     * Returns the logic object for the entity with the provided key, if any.
     */
    public Logic getLogic (EntityKey key)
    {
        if (key instanceof EntityKey.Entry) {
            return getEntryLogic(((EntityKey.Entry)key).getKey());
        } else if (key instanceof EntityKey.Actor) {
            return getActorLogic(((EntityKey.Actor)key).getId());
        } else {
            return null;
        }
    }

    /**
     * Returns the logic object for the entry with the provided key, if any.
     */
    public EntryLogic getEntryLogic (Object key)
    {
        return _entries.get(key);
    }

    /**
     * Returns the logic object for the actor with the provided id, if any.
     */
    public ActorLogic getActorLogic (int id)
    {
        return _actors.get(id);
    }

    /**
     * Populates the supplied collection with references to all non-static actors visible to the
     * specified target whose influence regions intersect the provided bounds.
     */
    public void getVisibleActors (PawnLogic target, Rect bounds, Collection<ActorLogic> results)
    {
        _actorSpace.getElements(bounds, _elements);
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            ActorLogic actor = (ActorLogic)_elements.get(ii).getUserObject();
            if (!actor.isStatic() && (target == null || actor.isVisible(target))) {
                results.add(actor);
            }
        }
        _elements.clear();
    }

    /**
     * Returns a reference to the set of static actors.
     */
    public Set<ActorLogic> getStaticActors ()
    {
        return _staticActors;
    }

    /**
     * Returns a reference to the set of static actors added on the current tick.
     */
    public Set<ActorLogic> getStaticActorsAdded ()
    {
        return _staticActorsAdded;
    }

    /**
     * Returns a reference to the set of static actors updated on the current tick.
     */
    public Set<ActorLogic> getStaticActorsUpdated ()
    {
        return _staticActorsUpdated;
    }

    /**
     * Returns a reference to the set of static actors removed on the current tick.
     */
    public Set<ActorLogic> getStaticActorsRemoved ()
    {
        return _staticActorsRemoved;
    }

    /**
     * Returns an array containing all effects fired on the current tick whose influence regions
     * intersect the provided bounds.
     */
    public Effect[] getEffectsFired (PawnLogic target, Rect bounds)
    {
        for (int ii = 0, nn = _effectsFired.size(); ii < nn; ii++) {
            EffectLogic logic = _effectsFired.get(ii);
            if (logic.getShape().getBounds().intersects(bounds) &&
                    (target == null || logic.isVisible(target))) {
                _effects.add(logic.getEffect());
            }
        }
        Effect[] array = _effects.toArray(new Effect[_effects.size()]);
        _effects.clear();
        return array;
    }

    /**
     * Returns a reference to the target of the specified client, if any.
     */
    public PawnLogic getTarget (ClientObject clobj)
    {
        ClientLiaison client = _clients.get(clobj.getOid());
        return (client == null) ? null : client.getTarget();
    }

    /**
     * Removes the logic mapping for the actor with the given id.
     */
    public void removeActorLogic (int id)
    {
        final ActorLogic logic = _actors.remove(id);
        if (logic == null) {
            log.warning("Missing actor to remove.", "where", where(), "id", id);
            return;
        }
        // remove mappings
        removeMappings(logic);

        // special handling for static actors
        if (logic.isStatic()) {
            _staticActors.remove(logic);
            if (!_staticActorsAdded.remove(logic)) {
                _staticActorsUpdated.remove(logic);
                _staticActorsRemoved.add(logic);
            }
        }

        // notify observers
        _actorObservers.apply(new ObserverList.ObserverOp<ActorObserver>() {
            public boolean apply (ActorObserver observer) {
                observer.actorRemoved(logic);
                return true;
            }
        });
    }

    /**
     * Triggers any intersection sensors intersecting the specified shape.
     */
    public int triggerIntersectionSensors (int timestamp, ActorLogic actor)
    {
        return triggerSensors(
            IntersectionSensor.class, timestamp, actor.getShape(),
            actor.getActor().getCollisionFlags(), actor);
    }

    /**
     * Triggers any sensors of the specified type intersecting the specified shape.
     */
    public int triggerSensors (
        Class<? extends Sensor> type, int timestamp, Shape shape, int flags, ActorLogic actor)
    {
        return triggerSensors(type, timestamp, ImmutableList.of(shape), flags, actor);
    }

    /**
     * Triggers any sensors of the specified type intersecting the specified shape.
     */
    public int triggerSensors (Class<? extends Sensor> type, int timestamp,
            Collection<Shape> shapes, int flags, ActorLogic actor)
    {
        if (flags == 0) {
            return 0;
        }
        Set<SpaceElement> elements = Sets.newHashSet();
        for (Shape shape : shapes) {
            _sensorSpace.getIntersecting(shape, elements);
        }
        int count = 0;
        for (SpaceElement element : elements) {
            Sensor sensor = (Sensor)element.getUserObject();
            if (type.isInstance(sensor) && (flags & sensor.getMask()) != 0) {
                sensor.trigger(timestamp, actor);
                count++;
            }
        }
        return count;
    }

    /**
     * Determines whether the specified actor collides with anything in the environment.
     */
    public final boolean collides (ActorLogic logic)
    {
        return collides(logic, logic.getShape());
    }

    /**
     * Determines whether the specified actor collides with anything in the environment.
     */
    public final boolean collides (ActorLogic logic, Shape shape)
    {
        return collides(logic, shape, _timestamp);
    }

    /**
     * Determines whether the specified actor collides with anything in the environment.
     */
    public final boolean collides (ActorLogic logic, Shape shape, int timestamp)
    {
        return collides(logic.getActor(), shape, timestamp);
    }

    /**
     * Determines whether the specified actor collides with anything in the environment.
     */
    public boolean collides (Actor actor, Shape shape, int timestamp)
    {
        // check the scene model
        if (((TudeySceneModel)_scene.getSceneModel()).collides(actor, shape)) {
            return true;
        }

        // look for intersecting elements
        _actorSpace.getIntersecting(shape, _elements);
        try {
            for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
                SpaceElement element = _elements.get(ii);
                Actor oactor = ((ActorLogic)element.getUserObject()).getActor();
                if (timestamp < oactor.getDestroyed() && actor.canCollide(oactor)) {
                    return true;
                }
            }
        } finally {
            _elements.clear();
        }
        return false;
    }

    /**
     * Determines whether the specified shape collides with anything in the environment.
     */
    public final boolean collides (int mask, Shape shape)
    {
        return collides(mask, shape, _timestamp);
    }

    /**
     * Determines whether the specified shape collides with anything in the environment.
     */
    public final boolean collides (int mask, Shape shape, int timestamp)
    {
        return collides(mask, shape, timestamp, Predicates.alwaysTrue());
    }

    /**
     * Determines whether the specified shape collides with anything in the environment.
     */
    public boolean collides (
            int mask, Shape shape, int timestamp, Predicate<? super Actor> canCollidePred)
    {
        // make sure we can actually collide with anything
        if (mask == 0) {
            return false;
        }

        // check the scene model
        if (((TudeySceneModel)_scene.getSceneModel()).collides(mask, shape)) {
            return true;
        }

        // look for intersecting elements
        _actorSpace.getIntersecting(shape, _elements);
        try {
            for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
                SpaceElement element = _elements.get(ii);
                Actor actor = ((ActorLogic)element.getUserObject()).getActor();
                if ((timestamp < actor.getDestroyed()) &&
                        ((actor.getCollisionFlags() & mask) != 0) &&
                        canCollidePred.apply(actor)) {
                    return true;
                }
            }
        } finally {
            _elements.clear();
        }
        return false;
    }

    /**
     * Determines the intersection point of this segment in the environment.
     */
    public final boolean getIntersection (
        Ray2D ray, float length, int mask, int timestamp, Vector2f intersection)
    {
        return getIntersection(ray, length, mask, timestamp, intersection, null);
    }

    /**
     * Determines the intersection point of this segment in the environment and returns the
     * intersecting actor.
     *
     * @param intersectingActor An array of size 1 where the ActorLogic is stored if it was the
     * nearest intersection, or null
     */
    public boolean getIntersection (
        Ray2D ray, float length, int mask, int timestamp, Vector2f intersection,
        ActorLogic[] intersectingActor)
    {
        if (mask == 0) {
            return false;
        }

        boolean intersects = getSceneModel().getIntersection(ray, length, mask, intersection);
        float resultDist = intersects ?
            ray.getOrigin().distanceSquared(intersection) : length * length;

        Segment seg = new Segment(
                ray.getOrigin(), ray.getOrigin().add(ray.getDirection().mult(length)));
        _actorSpace.getIntersecting(seg, _elements);
        Vector2f result = new Vector2f();
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SpaceElement element = _elements.get(ii);
            ActorLogic logic = (ActorLogic)element.getUserObject();
            Actor actor = logic.getActor();
            if (timestamp < actor.getDestroyed() && (actor.getCollisionFlags() & mask) != 0 &&
                    logic.getShape().getIntersection(ray, result)) {
                float dist = ray.getOrigin().distanceSquared(result);
                if (dist < resultDist) {
                    intersection.set(result);
                    resultDist = dist;
                    if (intersectingActor != null) {
                        intersectingActor[0] = logic;
                    }
                }
            }
        }
        _elements.clear();
        return resultDist < length * length;
    }

    /**
     * Notes that a static actor's state has changed.
     */
    public void staticActorUpdated (ActorLogic logic)
    {
        if (!_staticActorsAdded.contains(logic)) {
            _staticActorsUpdated.add(logic);
        }
    }

    /**
     * Notes that a body will be entering via the identified portal.
     */
    public void mapEnteringBody (BodyObject body, Object portalKey)
    {
        _entering.put(body.getOid(), portalKey);
    }

    /**
     * Clears out the mapping for an entering body.
     */
    public void clearEnteringBody (BodyObject body)
    {
        _entering.remove(body.getOid());
    }

    @Override // from PlaceManager
    public void bodyWillEnter (BodyObject body)
    {
        // configure the client's message throttle to 1.5 times the absolute minimum
        PresentsSession client = _clmgr.getClient(body.username);
        if (client != null) {
            client.setIncomingMessageThrottle(1500 / getTransmitInterval());
        }

        // add the pawn and configure a local to provide its id
        ConfigReference<ActorConfig> ref = getPawnConfig(body);
        if (ref != null) {
            Object portalKey = _entering.remove(body.getOid());
            Transform2D transform = getPortalTransform(portalKey);
            Vector2f translation = transform == null ?
                Vector2f.ZERO : transform.extractTranslation();
            float rotation = transform == null ? 0f : transform.extractRotation();
            if (transform == null) {
                // select a default entrance
                Logic entrance = getDefaultEntrance(body);
                if (entrance != null) {
                    translation = entrance.getTranslation();
                    rotation = entrance.getRotation();
                }
            }
            final ActorLogic logic = spawnActor(getNextTimestamp(), translation, rotation, ref);
            if (logic != null) {
                logic.bodyWillEnter(body);
                ((TudeyBodyObject)body).setPawnId(logic.getActor().getId());
            }
        }

        // now let the body actually enter the scene
        super.bodyWillEnter(body);
    }

    /**
     * Returns the transform for a portal, or null or no portal found.
     */
    public Transform2D getPortalTransform (Object portalKey)
    {
        Transform2D result = new Transform2D(Vector2f.ZERO, 0f);
        if (portalKey instanceof Logic || portalKey instanceof EntityKey.Actor) {
            // make sure the logic is still active
            Logic entrance = (portalKey instanceof EntityKey.Actor) ?
                getActorLogic(((EntityKey.Actor)portalKey).getId()) : (Logic)portalKey;
            if (entrance != null && entrance.isActive()) {
                return entrance.getPortalTransform();
            }
        } else if (portalKey instanceof String) {
            Logic entrance = Randoms.threadLocal().pick(getTagged((String)portalKey), null);
            if (entrance != null) {
                return new Transform2D(entrance.getTranslation(), entrance.getRotation());
            }
        } else if (portalKey instanceof Transform2D) {
            return (Transform2D)portalKey;

        } else if (portalKey != null) {
            // get the translation/rotation from the entering portal
            Entry entry = ((TudeySceneModel)_scene.getSceneModel()).getEntry(
                portalKey instanceof EntityKey.Entry ?
                    ((EntityKey.Entry)portalKey).getKey() : portalKey);
            if (entry != null) {
                return new Transform2D(entry.getTranslation(_cfgmgr), entry.getRotation(_cfgmgr));
            }
        }
        return null;
    }

    @Override // from PlaceManager
    public void bodyWillLeave (BodyObject body)
    {
        super.bodyWillLeave(body);
        TudeyBodyObject tbody = (TudeyBodyObject)body;
        if (tbody.pawnId != 0) {
            ActorLogic logic = _actors.get(tbody.pawnId);
            if (logic != null) {
                logic.bodyWillLeave(body);
            } else {
                log.warning("Missing pawn for leaving body.", "pawnId", tbody.pawnId,
                    "who", tbody, "where", where());
            }
            tbody.setPawnId(0);
        }
    }

    // documentation inherited from interface TudeySceneProvider
    public void enteredPlace (ClientObject caller)
    {
        // forward to client liaison
        ClientLiaison client = _clients.get(caller.getOid());
        if (client == null) {
            log.warning("Received entrance notification from unknown client.",
                "who", caller, "where", where());
            return;
        }
        client.enteredPlace();
    }

    // documentation inherited from interface TudeySceneProvider
    public void enqueueInputReliable (
        ClientObject caller, int acknowledge, int smoothedTime, InputFrame[] frames)
    {
        // these are handled in exactly the same way; the methods are separate to provide different
        // transport options
        enqueueInputUnreliable(caller, acknowledge, smoothedTime, frames);
    }

    // documentation inherited from interface TudeySceneProvider
    public void enqueueInputUnreliable (
        ClientObject caller, int acknowledge, int smoothedTime, InputFrame[] frames)
    {
        // forward to client liaison
        ClientLiaison client = _clients.get(caller.getOid());
        if (client != null) {
            // ping is current time minus client's smoothed time estimate
            int currentTime = _timestamp + (int)(RunAnywhere.currentTimeMillis() - _lastTick);
            client.enqueueInput(acknowledge, currentTime - smoothedTime, frames);
        } else {
            // this doesn't require a warning; it's probably an out-of-date packet from a client
            // that has just left the scene
            log.debug("Received input from unknown client.",
                "who", caller, "where", where());
        }
    }

    // documentation inherited from interface TudeySceneProvider
    public void setTarget (ClientObject caller, int pawnId)
    {
        // get the client liaison
        int cloid = caller.getOid();
        ClientLiaison client = _clients.get(cloid);
        if (client == null) {
            log.warning("Received target request from unknown client.",
                "who", caller, "where", where());
            return;
        }

        // retrieve the actor and ensure it's a pawn
        ActorLogic target = _actors.get(pawnId);
        if (target instanceof PawnLogic) {
            client.setTarget((PawnLogic)target);
        } else {
            log.warning("User tried to target non-pawn.", "who",
                caller, "actor", (target == null) ? null : target.getActor());
        }
    }

    // documentation inherited from interface TudeySceneProvider
    public void setCameraParams (ClientObject caller, CameraConfig config, float aspect)
    {
        // forward to client liaison
        ClientLiaison client = _clients.get(caller.getOid());
        if (client != null) {
            client.setCameraParams(config, aspect);
        } else {
            log.warning("Received camera params from unknown client.",
                "who", caller, "where", where());
        }
    }

    // documentation inherited from interface TudeySceneProvider
    public void submitActorRequest (ClientObject caller, int actorId, String name)
    {
        // get the client liaison
        int cloid = caller.getOid();
        ClientLiaison client = _clients.get(cloid);
        if (client == null) {
            log.warning("Received actor request from unknown client.",
                "who", caller, "where", where());
            return;
        }

        // get their pawn logic to act as a source
        int pawnId = _tsobj.getPawnId(cloid);
        if (pawnId <= 0) {
            log.warning("User without pawn tried to submit actor request.", "who", caller);
            return;
        }
        PawnLogic source = (PawnLogic)_actors.get(pawnId);

        // get the target logic
        ActorLogic target = _actors.get(actorId);
        if (target == null) {
            log.warning("Missing actor for request.", "who", caller, "id", actorId);
            return;
        }

        // process the request
        target.request(getNextTimestamp(), source, name);
    }

    // documentation inherited from interface TudeySceneProvider
    public void submitEntryRequest (ClientObject caller, Object key, String name)
    {
        // get the client liaison
        int cloid = caller.getOid();
        ClientLiaison client = _clients.get(cloid);
        if (client == null) {
            log.warning("Received entry request from unknown client.",
                "who", caller, "where", where());
            return;
        }

        // get their pawn logic to act as a source
        int pawnId = _tsobj.getPawnId(cloid);
        if (pawnId <= 0) {
            log.warning("User without pawn tried to submit entry request.", "who", caller);
            return;
        }
        PawnLogic source = (PawnLogic)_actors.get(pawnId);

        // get the target logic
        EntryLogic target = _entries.get(key);
        if (target == null) {
            log.warning("Missing entry for request.", "who", caller, "key", key);
            return;
        }

        // process the request
        target.request(getNextTimestamp(), source, name);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (Entry entry)
    {
        addLogic(entry, true);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        removeLogic(oentry.getKey());
        addLogic(nentry, true);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
        removeLogic(oentry.getKey());
    }

    // documentation inherited from interface ActorAdvancer.Environment
    public TudeySceneModel getSceneModel ()
    {
        return (TudeySceneModel)_scene.getSceneModel();
    }

    // documentation inherited from interface ActorAdvancer.Environment
    public boolean getPenetration (Actor actor, Shape shape, Vector2f result)
    {
        // start with zero penetration
        result.set(Vector2f.ZERO);

        // check the scene model
        ((TudeySceneModel)_scene.getSceneModel()).getPenetration(actor, shape, result);

        // get the intersecting elements
        _actorSpace.getIntersecting(shape, _elements);
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SpaceElement element = _elements.get(ii);
            Actor oactor = ((ActorLogic)element.getUserObject()).getActor();
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
    public final boolean collides (Actor actor, Shape shape)
    {
        return collides(actor, shape, _timestamp);
    }

    // documentation inherited from interface ActorAdvancer.Environment
    public int getDirections (Actor actor, Shape shape)
    {
        return ((TudeySceneModel)_scene.getSceneModel()).getDirections(actor, shape);
    }

    // documentation inherited from interface RunQueue
    public void postRunnable (Runnable runnable)
    {
        synchronized (_runnables) {
            _runnables.add(runnable);
        }
    }

    // documentation inherited from interface RunQueue
    public boolean isDispatchThread ()
    {
        return _omgr.isDispatchThread();
    }

    // documentation inherited from interface RunQueue
    public boolean isRunning ()
    {
        return _ticker != null;
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return (_tsobj = new TudeySceneObject());
    }

    @Override
    protected void didStartup ()
    {
        super.didStartup();

        // get a reference to the scene's config manager
        TudeySceneModel sceneModel = (TudeySceneModel)_scene.getSceneModel();
        _cfgmgr = sceneModel.getConfigManager();

        // create the pathfinder
        _pathfinder = new Pathfinder(this);

        // get a reference to the ticker
        _ticker = getTicker();

        // create logic objects for scene entries and listen for changes
        createEntryLogics(sceneModel);
        sceneModel.addObserver(this);

        // register and fill in our tudey scene service
        _tsobj.setTudeySceneService(addProvider(this, TudeySceneMarshaller.class));
    }

    /**
     * Creates logics for an entries that have them.
     */
    protected void createEntryLogics (TudeySceneModel sceneModel)
    {
        // add first, then notify; the entries may be looking for other tagged entries
        for (Entry entry : sceneModel.getEntries()) {
            addLogic(entry, false);
        }
        for (EntryLogic logic : _entries.values()) {
            logic.added();
        }
    }

    @Override
    protected void didShutdown ()
    {
        super.didShutdown();

        // stop listening to the scene model
        ((TudeySceneModel)_scene.getSceneModel()).removeObserver(this);

        // flag the spaces as disposed to avoid extra unnecessary removal computation
        _actorSpace.dispose();
        _sensorSpace.dispose();

        // destroy/remove all actors
        ActorLogic[] actors = _actors.values().toArray(new ActorLogic[_actors.size()]);
        int timestamp = getNextTimestamp();
        for (ActorLogic logic : actors) {
            logic.destroy(timestamp, logic, true);
            logic.remove();
        }
        _actors.clear();

        // remove all scene entries
        for (EntryLogic logic : _entries.values()) {
            logic.removed(true);
        }

        _shutdownObservers.apply(_shutdownOp);

        // remove from the ticker
        _ticker.remove(this);
        _ticker = null;

        // shut down the pathfinder
        _pathfinder.shutdown();
        _pathfinder = null;
    }

    @Override
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        // create and map the client liaison
        BodyObject bodyobj = (BodyObject)_omgr.getObject(bodyOid);
        CrowdSession session = (CrowdSession)_clmgr.getClient(bodyobj.username);
        _clients.put(bodyOid, createClientLiaison(bodyobj, session));

        // register with the ticker when the first occupant enters
        if (!_ticker.contains(this)) {
            _lastTick = RunAnywhere.currentTimeMillis();
            _ticker.add(this);
        }
    }

    @Override
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // remove the client liaison
        _clients.remove(bodyOid);
    }

    @Override
    protected void placeBecameEmpty ()
    {
        super.placeBecameEmpty();

        // record the time
        _emptyTime = RunAnywhere.currentTimeMillis();
    }

    @Override
    protected void bodyUpdated (OccupantInfo info)
    {
        super.bodyUpdated(info);

        // pass the information on to the liaison
        _clients.get(info.getBodyOid()).bodyUpdated(info);
    }

    /**
     * Creates the client liaison for the specified body.
     */
    protected ClientLiaison createClientLiaison (BodyObject bodyobj, CrowdSession session)
    {
        return new ClientLiaison(this, bodyobj, session);
    }

    /**
     * Selects a default entrance for an entering player.
     *
     * @return the default entrance, or null if no such entrance is available.
     */
    protected Logic getDefaultEntrance (BodyObject body)
    {
        return Randoms.threadLocal().pick(_defaultEntrances, null);
    }

    /**
     * Adds a logic to the default entrance if it has been marked as such.
     */
    protected void maybeAddDefaultEntrance (Logic logic)
    {
        if (logic.isDefaultEntrance()) {
            _defaultEntrances.add(logic);
        }
    }

    /**
     * Adds the logic object for the specified scene entry, if any.
     *
     * @param notify whether or not to notify the logic that it has been added.
     */
    protected EntryLogic addLogic (Entry entry, boolean notify)
    {
        String cname = entry.getLogicClassName(_cfgmgr);
        if (cname == null) {
            return null;
        }
        EntryLogic logic = (EntryLogic)createLogic(cname);
        if (logic == null) {
            return null;
        }
        logic.init(this, entry);
        _entries.put(entry.getKey(), logic);
        addMappings(logic);
        if (notify) {
            logic.added();
        }
        return logic;
    }

    /**
     * Removes the logic object for the specified scene entry, if any.
     */
    protected void removeLogic (Object key)
    {
        EntryLogic logic = _entries.remove(key);
        if (logic != null) {
            removeMappings(logic);
            logic.removed(false);
        }
    }

    /**
     * Registers the specified logic object unders its mappings.
     */
    public void addMappings (Logic logic)
    {
        for (String tag : logic.getTags()) {
            ArrayList<Logic> list = _tagged.get(tag);
            if (list == null) {
                _tagged.put(tag, list = new ArrayList<Logic>());
            }
            list.add(logic);
        }
        for (Class<?> clazz = logic.getClass(); Logic.class.isAssignableFrom(clazz);
                clazz = clazz.getSuperclass()) {
            ArrayList<Logic> list = _instances.get(clazz);
            if (list == null) {
                _instances.put(clazz, list = new ArrayList<Logic>());
            }
            list.add(logic);
        }
        maybeAddDefaultEntrance(logic);
    }

    /**
     * Remove the specified logic object from the mappings.
     */
    public void removeMappings (Logic logic)
    {
        for (String tag : logic.getTags()) {
            ArrayList<Logic> list = _tagged.get(tag);
            if (list == null || !list.remove(logic)) {
                log.warning("Missing tag mapping for logic.", "tag", tag, "logic", logic);
                continue;
            }
            if (list.isEmpty()) {
                _tagged.remove(tag);
            }
        }
        for (Class<?> clazz = logic.getClass(); Logic.class.isAssignableFrom(clazz);
                clazz = clazz.getSuperclass()) {
            ArrayList<Logic> list = _instances.get(clazz);
            if (list == null || !list.remove(logic)) {
                log.warning("Missing class mapping for logic.", "class", clazz, "logic", logic);
                continue;
            }
            if (list.isEmpty()) {
                _instances.remove(clazz);
            }
        }
        if (logic.isDefaultEntrance()) {
            _defaultEntrances.remove(logic);
        }
    }

    /**
     * Updates the scene.
     */
    public void tick ()
    {
        // cancel the ticker if enough time has elapsed with no occupants
        long now = RunAnywhere.currentTimeMillis();
        if (_plobj.occupants.size() == 0 && (now - _emptyTime) >= idleTickPeriod()) {
            _ticker.remove(this);
            return;
        }

        // update the scene timestamp
        _previousTimestamp = _timestamp;
        _timestamp += (int)(now - _lastTick);
        _lastTick = now;

        // copy the runnables into another list and clear
        synchronized (_runnables) {
            _runlist.addAll(_runnables);
            _runnables.clear();
        }
        _ticking = true;
        if (_tickProfEnabled) {
            // tick the participants
            _profileTickOp.init(_timestamp);
            _tickParticipants.apply(_profileTickOp);

            // process the runnables in the list
            for (int ii = 0, nn = _runlist.size(); ii < nn; ii++) {
                Runnable runnable = _runlist.get(ii);
                try {
                    if (_tickParticipantCount++ % _tickProfInterval == 0) {
                        long started = System.nanoTime();
                        runnable.run();
                        updateTickProfile(runnable, started);
                    } else {
                        runnable.run();
                    }
                } catch (Throwable t) {
                    log.warning("Caught throwable executing runnable.",
                        "where", where(), "runnable", runnable, t);
                }
            }
            _runlist.clear();

            // post deltas for all clients
            for (ClientLiaison client : _clients.values()) {
                try {
                    if (_tickParticipantCount++ % _tickProfInterval == 0) {
                        long started = System.nanoTime();
                        client.postDelta();
                        updateTickProfile(client, started);
                    } else {
                        client.postDelta();
                    }
                } catch (Throwable t) {
                    log.warning("Caught throwable posting delta.",
                        "where", where(), "client", client, t);
                }
            }
        } else {
            // tick the participants
            _tickOp.init(_timestamp);
            _tickParticipants.apply(_tickOp);

            // process the runnables in the list
            for (int ii = 0, nn = _runlist.size(); ii < nn; ii++) {
                Runnable runnable = _runlist.get(ii);
                try {
                    runnable.run();
                } catch (Throwable t) {
                    log.warning("Caught throwable executing runnable.",
                        "where", where(), "runnable", runnable, t);
                }
            }
            _runlist.clear();

            // post deltas for all clients
            for (ClientLiaison client : _clients.values()) {
                try {
                    client.postDelta();
                } catch (Throwable t) {
                    log.warning("Caught throwable posting delta.",
                        "where", where(), "client", client, t);
                }
            }
        }
        _ticking = false;

        // clear the lists
        _staticActorsAdded.clear();
        _staticActorsUpdated.clear();
        _staticActorsRemoved.clear();
        _effectsFired.clear();

        // note how long the tick took
        _tickDuration = (RunAnywhere.currentTimeMillis() - _lastTick);
    }

    /**
     * Returns a reference to the configuration to use for the specified body's pawn or
     * <code>null</code> for none.
     */
    protected ConfigReference<ActorConfig> getPawnConfig (BodyObject body)
    {
        return null;
    }

    /**
     * Returns the number of milliseconds to continue ticking when there are no occupants in
     * the scene.
     */
    protected long idleTickPeriod ()
    {
        return 5 * 1000L;
    }

    /**
     * Returns the ticker with which to tick the scene.
     */
    protected SceneTicker getTicker ()
    {
        return ((TudeySceneRegistry)_screg).getDefaultTicker();
    }

    /**
     * Updates the tick profile for the specified participant.
     */
    protected static void updateTickProfile (Object participant, long started)
    {
        long elapsed = (System.nanoTime() - started) / 1000L;
        String cname;
        if (participant instanceof Interval.RunBuddy) {
            cname = StringUtil.shortClassName(
                ((Interval.RunBuddy)participant).getIntervalClassName());
        } else {
            cname = StringUtil.shortClassName(participant);
        }
        if (participant instanceof Logic) {
            Logic logic = (Logic)participant;
            participant = logic.getSceneManager().getLogic(logic.getEntityKey());
        }
        ConfigReference<?> ref = null;
        if (participant instanceof ActorLogic) {
            ref = ((ActorLogic)participant).getActor().getConfig();
        } else if (participant instanceof EntryLogic) {
            ref = ((EntryLogic)participant).getEntry().getReference();
        }
        if (ref != null) {
            String rname = ref.getName();
            cname += ":" + rname.substring(rname.lastIndexOf('/') + 1);
        }
        TickProfile tprof = _profiles.get(cname);
        if (tprof == null) {
            _profiles.put(cname, tprof = new TickProfile());
        }
        tprof.record(elapsed);
    }

    /**
     * (Re)used to tick the participants.
     */
    protected static class TickOp
        implements ObserverList.ObserverOp<TickParticipant>
    {
        /**
         * (Re)initializes the op with the current timestamp.
         */
        public void init (int timestamp)
        {
            _timestamp = timestamp;
        }

        // documentation inherited from interface ObserverList.ObserverOp
        public boolean apply (TickParticipant participant)
        {
            try {
                return participant.tick(_timestamp);
            } catch (Throwable t) {
                log.warning("Caught throwable ticking participant.",
                    "participant", participant, t);
                return false;
            }
        }

        /** The timestamp of the current tick. */
        protected int _timestamp;
    }

    /**
     * Extends the tick op with profiling bits.
     */
    protected static class ProfileTickOp extends TickOp
    {
        @Override
        public boolean apply (TickParticipant participant)
        {
            try {
                if (_tickParticipantCount++ % _tickProfInterval != 0) {
                    return participant.tick(_timestamp);
                }
                long started = System.nanoTime();
                boolean result = participant.tick(_timestamp);
                updateTickProfile(participant, started);
                return result;

            } catch (Throwable t) {
                log.warning("Caught throwable ticking participant.",
                    "participant", participant, t);
                return false;
            }
        }
    }

    /**
     * Records information about a tick participant.
     */
    protected static class TickProfile
    {
        public void record (long elapsed)
        {
            _totalElapsed += elapsed;
            _histo.addValue((int)elapsed);
            _longest = Math.max(elapsed, _longest);
        }

        @Override
        public String toString ()
        {
            int count = _histo.size();
            return _totalElapsed + "us/" + count + " = " + (_totalElapsed/count) + "us avg " +
                StringUtil.toString(_histo.getBuckets()) + " " + _longest + "us longest";
        }

        protected long _totalElapsed, _longest;
        protected Histogram _histo = new Histogram(0, 20000, 10);
    }

    /** The injector that we use to create and initialize our logic objects. */
    @Inject protected Injector _injector;

    /** The client manager. */
    @Inject protected ClientManager _clmgr;

    /** A casted reference to the Tudey scene object. */
    protected TudeySceneObject _tsobj;

    /** A reference to the scene model's configuration manager. */
    protected ConfigManager _cfgmgr;

    /** The ticker. */
    protected SceneTicker _ticker;

    /** The system time of the last tick. */
    protected long _lastTick;

    /** The duration of processing for the last tick. */
    protected long _tickDuration;

    /** The timestamp of the current and previous ticks. */
    protected int _timestamp, _previousTimestamp;

    /** The time at which the last occupant left. */
    protected long _emptyTime;

    /** The last actor id assigned. */
    protected int _lastActorId;

    /** Maps oids of entering bodies to the keys of the portals through which they're entering. */
    protected HashIntMap<Object> _entering = IntMaps.newHashIntMap();

    /** Maps body oids to client liaisons. */
    protected HashIntMap<ClientLiaison> _clients = IntMaps.newHashIntMap();

    /** The list of participants in the tick. */
    protected ObserverList<TickParticipant> _tickParticipants = ObserverList.newSafeInOrder();

    /** Set when we're actually in the process of ticking. */
    protected boolean _ticking;

    /** The list of actor observers. */
    protected ObserverList<ActorObserver> _actorObservers = ObserverList.newFastUnsafe();

    /** The list of shutdown observers. */
    protected ObserverList<ShutdownObserver> _shutdownObservers = ObserverList.newFastUnsafe();

    /** Scene entry logic objects mapped by key. */
    protected HashMap<Object, EntryLogic> _entries = Maps.newHashMap();

    /** Actor logic objects mapped by id. */
    protected HashIntMap<ActorLogic> _actors = IntMaps.newHashIntMap();

    /** "Static" actors. */
    protected Set<ActorLogic> _staticActors = Sets.newHashSet();

    /** Maps tags to lists of logic objects with that tag. */
    protected HashMap<String, ArrayList<Logic>> _tagged = Maps.newHashMap();

    /** Maps logic classes to lists of logic instances. */
    protected HashMap<Class<?>, ArrayList<Logic>> _instances = Maps.newHashMap();

    /** The logic objects corresponding to default entrances. */
    protected ArrayList<Logic> _defaultEntrances = Lists.newArrayList();

    /** The actor space.  Used to find the actors within a client's area of interest. */
    protected HashSpace _actorSpace = new HashSpace(64f, 6);

    /** The sensor space.  Used to detect mobile objects. */
    protected HashSpace _sensorSpace = new HashSpace(64f, 6);

    /** The pathfinder used for path computation. */
    protected Pathfinder _pathfinder;

    /** The logic for static actors added on the current tick. */
    protected Set<ActorLogic> _staticActorsAdded = Sets.newHashSet();

    /** The logic for static actors updated on the current tick. */
    protected Set<ActorLogic> _staticActorsUpdated = Sets.newHashSet();

    /** The logic for static actors removed on the current tick. */
    protected Set<ActorLogic> _staticActorsRemoved = Sets.newHashSet();

    /** The logic for effects fired on the current tick. */
    protected ArrayList<EffectLogic> _effectsFired = Lists.newArrayList();

    /** Runnables enqueued for the next tick. */
    protected List<Runnable> _runnables = Lists.newArrayList();

    /** The default local interest region. */
    protected Rect _defaultLocalInterest = TudeySceneMetrics.getDefaultLocalInterest();

    /** Holds collected elements during queries. */
    protected ArrayList<SpaceElement> _elements = Lists.newArrayList();

    /** Holds collected effects during queries. */
    protected ArrayList<Effect> _effects = Lists.newArrayList();

    /** Holds runnables during tick. */
    protected List<Runnable> _runlist = Lists.newArrayList();

    /** Used to tick the participants. */
    protected TickOp _tickOp = new TickOp();

    /** The tick op used when profiling. */
    protected ProfileTickOp _profileTickOp = new ProfileTickOp();

    /** Stores penetration vector during queries. */
    protected Vector2f _penetration = new Vector2f();

    /** Whether or not we're profiling tick participants. */
    protected static boolean _tickProfEnabled;

    /** The frequency at which we take tick samples. */
    protected static int _tickProfInterval = 100;

    /** Used to profile our tick participants. */
    protected static Map<String, TickProfile> _profiles = Maps.newHashMap();

    /** Incremented on each participant tick when profiling. */
    protected static long _tickParticipantCount;

    /** Shutdown observer op. */
    protected static final ObserverList.ObserverOp<ShutdownObserver> _shutdownOp =
        new ObserverList.ObserverOp<ShutdownObserver>() {
        public boolean apply (ShutdownObserver observer) {
            observer.didShutdown();
            return false;
        }
    };
}
