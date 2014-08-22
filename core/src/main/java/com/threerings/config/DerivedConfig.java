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
        @SuppressWarnings("unchecked")
        ConfigReference<ManagedConfig> ref = (ConfigReference<ManagedConfig>)base;
        @SuppressWarnings("unchecked")
        Class<ManagedConfig> clazz = (Class<ManagedConfig>)cclass;

        ManagedConfig cfg = cfgmgr.getConfig(clazz, ref);
        if (cfg != null) {
            // TODO: this is super dangerous because of the way that ParameterizedConfig caches
            cfg.setName(getName());
        }
        return cfg;
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
