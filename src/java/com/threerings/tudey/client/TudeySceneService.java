//
// $Id$

package com.threerings.tudey.client;

import com.threerings.presents.annotation.TransportHint;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.net.Transport;

import com.threerings.tudey.data.InputFrame;

/**
 * Provides services relating to Tudey scenes.
 */
public interface TudeySceneService extends InvocationService
{
    /**
     * Requests to enqueue a batch of input frames recorded on the client.
     *
     * @param acknowledge the timestamp of the last delta received by the client.
     */
    @TransportHint(type=Transport.Type.UNRELIABLE_UNORDERED)
    public void enqueueInput (Client client, long acknowledge, InputFrame[] frames);
}
