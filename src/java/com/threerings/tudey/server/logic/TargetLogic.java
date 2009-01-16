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
            if (tagged == null) {
                return;
            }
            int size = tagged.size();
            if (size <= config.limit) {
                results.addAll(tagged);
            } else if (config.limit == 1) {
                results.add(RandomUtil.pickRandom(tagged));
            } else {
                results.addAll(CollectionUtil.selectRandomSubset(tagged, config.limit));
            }
        }
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
