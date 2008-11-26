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
import com.threerings.opengl.GlView;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.renderer.Color4f;

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

        // create the ui root
        _root = createRoot();
        _root.setModalShade(new Color4f(0f, 0f, 0f, 0.5f));
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
        setView((GlView)view);
    }

    // documentation inherited from interface CrowdContext
    public void clearPlaceView (PlaceView view)
    {
        setView(null);
    }

    // documentation inherited from interface CrowdContext
    public SceneDirector getSceneDirector ()
    {
        return _scenedir;
    }

    // documentation inherited from interface TudeyContext
    public Root getRoot ()
    {
        return _root;
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

    @Override // documentation inherited
    protected void updateView (float elapsed)
    {
        if (_view != null) {
            _view.tick(elapsed);
        }
        _root.tick(elapsed);

        // call super.updateView afterwards so that the camera position will be up-to-date
        super.updateView(elapsed);
    }

    @Override // documentation inherited
    protected void enqueueView ()
    {
        super.enqueueView();
        if (_view != null) {
            _view.enqueue();
        }
        _root.enqueue();
    }

    /**
     * Sets the current view.
     */
    protected void setView (GlView view)
    {
        if (_view != null) {
            _view.wasRemoved();
        }
        if ((_view = view) != null) {
            _view.wasAdded();
        }
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

    /** The user interface root. */
    protected Root _root;

    /** The current view, if any. */
    protected GlView _view;

    /** The tool server. */
    protected static ToolServer _server;
}
