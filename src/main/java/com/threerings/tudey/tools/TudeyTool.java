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

        // create the various directors
        _locdir = new LocationDirector(this);
        _occdir = new OccupantDirector(this);
        _chatdir = new ChatDirector(this, "chat");

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

    @Override
    protected void initSharedManagers ()
    {
        // create the Presents client
        _client = new Client(null, getRunQueue());

        // create the tool server
        Injector injector = Guice.createInjector(new ToolServer.ToolModule(_client));
        _server = injector.getInstance(ToolServer.class);
        try {
            _server.init(injector);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize server.", e);
        }

        // get references to the server managers
        _rsrcmgr = _server.getResourceManager();
        _cfgmgr = _server.getConfigManager();
        _colorpos = _server.getColorPository();
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        // log on to our local server
        _server.startStandaloneClient(new Name("editor"));
    }

    @Override
    protected void willShutdown ()
    {
        super.willShutdown();

        // log off of our local server
        _server.stopStandaloneClient();
    }

    @Override
    protected void updateView (float elapsed)
    {
        if (_view != null) {
            _view.tick(elapsed);
        }
        _root.tick(elapsed);

        // call super.updateView afterwards so that the camera position will be up-to-date
        super.updateView(elapsed);
    }

    @Override
    protected void compositeView ()
    {
        super.compositeView();
        if (_view != null) {
            _view.composite();
        }
        _root.composite();
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

    /** The tool configuration. */
    protected Config _config = new Config("tool");

    /** The Presents client. */
    protected Client _client;

    /** The tool server. */
    protected ToolServer _server;

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
}
