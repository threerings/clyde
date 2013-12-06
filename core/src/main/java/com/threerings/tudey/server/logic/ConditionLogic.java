//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
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

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.Lists;

import com.samskivert.util.ListUtil;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.config.ConditionConfig;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;

import static com.threerings.tudey.Log.log;

/**
 * Handles the evaluation of conditions.
 */
public abstract class ConditionLogic extends Logic
{
    /**
     * Simple base class for conditions with targets.
     */
    public static abstract class Targeted extends ConditionLogic
    {
        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _target.transfer(((Targeted)source)._target, refs);
        }

        /** The target logic. */
        protected TargetLogic _target;
    }

    /**
     * Evaluates the tagged condition.
     */
    public static class Tagged extends Targeted
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            ConditionConfig.Tagged config = (ConditionConfig.Tagged)_config;
            _target.resolve(activator, _targets);
            try {
                for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                    if (ListUtil.contains(_targets.get(ii).getTags(), config.tag) != config.all) {
                        return !config.all;
                    }
                }
                return config.all;

            } finally {
                _targets.clear();
            }
        }

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ConditionConfig.Tagged)_config).target, _source);
        }

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the instance of condition.
     */
    public static class InstanceOf extends Targeted
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            boolean all = ((ConditionConfig.InstanceOf)_config).all;
            _target.resolve(activator, _targets);
            try {
                for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                    if (_logicClass.isInstance(_targets.get(ii)) != all) {
                        return !all;
                    }
                }
                return all;

            } finally {
                _targets.clear();
            }
        }

        @Override
        protected void didInit ()
        {
            ConditionConfig.InstanceOf config = (ConditionConfig.InstanceOf)_config;
            try {
                _logicClass = Class.forName(config.logicClass);
            } catch (ClassNotFoundException e) {
                log.warning("Missing logic class for InstanceOf condition.", e);
                _logicClass = Logic.class;
            }
            _target = createTarget(config.target, _source);
        }

        /** The test class. */
        protected Class<?> _logicClass;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the intersect condition logic.
     */
    public static class Intersecting extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            boolean all = ((ConditionConfig.Intersecting)_config).allFirst;
            _first.resolve(activator, _firsts);
            _second.resolve(activator, _seconds);
            try {
                for (int ii = 0, nn = _firsts.size(); ii < nn; ii++) {
                    if (intersectsSecond(_firsts.get(ii)) != all) {
                        return !all;
                    }
                }
                return all;

            } finally {
                _firsts.clear();
                _seconds.clear();
            }
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            Intersecting isource = (Intersecting)source;
            _first.transfer(isource._first, refs);
            _second.transfer(isource._second, refs);
        }

        @Override
        protected void didInit ()
        {
            ConditionConfig.Intersecting config = (ConditionConfig.Intersecting)_config;
            _first = createRegion(config.first, _source);
            _second = createRegion(config.second, _source);
        }

        /**
         * Determines whether the specified shape from the first target satisfies the intersection
         * condition.
         */
        protected boolean intersectsSecond (Shape shape)
        {
            boolean all = ((ConditionConfig.Intersecting)_config).allSecond;
            for (int ii = 0, nn = _seconds.size(); ii < nn; ii++) {
                if (shape.intersects(_seconds.get(ii)) != all) {
                    return !all;
                }
            }
            return all;
        }

        /** The regions to check. */
        protected RegionLogic _first, _second;

        /** Holds shapes during evaluation. */
        protected ArrayList<Shape> _firsts = Lists.newArrayList(), _seconds = Lists.newArrayList();
    }

    /**
     * Evaluates the intersects scene condition logic.
     */
    public static class IntersectsScene extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            _region.resolve(activator, _shapes);
            try {
                for (Shape shape : _shapes) {
                    if (_scenemgr.collides(
                                ((ConditionConfig.IntersectsScene)_config).collisionMask, shape)) {
                        return true;
                    }
                }
                return false;

            } finally {
                _shapes.clear();
            }
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            _region.transfer(((IntersectsScene)source)._region, refs);
        }

        @Override
        protected void didInit ()
        {
            _region = createRegion(((ConditionConfig.IntersectsScene)_config).region, _source);
        }

        /** The region to check. */
        protected RegionLogic _region;

        /** Holds shapes during evaluation. */
        protected ArrayList<Shape> _shapes = Lists.newArrayList();
    }

    /**
     * Evaluates the distance within condition logic.
     */
    public static class DistanceWithin extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            boolean all = ((ConditionConfig.DistanceWithin)_config).allFirst;
            _first.resolve(activator, _firsts);
            _second.resolve(activator, _seconds);
            try {
                for (int ii = 0, nn = _firsts.size(); ii < nn; ii++) {
                    if (withinSecond(_firsts.get(ii).getTranslation()) != all) {
                        return !all;
                    }
                }
                return all;

            } finally {
                _firsts.clear();
                _seconds.clear();
            }
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            DistanceWithin dsource = (DistanceWithin)source;
            _first.transfer(dsource._first, refs);
            _second.transfer(dsource._second, refs);
        }

        @Override
        protected void didInit ()
        {
            ConditionConfig.DistanceWithin config = (ConditionConfig.DistanceWithin)_config;
            _first = createTarget(config.first, _source);
            _second = createTarget(config.second, _source);
        }

        /**
         * Determines whether the specified shape from the first target satisfies the distance
         * condition.
         */
        protected boolean withinSecond (Vector2f t1)
        {
            ConditionConfig.DistanceWithin config = (ConditionConfig.DistanceWithin)_config;
            for (int ii = 0, nn = _seconds.size(); ii < nn; ii++) {
                Vector2f t2 = _seconds.get(ii).getTranslation();
                if (FloatMath.isWithin(t1.distance(t2), config.minimum, config.maximum) !=
                        config.allSecond) {
                    return !config.allSecond;
                }
            }
            return config.allSecond;
        }

        /** The targets to check. */
        protected TargetLogic _first, _second;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _firsts = Lists.newArrayList(), _seconds = Lists.newArrayList();
    }

    /**
     * Evaluates the random condition.
     */
    public static class Random extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            return FloatMath.random() < ((ConditionConfig.Random)_config).probability;
        }
    }

    /**
     * Evaluates the limit condition.
     */
    public static class Limit extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            if (_limit > 0) {
                _limit--;
                return true;
            }
            return false;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _limit = ((Limit)source)._limit;
        }

        @Override
        protected void didInit ()
        {
            _limit = ((ConditionConfig.Limit)_config).limit;
        }

        /** The remaining limit. */
        protected int _limit;
    }

    /**
     * Evaluates the all condition.
     */
    public static class All extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            for (ConditionLogic condition : _conditions) {
                if (!condition.isSatisfied(activator)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            ConditionLogic[] sconditions = ((All)source)._conditions;
            for (int ii = 0; ii < _conditions.length; ii++) {
                _conditions[ii].transfer(sconditions[ii], refs);
            }
        }

        @Override
        protected void didInit ()
        {
            ArrayList<ConditionLogic> list = Lists.newArrayList();
            for (ConditionConfig config : ((ConditionConfig.All)_config).conditions) {
                ConditionLogic condition = createCondition(config, _source);
                if (condition != null) {
                    list.add(condition);
                }
            }
            _conditions = list.toArray(new ConditionLogic[list.size()]);
        }

        /** The component conditions. */
        protected ConditionLogic[] _conditions;
    }

    /**
     * Evaluates the any condition.
     */
    public static class Any extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            for (ConditionLogic condition : _conditions) {
                if (condition.isSatisfied(activator)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);

            ConditionLogic[] sconditions = ((Any)source)._conditions;
            for (int ii = 0; ii < _conditions.length; ii++) {
                _conditions[ii].transfer(sconditions[ii], refs);
            }
        }

        @Override
        protected void didInit ()
        {
            ArrayList<ConditionLogic> list = Lists.newArrayList();
            for (ConditionConfig config : ((ConditionConfig.Any)_config).conditions) {
                ConditionLogic condition = createCondition(config, _source);
                if (condition != null) {
                    list.add(condition);
                }
            }
            _conditions = list.toArray(new ConditionLogic[list.size()]);
        }

        /** The component conditions. */
        protected ConditionLogic[] _conditions;
    }

    /**
     * Evaluates the flag set condition.
     */
    public static class FlagSet extends Targeted
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            ConditionConfig.FlagSet config = (ConditionConfig.FlagSet)_config;
            _target.resolve(activator, _targets);
            try {
                for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                    Logic logic = _targets.get(ii);
                    if (logic instanceof ActorLogic) {
                        Actor actor = ((ActorLogic)logic).getActor();
                        try {
                            Field flag = actor.getClass().getField(config.flagName);
                            if (actor.isSet(flag.getInt(actor))) {
                                return config.set;
                            }
                        } catch (NoSuchFieldException e) {
                            log.warning("Flag field not found in class for Flag Set Condition.", e);
                        } catch (IllegalAccessException e) {
                            log.warning("Cannot access flag field for Flag Set Condition.", e);
                        }
                    }
                }
                return !config.set;

            } finally {
                _targets.clear();
            }
        }

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ConditionConfig.FlagSet)_config).target, _source);
        }

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the cooldown condition.
     */
    public static class Cooldown extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            int timestamp = _scenemgr.getTimestamp();
            if (timestamp > _nextTimestamp) {
                _nextTimestamp = timestamp + ((ConditionConfig.Cooldown)_config).time;
                return true;
            }
            return false;
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _nextTimestamp = ((Cooldown)source)._nextTimestamp;
        }

        /** The next timestamp before we'll be satisfied. */
        protected int _nextTimestamp = -1;
    }

    /**
     * Evaluates the not condition.
     */
    public static class Not extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            return !_condition.isSatisfied(activator);
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _condition.transfer(((Not)source)._condition, refs);
        }

        @Override
        protected void didInit ()
        {
            _condition = createCondition(((ConditionConfig.Not)_config).condition, _source);
        }

        /** The component condition. */
        protected ConditionLogic _condition;
    }

    /**
     * Evaluates the always condition.
     */
    public static class Always extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            return true;
        }
    }

    /**
     * Evaluates the evaluate condition.
     */
    public static class Evaluate extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            return ExpressionLogic.coerceToBoolean(_expression.evaluate(activator, null));
        }

        @Override
        protected void didInit ()
        {
            _expression = createExpression(
                ((ConditionConfig.Evaluate)_config).expression, _source);
        }

        /** The expression to evaluate. */
        protected ExpressionLogic _expression;
    }

    /**
     * Evaluates the action condition.
     */
    public static class Action extends ConditionLogic
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            return _action.execute(_scenemgr.getTimestamp(), activator);
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _action.transfer(((Action)source)._action, refs);
        }

        @Override
        protected void didInit ()
        {
            _action = createAction(((ConditionConfig.Action)_config).action, _source);
        }

        // TODO: a way to call removed() on the action

        /** The action to execute. */
        protected ActionLogic _action;
    }

    /**
     * Evaluates the is condition.
     */
    public static class Is extends Targeted
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            _sourceTarget.resolve(activator, _targets);
            Logic sourceTarget = _targets.isEmpty() ? null : _targets.get(0);
            _targets.clear();
            if (sourceTarget == null) {
                return false;
            }
            ConditionConfig.Is config = (ConditionConfig.Is)_config;
            _target.resolve(activator, _targets);
            try {
                for (Logic target : _targets) {
                    if ((target == sourceTarget) != config.all) {
                        return !config.all;
                    }
                }
                return config.all;

            } finally {
                _targets.clear();
            }
        }

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ConditionConfig.Is)_config).target, _source);
            _sourceTarget = createTarget(((ConditionConfig.Is)_config).source, _source);
        }

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();

        /** The source target logic. */
        protected TargetLogic _sourceTarget;
    }

    /**
     * Evaluates the date range condition.
     */
    public static class DateRange extends Targeted
    {
        @Override
        public boolean isSatisfied (Logic activator)
        {
            ConditionConfig.DateRange config = (ConditionConfig.DateRange)_config;
            long now = System.currentTimeMillis();
            // either endpoint can be null to be open-ended
            return ((config.start == null) || (now >= config.start)) &&
                ((config.end == null) || (now <= config.end));
        }
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, ConditionConfig config, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _source = source;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Determines whether the condition is satisfied.
     *
     * @param activator the entity that triggered the action.
     */
    public abstract boolean isSatisfied (Logic activator);

    @Override
    public boolean isActive ()
    {
        return _source.isActive();
    }

    @Override
    public EntityKey getEntityKey ()
    {
        return _source.getEntityKey();
    }

    @Override
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override
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

    /** The condition configuration. */
    protected ConditionConfig _config;

    /** The action source. */
    protected Logic _source;
}
