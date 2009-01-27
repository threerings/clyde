//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.Lists;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.math.Vector2f;

import com.threerings.tudey.config.TargetConfig;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Handles the resolution of targets.
 */
public abstract class TargetLogic extends Logic
{
    /**
     * Refers to the action source.
     */
    public static class Source extends TargetLogic
    {
        @Override // documentation inherited
        public void resolve (Logic activator, Collection<Logic> results)
        {
            results.add(_source);
        }
    }

    /**
     * Refers to the actor that triggered the action.
     */
    public static class Activator extends TargetLogic
    {
        @Override // documentation inherited
        public void resolve (Logic activator, Collection<Logic> results)
        {
            results.add(activator);
        }
    }

    /**
     * Refers to an entity or entities bearing a certain tag.
     */
    public static class Tagged extends TargetLogic
    {
        @Override // documentation inherited
        public void resolve (Logic activator, Collection<Logic> results)
        {
            TargetConfig.Tagged config = (TargetConfig.Tagged)_config;
            ArrayList<Logic> tagged = _scenemgr.getTagged(config.tag);
            if (tagged != null) {
                results.addAll(tagged);
            }
        }
    }

    /**
     * Refers to the entities intersecting a reference entity.
     */
    public static class Intersecting extends TargetLogic
    {
        @Override // documentation inherited
        public void resolve (Logic activator, Collection<Logic> results)
        {
            TargetConfig.Intersecting config = (TargetConfig.Intersecting)_config;
            if (config.actors) {
            }
            if (config.entries) {
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _region = createTarget(((TargetConfig.Intersecting)_config).region, _source);
        }

        /** The target defining the region of interest. */
        protected TargetLogic _region;
    }

    /**
     * Base class for targets limited to a sized subset.
     */
    public static abstract class Subset extends TargetLogic
    {
        @Override // documentation inherited
        public void resolve (Logic activator, Collection<Logic> results)
        {
            _target.resolve(activator, _targets);
            selectSubset(((TargetConfig.Subset)_config).size, results);
            _targets.clear();
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((TargetConfig.Subset)_config).target, _source);
        }

        /**
         * Selects a subset of the specified size and places the objects in the results.
         */
        protected abstract void selectSubset (int size, Collection<Logic> results);

        /** The contained target. */
        protected TargetLogic _target;

        /** Holds the targets during processing. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Limits targets to a random subset.
     */
    public static class RandomSubset extends Subset
    {
        @Override // documentation inherited
        protected void selectSubset (int size, Collection<Logic> results)
        {
            int ntargets = _targets.size();
            if (ntargets <= size) {
                results.addAll(_targets);
            } else if (size == 1) {
                results.add(RandomUtil.pickRandom(_targets));
            } else {
                results.addAll(CollectionUtil.selectRandomSubset(_targets, size));
            }
        }
    }

    /**
     * Limits targets to the nearest subset.
     */
    public static class NearestSubset extends Subset
    {
        @Override // documentation inherited
        protected void didInit ()
        {
            super.didInit();
            _location = createTarget(((TargetConfig.NearestSubset)_config).location, _source);
        }

        @Override // documentation inherited
        protected void selectSubset (int size, Collection<Logic> results)
        {
        }

        /** The reference location. */
        protected TargetLogic _location;
    }

    /**
     * Limits targets to the farthest subset.
     */
    public static class FarthestSubset extends Subset
    {
        @Override // documentation inherited
        protected void didInit ()
        {
            super.didInit();
            _location = createTarget(((TargetConfig.FarthestSubset)_config).location, _source);
        }

        @Override // documentation inherited
        protected void selectSubset (int size, Collection<Logic> results)
        {
        }

        /** The reference location. */
        protected TargetLogic _location;
    }

    /**
     * Limits targets to those satisfying a condition.
     */
    public static class Conditional extends TargetLogic
    {
        @Override // documentation inherited
        public void resolve (Logic activator, Collection<Logic> results)
        {
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Logic target = _targets.get(ii);
                if (_condition.isSatisfied(target)) {
                    results.add(target);
                }
            }
            _targets.clear();
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            TargetConfig.Conditional config = (TargetConfig.Conditional)_config;
            _condition = createCondition(config.condition, _source);
            _target = createTarget(config.target, _source);
        }

        /** The condition to evaluate for each potential target. */
        protected ConditionLogic _condition;

        /** The contained target. */
        protected TargetLogic _target;

        /** Holds the targets during processing. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Refers to multiple targets.
     */
    public static class Compound extends TargetLogic
    {
        @Override // documentation inherited
        public void resolve (Logic activator, Collection<Logic> results)
        {
            for (TargetLogic target : _targets) {
                target.resolve(activator, results);
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ArrayList<TargetLogic> list = Lists.newArrayList();
            for (TargetConfig config : ((TargetConfig.Compound)_config).targets) {
                TargetLogic target = createTarget(config, _source);
                if (target != null) {
                    list.add(target);
                }
            }
            _targets = list.toArray(new TargetLogic[list.size()]);
        }

        /** The component targets. */
        protected TargetLogic[] _targets;
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, TargetConfig config, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _source = source;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Resolves the list of targets, placing the results in the supplied collection.
     *
     * @param activator the entity that triggered the action.
     */
    public abstract void resolve (Logic activator, Collection<Logic> results);

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

    /** The target configuration. */
    protected TargetConfig _config;

    /** The action source. */
    protected Logic _source;
}
