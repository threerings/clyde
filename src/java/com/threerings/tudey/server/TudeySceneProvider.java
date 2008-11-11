//
// $Id$
package com.threerings.tudey.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.Transport;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.tudey.client.TudeySceneService;
import com.threerings.tudey.data.InputFrame;

/**
 * Defines the server-side of the {@link TudeySceneService}.
 */
public interface TudeySceneProvider extends InvocationProvider
{
    /**
     * Handles a {@link TudeySceneService#enqueueInput} request.
     */
    void enqueueInput (ClientObject caller, long arg1, InputFrame[] arg2);
}
