//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Controls the state of a mobile actor.
 */
public class MobileLogic extends ActorLogic
    implements TudeySceneManager.TickParticipant
{
    // documentation inherited from interface TudeySceneManager.TickParticipant
    public boolean tick (int timestamp)
    {
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
    }
}
