//
// $Id$

package com.threerings.tudey.server;

import java.util.ArrayList;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.RunAnywhere;

import com.threerings.presents.data.ClientObject;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.server.SceneManager;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.math.Rect;

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
import com.threerings.tudey.space.HashSpace;
import com.threerings.tudey.space.SpaceElement;

import static com.threerings.tudey.Log.*;

/**
 * Manager for Tudey scenes.
 */
public class TudeySceneManager extends SceneManager
    implements TudeySceneProvider
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
     * Returns a reference to the actor influence space.
     */
    public HashSpace getInfluenceSpace ()
    {
        return _influenceSpace;
    }

    /**
     * Spawns an actor with the named configuration.
     */
    public ActorLogic spawnActor (int timestamp, String name)
    {
        return spawnActor(timestamp, new ConfigReference<ActorConfig>(name));
    }

    /**
     * Spawns an actor with the supplied name and arguments.
     */
    public ActorLogic spawnActor (
        int timestamp, String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        return spawnActor(timestamp, new ConfigReference<ActorConfig>(
            name, firstKey, firstValue, otherArgs));
    }

    /**
     * Spawns an actor with the referenced configuration.
     */
    public ActorLogic spawnActor (int timestamp, ConfigReference<ActorConfig> ref)
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
        logic.init(this, ref, original, ++_lastActorId, timestamp);
        _actors.put(_lastActorId, logic);

        return logic;
    }

    /**
     * Fires off an effect at the with the named configuration and the supplied timestamp.
     */
    public EffectLogic fireEffect (int timestamp, String name)
    {
        return fireEffect(timestamp, new ConfigReference<EffectConfig>(name));
    }

    /**
     * Fires off an effect with the supplied name, arguments, and timestamp.
     */
    public EffectLogic fireEffect (
        int timestamp, String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        return fireEffect(timestamp, new ConfigReference<EffectConfig>(
            name, firstKey, firstValue, otherArgs));
    }

    /**
     * Fires off an effect with the referenced configuration and the given timestamp.
     */
    public EffectLogic fireEffect (int timestamp, ConfigReference<EffectConfig> ref)
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
        logic.init(this, ref, original, timestamp);
        _effectsFired.add(logic);

        return logic;
    }

    /**
     * Returns a map containing all actors whose influence regions intersect the provided bounds.
     */
    public HashIntMap<Actor> getActors (Rect bounds)
    {
        _influenceSpace.getElements(bounds, _elements);
        HashIntMap<Actor> map = new HashIntMap<Actor>();
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            Actor actor = ((ActorLogic)_elements.get(ii).getUserObject()).getActor();
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
            if (logic.getInfluence().intersects(bounds)) {
                _effects.add(logic.getEffect());
            }
        }
        Effect[] array = _effects.toArray(new Effect[_effects.size()]);
        _effects.clear();
        return array;
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
        ((TudeyOccupantInfo)info).pawnId = 0;

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

        // tick the participants in reverse order, to allow removal
        for (int ii = _tickParticipants.size() - 1; ii >= 0; ii--) {
            if (!_tickParticipants.get(ii).tick(_timestamp)) {
                _tickParticipants.remove(ii);
            }
        }

        // post deltas for all clients
        for (ClientLiaison client : _clients.values()) {
            client.postDelta();
        }

        // clear the effect list
        _effectsFired.clear();
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
    protected ArrayList<TickParticipant> _tickParticipants = new ArrayList<TickParticipant>();

    /** Actor logic objects mapped by id. */
    protected HashIntMap<ActorLogic> _actors = new HashIntMap<ActorLogic>();

    /** The actor influence space.  Used to find the actors within a client's area of interest. */
    protected HashSpace _influenceSpace = new HashSpace(64f, 6);

    /** The logic for effects fired on the current tick. */
    protected ArrayList<EffectLogic> _effectsFired = new ArrayList<EffectLogic>();

    /** Holds collected elements during queries. */
    protected ArrayList<SpaceElement> _elements = new ArrayList<SpaceElement>();

    /** Holds collected effects during queries. */
    protected ArrayList<Effect> _effects = new ArrayList<Effect>();
}
