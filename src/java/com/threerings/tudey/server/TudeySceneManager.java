//
// $Id$

package com.threerings.tudey.server;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Interval;
import com.samskivert.util.ObserverList;
import com.samskivert.util.Queue;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.ClientObject;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.server.SceneManager;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.math.Rect;
import com.threerings.math.SphereCoords;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.EffectConfig;
import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.data.TudeySceneLocal;
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
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.ShapeElement;
import com.threerings.tudey.space.HashSpace;
import com.threerings.tudey.space.SpaceElement;
import com.threerings.tudey.util.ActorAdvancer;

import static com.threerings.tudey.Log.*;

/**
 * Manager for Tudey scenes.
 */
public class TudeySceneManager extends SceneManager
    implements TudeySceneProvider, TudeySceneModel.Observer, ActorAdvancer.Environment, RunQueue
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
     * An interface for objects that should be notified when actors intersect them.
     */
    public interface Sensor
    {
        /**
         * Triggers the sensor.
         *
         * @param actor the logic object of the actor that triggered the sensor.
         */
        public void trigger (ActorLogic actor);
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
     * Returns the timestamp of the current tick.
     */
    public int getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns the list of logic objects with the supplied tag, or <code>null</code> for none.
     */
    public ArrayList<Logic> getTagged (String tag)
    {
        return _tagged.get(tag);
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
     * Spawns an actor with the named configuration.
     */
    public ActorLogic spawnActor (
        int timestamp, Vector2f translation, float rotation, String name)
    {
        return spawnActor(timestamp, translation, rotation,
            new ConfigReference<ActorConfig>(name));
    }

    /**
     * Spawns an actor with the supplied name and arguments.
     */
    public ActorLogic spawnActor (
        int timestamp, Vector2f translation, float rotation, String name,
        String firstKey, Object firstValue, Object... otherArgs)
    {
        return spawnActor(timestamp, translation, rotation,
            new ConfigReference<ActorConfig>(name, firstKey, firstValue, otherArgs));
    }

    /**
     * Spawns an actor with the referenced configuration.
     */
    public ActorLogic spawnActor (
        int timestamp, Vector2f translation, float rotation, ConfigReference<ActorConfig> ref)
    {
        // attempt to resolve the implementation
        ActorConfig config = _cfgmgr.getConfig(ActorConfig.class, ref);
        ActorConfig.Original original = (config == null) ? null : config.getOriginal(_cfgmgr);
        if (original == null) {
            log.warning("Failed to resolve actor config.", "actor", ref);
            return null;
        }

        // create the logic object
        ActorLogic logic;
        try {
            logic = (ActorLogic)Class.forName(original.getLogicClassName()).newInstance();
        } catch (Exception e) {
            log.warning("Failed to instantiate actor logic.",
                "class", original.getLogicClassName(), e);
            return null;
        }

        // initialize the logic and add it to the map
        logic.init(this, ref, original, ++_lastActorId, timestamp, translation, rotation);
        _actors.put(_lastActorId, logic);
        addTagMapping(logic);

        return logic;
    }

    /**
     * Fires off an effect at the with the named configuration.
     */
    public EffectLogic fireEffect (
        int timestamp, Vector2f translation, float rotation, String name)
    {
        return fireEffect(timestamp, translation, rotation,
            new ConfigReference<EffectConfig>(name));
    }

    /**
     * Fires off an effect with the supplied name and arguments.
     */
    public EffectLogic fireEffect (
        int timestamp, Vector2f translation, float rotation, String name,
        String firstKey, Object firstValue, Object... otherArgs)
    {
        return fireEffect(timestamp, translation, rotation,
            new ConfigReference<EffectConfig>(name, firstKey, firstValue, otherArgs));
    }

    /**
     * Fires off an effect with the referenced configuration.
     */
    public EffectLogic fireEffect (
        int timestamp, Vector2f translation, float rotation, ConfigReference<EffectConfig> ref)
    {
        // attempt to resolve the implementation
        EffectConfig config = _cfgmgr.getConfig(EffectConfig.class, ref);
        EffectConfig.Original original = (config == null) ? null : config.getOriginal(_cfgmgr);
        if (original == null) {
            log.warning("Failed to resolve effect config.", "effect", ref);
            return null;
        }

        // create the logic class
        EffectLogic logic;
        try {
            logic = (EffectLogic)Class.forName(original.getLogicClassName()).newInstance();
        } catch (Exception e) {
            log.warning("Failed to instantiate effect logic.",
                "class", original.getLogicClassName(), e);
            return null;
        }

        // initialize the logic and add it to the list
        logic.init(this, ref, original, timestamp, translation, rotation);
        _effectsFired.add(logic);

        return logic;
    }

    /**
     * Returns the logic object for the actor with the provided id, if any.
     */
    public ActorLogic getActorLogic (int id)
    {
        return _actors.get(id);
    }

    /**
     * Returns a map containing the snapshots of all actors whose influence regions intersect the
     * provided bounds.
     */
    public HashIntMap<Actor> getActorSnapshots (Rect bounds)
    {
        _actorSpace.getElements(bounds, _elements);
        HashIntMap<Actor> map = new HashIntMap<Actor>();
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            Actor actor = ((ActorLogic)_elements.get(ii).getUserObject()).getSnapshot();
            map.put(actor.getId(), actor);
        }
        _elements.clear();
        return map;
    }

    /**
     * Returns an array containing all effects fired on the current tick whose influence regions
     * intersect the provided bounds.
     */
    public Effect[] getEffectsFired (Rect bounds)
    {
        for (int ii = 0, nn = _effectsFired.size(); ii < nn; ii++) {
            EffectLogic logic = _effectsFired.get(ii);
            if (logic.getShape().getBounds().intersects(bounds)) {
                _effects.add(logic.getEffect());
            }
        }
        Effect[] array = _effects.toArray(new Effect[_effects.size()]);
        _effects.clear();
        return array;
    }

    /**
     * Removes the logic mapping for the actor with the given id.
     */
    public void removeActorLogic (int id)
    {
        ActorLogic logic = _actors.remove(id);
        if (logic != null) {
            removeTagMapping(logic);
        } else {
            log.warning("Missing actor to remove.", "where", where(), "id", id);
        }
    }

    /**
     * Triggers any sensors intersecting the specified shape.
     */
    public void triggerSensors (Shape shape, ActorLogic actor)
    {
        _sensorSpace.getIntersecting(shape, _elements);
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            ((Sensor)_elements.get(ii).getUserObject()).trigger(actor);
        }
        _elements.clear();
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
        // add the pawn and configure a local to provide its id
        ConfigReference<ActorConfig> ref = getPawnConfig(body);
        if (ref != null) {
            final ActorLogic logic = spawnActor(
                _timestamp + getTickInterval(), Vector2f.ZERO, 0f, ref);
            if (logic != null) {
                body.setLocal(TudeySceneLocal.class, new TudeySceneLocal() {
                    public int getPawnId () {
                        return logic.getActor().getId();
                    }
                });
            }
        }

        // now let the body actually enter the scene
        super.bodyWillEnter(body);
    }

    @Override // from PlaceManager
    public void bodyWillLeave (BodyObject body)
    {
        super.bodyWillLeave(body);
        body.setLocal(TudeySceneLocal.class, null);
    }

    // documentation inherited from interface TudeySceneProvider
    public void enqueueInput (
        ClientObject caller, int acknowledge, int smoothedTime, InputFrame[] frames)
    {
        // forward to client liaison
        ClientLiaison client = _clients.get(caller.getOid());
        if (client != null) {
            // ping is current time minus client's smoothed time estimate
            int currentTime = _timestamp + (int)(RunAnywhere.currentTimeMillis() - _lastTick);
            client.enqueueInput(acknowledge, currentTime - smoothedTime, frames);
        } else {
            log.warning("Received input from unknown client.",
                "who", caller.who(), "where", where());
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
                "who", caller.who(), "where", where());
            return;
        }

        // make sure they're not controlling a pawn of their own
        if (_tsobj.getPawnId(cloid) > 0) {
            log.warning("User with pawn tried to set target.",
                "who", caller.who(), "pawnId", pawnId);
            return;
        }

        // retrieve the actor and ensure it's a pawn
        ActorLogic target = _actors.get(pawnId);
        if (target instanceof PawnLogic) {
            client.setTarget((PawnLogic)target);
        } else {
            log.warning("User tried to target non-pawn.", "who",
                caller.who(), "actor", target.getActor());
        }
    }

    // documentation inherited from interface TudeySceneProvider
    public void setCameraParams (
        ClientObject caller, float fovy, float aspect, float near, float far, SphereCoords coords)
    {
        // forward to client liaison
        ClientLiaison client = _clients.get(caller.getOid());
        if (client != null) {
            client.setCameraParams(fovy, aspect, near, far, coords);
        } else {
            log.warning("Received camera params from unknown client.",
                "who", caller.who(), "where", where());
        }
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (Entry entry)
    {
        addLogic(entry);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        removeLogic(oentry.getKey());
        addLogic(nentry);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
        removeLogic(oentry.getKey());
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

    // documentation inherited from interface RunQueue
    public void postRunnable (Runnable runnable)
    {
        _runnables.append(runnable);
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

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return (_tsobj = new TudeySceneObject());
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // get a reference to the scene's config manager
        TudeySceneModel sceneModel = (TudeySceneModel)_scene.getSceneModel();
        _cfgmgr = sceneModel.getConfigManager();

        // create logic objects for scene entries and listen for changes
        for (Entry entry : sceneModel.getEntries()) {
            addLogic(entry);
        }
        sceneModel.addObserver(this);

        // register and fill in our tudey scene service
        _tsobj.setTudeySceneService(_invmgr.registerDispatcher(new TudeySceneDispatcher(this)));

        // initialize the last tick timestamp
        _lastTick = RunAnywhere.currentTimeMillis();

        // start the ticker
        _ticker = new Interval(_omgr) {
            public void expired () {
                tick();
            }
        };
        _ticker.schedule(getTickInterval(), true);
    }

    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();

        // stop the ticker
        _ticker.cancel();
        _ticker = null;

        // clear out the scene service
        _invmgr.clearDispatcher(_tsobj.tudeySceneService);

        // stop listening to the scene model
        ((TudeySceneModel)_scene.getSceneModel()).removeObserver(this);
    }

    @Override // documentation inherited
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        // create and map the client liaison
        _clients.put(bodyOid, new ClientLiaison(this, (BodyObject)_omgr.getObject(bodyOid)));
    }

    @Override // documentation inherited
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // remove the client liaison
        _clients.remove(bodyOid);
    }

    /**
     * Returns the interval at which we call the {@link #tick} method.
     */
    protected int getTickInterval ()
    {
        return 50;
    }

    /**
     * Adds the logic object for the specified scene entry, if any.
     */
    protected void addLogic (Entry entry)
    {
        String cname = entry.getLogicClassName(_cfgmgr);
        if (cname == null) {
            return;
        }
        EntryLogic logic;
        try {
            logic = (EntryLogic)Class.forName(cname).newInstance();
        } catch (Exception e) {
            log.warning("Failed to instantiate entry logic.", "class", cname, e);
            return;
        }
        logic.init(this, entry);
        _entries.put(entry.getKey(), logic);
        addTagMapping(logic);
    }

    /**
     * Removes the logic object for the specified scene entry, if any.
     */
    protected void removeLogic (Object key)
    {
        EntryLogic logic = _entries.remove(key);
        if (logic != null) {
            removeTagMapping(logic);
            logic.removed();
        }
    }

    /**
     * Registers the specified logic object under its tag, if it has one.
     */
    public void addTagMapping (Logic logic)
    {
        String tag = logic.getTag();
        if (StringUtil.isBlank(tag)) {
            return;
        }
        ArrayList<Logic> list = _tagged.get(tag);
        if (list == null) {
            _tagged.put(tag, list = new ArrayList<Logic>());
        }
        list.add(logic);
    }

    /**
     * Remove the specified logic object from the tag mapping.
     */
    public void removeTagMapping (Logic logic)
    {
        String tag = logic.getTag();
        if (StringUtil.isBlank(tag)) {
            return;
        }
        ArrayList<Logic> list = _tagged.get(tag);
        if (list == null || !list.remove(logic)) {
            log.warning("Missing tag mapping for logic.", "tag", tag, "logic", logic);
            return;
        }
        if (list.isEmpty()) {
            _tagged.remove(tag);
        }
    }

    /**
     * Updates the scene.
     */
    protected void tick ()
    {
        // update the scene timestamp
        long now = RunAnywhere.currentTimeMillis();
        _timestamp += (int)(now - _lastTick);
        _lastTick = now;

        // tick the participants
        _tickOp.init(_timestamp);
        _tickParticipants.apply(_tickOp);

        // process the runnables in the queue
        Runnable runnable;
        while ((runnable = _runnables.getNonBlocking()) != null) {
            runnable.run();
        }

        // post deltas for all clients
        for (ClientLiaison client : _clients.values()) {
            client.postDelta();
        }

        // clear the effect list
        _effectsFired.clear();
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
            return participant.tick(_timestamp);
        }

        /** The timestamp of the current tick. */
        protected int _timestamp;
    }

    /** A casted reference to the Tudey scene object. */
    protected TudeySceneObject _tsobj;

    /** A reference to the scene model's configuration manager. */
    protected ConfigManager _cfgmgr;

    /** The tick interval. */
    protected Interval _ticker;

    /** The system time of the last tick. */
    protected long _lastTick;

    /** The timestamp of the current tick. */
    protected int _timestamp;

    /** The last actor id assigned. */
    protected int _lastActorId;

    /** Maps oids of entering bodies to the keys of the portals through which they're entering. */
    protected HashIntMap<Object> _entering = IntMaps.newHashIntMap();

    /** Maps body oids to client liaisons. */
    protected HashIntMap<ClientLiaison> _clients = IntMaps.newHashIntMap();

    /** The list of participants in the tick. */
    protected ObserverList<TickParticipant> _tickParticipants = ObserverList.newSafeInOrder();

    /** Scene entry logic objects mapped by key. */
    protected HashMap<Object, EntryLogic> _entries = Maps.newHashMap();

    /** Actor logic objects mapped by id. */
    protected HashIntMap<ActorLogic> _actors = IntMaps.newHashIntMap();

    /** Maps tags to lists of logic objects with that tag. */
    protected HashMap<String, ArrayList<Logic>> _tagged = Maps.newHashMap();

    /** The actor space.  Used to find the actors within a client's area of interest. */
    protected HashSpace _actorSpace = new HashSpace(64f, 6);

    /** The sensor space.  Used to detect mobile objects. */
    protected HashSpace _sensorSpace = new HashSpace(64f, 6);

    /** The logic for effects fired on the current tick. */
    protected ArrayList<EffectLogic> _effectsFired = Lists.newArrayList();

    /** Runnables enqueued for the next tick. */
    protected Queue<Runnable> _runnables = Queue.newQueue();

    /** Holds collected elements during queries. */
    protected ArrayList<SpaceElement> _elements = Lists.newArrayList();

    /** Holds collected effects during queries. */
    protected ArrayList<Effect> _effects = Lists.newArrayList();

    /** Used to tick the participants. */
    protected TickOp _tickOp = new TickOp();

    /** Stores penetration vector during queries. */
    protected Vector2f _penetration = new Vector2f();
}
