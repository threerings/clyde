//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Pawn;
import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.util.PawnAdvancer;

/**
 * Handles the state of a player-controlled actor.
 */
public class PawnLogic extends MobileLogic
{
    /**
     * Enqueues a single frame of input for processing.
     */
    public void enqueueInput (InputFrame frame)
    {
        _input.add(frame);
    }

    @Override // documentation inherited
    public boolean tick (int timestamp)
    {
        // process the enqueued input
        while (!_input.isEmpty() && _input.get(0).getTimestamp() <= timestamp) {
            _advancer.advance(_input.remove(0));
        }

        // advance to the current timestamp
        _advancer.advance(timestamp);

        // update the pawn's shape, notify any sensors
        updateShape();
        _scenemgr.triggerSensors(_shape.getWorldShape(), this);

        return true;
    }

    @Override // documentation inherited
    protected Actor createActor (
        ConfigReference<ActorConfig> ref, int id, int timestamp,
        Vector2f translation, float rotation)
    {
        return new Pawn(ref, id, timestamp, translation, rotation);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();
        _advancer = new PawnAdvancer(_scenemgr, (Pawn)_actor, _actor.getCreated());
    }

    /** Used to advance the state of the pawn. */
    protected PawnAdvancer _advancer;

    /** The list of pending input frames for the pawn. */
    protected ArrayList<InputFrame> _input = new ArrayList<InputFrame>();
}
