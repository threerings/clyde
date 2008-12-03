//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

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
        public void execute (int timestamp)
        {
            _scenemgr.spawnActor(
                timestamp, _source.getTranslation(), _source.getRotation(),
                ((ActionConfig.SpawnActor)_config).actor);
        }
    }

    /**
     * Handles a fire effect action.
     */
    public static class FireEffect extends ActionLogic
    {
        @Override // documentation inherited
        public void execute (int timestamp)
        {
            _scenemgr.fireEffect(
                timestamp, _source.getTranslation(), _source.getRotation(),
                ((ActionConfig.FireEffect)_config).effect);
        }
    }

    /**
     * Handles a compound action.
     */
    public static class Compound extends ActionLogic
    {
        @Override // documentation inherited
        public void execute (int timestamp)
        {
            for (ActionLogic action : _actions) {
                action.execute(timestamp);
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ArrayList<ActionLogic> actions = new ArrayList<ActionLogic>();
            for (ActionConfig config : ((ActionConfig.Compound)_config).actions) {
                ActionLogic action = createAction(config, _source);
                if (action != null) {
                    actions.add(action);
                }
            }
            _actions = actions.toArray(new ActionLogic[actions.size()]);
        }

        /** Logic objects for the actions. */
        protected ActionLogic[] _actions;
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, ActionConfig config, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _source = source;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Executes the action.
     */
    public abstract void execute (int timestamp);

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
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /** The action configuration. */
    protected ActionConfig _config;

    /** The action source. */
    protected Logic _source;
}
