//
// $Id$

package com.threerings.tudey.client;

import java.util.ArrayList;

import com.samskivert.util.RunAnywhere;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;

import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.KeyListener;
import com.threerings.opengl.util.Tickable;

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
    implements SceneDeltaListener, KeyListener, Tickable
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

        // pass it on to the view for visualization
        _tsview.processSceneDelta(event);
    }

    // documentation inherited from interface KeyListener
    public void keyPressed (KeyEvent event)
    {
    }

    // documentation inherited from interface KeyListener
    public void keyReleased (KeyEvent event)
    {
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // perhaps transmit our acknowledgement and input frames
        long now = RunAnywhere.currentTimeMillis();
        if (now - _lastTransmit >= getTransmitInterval()) {
            transmitInput();
            _lastTransmit = now;
        }
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _tsobj = (TudeySceneObject)plobj;

        // listen to the client object for delta events
        _ctx.getClient().getClientObject().addListener(this);

        // listen for input events
        _tsview.getInputWindow().addListener(this);

        // register with the root as a tick participant
        _tctx.getRoot().addTickParticipant(this);
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        super.didLeavePlace(plobj);

        // stop listening to the client object, input events, ticks
        _ctx.getClient().getClientObject().removeListener(this);
        _tsview.getInputWindow().removeListener(this);
        _tctx.getRoot().removeTickParticipant(this);
    }

    @Override // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_tsview = new TudeySceneView((TudeyContext)ctx));
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();
        _tctx = (TudeyContext)_ctx;
    }

    /**
     * Returns the interval at which we transmit our input frames.
     */
    protected long getTransmitInterval ()
    {
        return 110L;
    }

    /**
     * Sends all enqueued input to the server.
     */
    protected void transmitInput ()
    {
        _tsobj.tudeySceneService.enqueueInput(
            _ctx.getClient(), _lastDelta, _input.toArray(new InputFrame[_input.size()]));
    }

    /** A casted reference to the context. */
    protected TudeyContext _tctx;

    /** A casted reference to the scene view. */
    protected TudeySceneView _tsview;

    /** A casted reference to the scene object. */
    protected TudeySceneObject _tsobj;

    /** The list of outgoing input frames. */
    protected ArrayList<InputFrame> _input = new ArrayList<InputFrame>();

    /** The time at which we last transmitted our input.  */
    protected long _lastTransmit;

    /** The timestamp of the last delta received from the client. */
    protected long _lastDelta;
}
