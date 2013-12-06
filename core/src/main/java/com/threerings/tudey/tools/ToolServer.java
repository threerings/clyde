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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.BootstrapData;
import com.threerings.presents.server.ClientResolutionListener;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.LocalDObjectMgr;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.SessionFactory;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.WhirledSession;
import com.threerings.whirled.server.persist.SceneRepository;

import com.threerings.config.ConfigManager;
import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.Name;

import com.threerings.tudey.data.TudeyBodyObject;
import com.threerings.tudey.server.TudeyServer;

import static com.threerings.tudey.Log.log;

/**
 * A local server for use with the tools.
 */
@Singleton
public class ToolServer extends TudeyServer
{
    /** Configures dependencies needed by the local server. */
    public static class ToolModule extends TudeyModule
    {
        public ToolModule (Client client)
        {
            _client = client;
        }

        @Override protected void configure ()
        {
            super.configure();
            bind(TudeyServer.class).to(ToolServer.class);
            bind(PresentsDObjectMgr.class).to(LocalDObjectMgr.class);
            bind(Client.class).toInstance(_client);
            bind(SceneRepository.class).to(ToolSceneRepository.class);
            bind(SceneRegistry.ConfigFactory.class).toInstance(new SceneRegistry.ConfigFactory() {
                public PlaceConfig createPlaceConfig (SceneModel model) {
                    return new ToolSceneConfig();
                }
            });
        }

        @Override
        protected boolean shouldInitConfigManager ()
        {
            return false; // will be configured on application init
        }

        protected Client _client;
    }

    /**
     * Returns a reference to the server resource manager.
     */
    public ResourceManager getResourceManager ()
    {
        return _rsrcmgr;
    }

    /**
     * Returns a reference to the server config manager.
     */
    public ConfigManager getConfigManager ()
    {
        return _cfgmgr;
    }

    /**
     * Returns a reference to the server color pository.
     */
    public ColorPository getColorPository ()
    {
        return _colorpos;
    }

    /**
     * Returns a reference to the scene repository.
     */
    public ToolSceneRepository getSceneRepository ()
    {
        return _scenerepo;
    }

    @Override
    public void init (Injector injector)
        throws Exception
    {
        super.init(injector);

        // configure the client manager to use the appropriate client object class
        _clmgr.setDefaultSessionFactory(new SessionFactory() {
            public Class<? extends PresentsSession> getSessionClass (AuthRequest areq) {
                return WhirledSession.class;
            }
            public Class<? extends ClientResolver> getClientResolverClass (Name username) {
                return ToolClientResolver.class;
            }
        });
    }

    /**
     * Called to cause the standalone client to "logon."
     */
    public void startStandaloneClient (Name username)
    {
        // create our client object
        ClientResolutionListener clr = new ClientResolutionListener() {
            public void clientResolved (Name username, ClientObject clobj) {
                // flag the client as standalone
                String[] groups = _client.prepareStandaloneLogon();

                // fake up a bootstrap; I need to expose the mechanisms in Presents that create it
                // in a network environment
                BootstrapData data = new BootstrapData();
                data.clientOid = clobj.getOid();
                data.services = _invmgr.getBootstrapServices(groups);

                // and configure the client to use the server's distributed object manager
                _client.standaloneLogon(
                    data, ((LocalDObjectMgr)_omgr).getClientDObjectMgr(clobj.getOid()));
            }
            public void resolutionFailed (Name username, Exception cause) {
                log.warning("Failed to resolve client.", "who", username, cause);
            }
        };
        _clmgr.resolveClientObject(username, clr);
    }

    /**
     * Called to cause the standalone client to "logoff."
     */
    public void stopStandaloneClient ()
    {
        _client.standaloneLogoff();
    }

    /**
     * Client resolver class.
     */
    protected static class ToolClientResolver extends CrowdClientResolver
    {
        @Override
        public ClientObject createClientObject ()
        {
            return new TudeyBodyObject();
        }
    }

    /** The standalone client. */
    @Inject protected Client _client;

    /** The server's resource manager. */
    @Inject protected ResourceManager _rsrcmgr;

    /** The server's config manager. */
    @Inject protected ConfigManager _cfgmgr;

    /** The server's color pository. */
    @Inject protected ColorPository _colorpos;

    /** The scene repository. */
    @Inject protected ToolSceneRepository _scenerepo;
}
