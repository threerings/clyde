//
// $Id$

package com.threerings.tudey.data;

import com.threerings.whirled.data.SceneModel;

import com.threerings.config.ConfigManager;
import com.threerings.export.Exportable;

/**
 * Contains a representation of a Tudey scene.
 */
public class TudeySceneModel extends SceneModel
    implements Exportable
{
    /**
     * Initializes the model.
     */
    public void init (ConfigManager cfgmgr)
    {
        _cfgmgr.init("scene", cfgmgr);
    }

    /**
     * Returns a reference to the scene's configuration manager.
     */
    public ConfigManager getConfigManager ()
    {
        return _cfgmgr;
    }

    /** The scene configuration manager. */
    protected ConfigManager _cfgmgr = new ConfigManager();
}
