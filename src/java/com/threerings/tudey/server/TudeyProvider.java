//
// $Id$
package com.threerings.tudey.server;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.tudey.client.TudeyService;
import com.threerings.tudey.data.TudeySceneUpdate;

/**
 * Defines the server-side of the {@link TudeyService}.
 */
public interface TudeyProvider extends InvocationProvider
{
    /**
     * Handles a {@link TudeyService#updateScene} request.
     */
    public void updateScene (ClientObject caller, TudeySceneUpdate arg1);
}
