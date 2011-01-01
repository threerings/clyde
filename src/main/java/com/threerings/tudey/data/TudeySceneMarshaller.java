//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
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

package com.threerings.tudey.data;

import javax.annotation.Generated;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.net.Transport;
import com.threerings.tudey.client.TudeySceneService;
import com.threerings.tudey.config.CameraConfig;

/**
 * Provides the implementation of the {@link TudeySceneService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from TudeySceneService.java.")
public class TudeySceneMarshaller extends InvocationMarshaller
    implements TudeySceneService
{
    /** The method id used to dispatch {@link #enqueueInputReliable} requests. */
    public static final int ENQUEUE_INPUT_RELIABLE = 1;

    // from interface TudeySceneService
    public void enqueueInputReliable (Client arg1, int arg2, int arg3, InputFrame[] arg4)
    {
        sendRequest(arg1, ENQUEUE_INPUT_RELIABLE, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), arg4
        });
    }

    /** The method id used to dispatch {@link #enqueueInputUnreliable} requests. */
    public static final int ENQUEUE_INPUT_UNRELIABLE = 2;

    // from interface TudeySceneService
    public void enqueueInputUnreliable (Client arg1, int arg2, int arg3, InputFrame[] arg4)
    {
        sendRequest(arg1, ENQUEUE_INPUT_UNRELIABLE, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), arg4
        }, Transport.getInstance(Transport.Type.UNRELIABLE_UNORDERED, 0));
    }

    /** The method id used to dispatch {@link #enteredPlace} requests. */
    public static final int ENTERED_PLACE = 3;

    // from interface TudeySceneService
    public void enteredPlace (Client arg1)
    {
        sendRequest(arg1, ENTERED_PLACE, new Object[] {});
    }

    /** The method id used to dispatch {@link #setCameraParams} requests. */
    public static final int SET_CAMERA_PARAMS = 4;

    // from interface TudeySceneService
    public void setCameraParams (Client arg1, CameraConfig arg2, float arg3)
    {
        sendRequest(arg1, SET_CAMERA_PARAMS, new Object[] {
            arg2, Float.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #setTarget} requests. */
    public static final int SET_TARGET = 5;

    // from interface TudeySceneService
    public void setTarget (Client arg1, int arg2)
    {
        sendRequest(arg1, SET_TARGET, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #submitActorRequest} requests. */
    public static final int SUBMIT_ACTOR_REQUEST = 6;

    // from interface TudeySceneService
    public void submitActorRequest (Client arg1, int arg2, String arg3)
    {
        sendRequest(arg1, SUBMIT_ACTOR_REQUEST, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #submitEntryRequest} requests. */
    public static final int SUBMIT_ENTRY_REQUEST = 7;

    // from interface TudeySceneService
    public void submitEntryRequest (Client arg1, Object arg2, String arg3)
    {
        sendRequest(arg1, SUBMIT_ENTRY_REQUEST, new Object[] {
            arg2, arg3
        });
    }
}
