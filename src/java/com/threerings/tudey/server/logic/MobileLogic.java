//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.MobileAdvancer;

/**
 * Controls the state of a mobile actor.
 */
public class MobileLogic extends ActorLogic
    implements TudeySceneManager.TickParticipant, ActorAdvancer.Environment
{
    // documentation inherited from interface TudeySceneManager.TickParticipant
    public boolean tick (int timestamp)
    {
        // advance to the current timestamp
        _advancer.advance(timestamp);

        // note and clear penetration
        if (_penetrationCount > 0) {
            penetrated(_penetrationSum.multLocal(1f / _penetrationCount));
            _penetrationCount = 0;
            _penetrationSum.set(Vector2f.ZERO);
        }

        // update the actor's shape, notify any sensors
        updateShape();
        _scenemgr.triggerIntersectionSensors(timestamp, this);

        return true;
    }

    // documentation inherited from ActorAdvancer.Environment
    public boolean getPenetration (Actor actor, Shape shape, Vector2f result)
    {
        if (!_scenemgr.getPenetration(actor, shape, result)) {
            return false;
        }
        // record penetration info
        _penetrationCount++;
        _penetrationSum.addLocal(result);
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
        return new MobileAdvancer(this, (Mobile)_actor, _actor.getCreated());
    }

    /**
     * Notes that the actor collided with one or more things during its advancement.
     *
     * @param penetration the sum of the penetration vectors.
     */
    protected void penetrated (Vector2f penetration)
    {
        // nothing by default
    }

    /** Used to advance the state of the actor. */
    protected ActorAdvancer _advancer;

    /** The number of penetrations. */
    protected int _penetrationCount;

    /** The penetration vector sum. */
    protected Vector2f _penetrationSum = new Vector2f();
}
