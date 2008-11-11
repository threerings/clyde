//
// $Id$

package com.threerings.tudey.server;

import java.util.ArrayList;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.RunAnywhere;

import com.threerings.presents.data.ClientObject;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.server.SceneManager;

import com.threerings.tudey.data.Actor;
import com.threerings.tudey.data.Effect;
import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.data.TudeySceneObject;

/**
 * Manager for Tudey scenes.
 */
public class TudeySceneManager extends SceneManager
    implements TudeySceneProvider
{
    /**
     * Adds an actor to the scene.
     */
    public void addActor (Actor actor)
    {
    }

    /**
     * Updates an actor within the scene.
     */
    public void updateActor (Actor actor)
    {
    }

    /**
     * Removes the actor with the specified identifier from the scene.
     */
    public void removeActor (int id)
    {
    }

    /**
     * Fires an effect in the current tick.
     */
    public void fireEffect (Effect effect)
    {

    }

    // documentation inherited from interface TudeySceneProvider
    public void processInput (ClientObject caller, long acknowledge, InputFrame[] frames)
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
    protected long getTickInterval ()
    {
        return 50L;
    }

    /**
     * Updates the scene.
     */
    protected void tick ()
    {
        // update the scene timestamp
        long now = RunAnywhere.currentTimeMillis();
        _tsobj.timestamp += (now - _lastTick);
        _lastTick = now;

        // post deltas for all clients
        for (ClientLiaison client : _clients.values()) {
            client.postDelta();
        }
    }

    /** A casted reference to the Tudey scene object. */
    protected TudeySceneObject _tsobj;

    /** The tick interval. */
    protected Interval _ticker;

    /** The system time of the last tick. */
    protected long _lastTick;

    /** Maps body oids to client liaisons. */
    protected HashIntMap<ClientLiaison> _clients = new HashIntMap<ClientLiaison>();
}
