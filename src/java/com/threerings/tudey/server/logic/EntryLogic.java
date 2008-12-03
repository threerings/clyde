//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.config.ConfigManager;
import com.threerings.math.Vector2f;

import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * A logic object associated with a scene entry.
 */
public class EntryLogic extends Logic
{
    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, Entry entry)
    {
        super.init(scenemgr);
        _entry = entry;
        ConfigManager cfgmgr = scenemgr.getConfigManager();
        _translation = entry.getTranslation(cfgmgr);
        _rotation = entry.getRotation(cfgmgr);

        // give subclasses a chance to set up
        didInit();
    }

    /**
     * Notes that the entry has been removed from the scene.
     */
    public void removed ()
    {
        // nothing by default
    }

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _translation;
    }

    @Override // documentation inherited
    public float getRotation ()
    {
        return _rotation;
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /** The scene entry. */
    protected Entry _entry;

    /** The entry's approximate translation. */
    protected Vector2f _translation;

    /** The entry's approximate rotation. */
    protected float _rotation;
}
