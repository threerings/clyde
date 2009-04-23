//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.tudey.server.logic;

import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.util.Interval;

import com.threerings.math.Transform2D;
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
        public void startup (int timestamp)
        {
            execute(timestamp);
        }
    }

    /**
     * Handles the shutdown event.
     */
    public static class Shutdown extends HandlerLogic
    {
        @Override // documentation inherited
        public void shutdown (int timestamp)
        {
            execute(timestamp);
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
        public void startup (int timestamp)
        {
            _scenemgr.addTickParticipant(this);
        }

        @Override // documentation inherited
        public void shutdown (int timestamp)
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
        public void startup (int timestamp)
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
        public void shutdown (int timestamp)
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
        public void shutdown (int timestamp)
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
        public void shapeWillChange (Logic source)
        {
            // no-op
        }

        // documentation inherited from interface ShapeObserver
        public void shapeDidChange (Logic source)
        {
            Shape shape = ((HandlerConfig.BaseIntersection)_config).shape.getShape(
                _source.getShape(), _source.getTransform(_transform), _shape.getLocalShape());
            _shape.setLocalShape(shape);
        }

        @Override // documentation inherited
        public void startup (int timestamp)
        {
            Shape shape = ((HandlerConfig.BaseIntersection)_config).shape.getShape(
                _source.getShape(), _source.getTransform(_transform), null);
            if (shape == null) {
                return;
            }
            _shape = new ShapeElement(shape);
            _shape.setUserObject(this);
            _scenemgr.getSensorSpace().add(_shape);
            _source.addShapeObserver(this);
        }

        @Override // documentation inherited
        public void shutdown (int timestamp)
        {
            if (_shape != null) {
                _scenemgr.getSensorSpace().remove(_shape);
                _source.removeShapeObserver(this);
            }
        }

        /** The shape element in the sensor space. */
        protected ShapeElement _shape;

        /** Holds the source transform. */
        protected Transform2D _transform = new Transform2D();
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
     * Handles a client request event.
     */
    public static class Request extends HandlerLogic
    {
        @Override // documentation inherited
        public void request (int timestamp, PawnLogic source, String name)
        {
            HandlerConfig.Request config = (HandlerConfig.Request)_config;
            if (config.name.equals(name)) {
                execute(timestamp, source);
            }
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
     * Starts up the handler.
     */
    public void startup (int timestamp)
    {
        // nothing by default
    }

    /**
     * Shuts down the handler.
     */
    public void shutdown (int timestamp)
    {
        // nothing by default
    }

    /**
     * Notes that the logic has been removed.
     */
    public void removed ()
    {
        // nothing by default
    }

    @Override // documentation inherited
    public boolean isActive ()
    {
        return _source.isActive();
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
