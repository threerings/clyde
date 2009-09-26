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
package com.threerings.config.dist.data;

import com.threerings.config.dist.client.DConfigService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link DConfigService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class DConfigMarshaller extends InvocationMarshaller
    implements DConfigService
{
    /** The method id used to dispatch {@link #addConfig} requests. */
    public static final int ADD_CONFIG = 1;

    // from interface DConfigService
    public void addConfig (Client arg1, ConfigEntry arg2)
    {
        sendRequest(arg1, ADD_CONFIG, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #removeConfig} requests. */
    public static final int REMOVE_CONFIG = 2;

    // from interface DConfigService
    public void removeConfig (Client arg1, ConfigKey arg2)
    {
        sendRequest(arg1, REMOVE_CONFIG, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #updateConfig} requests. */
    public static final int UPDATE_CONFIG = 3;

    // from interface DConfigService
    public void updateConfig (Client arg1, ConfigEntry arg2)
    {
        sendRequest(arg1, UPDATE_CONFIG, new Object[] {
            arg2
        });
    }
}
