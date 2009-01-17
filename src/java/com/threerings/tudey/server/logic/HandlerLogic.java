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
            HandlerConfig.Timer config = (HandlerConfig.Timer)_config;
            _limit = (config.limit == 0) ? Integer.MAX_VALUE : config.limit;
            (_interval = new Interval(_scenemgr) {
                public void expired () {
                    execute(_scenemgr.getTimestamp());
                    if (--_limit == 0) {
                        _interval.cancel();
                    }
                }
            }).schedule((long)(config.interval * 1000f), true);
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            _interval.cancel();
        }

        /** The number of times remaining to fire. */
        protected int _limit;

        /** The timer interval. */
        protected Interval _interval;
    }

    /**
     * Handles a signal event.
     */
    public static class Signal extends HandlerLogic
    {
        @Override // documentation inherited
        public void signal (int timestamp, Logic source, String name)
        {
            HandlerConfig.Signal config = (HandlerConfig.Signal)_config;
            if (config.name.equals(name) && timestamp >= _minTimestamp) {
                execute(timestamp, source);
                _minTimestamp = Math.round(timestamp + config.refractoryPeriod * 1000f);
            }
        }

        /** The earliest time at which we may execute. */
        protected int _minTimestamp;
    }

    /**
     * Handles a signal start event.
     */
    public static class SignalStart extends HandlerLogic
        implements TudeySceneManager.TickParticipant
    {
        // documentation inherited from interface TudeySceneManager.TickParticipant
        public boolean tick (int timestamp)
        {
            if (!(_receivedOnFrame || _receivedOnLastFrame)) {
                _receiving = false;
                return false;
            }
            _receivedOnLastFrame = _receivedOnFrame;
            _receivedOnFrame = false;
            return true;
        }

        @Override // documentation inherited
        public void signal (int timestamp, Logic source, String name)
        {
            _receivedOnFrame = true;
            if (_receiving) {
                return;
            }
            _receiving = true;
            execute(timestamp, source);
            _scenemgr.addTickParticipant(this);
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            _scenemgr.removeTickParticipant(this);
        }

        /** Whether or not we're currently receiving the signal. */
        protected boolean _receiving;

        /** Whether or not we're received the signal on the current frame. */
        protected boolean _receivedOnFrame;

        /** Whether or not we received the signal on the last frame. */
        protected boolean _receivedOnLastFrame;
    }

    /**
     * Handles a signal stop event.
     */
    public static class SignalStop extends HandlerLogic
        implements TudeySceneManager.TickParticipant
    {
        // documentation inherited from interface TudeySceneManager.TickParticipant
        public boolean tick (int timestamp)
        {
            if (!(_receivedOnFrame || _receivedOnLastFrame)) {
                _receiving = false;
                execute(timestamp);
                return false;
            }
            _receivedOnLastFrame = _receivedOnFrame;
            _receivedOnFrame = false;
            return true;
        }

        @Override // documentation inherited
        public void signal (int timestamp, Logic source, String name)
        {
            _receivedOnFrame = true;
            if (_receiving) {
                return;
            }
            _receiving = true;
            _scenemgr.addTickParticipant(this);
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            _scenemgr.removeTickParticipant(this);
        }

        /** Whether or not we're currently receiving the signal. */
        protected boolean _receiving;

        /** Whether or not we're received the signal on the current frame. */
        protected boolean _receivedOnFrame;

        /** Whether or not we received the signal on the last frame. */
        protected boolean _receivedOnLastFrame;
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
            int timestamp = _scenemgr.getTimestamp();
            if (timestamp >= _minTimestamp) {
                execute(timestamp, actor);
                _minTimestamp = Math.round(timestamp +
                    ((HandlerConfig.Intersection)_config).refractoryPeriod * 1000f);
            }
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

        /** The earliest time at which we may execute. */
        protected int _minTimestamp;
    }

    /**
     * Handles the interaction event.
     */
    public static class Interaction extends HandlerLogic
    {
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, HandlerConfig config, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _source = source;
        _action = createAction(config.action, source);

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
     * Executes the handler's action with the source as the activator.
     */
    protected void execute (int timestamp)
    {
        execute(timestamp, _source);
    }

    /**
     * Executes the handler's action.
     *
     * @param activator the entity that triggered the action.
     */
    protected void execute (int timestamp, Logic activator)
    {
        _action.execute(timestamp, activator);
    }

    /** The handler configuration. */
    protected HandlerConfig _config;

    /** The action source. */
    protected Logic _source;

    /** The action to execute in response to the event. */
    protected ActionLogic _action;
}
