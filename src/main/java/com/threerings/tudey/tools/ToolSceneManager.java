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

package com.threerings.tudey.tools;

import java.util.prefs.Preferences;

import com.threerings.crowd.data.BodyObject;

import com.threerings.config.ConfigReference;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Scene manager for tools.
 */
public class ToolSceneManager extends TudeySceneManager
{
    @Override
    public boolean getDebugRegions ()
    {
        return _prefs.getBoolean("debug_regions", false);
    }

    @Override
    protected ConfigReference<ActorConfig> getPawnConfig (BodyObject body)
    {
        return new ConfigReference<ActorConfig>("Character/PC/Editor");
    }

    @Override
    protected void placeBecameEmpty ()
    {
        shutdown();
    }

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ToolSceneManager.class);
}
