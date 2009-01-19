//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.util.Interval;

import com.threerings.math.Vector2f;

import com.threerings.tudey.config.HandlerConfig;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.ShapeElement;
import com.threerings.tudey.space.HashSpace;

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
     * Base class for transition handlers.
     */
    public static abstract class Transition extends HandlerLogic
        implements TudeySceneManager.TickParticipant
    {
        /**
         * Creates a new transition handler.
         *
         * @param start if true, execute the action on activation start.
         * @param stop if true, execute the action on activation stop.
         */
        public Transition (boolean start, boolean stop)
        {
            _start = start;
            _stop = stop;
        }

        // documentation inherited from interface TudeySceneManager.TickParticipant
        public boolean tick (int timestamp)
        {
            for (Iterator<Map.Entry<Logic, Boolean>> it = _activated.entrySet().iterator();
                    it.hasNext(); ) {
                Map.Entry<Logic, Boolean> entry = it.next();
                if (entry.getValue()) {
                    entry.setValue(false);
                } else {
                    if (_stop) {
                        execute(timestamp, entry.getKey());
                    }
                    it.remove();
                }
            }
            return !_activated.isEmpty();
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            _scenemgr.removeTickParticipant(this);
        }

        /**
         * Notes that the source has been activated.
         */
        protected void activate (int timestamp, Logic source)
        {
            Boolean value = _activated.get(source);
            if (value == null) {
                if (_activated.isEmpty()) {
                    _scenemgr.addTickParticipant(this);
                }
                if (_start) {
                    execute(timestamp, source);
                }
            }
            _activated.put(source, true);
        }

        /** Whether or not to execute the action on start/stop. */
        protected boolean _start, _stop;

        /** Whether or not each activator has activated on the current tick. */
        protected Map<Logic, Boolean> _activated = Maps.newIdentityHashMap();
    }

    /**
     * Handles a signal start event.
     */
    public static class SignalStart extends Transition
    {
        /**
         * Creates a signal start handler.
         */
        public SignalStart ()
        {
            super(true, false);
        }

        @Override // documentation inherited
        public void signal (int timestamp, Logic source, String name)
        {
            if (((HandlerConfig.SignalStart)_config).name.equals(name)) {
                activate(timestamp, source);
            }
        }
    }

    /**
     * Handles a signal stop event.
     */
    public static class SignalStop extends Transition
    {
        /**
         * Creates a signal stop handler.
         */
        public SignalStop ()
        {
            super(false, true);
        }

        @Override // documentation inherited
        public void signal (int timestamp, Logic source, String name)
        {
            if (((HandlerConfig.SignalStop)_config).name.equals(name)) {
                activate(timestamp, source);
            }
        }
    }

    /**
     * Base class for the various intersection-related handler logic classes.
     */
    public static abstract class BaseIntersection extends Transition
        implements ShapeObserver
    {
        /**
         * Base intersection constructor.
         */
        public BaseIntersection (boolean start, boolean stop)
        {
            super(start, stop);
        }

        // documentation inherited from interface ShapeObserver
        public void shapeUpdated (Logic source)
        {
            Shape shape = _source.getShape();
            float expansion = ((HandlerConfig.BaseIntersection)_config).expansion;
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
            float expansion = ((HandlerConfig.BaseIntersection)_config).expansion;
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
     * Handles the intersection event.
     */
    public static class Intersection extends BaseIntersection
        implements TudeySceneManager.IntersectionSensor
    {
        /**
         * Creates a new intersection handler.
         */
        public Intersection ()
        {
            super(false, false);
        }

        // documentation inherited from interface TudeySceneManager.IntersectionSensor
        public void trigger (int timestamp, ActorLogic actor)
        {
            if (timestamp >= _minTimestamp) {
                execute(timestamp, actor);
                _minTimestamp = Math.round(timestamp +
                    ((HandlerConfig.Intersection)_config).refractoryPeriod * 1000f);
            }
        }

        /** The earliest time at which we may execute. */
        protected int _minTimestamp;
    }

    /**
     * Handles the intersection start event.
     */
    public static class IntersectionStart extends BaseIntersection
        implements TudeySceneManager.IntersectionSensor
    {
        /**
         * Creates a new intersection start handler.
         */
        public IntersectionStart ()
        {
            super(true, false);
        }

        // documentation inherited from interface TudeySceneManager.IntersectionSensor
        public void trigger (int timestamp, ActorLogic actor)
        {
            activate(timestamp, actor);
        }
    }

    /**
     * Handles the intersection stop event.
     */
    public static class IntersectionStop extends BaseIntersection
        implements TudeySceneManager.IntersectionSensor
    {
        /**
         * Creates a new intersection stop handler.
         */
        public IntersectionStop ()
        {
            super(false, true);
        }

        // documentation inherited from interface TudeySceneManager.IntersectionSensor
        public void trigger (int timestamp, ActorLogic actor)
        {
            activate(timestamp, actor);
        }
    }

    /**
     * Handles the interaction event.
     */
    public static class Interaction extends BaseIntersection
        implements TudeySceneManager.InteractionSensor
    {
        /**
         * Creates a new interaction handler.
         */
        public Interaction ()
        {
            super(false, false);
        }

        // documentation inherited from interface TudeySceneManager.InteractionSensor
        public void trigger (int timestamp, ActorLogic actor)
        {
            execute(timestamp, actor);
        }
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
