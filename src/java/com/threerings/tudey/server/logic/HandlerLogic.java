//
// $Id$

package com.threerings.tudey.server.logic;

import com.samskivert.util.Interval;

import com.threerings.math.Vector2f;

import com.threerings.tudey.config.HandlerConfig;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.ShapeElement;

/**
 * Handles the server-side processing for an event handler type.
 */
public abstract class HandlerLogic extends Logic
{
    /**
     * Handles the startup event.
     */
    public static class Startup extends HandlerLogic
    {
        @Override // documentation inherited
        protected void didInit ()
        {
            execute(_scenemgr.getTimestamp());
        }
    }

    /**
     * Handles the tick event.
     */
    public static class Tick extends HandlerLogic
        implements TudeySceneManager.TickParticipant
    {
        // documentation inherited from interface TudeySceneManager.TickParticipant
        public boolean tick (int timestamp)
        {
            execute(timestamp);
            return true;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _scenemgr.addTickParticipant(this);
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            _scenemgr.removeTickParticipant(this);
        }
    }

    /**
     * Handles the timer event.
     */
    public static class Timer extends HandlerLogic
    {
        @Override // documentation inherited
        protected void didInit ()
        {
            (_interval = new Interval(_scenemgr) {
                public void expired () {
                    execute(_scenemgr.getTimestamp());
                }
            }).schedule((long)(((HandlerConfig.Timer)_config).interval * 1000f), true);
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            _interval.cancel();
        }

        /** The timer interval. */
        protected Interval _interval;
    }

    /**
     * Handles the intersection event.
     */
    public static class Intersection extends HandlerLogic
        implements TudeySceneManager.Sensor, ShapeObserver
    {
        // documentation inherited from interface TudeySceneManager.Sensor
        public void trigger (ActorLogic actor)
        {
            execute(_scenemgr.getTimestamp(), actor);
        }

        // documentation inherited from interface ShapeObserver
        public void shapeUpdated (Logic source)
        {
            Shape shape = _source.getShape();
            float expansion = ((HandlerConfig.Intersection)_config).expansion;
            _shape.setLocalShape(expansion == 0f ?
                shape : shape.expand(expansion, _shape.getLocalShape()));
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            Shape shape = _source.getShape();
            if (shape == null) {
                return;
            }
            float expansion = ((HandlerConfig.Intersection)_config).expansion;
            if (expansion != 0f) {
                shape = shape.expand(expansion);
            }
            _shape = new ShapeElement(shape);
            _shape.setUserObject(this);
            _scenemgr.getSensorSpace().add(_shape);
            _source.addShapeObserver(this);
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            if (_shape != null) {
                _scenemgr.getSensorSpace().remove(_shape);
                _source.removeShapeObserver(this);
            }
        }

        /** The shape element in the sensor space. */
        protected ShapeElement _shape;
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, HandlerConfig config, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _source = source;
        _action = (config.action == null) ? null : createAction(config.action, source);

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Notes that the logic has been removed.
     */
    public void removed ()
    {
        wasRemoved();
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

    /**
     * Executes the handler's action with no target.
     */
    protected void execute (int timestamp)
    {
        execute(timestamp, null);
    }

    /**
     * Executes the handler's action.
     *
     * @param target the target of the action, if any.
     */
    protected void execute (int timestamp, ActorLogic target)
    {
        if (_action != null) {
            _action.execute(timestamp, target);
        }
    }

    /** The handler configuration. */
    protected HandlerConfig _config;

    /** The action source. */
    protected Logic _source;

    /** The action to execute in response to the event. */
    protected ActionLogic _action;
}
