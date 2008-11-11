//
// $Id$

package com.threerings.tudey.server;

import com.threerings.presents.net.Transport;

import com.threerings.crowd.data.BodyObject;

import com.threerings.tudey.data.Effect;
import com.threerings.tudey.data.TudeySceneObject;
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
     * Posts the scene delta for this client, informing it any all relevant changes to the scene
     * since its last acknowledged delta.
     */
    public void postDelta ()
    {
        // create and post the event
        _bodyobj.postEvent(new SceneDeltaEvent(
            _bodyobj.getOid(), _tsobj.getOid(), _tsobj.timestamp,
            new AddedActor[0], new ActorDelta[0], new RemovedActor[0],
            new Effect[0], Transport.UNRELIABLE_UNORDERED));
    }

    /** The scene manager that created the liaison. */
    protected TudeySceneManager _scenemgr;

    /** The scene object. */
    protected TudeySceneObject _tsobj;

    /** The client body object. */
    protected BodyObject _bodyobj;
}
