//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.google.inject.Inject;

import com.threerings.crowd.data.BodyObject;
import com.threerings.presents.dobj.OidList;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActionConfig;
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
        public void execute (int timestamp, ActorLogic target)
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
        public void execute (int timestamp, ActorLogic target)
        {
            _scenemgr.fireEffect(
                timestamp, _source.getTranslation(), _source.getRotation(),
                ((ActionConfig.FireEffect)_config).effect);
        }
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
        public void execute (int timestamp, ActorLogic target)
        {
            if (target == null) {
                return;
            }
            int pawnId = target.getActor().getId();
            TudeyOccupantInfo info =
                ((TudeySceneObject)_scenemgr.getPlaceObject()).getOccupantInfo(pawnId);
            if (info != null) {
                moveBody(info.getBodyOid());
            }
        }
    }

    /**
     * Handles a move all action.
     */
    public static class MoveAll extends AbstractMove
    {
        @Override // documentation inherited
        public void execute (int timestamp, ActorLogic target)
        {
            OidList occupants = _scenemgr.getPlaceObject().occupants;
            for (int ii = 0, nn = occupants.size(); ii < nn; ii++) {
                moveBody(occupants.get(ii));
            }
        }
    }

    /**
     * Handles a destroy source action.
     */
    public static class DestroySource extends ActionLogic
    {
        @Override // documentation inherited
        public void execute (int timestamp, ActorLogic target)
        {
            if (_source instanceof ActorLogic) {
                ((ActorLogic)_source).destroy(timestamp);
            }
        }
    }

    /**
     * Handles a compound action.
     */
    public static class Compound extends ActionLogic
    {
        @Override // documentation inherited
        public void execute (int timestamp, ActorLogic target)
        {
            for (ActionLogic action : _actions) {
                action.execute(timestamp, target);
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
     * @param target the target of the action, if any.
     */
    public abstract void execute (int timestamp, ActorLogic target);

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
