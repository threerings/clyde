//
// $Id$

package com.threerings.tudey.server;

import java.util.ArrayList;

import com.samskivert.util.StringUtil;

import com.threerings.presents.net.Transport;

import com.threerings.crowd.data.BodyObject;

import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.dobj.ActorDelta;
import com.threerings.tudey.dobj.AddedActor;
import com.threerings.tudey.dobj.RemovedActor;
import com.threerings.tudey.dobj.SceneDeltaEvent;

/**
 * Handles interaction with a single client.
 */
public class ClientLiaison
{
    /**
     * Creates a new liaison for the specified client.
     */
    public ClientLiaison (TudeySceneManager scenemgr, BodyObject bodyobj)
    {
        _scenemgr = scenemgr;
        _tsobj = (TudeySceneObject)scenemgr.getPlaceObject();
        _bodyobj = bodyobj;
    }

    /**
     * Processes a request to enqueue input received from a client.
     */
    public void enqueueInput (long acknowledge, InputFrame[] frames)
    {
        // remember acknowledgement
        _acknowledged = Math.max(_acknowledged, acknowledge);

        // remove any acknowledged effects
        while (!_effects.isEmpty() && acknowledge >= _effects.get(0).getTimestamp()) {
            _effects.remove(0);
        }

        // enqueue input frames
        for (InputFrame frame : frames) {
            long timestamp = frame.getTimestamp();
            if (timestamp <= _lastInput) {
                continue; // already processed
            }
            _lastInput = timestamp;
            if (timestamp < _tsobj.timestamp) {
                continue; // out of date
            }
            _pendingInput.add(frame);
        }
    }

    /**
     * Posts the scene delta for this client, informing it any all relevant changes to the scene
     * since its last acknowledged delta.
     */
    public void postDelta ()
    {
        // remove any expired effects
        for (int ii = _effects.size() - 1; ii >= 0; ii--) {
            if (_tsobj.timestamp >= _effects.get(ii).getExpiry()) {
                _effects.remove(ii);
            }
        }

        // create and post the event
        _bodyobj.postEvent(new SceneDeltaEvent(
            _bodyobj.getOid(), _tsobj.getOid(), _lastInput, _tsobj.timestamp,
            new AddedActor[0], new ActorDelta[0], new RemovedActor[0],
            _effects.toArray(new Effect[_effects.size()]),
            Transport.UNRELIABLE_UNORDERED));
    }

    /** The scene manager that created the liaison. */
    protected TudeySceneManager _scenemgr;

    /** The scene object. */
    protected TudeySceneObject _tsobj;

    /** The client body object. */
    protected BodyObject _bodyobj;

    /** Effects fired since last acknowledged state. */
    protected ArrayList<Effect> _effects = new ArrayList<Effect>();

    /** The timestamp of the last delta acknowledged by the client. */
    protected long _acknowledged;

    /** The timestamp of the last input frame received from the client. */
    protected long _lastInput;

    /** The pending input frames. */
    protected ArrayList<InputFrame> _pendingInput = new ArrayList<InputFrame>();
}
