//
// $Id$

package com.threerings.tudey.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.tudey.data.Tile;

/**
 * Handles requests related to the Tudey scene.
 */
public interface TudeyService extends InvocationService
{
    /**
     * Handles a request to place a tile in the scene.
     */
    public void placeTile (Client client, Tile tile);
}
