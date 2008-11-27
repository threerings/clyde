//
// $Id$

package com.threerings.tudey.server;

import java.util.ArrayList;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.ObserverList;
import com.samskivert.util.RunAnywhere;

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
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.server.logic.ActorLogic;
import com.threerings.tudey.server.logic.EffectLogic;
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
    implements TudeySceneProvider, ActorAdvancer.Environment
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
     * Returns a reference to the actor space.
     */
    public HashSpace getActorSpace ()
    {
        return _actorSpace;
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
        if (_actors.remove(id) == null) {
            log.warning("Missing actor to remove.", "where", where(), "id", id);
        }
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

    // documentation inherited from interface ActorAdvancer.Environment
    public boolean getPenetration (Actor actor, Shape shape, Vector2f result)
    {
        // start with zero penetration
        result.set(Vector2f.ZERO);

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
        return result.lengthSquared() > 0f;
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
        _cfgmgr = ((TudeySceneModel)_scene.getSceneModel()).getConfigManager();

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
    }

    @Override // documentation inherited
    protected void insertOccupantInfo (OccupantInfo info, BodyObject body)
    {
        // add the pawn and fill in its id
        ConfigReference<ActorConfig> ref = getPawnConfig(body);
        if (ref != null) {
            spawnActor(_timestamp + getTickInterval(), new Vector2f(1f, 1f), 0f, ref);
            ActorLogic logic = spawnActor(_timestamp + getTickInterval(), Vector2f.ZERO, 0f, ref);
            if (logic != null) {
                ((TudeyOccupantInfo)info).pawnId = logic.getActor().getId();
            }
        }
        super.insertOccupantInfo(info, body);
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

    /** Maps body oids to client liaisons. */
    protected HashIntMap<ClientLiaison> _clients = new HashIntMap<ClientLiaison>();

    /** The list of participants in the tick. */
    protected ObserverList<TickParticipant> _tickParticipants = ObserverList.newSafeInOrder();

    /** Actor logic objects mapped by id. */
    protected HashIntMap<ActorLogic> _actors = new HashIntMap<ActorLogic>();

    /** The actor space.  Used to find the actors within a client's area of interest. */
    protected HashSpace _actorSpace = new HashSpace(64f, 6);

    /** The logic for effects fired on the current tick. */
    protected ArrayList<EffectLogic> _effectsFired = new ArrayList<EffectLogic>();

    /** Holds collected elements during queries. */
    protected ArrayList<SpaceElement> _elements = new ArrayList<SpaceElement>();

    /** Holds collected effects during queries. */
    protected ArrayList<Effect> _effects = new ArrayList<Effect>();

    /** Used to tick the participants. */
    protected TickOp _tickOp = new TickOp();

    /** Stores penetration vector during queries. */
    protected Vector2f _penetration = new Vector2f();
}
