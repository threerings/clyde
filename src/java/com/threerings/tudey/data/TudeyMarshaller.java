//
// $Id$
package com.threerings.tudey.data;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;
import com.threerings.tudey.client.TudeyService;
import com.threerings.tudey.data.TudeySceneUpdate;

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
    /** The method id used to dispatch {@link #updateScene} requests. */
    public static final int UPDATE_SCENE = 1;

    // from interface TudeyService
    public void updateScene (Client arg1, TudeySceneUpdate arg2)
    {
        sendRequest(arg1, UPDATE_SCENE, new Object[] {
            arg2
        });
    }
}
