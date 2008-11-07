//
// $Id$

package com.threerings.tudey.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.tudey.client.TudeySceneController;

/**
 * Place configuration for Tudey scenes.
 */
public class TudeySceneConfig extends PlaceConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new TudeySceneController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.tudey.server.TudeySceneManager";
    }
}
