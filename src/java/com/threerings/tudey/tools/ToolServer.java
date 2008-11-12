//
// $Id$

package com.threerings.tudey.tools;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.threerings.util.Name;

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

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.whirled.server.WhirledSession;
import com.threerings.whirled.server.persist.DummySceneRepository;
import com.threerings.whirled.server.persist.SceneRepository;

import com.threerings.tudey.server.TudeyServer;

import static com.threerings.tudey.Log.*;

/**
 * A local server for use with the tools.
 */
@Singleton
public class ToolServer extends TudeyServer
{
    /** Configures dependencies needed by the local server. */
    public static class Module extends TudeyServer.Module
    {
        @Override protected void configure () {
            super.configure();
            bind(PresentsDObjectMgr.class).to(LocalDObjectMgr.class);
            bind(SceneRepository.class).to(DummySceneRepository.class);
        }
    }

    @Override // documentation inherited
    public void init (Injector injector)
        throws Exception
    {
        super.init(injector);

        // configure the client manager to use the appropriate client object class
        _clmgr.setSessionFactory(new SessionFactory() {
            public Class<? extends PresentsSession> getSessionClass (AuthRequest areq) {
                return WhirledSession.class;
            }
            public Class<? extends ClientResolver> getClientResolverClass (Name username) {
                return CrowdClientResolver.class;
            }
        });
    }

    /**
     * Called to cause the standalone client to "logon."
     */
    public void startStandaloneClient (final Client client, Name username)
    {
        // create our client object
        ClientResolutionListener clr = new ClientResolutionListener() {
            public void clientResolved (Name username, ClientObject clobj) {
                // flag the client as standalone
                String[] groups = client.prepareStandaloneLogon();

                // fake up a bootstrap; I need to expose the mechanisms in Presents that create it
                // in a network environment
                BootstrapData data = new BootstrapData();
                data.clientOid = clobj.getOid();
                data.services = _invmgr.getBootstrapServices(groups);

                // and configure the client to use the server's distributed object manager
                client.standaloneLogon(
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
    public void stopStandaloneClient (Client client)
    {
        client.standaloneLogoff();
    }
}
