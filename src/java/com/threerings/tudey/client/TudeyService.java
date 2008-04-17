//
// $Id$

package com.threerings.tudey.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.tudey.data.TudeySceneUpdate;

/**
 * Handles requests related to the Tudey scene.
 */
public interface TudeyService extends InvocationService
{
    /**
     * Handles a request to update the scene.
     */
    public void updateScene (Client client, TudeySceneUpdate update);
}
