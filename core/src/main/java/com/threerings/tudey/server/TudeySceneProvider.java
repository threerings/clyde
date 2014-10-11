//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
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

package com.threerings.tudey.server;

import javax.annotation.Generated;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.Transport;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.tudey.client.TudeySceneService;
import com.threerings.tudey.config.CameraConfig;
import com.threerings.tudey.data.InputFrame;

/**
 * Defines the server-side of the {@link TudeySceneService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from TudeySceneService.java.")
public interface TudeySceneProvider extends InvocationProvider
{
    /**
     * Handles a {@link TudeySceneService#enqueueInputReliable} request.
     */
    void enqueueInputReliable (ClientObject caller, int arg1, int arg2, InputFrame[] arg3);

    /**
     * Handles a {@link TudeySceneService#enqueueInputUnreliable} request.
     */
    void enqueueInputUnreliable (ClientObject caller, int arg1, int arg2, InputFrame[] arg3);

    /**
     * Handles a {@link TudeySceneService#enteredPlace} request.
     */
    void enteredPlace (ClientObject caller);

    /**
     * Handles a {@link TudeySceneService#setCameraParams} request.
     */
    void setCameraParams (ClientObject caller, CameraConfig arg1, float arg2);

    /**
     * Handles a {@link TudeySceneService#setTarget} request.
     */
    void setTarget (ClientObject caller, int arg1);

    /**
     * Handles a {@link TudeySceneService#submitActorRequest} request.
     */
    void submitActorRequest (ClientObject caller, int arg1, String arg2);

    /**
     * Handles a {@link TudeySceneService#submitEntryRequest} request.
     */
    void submitEntryRequest (ClientObject caller, Object arg1, String arg2);
}
