//
// $Id$

package com.threerings.config;

import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.threerings.editor.Editable;
import com.threerings.expr.Scope;

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

    @Override
    protected ManagedConfig getBound (Scope scope)
    {
        // A derived config can never itself be a bound config, because we are final.
        // We use this method to actualize the config.
        assert(java.lang.reflect.Modifier.isFinal(getClass().getModifiers()));

        // if we have no base then jump ship now
        if (base == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        ConfigReference<ManagedConfig> ref = (ConfigReference<ManagedConfig>)base;
        @SuppressWarnings("unchecked")
        Class<ManagedConfig> clazz = (Class<ManagedConfig>)cclass;

        ManagedConfig cfg = _cfgmgr.getConfig(clazz, ref, scope);
        if (cfg != null) {
            // TODO: this value should be cached
            cfg = (ManagedConfig)cfg.clone();
            cfg._cfgmgr = _cfgmgr;
            cfg.setName(getName());
            cfg.setComment(getComment());
            if (cfg instanceof ParameterizedConfig) {
                // translate our parameter paths...
                List<Parameter> params = Lists.newArrayList();
                for (Parameter p : parameters) {
                    // TODO: handle Choice
                    if (p instanceof Parameter.Direct) {
                        Parameter.Direct pDirect = (Parameter.Direct)p;
                        for (String path : pDirect.paths) {
                            if (!path.startsWith("base[\"") || !path.endsWith("\"]")) {
                                throw new RuntimeException("What the hell?");
                            }
                            String name = path.substring(6, path.length() - 2);
                            // find the parameter on actual with that path
                            Parameter actualParam = ParameterizedConfig.getParameter(
                                    ((ParameterizedConfig)cfg).parameters, name);
                            if (actualParam != null) {
                                actualParam.name = p.name;
                                params.add(actualParam);
                            }
                        }
                    } else {
                        throw new RuntimeException("TODO: handle translating " + p.getClass());
                    }
                }
                ((ParameterizedConfig)cfg).parameters = Iterables.toArray(params, Parameter.class);
            }
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
