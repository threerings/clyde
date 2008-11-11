//
// $Id$
package com.threerings.tudey.data;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.net.Transport;
import com.threerings.tudey.client.TudeySceneService;

/**
 * Provides the implementation of the {@link TudeySceneService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class TudeySceneMarshaller extends InvocationMarshaller
    implements TudeySceneService
{
    /** The method id used to dispatch {@link #processInput} requests. */
    public static final int PROCESS_INPUT = 1;

    // from interface TudeySceneService
    public void processInput (Client arg1, long arg2, InputFrame[] arg3)
    {
        sendRequest(arg1, PROCESS_INPUT, new Object[] {
            Long.valueOf(arg2), arg3
        }, Transport.getInstance(Transport.Type.UNRELIABLE_UNORDERED, 0));
    }
}
