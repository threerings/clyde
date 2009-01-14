//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.math.Vector2f;

import com.threerings.tudey.config.BehaviorConfig;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Handles the server-side processing for agent behavior.
 */
public abstract class BehaviorLogic extends Logic
{
    /**
     * Handles the idle behavior.
     */
    public static class Idle extends BehaviorLogic
    {
    }

    /**
     * Handles the wander behavior.
     */
    public static class Wander extends BehaviorLogic
    {
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, BehaviorConfig config, AgentLogic agent)
    {
        super.init(scenemgr);
        _config = config;
        _agent = agent;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Ticks the behavior.
     */
    public void tick (int timestamp)
    {
        // nothing by default
    }

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _agent.getTranslation();
    }

    @Override // documentation inherited
    public float getRotation ()
    {
        return _agent.getRotation();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /** The behavior configuration. */
    protected BehaviorConfig _config;

    /** The controlled agent. */
    protected AgentLogic _agent;
}
