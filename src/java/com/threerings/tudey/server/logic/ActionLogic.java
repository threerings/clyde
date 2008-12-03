//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActionConfig;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Handles the server-side processing for an action.
 */
public abstract class ActionLogic extends Logic
{
    /**
     * Handles a spawn actor action.
     */
    public static class SpawnActor extends ActionLogic
    {
        @Override // documentation inherited
        protected void execute ()
        {
            _scenemgr.spawnActor(
                _timestamp, _source.getTranslation(), _source.getRotation(),
                ((ActionConfig.SpawnActor)_config).actor);
        }
    }

    /**
     * Handles a fire effect action.
     */
    public static class FireEffect extends ActionLogic
    {
        @Override // documentation inherited
        protected void execute ()
        {
            _scenemgr.fireEffect(
                _timestamp, _source.getTranslation(), _source.getRotation(),
                ((ActionConfig.FireEffect)_config).effect);
        }
    }

    /**
     * Handles a compound action.
     */
    public static class Compound extends ActionLogic
    {
        @Override // documentation inherited
        protected void execute ()
        {
            for (ActionConfig action : ((ActionConfig.Compound)_config).actions) {
                _source.execute(action, _timestamp);
            }
        }
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, ActionConfig config, int timestamp, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _timestamp = timestamp;
        _source = source;

        // execute the action
        execute();
    }

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override // documentation inherited
    public float getRotation ()
    {
        return _source.getRotation();
    }

    /**
     * Executes the action.
     */
    protected abstract void execute ();

    /** The action configuration. */
    protected ActionConfig _config;

    /** The action timestamp. */
    protected int _timestamp;

    /** The action source. */
    protected Logic _source;
}
