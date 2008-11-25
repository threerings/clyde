//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.threerings.tudey.data.actor.Mobile;
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
            InputFrame frame = _input.remove(0);
            _advancer.advance(frame.getTimestamp());
            _advancer.setInput(frame);
        }

        // advance to the current timestamp
        _advancer.advance(timestamp);

        updateShape();

        return true;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();
        _advancer = new PawnAdvancer(_mobile, _mobile.getCreated());
    }

    /** Used to advance the state of the pawn. */
    protected PawnAdvancer _advancer;

    /** The list of pending input frames for the pawn. */
    protected ArrayList<InputFrame> _input = new ArrayList<InputFrame>();
}
