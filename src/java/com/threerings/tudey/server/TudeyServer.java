//
// $Id$

package com.threerings.tudey.server;

import com.google.inject.Inject;
import com.google.inject.Injector;

import com.threerings.config.ConfigManager;
import com.threerings.resource.ResourceManager;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.WhirledServer;
import com.threerings.whirled.util.SceneFactory;

import com.threerings.tudey.data.TudeySceneConfig;
import com.threerings.tudey.util.TudeySceneFactory;

/**
 * The base Tudey server.
 */
public abstract class TudeyServer extends WhirledServer
{
    /** Configures dependencies needed by the Tudey services. */
    public static class Module extends WhirledServer.Module
    {
        @Override protected void configure () {
            super.configure();
            ResourceManager rsrcmgr = new ResourceManager("rsrc/");
            bind(ResourceManager.class).toInstance(rsrcmgr);
            bind(ConfigManager.class).toInstance(new ConfigManager(rsrcmgr, "config/"));
            bind(SceneRegistry.ConfigFactory.class).toInstance(new SceneRegistry.ConfigFactory() {
                public PlaceConfig createPlaceConfig (SceneModel model) {
                    return new TudeySceneConfig();
                }
            });
            bind(SceneFactory.class).to(TudeySceneFactory.class);
        }
    }

    /** The scene registry. */
    @Inject protected SceneRegistry _scenereg;
}
