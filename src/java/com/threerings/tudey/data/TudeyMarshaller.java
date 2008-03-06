package com.threerings.tudey.data;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;
import com.threerings.tudey.client.TudeyService;
import com.threerings.tudey.data.Tile;

/**
 * Provides the implementation of the {@link TudeyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class TudeyMarshaller extends InvocationMarshaller
    implements TudeyService
{
    /** The method id used to dispatch {@link #placeTile} requests. */
    public static final int PLACE_TILE = 1;

    // from interface TudeyService
    public void placeTile (Client arg1, Tile arg2)
    {
        sendRequest(arg1, PLACE_TILE, new Object[] {
            arg2
        });
    }
}
