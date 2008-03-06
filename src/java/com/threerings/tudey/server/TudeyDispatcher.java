package com.threerings.tudey.server;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.tudey.client.TudeyService;
import com.threerings.tudey.data.Tile;
import com.threerings.tudey.data.TudeyMarshaller;

/**
 * Dispatches requests to the {@link TudeyProvider}.
 */
public class TudeyDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public TudeyDispatcher (TudeyProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public InvocationMarshaller createMarshaller ()
    {
        return new TudeyMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case TudeyMarshaller.PLACE_TILE:
            ((TudeyProvider)provider).placeTile(
                source,
                (Tile)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
