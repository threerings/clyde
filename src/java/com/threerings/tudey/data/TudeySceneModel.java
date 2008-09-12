//
// $Id$

package com.threerings.tudey.data;

import com.threerings.whirled.data.SceneModel;

import com.threerings.config.ConfigManager;
import com.threerings.export.Exportable;
import com.threerings.util.DeepUtil;

/**
 * Contains a representation of a Tudey scene.
 */
public class TudeySceneModel extends SceneModel
    implements Exportable
{
    /** The global scene properties. */
    public SceneGlobals globals = new SceneGlobals();

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

    @Override // documentation inherited
    public Object clone ()
    {
        return DeepUtil.copy(this, null);
    }

    /** The scene configuration manager. */
    protected ConfigManager _cfgmgr = new ConfigManager();
}
