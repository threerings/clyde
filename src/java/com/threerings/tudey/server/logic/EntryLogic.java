//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.threerings.config.ConfigManager;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.HandlerConfig;
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
        _tag = entry.getTag(cfgmgr);

        // create the handler logic objects
        ArrayList<HandlerLogic> handlers = new ArrayList<HandlerLogic>();
        for (HandlerConfig config : entry.getHandlers(cfgmgr)) {
            HandlerLogic handler = createHandler(config, this);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        _handlers = handlers.toArray(new HandlerLogic[handlers.size()]);

        // give subclasses a chance to set up
        didInit();
    }

    /**
     * Notes that the entry has been removed from the scene.
     */
    public void removed ()
    {
        // notify the handlers
        for (HandlerLogic handler : _handlers) {
            handler.removed();
        }

        // give subclasses a chance to cleanup
        wasRemoved();
    }

    @Override // documentation inherited
    public String getTag ()
    {
        return _tag;
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

    /**
     * Override to perform custom cleanup.
     */
    protected void wasRemoved ()
    {
        // nothing by default
    }

    /** The scene entry. */
    protected Entry _entry;

    /** The entry's tag. */
    protected String _tag;

    /** The entry's approximate translation. */
    protected Vector2f _translation;

    /** The entry's approximate rotation. */
    protected float _rotation;

    /** The entry's event handlers. */
    protected HandlerLogic[] _handlers;
}
