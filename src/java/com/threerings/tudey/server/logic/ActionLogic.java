//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import com.google.inject.Inject;

import com.threerings.crowd.data.BodyObject;
import com.threerings.presents.dobj.OidList;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActionConfig;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.EffectConfig;
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.server.TudeySceneRegistry;

import static com.threerings.tudey.Log.*;

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
        public void execute (int timestamp, Logic activator)
        {
            ConfigReference<ActorConfig> actor = ((ActionConfig.SpawnActor)_config).actor;
            _location.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                _scenemgr.spawnActor(
                    timestamp, target.getTranslation(), target.getRotation(), actor);
            }
            _targets.clear();
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _location = createTarget(((ActionConfig.SpawnActor)_config).location, _source);
        }

        /** The target location. */
        protected TargetLogic _location;
    }

    /**
     * Handles a destroy actor action.
     */
    public static class DestroyActor extends ActionLogic
    {
        @Override // documentation inherited
        public void execute (int timestamp, Logic activator)
        {
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (target instanceof ActorLogic) {
                    ((ActorLogic)target).destroy(timestamp);
                }
            }
            _targets.clear();
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.DestroyActor)_config).target, _source);
        }

        /** The target actor. */
        protected TargetLogic _target;
    }

    /**
     * Handles a fire effect action.
     */
    public static class FireEffect extends ActionLogic
    {
        @Override // documentation inherited
        public void execute (int timestamp, Logic activator)
        {
            ConfigReference<EffectConfig> effect = ((ActionConfig.FireEffect)_config).effect;
            _location.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                _scenemgr.fireEffect(
                    timestamp, target.getTranslation(), target.getRotation(), effect);
            }
            _targets.clear();
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _location = createTarget(((ActionConfig.FireEffect)_config).location, _source);
        }

        /** The target location. */
        protected TargetLogic _location;
    }

    /**
     * Handles a signal action.
     */
    public static class Signal extends ActionLogic
    {
        @Override // documentation inherited
        public void execute (int timestamp, Logic activator)
        {
            String name = ((ActionConfig.Signal)_config).name;
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                _targets.get(ii).signal(timestamp, _source, name);
            }
            _targets.clear();
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.Signal)_config).target, _source);
        }

        /** The target entity. */
        protected TargetLogic _target;
    }

    /**
     * Superclass of the move logic classes.
     */
    public static abstract class AbstractMove extends ActionLogic
    {
        /**
         * Moves a single body to the destination.
         */
        protected void moveBody (int bodyOid)
        {
            BodyObject body = (BodyObject)_omgr.getObject(bodyOid);
            ActionConfig.MoveBody mconfig = (ActionConfig.MoveBody)_config;
            _screg.moveBody(body, mconfig.sceneId, mconfig.portalKey);
        }

        /** The distributed object manager. */
        @Inject protected PresentsDObjectMgr _omgr;

        /** The scene registry. */
        @Inject protected TudeySceneRegistry _screg;
    }

    /**
     * Handles a move body action.
     */
    public static class MoveBody extends AbstractMove
    {
        @Override // documentation inherited
        public void execute (int timestamp, Logic activator)
        {
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (!(target instanceof PawnLogic)) {
                    continue;
                }
                int pawnId = ((PawnLogic)target).getActor().getId();
                TudeyOccupantInfo info =
                    ((TudeySceneObject)_scenemgr.getPlaceObject()).getOccupantInfo(pawnId);
                if (info != null) {
                    moveBody(info.getBodyOid());
                }
            }
            _targets.clear();
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((ActionConfig.MoveBody)_config).target, _source);
        }

        /** The target actor. */
        protected TargetLogic _target;
    }

    /**
     * Handles a move all action.
     */
    public static class MoveAll extends AbstractMove
    {
        @Override // documentation inherited
        public void execute (int timestamp, Logic activator)
        {
            OidList occupants = _scenemgr.getPlaceObject().occupants;
            for (int ii = 0, nn = occupants.size(); ii < nn; ii++) {
                moveBody(occupants.get(ii));
            }
        }
    }

    /**
     * Handles a conditional action.
     */
    public static class Conditional extends ActionLogic
    {
        @Override // documentation inherited
        public void execute (int timestamp, Logic activator)
        {
            if (_condition.isSatisfied(activator)) {
                _action.execute(timestamp, activator);
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ActionConfig.Conditional config = (ActionConfig.Conditional)_config;
            _condition = createCondition(config.condition, _source);
            _action = createAction(config.action, _source);
        }

        /** The condition to evaluate. */
        protected ConditionLogic _condition;

        /** The action to take if the condition is satisfied. */
        protected ActionLogic _action;
    }

    /**
     * Handles a compound action.
     */
    public static class Compound extends ActionLogic
    {
        @Override // documentation inherited
        public void execute (int timestamp, Logic activator)
        {
            for (ActionLogic action : _actions) {
                action.execute(timestamp, activator);
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
     *
     * @param activator the entity that triggered the action.
     */
    public abstract void execute (int timestamp, Logic activator);

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

    /** Temporary container for targets. */
    protected ArrayList<Logic> _targets = Lists.newArrayList();
}
