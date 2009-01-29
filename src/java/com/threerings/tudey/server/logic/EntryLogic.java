//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.threerings.config.ConfigManager;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.HandlerConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;

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
        _tags = entry.getTags(cfgmgr);
        _shape = entry.createShape(cfgmgr);

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
    public String[] getTags ()
    {
        return _tags;
    }

    @Override // documentation inherited
    public boolean isDefaultEntrance ()
    {
        return _defaultEntrance;
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

    @Override // documentation inherited
    public Shape getShape ()
    {
        return _shape;
    }

    @Override // documentation inherited
    public Vector2f[] getPatrolPath ()
    {
        return _entry.createPatrolPath(_shape);
    }

    @Override // documentation inherited
    public void signal (int timestamp, Logic source, String name)
    {
        for (HandlerLogic handler : _handlers) {
            handler.signal(timestamp, source, name);
        }
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

    /** The entry's tags. */
    protected String[] _tags;

    /** Whether or not the entry represents a default entrance. */
    protected boolean _defaultEntrance;

    /** The entry's approximate translation. */
    protected Vector2f _translation;

    /** The entry's approximate rotation. */
    protected float _rotation;

    /** The entry's shape. */
    protected Shape _shape;

    /** The entry's patrol path. */
    protected Vector2f[] _patrolPath;

    /** The entry's event handlers. */
    protected HandlerLogic[] _handlers;
}
