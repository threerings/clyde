//
// $Id$

package com.threerings.tudey.tools;

import com.threerings.crowd.client.PlaceController;

import com.threerings.tudey.data.TudeySceneConfig;

/**
 * Place configuration for Tudey scenes.
 */
public class ToolSceneConfig extends TudeySceneConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new ToolSceneController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.tudey.tools.ToolSceneManager";
    }
}
