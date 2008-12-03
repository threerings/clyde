//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActionConfig;
import com.threerings.tudey.config.HandlerConfig;
import com.threerings.tudey.server.TudeySceneManager;

import static com.threerings.tudey.Log.*;

/**
 * Handles the server-side processing for some entity.
 */
public abstract class Logic
{
    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr)
    {
        _scenemgr = scenemgr;
    }

    /**
     * Returns the tag for this logic, or the empty string for none.
     */
    public String getTag ()
    {
        return "";
    }

    /**
     * Returns the translation of this logic for the purpose of spawning actors, etc.
     */
    public Vector2f getTranslation ()
    {
        return Vector2f.ZERO;
    }

    /**
     * Returns the rotation of this logic for the purpose of spawning actors, etc.
     */
    public float getRotation ()
    {
        return 0f;
    }

    /**
     * Creates a handler with the supplied configuration and source.
     */
    protected HandlerLogic createHandler (HandlerConfig config, Logic source)
    {
        // create the logic class
        HandlerLogic logic;
        try {
            logic = (HandlerLogic)Class.forName(config.getLogicClassName()).newInstance();
        } catch (Exception e) {
            log.warning("Failed to instantiate handler logic.",
                "class", config.getLogicClassName(), e);
            return null;
        }

        // initialize, return the logic
        logic.init(_scenemgr, config, source);
        return logic;
    }

    /**
     * Creates an action with the supplied configuration and source.
     */
    protected ActionLogic createAction (ActionConfig config, Logic source)
    {
        // create the logic class
        ActionLogic logic;
        try {
            logic = (ActionLogic)Class.forName(config.getLogicClassName()).newInstance();
        } catch (Exception e) {
            log.warning("Failed to instantiate action logic.",
                "class", config.getLogicClassName(), e);
            return null;
        }

        // initialize, return the logic
        logic.init(_scenemgr, config, source);
        return logic;
    }

    /** The scene manager. */
    protected TudeySceneManager _scenemgr;
}
