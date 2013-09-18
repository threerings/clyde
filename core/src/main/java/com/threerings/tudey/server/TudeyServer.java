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

import com.google.inject.Inject;
import com.threerings.config.ConfigManager;
import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.presents.server.PresentsServer;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.WhirledServer;
import com.threerings.whirled.util.SceneFactory;
import com.threerings.whirled.zone.server.ZoneRegistry;

import com.threerings.tudey.util.TudeySceneFactory;

/**
 * The base Tudey server.
 */
public abstract class TudeyServer extends WhirledServer
{
    /** Configures dependencies needed by the Tudey services. */
    public static class TudeyModule extends WhirledModule
    {
        @Override protected void configure () {
            super.configure();
            bind(PresentsServer.class).to(TudeyServer.class);
            ResourceManager rsrcmgr = new ResourceManager("rsrc/");
            rsrcmgr.activateResourceProtocol();
            bind(ResourceManager.class).toInstance(rsrcmgr);
            MessageManager msgmgr = new MessageManager("rsrc.i18n");
            bind(MessageManager.class).toInstance(msgmgr);
            ConfigManager cfgmgr = new ConfigManager(rsrcmgr, msgmgr, "config/");
            if (shouldInitConfigManager()) {
                cfgmgr.init();
            }
            bind(ConfigManager.class).toInstance(cfgmgr);
            bind(ColorPository.class).toInstance(ColorPository.loadColorPository(rsrcmgr));
            bind(SceneFactory.class).to(TudeySceneFactory.class);
            bind(SceneRegistry.class).to(TudeySceneRegistry.class);
            bind(ZoneRegistry.class).to(TudeyZoneRegistry.class);
        }

        /**
         * Checks whether we should initialize the config manager duration configuration (or
         * whether it will be initialized later, when we have some extra piece of information).
         */
        protected boolean shouldInitConfigManager ()
        {
            return true;
        }
    }

    /** The scene registry. */
    @Inject protected SceneRegistry _scenereg;
}
