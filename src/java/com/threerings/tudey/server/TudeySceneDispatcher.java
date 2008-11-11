//
// $Id$
package com.threerings.tudey.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.Transport;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.data.TudeySceneMarshaller;

/**
 * Dispatches requests to the {@link TudeySceneProvider}.
 */
public class TudeySceneDispatcher extends InvocationDispatcher<TudeySceneMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public TudeySceneDispatcher (TudeySceneProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public TudeySceneMarshaller createMarshaller ()
    {
        return new TudeySceneMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case TudeySceneMarshaller.PROCESS_INPUT:
            ((TudeySceneProvider)provider).processInput(
                source, ((Long)args[0]).longValue(), (InputFrame[])args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
