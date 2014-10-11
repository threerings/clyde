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

package com.threerings.tudey.client;

import com.threerings.presents.annotation.TransportHint;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.Transport;

import com.threerings.tudey.config.CameraConfig;
import com.threerings.tudey.data.InputFrame;

/**
 * Provides services relating to Tudey scenes.
 */
public interface TudeySceneService extends InvocationService<ClientObject>
{
    /**
     * Notifies the server that the client has successfully entered the place.
     */
    public void enteredPlace ();

    /**
     * Requests to enqueue a batch of input frames recorded on the client (reliable version).
     *
     * @param acknowledge the timestamp of the last delta received by the client.
     * @param smoothedTime the client's smoothed server time estimate.
     */
    public void enqueueInputReliable (int acknowledge, int smoothedTime, InputFrame[] frames);

    /**
     * Requests to enqueue a batch of input frames recorded on the client (unreliable version).
     *
     * @param acknowledge the timestamp of the last delta received by the client.
     * @param smoothedTime the client's smoothed server time estimate.
     */
    @TransportHint(type=Transport.Type.UNRELIABLE_UNORDERED)
    public void enqueueInputUnreliable (int acknowledge, int smoothedTime, InputFrame[] frames);

    /**
     * Requests to track the specified pawn.  This is only valid for clients that do not control
     * a pawn of their own.
     */
    public void setTarget (int pawnId);

    /**
     * Requests to change the client's camera parameters (which affect its area of interest).
     */
    public void setCameraParams (CameraConfig config, float aspect);

    /**
     * Submits a request related to the identified scene entry.
     */
    public void submitEntryRequest (Object key, String request);

    /**
     * Submits a request related to the identified actor.
     */
    public void submitActorRequest (int actorId, String request);
}
