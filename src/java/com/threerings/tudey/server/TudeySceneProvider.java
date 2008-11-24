//
// $Id$
package com.threerings.tudey.server;

import com.threerings.math.SphereCoords;
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
    void enqueueInput (ClientObject caller, int arg1, int arg2, InputFrame[] arg3);

    /**
     * Handles a {@link TudeySceneService#setCameraParams} request.
     */
    void setCameraParams (ClientObject caller, float arg1, float arg2, float arg3, float arg4, SphereCoords arg5);

    /**
     * Handles a {@link TudeySceneService#setTarget} request.
     */
    void setTarget (ClientObject caller, int arg1);
}
