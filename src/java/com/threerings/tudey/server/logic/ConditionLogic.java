//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import com.samskivert.util.ListUtil;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ConditionConfig;
import com.threerings.tudey.server.TudeySceneManager;

import static com.threerings.tudey.Log.*;

/**
 * Handles the evaluation of conditions.
 */
public abstract class ConditionLogic extends Logic
{
    /**
     * Evaluates the tagged condition.
     */
    public static class Tagged extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            String tag = ((ConditionConfig.Tagged)_config).tag;
            _target.resolve(activator, _targets);
            try {
                for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                    if (ListUtil.contains(_targets.get(ii).getTags(), tag)) {
                        return true;
                    }
                }
                return false;

            } finally {
                _targets.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((ConditionConfig.Tagged)_config).target, _source);
        }

        /** The target to check. */
        protected TargetLogic _target;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the instance of condition.
     */
    public static class InstanceOf extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            _target.resolve(activator, _targets);
            try {
                for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                    if (_logicClass.isInstance(_targets.get(ii))) {
                        return true;
                    }
                }
                return false;

            } finally {
                _targets.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ConditionConfig.InstanceOf config = (ConditionConfig.InstanceOf)_config;
            try {
                _logicClass = Class.forName(config.logicClass);
            } catch (ClassNotFoundException e) {
                log.warning("Missing logic class for InstanceOf condition.", e);
                _logicClass = Object.class;
            }
            _target = createTarget(config.target, _source);
        }

        /** The test class. */
        protected Class<?> _logicClass;

        /** The target to check. */
        protected TargetLogic _target;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the distance within condition logic.
     */
    public static class DistanceWithin extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            Logic first = resolve(_first, activator);
            Logic second = resolve(_second, activator);
            ConditionConfig.DistanceWithin config = (ConditionConfig.DistanceWithin)_config;
            return first != null && second != null && FloatMath.isWithin(
                first.getTranslation().distance(second.getTranslation()),
                config.minimum, config.maximum);
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ConditionConfig.DistanceWithin config = (ConditionConfig.DistanceWithin)_config;
            _first = createTarget(config.first, _source);
            _second = createTarget(config.second, _source);
        }

        /**
         * Resolves the first target, or returns <code>null</code> for none.
         */
        protected Logic resolve (TargetLogic target, Logic activator)
        {
            target.resolve(activator, _targets);
            try {
                return _targets.isEmpty() ? null : _targets.get(0);
            } finally {
                _targets.clear();
            }
        }

        /** The targets to check. */
        protected TargetLogic _first, _second;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the all condition.
     */
    public static class All extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            for (ConditionLogic condition : _conditions) {
                if (!condition.isSatisfied(activator)) {
                    return false;
                }
            }
            return true;
        }

        @Override // documentation inherited
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
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            for (ConditionLogic condition : _conditions) {
                if (condition.isSatisfied(activator)) {
                    return true;
                }
            }
            return false;
        }

        @Override // documentation inherited
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

    /** The condition configuration. */
    protected ConditionConfig _config;

    /** The action source. */
    protected Logic _source;
}
