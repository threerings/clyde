//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.Interval;

import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.HandlerConfig;
import com.threerings.tudey.data.EntityKey;
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
        public void shutdown (int timestamp, Logic activator)
        {
            execute(timestamp, activator);
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
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _scenemgr.addTickParticipant(this);
        }

        @Override // documentation inherited
        public void shutdown (int timestamp, Logic activator)
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
            final HandlerConfig.Timer config = (HandlerConfig.Timer)_config;
            _limit = (config.limit == 0) ? Integer.MAX_VALUE : config.limit;
            // offset -> initialDelay: makes offset 0 behave as before and effects negative offsets.
            float initialDelay = config.interval + config.offset;
            (_interval = new Interval(_scenemgr) {
                public void expired () {
                    execute(_scenemgr.getTimestamp());
                    if (--_limit > 0 && _interval != null) {
                        schedule((long)(config.interval * 1000f));
                    }
                }
            }).schedule((long)(initialDelay * 1000f));
        }

        @Override // documentation inherited
        public void shutdown (int timestamp, Logic activator)
        {
            _interval.cancel();
            _interval = null;
        }

        @Override // documentation inherited
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            startup(0);
            _limit = ((Timer)source)._limit;
        }

        /** The number of times remaining to fire. */
        protected int _limit;

        /** The timer interval. */
        protected Interval _interval;
    }

    /**
     * Handles the warn timer event.
     */
    public static class WarnTimer extends Timer
    {
        @Override // documentation inherited
        public void startup (int timestamp)
        {
            super.startup(timestamp);
            final HandlerConfig.WarnTimer config = (HandlerConfig.WarnTimer)_config;
            // offset -> initialDelay: makes offset 0 behave as before and effects negative offsets.
            if (config.warn == 0 || config.warn > config.interval) {
                return;
            }
            float initialDelay = config.interval - config.warn + config.offset;
            (_warnInterval = new Interval(_scenemgr) {
                public void expired () {
                    _warnAction.execute(_scenemgr.getTimestamp(), _source);
                    if (_limit > 1 && _warnInterval != null) {
                        schedule((long)(config.interval * 1000f));
                    }
                }
            }).schedule((long)(initialDelay * 1000f));
        }

        @Override // documentation inherited
        public void shutdown (int timestamp, Logic activator)
        {
            super.shutdown(timestamp, activator);
            if (_warnInterval != null) {
                _warnInterval.cancel();
                _warnInterval = null;
            }
        }

        @Override // documentation inherited
        public void didInit ()
        {
            super.didInit();
            HandlerConfig.WarnTimer config = (HandlerConfig.WarnTimer) _config;
            if (config.warnAction != null) {
                _warnAction = createAction(config.warnAction, _source);
            }
        }

        @Override // documentation inherited
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            if (_warnAction != null) {
                _warnAction.transfer(((WarnTimer)source)._warnAction, refs);
            }
        }

        /** The warning action. */
        protected ActionLogic _warnAction;

        /** The warning interval. */
        protected Interval _warnInterval;
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

        @Override // documentation inherited
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _minTimestamp = ((Signal)source)._minTimestamp;
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
            return (_added = !_activated.isEmpty());
        }

        @Override // documentation inherited
        public void shutdown (int timestamp, Logic activator)
        {
            _scenemgr.removeTickParticipant(this);
            _added = false;
        }

        /**
         * Notes that the source has been activated.
         */
        protected void activate (int timestamp, Logic source)
        {
            if (!_added) {
                _scenemgr.addTickParticipant(this);
                _added = true;
            }
            Boolean value = _activated.get(source);
            if (value == null) {
                if (_start) {
                    execute(timestamp, source);
                }
            }
            _activated.put(source, true);
        }

        /** Whether or not to execute the action on start/stop. */
        protected boolean _start, _stop;

        /** Whether or not we've been added as a tick participant. */
        protected boolean _added;

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
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            startup(0);
        }

        @Override // documentation inherited
        public void shutdown (int timestamp, Logic activator)
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
        public int getMask ()
        {
            return ((HandlerConfig.Intersection)_config).mask;
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

        @Override // documentation inherited
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _minTimestamp = ((Intersection)source)._minTimestamp;
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
        public int getMask ()
        {
            return ((HandlerConfig.IntersectionStart)_config).mask;
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
        public int getMask ()
        {
            return ((HandlerConfig.IntersectionStop)_config).mask;
        }

        // documentation inherited from interface TudeySceneManager.IntersectionSensor
        public void trigger (int timestamp, ActorLogic actor)
        {
            activate(timestamp, actor);
        }
    }

    /**
     * Handles the intersection count event.
     */
    public abstract static class BaseIntersectionCount extends BaseIntersection
        implements TudeySceneManager.IntersectionSensor
    {
        /**
         * Creates a new base intersection count handler.
         */
        public BaseIntersectionCount ()
        {
            super(false, false);
        }

        @Override // documentation inherited
        public void didInit ()
        {
            _condition = createCondition(
                    ((HandlerConfig.BaseIntersectionCount)_config).condition, _source);
        }

        // documentation inherited from interface TudeySceneManager.IntersectionSensor
        public int getMask ()
        {
            return ((HandlerConfig.BaseIntersectionCount)_config).mask;
        }

        // documentation inherited from interface TudeySceneManager.IntersectionSensor
        public void trigger (int timestamp, ActorLogic actor)
        {
            if (_condition.isSatisfied(actor)) {
                activate(timestamp, actor);
            }
        }

        @Override // documentation inherited
        public boolean tick (int timestamp)
        {
            super.tick(timestamp);

            int size = _activated.size();
            if (_lastCount != size) {
                countChanged(timestamp, size);
                _lastCount = size;
            }
            return _lastCount != 0;
        }

        @Override // documentation inherited
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            BaseIntersectionCount bsource = (BaseIntersectionCount)source;
            _condition.transfer(bsource._condition, refs);
            _lastCount = bsource._lastCount;
        }

        /**
         * Called when the intersection count changes.
         */
        protected abstract void countChanged (int timestamp, int newCount);

        /** The condition to evaluate. */
        protected ConditionLogic _condition;

        /** The last known count. */
        protected int _lastCount = -1;
    }

    /**
     * Handles a threshold intersection count event.
     */
    public static class ThresholdIntersectionCount extends BaseIntersectionCount
    {
        @Override // documentation inherited
        public void didInit ()
        {
            super.didInit();
            HandlerConfig.ThresholdIntersectionCount config =
                (HandlerConfig.ThresholdIntersectionCount) _config;
            if (config.underAction != null) {
                _underAction = createAction(config.underAction, _source);
            }
        }

        @Override // documentation inherited
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            if (_underAction != null) {
                _underAction.transfer(((ThresholdIntersectionCount)source)._underAction, refs);
            }
        }

        @Override // documentation inherited
        protected void countChanged (int timestamp, int newCount)
        {
            HandlerConfig.ThresholdIntersectionCount config =
                (HandlerConfig.ThresholdIntersectionCount) _config;
            if (_lastCount < config.threshold && newCount >= config.threshold) {
                execute(timestamp);
            } else if (_underAction != null && _lastCount >= config.threshold &&
                    newCount < config.threshold) {
                _underAction.execute(timestamp, _source);
            }
        }

        /** The action to perform when we go under the threshold. */
        protected ActionLogic _underAction;
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
     * Base class for {@link ActorAdded} and {@link ActorRemoved}.
     */
    public static abstract class BaseActorObserver extends HandlerLogic
        implements TudeySceneManager.ActorObserver
    {
        // documentation inherited from interface TudeySceneManager.ActorObserver
        public void actorAdded (ActorLogic logic)
        {
            _target.resolve(logic, _targets);
            int count = _targets.size();
            if (count > _lastCount) {
                targetActorAdded(logic);
                _lastCount = count;
            }
            _targets.clear();
        }

        // documentation inherited from interface TudeySceneManager.ActorObserver
        public void actorRemoved (ActorLogic logic)
        {
            if (_lastCount > 0) {
                _target.resolve(logic, _targets);
                int count = _targets.size();
                if (count < _lastCount) {
                    targetActorRemoved(logic);
                    _lastCount = count;
                }
                _targets.clear();
            }
        }

        @Override // documentation inherited
        public void startup (int timestamp)
        {
            _target.resolve(_source, _targets);
            _lastCount = _targets.size();
            _targets.clear();
            _scenemgr.addActorObserver(this);
        }

        @Override // documentation inherited
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _target.transfer(((BaseActorObserver)source)._target, refs);
            startup(0);
        }

        @Override // documentation inherited
        public void shutdown (int timestamp, Logic activator)
        {
            _scenemgr.removeActorObserver(this);
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((HandlerConfig.BaseActorObserver)_config).target, _source);
        }

        /**
         * Called when a new target has appeared.
         */
        protected void targetActorAdded (ActorLogic logic)
        {
            // nothing by default
        }

        /**
         * Called when a target has been removed.
         */
        protected void targetActorRemoved (ActorLogic logic)
        {
            // nothing by default
        }

        /** The target actors. */
        protected TargetLogic _target;

        /** Temporary container for targets. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();

        /** The number of relevant actors at last count. */
        protected int _lastCount;
    }

    /**
     * Handles an actor added event.
     */
    public static class ActorAdded extends BaseActorObserver
    {
        @Override // documentation inherited
        protected void targetActorAdded (ActorLogic logic)
        {
            execute(_scenemgr.getTimestamp(), logic);
        }
    }

    /**
     * Handles an actor removed event.
     */
    public static class ActorRemoved extends BaseActorObserver
    {
        @Override // documentation inherited
        protected void targetActorRemoved (ActorLogic logic)
        {
            if (_lastCount == 1 || !((HandlerConfig.ActorRemoved)_config).all) {
                execute(_scenemgr.getTimestamp(), logic);
            }
        }
    }

    /**
     * Handles variable changes.
     */
    public static class VariableChanged extends HandlerLogic
    {
        @Override // documentation inherited
        public void variableChanged (int timestamp, Logic activator, String name)
        {
            execute(timestamp, activator);
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
    public void shutdown (int timestamp, Logic activator)
    {
        // nothing by default
    }

    /**
     * Notes that a variable has changed.
     */
    public void variableChanged (int timestamp, Logic activator, String name)
    {
        // nothing by default
    }

    /**
     * Notes that the logic has been removed.
     */
    public void removed ()
    {
        // notify the action
        _action.removed();

        // give subclasses a chance to cleanup
        wasRemoved();
    }

    @Override // documentation inherited
    public boolean isActive ()
    {
        return _source.isActive();
    }

    @Override // documentation inherited
    public EntityKey getEntityKey ()
    {
        return _source.getEntityKey();
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

    @Override // documentation inherited
    public void transfer (Logic source, Map<Object, Object> refs)
    {
        super.transfer(source, refs);
        _action.transfer(((HandlerLogic)source)._action, refs);
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
