//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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
import java.util.Map;

import com.samskivert.util.SoftCache;

import com.threerings.editor.Editable;
import com.threerings.editor.Property;
import com.threerings.expr.Scope;
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
    public Parameter[] parameters = new Parameter[0];

    /**
     * Returns a reference to the parameter with the supplied name, or <code>null</code> if it
     * doesn't exist.
     */
    public Parameter getParameter (String name)
    {
        return getParameter(parameters, name);
    }

    @Override // documentation inherited
    public ConfigReference<? extends ManagedConfig> getReference ()
    {
        ConfigReference<? extends ManagedConfig> ref = super.getReference();
        if (_args != null) {
            _args.copy(ref.getArguments());
        }
        return ref;
    }

    @Override // documentation inherited
    public ParameterizedConfig getInstance (Scope scope, ArgumentMap args)
    {
        if (args == null || args.isEmpty() || parameters.length == 0) {
            return getBound(scope);
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
                return getBound(scope);
            }
        }
        if (_derived == null) {
            _derived = new SoftCache<ArgumentMap, ParameterizedConfig>(1);
        }
        ParameterizedConfig instance = _derived.get(args);
        if (instance == null) {
            ArgumentMap cargs = args.clone();
            _derived.put(cargs, instance = (ParameterizedConfig)clone());
            instance.init(_cfgmgr);
            instance._base = this;
            instance._args = cargs;
            applyArguments(instance, args);
        }
        return instance.getBound(scope);
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
        if (_derived.getMap().isEmpty()) {
            _derived = null;
        }
    }

    @Override // documentation inherited
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
    protected ParameterizedConfig getBound (Scope scope)
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
    protected transient SoftCache<ArgumentMap, ParameterizedConfig> _derived;
}
