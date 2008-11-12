//
// $Id$

package com.threerings.tudey.tools;

import com.google.inject.Guice;
import com.google.inject.Injector;

import com.samskivert.util.Config;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.util.Name;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.client.persist.SceneRepository;
import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.util.NoSuchSceneException;
import com.threerings.whirled.util.SceneFactory;

import com.threerings.opengl.GlCanvasTool;

import com.threerings.tudey.util.TudeyContext;
import com.threerings.tudey.util.TudeySceneFactory;

/**
 * Base class for Tudey tools.
 */
public abstract class TudeyTool extends GlCanvasTool
    implements TudeyContext
{
    /**
     * Creates a new tool.
     */
    public TudeyTool (String msgs)
    {
        super(msgs);

        // create the Presents client
        _client = new Client(null, getRunQueue());

        // create the various directors
        _locdir = new LocationDirector(this);
        _occdir = new OccupantDirector(this);
        _chatdir = new ChatDirector(this, _msgmgr, "chat");

        // create a fake repository that stores nothing
        SceneRepository screp = new SceneRepository() {
            public SceneModel loadSceneModel (int sceneId) throws NoSuchSceneException {
                throw new NoSuchSceneException(sceneId);
            }
            public void storeSceneModel (SceneModel model) {
                // no-op
            }
            public void deleteSceneModel (int sceneId) {
                // no-op
            }
        };
        _scenedir = new SceneDirector(this, _locdir, screp, new TudeySceneFactory());
    }

    // documentation inherited from interface PresentsContext
    public Config getConfig ()
    {
        return _config;
    }

    // documentation inherited from interface PresentsContext
    public Client getClient ()
    {
        return _client;
    }

    // documentation inherited from interface PresentsContext
    public DObjectManager getDObjectManager ()
    {
        return _client.getDObjectManager();
    }

    // documentation inherited from interface CrowdContext
    public LocationDirector getLocationDirector ()
    {
        return _locdir;
    }

    // documentation inherited from interface CrowdContext
    public OccupantDirector getOccupantDirector ()
    {
        return _occdir;
    }

    // documentation inherited from interface CrowdContext
    public ChatDirector getChatDirector ()
    {
        return _chatdir;
    }

    // documentation inherited from interface CrowdContext
    public void setPlaceView (PlaceView view)
    {
    }

    // documentation inherited from interface CrowdContext
    public void clearPlaceView (PlaceView view)
    {
    }

    // documentation inherited from interface CrowdContext
    public SceneDirector getSceneDirector ()
    {
        return _scenedir;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // log on to our local server
        _server.startStandaloneClient(_client, new Name("editor"));
    }

    @Override // documentation inherited
    protected void willShutdown ()
    {
        super.willShutdown();

        // log off of our local server
        _server.stopStandaloneClient(_client);
    }

    /**
     * Creates and initializes the local tool server.
     */
    protected static void createServer ()
        throws Exception
    {
        Injector injector = Guice.createInjector(new ToolServer.Module());
        _server = injector.getInstance(ToolServer.class);
        _server.init(injector);
    }

    /** The tool configuration. */
    protected Config _config = new Config("tool");

    /** The Presents client. */
    protected Client _client;

    /** Handles requests to change location. */
    protected LocationDirector _locdir;

    /** Provides access to occupant lists. */
    protected OccupantDirector _occdir;

    /** Handles chat requests. */
    protected ChatDirector _chatdir;

    /** Handles scene access. */
    protected SceneDirector _scenedir;

    /** The tool server. */
    protected static ToolServer _server;
}
