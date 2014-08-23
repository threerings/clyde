//
// $Id$

package com.threerings.config;

import com.threerings.editor.Editable;

import com.threerings.util.Shallow;

/**
 * Represents a config that is directly derived from another config.
 */
public final class DerivedConfig extends ParameterizedConfig
{
    /** The other configuration from which we derive. */
    @Editable(editor="derivedRef", nullable=true)
    public ConfigReference<? extends ManagedConfig> base;

    /** The config class of the base, that we are actually deriving from. */
    @Shallow
    public transient Class<? extends ManagedConfig> cclass;

    /**
     * Get the config.
     */
    public ManagedConfig getActualConfig (ConfigManager cfgmgr)
    {
        if (base == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        ConfigReference<ManagedConfig> ref = (ConfigReference<ManagedConfig>)base;
        @SuppressWarnings("unchecked")
        Class<ManagedConfig> clazz = (Class<ManagedConfig>)cclass;

        ManagedConfig other = cfgmgr.getRawConfig(clazz, ref.getName());
        if (other == null) {
            return null;
        }
        ManagedConfig actual = (ManagedConfig)other.copy(null);
        actual.init(cfgmgr);
        if (actual instanceof ParameterizedConfig) {
            ((ParameterizedConfig)other).applyArguments(
                    (ParameterizedConfig)actual, ref.getArguments());
            if (actual instanceof DerivedConfig) {
                actual = ((DerivedConfig)actual).getActualConfig(cfgmgr);
            }
            ((ParameterizedConfig)actual).parameters = Parameter.EMPTY_ARRAY;
        }
        actual.setComment(getComment());
        actual.setName(getName());
        return actual;
    }

    @Override
    public void getUpdateReferences (ConfigReferenceSet refs)
    {
        super.getUpdateReferences(refs);

        @SuppressWarnings("unchecked")
        ConfigReference<ManagedConfig> ref = (ConfigReference<ManagedConfig>)base;
        @SuppressWarnings("unchecked")
        Class<ManagedConfig> clazz = (Class<ManagedConfig>)cclass;
        refs.add(clazz, ref);
    }
}
