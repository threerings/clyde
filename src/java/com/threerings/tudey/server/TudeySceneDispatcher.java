//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
package com.threerings.tudey.server;

import com.threerings.math.SphereCoords;
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
        case TudeySceneMarshaller.ENQUEUE_INPUT:
            ((TudeySceneProvider)provider).enqueueInput(
                source, ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), (InputFrame[])args[2]
            );
            return;

        case TudeySceneMarshaller.SET_CAMERA_PARAMS:
            ((TudeySceneProvider)provider).setCameraParams(
                source, ((Float)args[0]).floatValue(), ((Float)args[1]).floatValue(), ((Float)args[2]).floatValue(), ((Float)args[3]).floatValue(), (SphereCoords)args[4]
            );
            return;

        case TudeySceneMarshaller.SET_TARGET:
            ((TudeySceneProvider)provider).setTarget(
                source, ((Integer)args[0]).intValue()
            );
            return;

        case TudeySceneMarshaller.SUBMIT_ACTOR_REQUEST:
            ((TudeySceneProvider)provider).submitActorRequest(
                source, ((Integer)args[0]).intValue(), (String)args[1]
            );
            return;

        case TudeySceneMarshaller.SUBMIT_ENTRY_REQUEST:
            ((TudeySceneProvider)provider).submitEntryRequest(
                source, args[0], (String)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
