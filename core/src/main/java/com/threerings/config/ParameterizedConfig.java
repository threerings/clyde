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

package com.threerings.config;

import java.lang.ref.SoftReference;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import com.threerings.editor.Editable;
import com.threerings.editor.Property;
import com.threerings.editor.util.Validator;
import com.threerings.expr.Scope;
import com.threerings.util.CacheUtil;
import com.threerings.util.DeepOmit;
import com.threerings.util.DeepUtil;

/**
 * A configuration that may include a number of parameters to be configured when the configuration
 * is referenced.
 */
public class ParameterizedConfig extends ManagedConfig
{
    /** The parameters of the configuration. */
    @Editable(weight=1)
    public Parameter[] parameters = Parameter.EMPTY_ARRAY;

    /**
     * Returns a reference to the parameter with the supplied name, or <code>null</code> if it
     * doesn't exist.
     */
    public Parameter getParameter (String name)
    {
        return getParameter(parameters, name);
    }

    @Override
    public ConfigReference<? extends ManagedConfig> getReference ()
    {
        ConfigReference<? extends ManagedConfig> ref = super.getReference();
        if (_args != null) {
            _args.copy(ref.getArguments());
        }
        return ref;
    }

    @Override
    public ManagedConfig getInstance (Scope scope, ArgumentMap args)
    {
        if (args == null || args.isEmpty() || parameters.length == 0) {
            return getBound(scope);
        }
        // filter the arguments, removing any non-parameters
        ArgumentMap filteredArgs = args;
        ArgumentMap derivedArgs = null;
        for (String name : args.keySet()) {
            if (getParameter(name) == null) {
                // We found an argument with no corresponding parameter: make a new args map.
                // Normally derivedArgs will be a clone, but we don't a clone of a fresh copy
                filteredArgs = derivedArgs = new ArgumentMap();
                for (Map.Entry<String, Object> entry : args.entrySet()) {
                    name = entry.getKey();
                    if (getParameter(name) != null) {
                        filteredArgs.put(name, entry.getValue());
                    }
                }
                if (filteredArgs.isEmpty()) {
                    return getBound(scope);
                }
                break;
            }
        }
        if (_derived == null) {
            _derived = CacheUtil.softValues(1);
        }
        ParameterizedConfig instance = _derived.get(filteredArgs);
        if (instance == null) {
            if (derivedArgs == null) {
                derivedArgs = filteredArgs.clone();
            }
            _derived.put(derivedArgs, instance = (ParameterizedConfig)clone());
            instance.init(_cfgmgr);
            instance._base = this;
            instance._args = derivedArgs;
            applyArguments(instance, derivedArgs);
        }
        return instance.getBound(scope);
    }

    @Override
    public void wasUpdated ()
    {
        // invalidate the parameter properties
        for (Parameter parameter : parameters) {
            parameter.invalidateProperties();
        }

        // fire the event
        super.wasUpdated();

        // update derived instances
        if (_derived != null) {
            for (Map.Entry<ArgumentMap, ParameterizedConfig> entry : _derived.entrySet()) {
                ParameterizedConfig instance = entry.getValue();
                copy(instance);
                applyArguments(instance, entry.getKey());
                instance.wasUpdated();
            }
            if (_derived.isEmpty()) {
                _derived = null;
            }
        }
    }

    /**
     * Is the specified parameter path blacklisted for this config?
     * If this method returns false that is no guarantee that the path is valid.
     */
    public boolean isInvalidParameterPath (String path)
    {
        // let's throw NPE if possible, and everything's valid except for 'comment'.
        // You can't parameterize the comment or the type (if using DerivedConfig).
        return path.equals("comment") || path.equals("config_type");
    }

    @Override
    public boolean validateReferences (Validator validator)
    {
        boolean result = super.validateReferences(validator);
        // validate the parameter paths, too
        for (Parameter parameter : parameters) {
            validator.pushWhere(parameter.name);
            try {
                result &= parameter.validatePaths(validator, this);
            } finally {
                validator.popWhere();
            }
        }
        return result;
    }

    @Override
    public void validateOuters (String where)
    {
        for (Parameter parameter : parameters) {
            parameter.validateOuters(where, this);
        }
    }

    @Override
    protected void maybeFireOnConfigManager ()
    {
        // only fire for the base config
        if (_base == null) {
            super.maybeFireOnConfigManager();
        }
    }

    /**
     * Returns an instance of this config bound in the specified scope.
     */
    protected ManagedConfig getBound (Scope scope)
    {
        return this;
    }

    /**
     * Applies the arguments in the provided map to the specified instance.
     */
    protected void applyArguments (ParameterizedConfig instance, ArgumentMap args)
    {
        applyArguments(instance, args, parameters);
    }

    /**
     * Applies the arguments in the provided map to the specified instance.
     */
    protected void applyArguments (
        ParameterizedConfig instance, ArgumentMap args, Parameter[] params)
    {
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            Parameter param = getParameter(params, entry.getKey());
            if (param != null) {
                Property prop = param.getProperty(this);
                if (prop != null) {
                    Object value = entry.getValue();
                    if (prop.isLegalValue(value)) {
                        prop.set(instance, DeepUtil.copy(value));
                    }
                }
            }
        }
    }

    /**
     * Returns a reference to the parameter with the supplied name, or <code>null</code> if it
     * doesn't exist.
     */
    protected static Parameter getParameter (Parameter[] params, String name)
    {
        for (Parameter param : params) {
            if (param.name.equals(name)) {
                return param;
            }
        }
        return null;
    }

    /** The instance from which the configuration is derived, if any (used to prevent the base
     * from being garbage-collected). */
    @DeepOmit
    protected transient ParameterizedConfig _base;

    /** The arguments applied to the configuration, if any. */
    @DeepOmit
    protected transient ArgumentMap _args;

    /** Maps arguments to derived instances. */
    @DeepOmit
    protected transient Map<ArgumentMap, ParameterizedConfig> _derived;
}
