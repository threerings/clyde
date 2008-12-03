//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.MobileAdvancer;

/**
 * Controls the state of a mobile actor.
 */
public class MobileLogic extends ActorLogic
    implements TudeySceneManager.TickParticipant
{
    // documentation inherited from interface TudeySceneManager.TickParticipant
    public boolean tick (int timestamp)
    {
        // advance to the current timestamp
        _advancer.advance(timestamp);

        // update the actor's shape, notify any sensors
        updateShape();
        _scenemgr.triggerSensors(_shape.getWorldShape(), this);

        return true;
    }

    @Override // documentation inherited
    public void destroy (int timestamp)
    {
        super.destroy(timestamp);

        // deregister as tick participant
        _scenemgr.removeTickParticipant(this);
    }

    @Override // documentation inherited
    protected Actor createActor (
        ConfigReference<ActorConfig> ref, int id, int timestamp,
        Vector2f translation, float rotation)
    {
        return new Mobile(ref, id, timestamp, translation, rotation);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        // register as tick participant
        _scenemgr.addTickParticipant(this);

        // create advancer
        _advancer = createAdvancer();
    }

    /**
     * Creates the advancer to use to update the actor.
     */
    protected ActorAdvancer createAdvancer ()
    {
        return new MobileAdvancer(_scenemgr, (Mobile)_actor, _actor.getCreated());
    }

    /** Used to advance the state of the actor. */
    protected ActorAdvancer _advancer;
}
