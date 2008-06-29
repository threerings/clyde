//
// $Id$

package com.threerings.config;

import java.lang.ref.SoftReference;

import java.util.Iterator;
import java.util.Map;

import com.samskivert.util.SoftCache;

import com.threerings.editor.Editable;
import com.threerings.editor.Property;
import com.threerings.util.DeepOmit;

/**
 * A configuration that may include a number of parameters to be configured when the configuration
 * is referenced.
 */
public class ParameterizedConfig extends ManagedConfig
{
    /** The parameters of the configuration. */
    @Editable(weight=1)
    public Parameter[] parameters = new Parameter[0];

    /**
     * Returns a reference to the config manager to use when resolving references within this
     * config.
     */
    public ConfigManager getConfigManager ()
    {
        return _cfgmgr;
    }

    /**
     * Returns a reference to the parameter with the supplied name, or <code>null</code> if it
     * doesn't exist.
     */
    public Parameter getParameter (String name)
    {
        for (Parameter parameter : parameters) {
            if (parameter.name.equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    @Override // documentation inherited
    public ParameterizedConfig getInstance (ArgumentMap args)
    {
        if (args == null || args.isEmpty() || parameters.length == 0) {
            return this;
        }
        // filter the arguments, removing any non-parameters
        ArgumentMap oargs = args;
        for (String name : args.keySet()) {
            if (getParameter(name) == null) {
                args = new ArgumentMap();
                break;
            }
        }
        if (args != oargs) {
            for (Map.Entry<String, Object> entry : oargs.entrySet()) {
                String name = entry.getKey();
                if (getParameter(name) != null) {
                    args.put(name, entry.getValue());
                }
            }
            if (args.isEmpty()) {
                return this;
            }
        }
        if (_derived == null) {
            _derived = new SoftCache<ArgumentMap, ParameterizedConfig>();
        }
        ParameterizedConfig instance = _derived.get(args);
        if (instance == null) {
            _derived.put((ArgumentMap)args.clone(), instance = (ParameterizedConfig)clone());
            instance.init(_cfgmgr);
            instance._base = this;
            applyArguments(instance, args);
        }
        return instance;
    }

    @Override // documentation inherited
    public void wasUpdated ()
    {
        // invalidate the parameter properties
        for (Parameter parameter : parameters) {
            parameter.invalidateProperties();
        }

        // fire the event
        super.wasUpdated();

        // update the derived instances
        if (_derived == null) {
            return;
        }
        for (Iterator<Map.Entry<ArgumentMap, SoftReference<ParameterizedConfig>>> it =
                _derived.getMap().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ArgumentMap, SoftReference<ParameterizedConfig>> entry = it.next();
            ParameterizedConfig instance = entry.getValue().get();
            if (instance == null) {
                it.remove();
                continue;
            }
            copy(instance);
            applyArguments(instance, entry.getKey());
            instance.wasUpdated();
        }
    }

    @Override // documentation inherited
    public void init (ConfigManager cfgmgr)
    {
        _cfgmgr = cfgmgr;
    }

    /**
     * Applies the arguments in the provided map to the specified instance.
     */
    protected void applyArguments (ParameterizedConfig instance, ArgumentMap args)
    {
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            Parameter param = getParameter(entry.getKey());
            if (param != null) {
                Property prop = param.getProperty(this);
                if (prop != null) {
                    Object value = entry.getValue();
                    if (prop.isLegalValue(value)) {
                        prop.set(instance, value);
                    }
                }
            }
        }
    }

    /** The config manager that we use to resolve references. */
    @DeepOmit
    protected transient ConfigManager _cfgmgr;

    /** The instance from which the configuration is derived, if any (used to prevent the base
     * from being garbage-collected). */
    @DeepOmit
    protected transient ParameterizedConfig _base;

    /** Maps arguments to derived instances. */
    @DeepOmit
    protected transient SoftCache<ArgumentMap, ParameterizedConfig> _derived;
}
