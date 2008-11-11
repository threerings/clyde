//
// $Id$

package com.threerings.tudey.client;

import java.util.ArrayList;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;

import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.dobj.SceneDeltaEvent;
import com.threerings.tudey.dobj.SceneDeltaListener;
import com.threerings.tudey.util.TudeyContext;

import static com.threerings.tudey.Log.*;

/**
 * The basic Tudey scene controller class.
 */
public class TudeySceneController extends SceneController
    implements SceneDeltaListener
{
    // documentation inherited from interface SceneDeltaListener
    public void sceneDeltaReceived (SceneDeltaEvent event)
    {
        // make sure it refers to this scene
        if (event.getSceneOid() != _tsobj.getOid()) {
            log.info("Received delta event for wrong scene.", "event", event);
            return;
        }
        // make sure it's not a out of order or a repeat
        long timestamp = event.getTimestamp();
        if (timestamp <= _lastDelta) {
            return;
        }
        _lastDelta = timestamp;

        // prune all acknowledged input frames
        long acknowledge = event.getAcknowledge();
        while (!_input.isEmpty() && _input.get(0).getTimestamp() <= acknowledge) {
            _input.remove(0);
        }
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _tsobj = (TudeySceneObject)plobj;

        // listen to the client object for delta events
        _ctx.getClient().getClientObject().addListener(this);
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        super.didLeavePlace(plobj);

        // stop listening to the client object
        _ctx.getClient().getClientObject().removeListener(this);
    }

    @Override // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_tsview = new TudeySceneView((TudeyContext)ctx));
    }

    /**
     * Sends all enqueued input to the server.
     */
    protected void transmitInput ()
    {
        _tsobj.tudeySceneService.enqueueInput(
            _ctx.getClient(), _lastDelta, _input.toArray(new InputFrame[_input.size()]));
    }

    /** A casted reference to the scene view. */
    protected TudeySceneView _tsview;

    /** A casted reference to the scene object. */
    protected TudeySceneObject _tsobj;

    /** The list of outgoing input frames. */
    protected ArrayList<InputFrame> _input = new ArrayList<InputFrame>();

    /** The timestamp of the last frame acknowledged by the client. */
    protected long _acknowledged;

    /** The timestamp of the last delta received from the client. */
    protected long _lastDelta;
}
