package com.threerings.tudey.server;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.tudey.client.TudeyService;
import com.threerings.tudey.data.Tile;

/**
 * Defines the server-side of the {@link TudeyService}.
 */
public interface TudeyProvider extends InvocationProvider
{
    /**
     * Handles a {@link TudeyService#placeTile} request.
     */
    public void placeTile (ClientObject caller, Tile arg1);
}
