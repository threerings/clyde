//
// $Id$

package com.threerings.config;

import java.lang.annotation.Annotation;

import java.lang.ref.SoftReference;
import java.lang.reflect.Type;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.samskivert.util.SoftCache;

import com.threerings.editor.Editable;
import com.threerings.editor.ArgumentPathProperty;
import com.threerings.editor.InvalidPathsException;
import com.threerings.editor.PathProperty;
import com.threerings.editor.Property;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

/**
 * A configuration that may include a number of parameters to be configured when the configuration
 * is referenced.
 */
public class ParameterizedConfig extends ManagedConfig
{
    /**
     * A single configuration parameter.
     */
    public static class Parameter extends DeepObject
        implements Exportable
    {
        /** The name of the parameter. */
        @Editable
        public String name = "";

        /** The reference paths of the properties that this parameter adjusts.  The first valid
         * path determines the type and default value. */
        @Editable(width=40)
        public String[] paths = new String[0];

        /**
         * Creates the property used to apply this parameter.
         *
         * @param reference the configuration to use as a reference to resolve paths.
         * @return the property, or <code>null</code> if none of the paths are valid.
         */
        public Property getProperty (ManagedConfig reference)
        {
            if (_property == INVALID_PROPERTY) {
                try {
                    _property = new PathProperty(name, reference, paths);
                } catch (InvalidPathsException e) {
                    _property = null;
                }
            }
            return _property;
        }

        /**
         * Creates the property used to set and retrieve the argument corresponding to this
         * property.  The property's {@link Property#get} and {@link Property#set} methods will
         * expect a {@link java.util.Map} instance with keys representing the parameter names
         * and values representing the argument values.  Retrieving the property value will
         * return the value in the map (or, if absent, the default value obtained from the
         * reference object) and setting the value will set the value in the map (unless it is
         * equal to the default, in which case it will be removed).
         *
         * @param reference the configuration to use as a reference to resolve paths and obtain
         * default values.
         * @return the property, or <code>null</code> if none of the paths are valid.
         */
        public Property getArgumentProperty (ManagedConfig reference)
        {
            if (_argumentProperty == INVALID_PROPERTY) {
                try {
                    _argumentProperty = new ArgumentPathProperty(name, reference, paths);
                } catch (InvalidPathsException e) {
                    _argumentProperty = null;
                }
            }
            return _argumentProperty;
        }

        /**
         * Invalidates the properties, forcing them to be recreated.
         */
        public void invalidateProperties ()
        {
            _property = _argumentProperty = INVALID_PROPERTY;
        }

        /** The property corresponding to this parameter. */
        @DeepOmit
        protected transient Property _property = INVALID_PROPERTY;

        /** The argument property corresponding to this parameter. */
        @DeepOmit
        protected transient Property _argumentProperty = INVALID_PROPERTY;
    }

    /** The parameters of the configuration. */
    @Editable(weight=1, nullable=false)
    public Parameter[] parameters = new Parameter[0];

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

    /**
     * Returns a reference to the parameter with the supplied name, or <code>null</code> if it
     * doesn't exist.
     */
    protected Parameter getParameter (String name)
    {
        for (Parameter parameter : parameters) {
            if (parameter.name.equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    /** The instance from which the configuration is derived, if any (used to prevent the base
     * from being garbage-collected). */
    @DeepOmit
    protected transient ParameterizedConfig _base;

    /** Maps arguments to derived instances. */
    @DeepOmit
    protected transient SoftCache<ArgumentMap, ParameterizedConfig> _derived;

    /** Indicates that a property field is invalid and should be (re)created. */
    protected static final Property INVALID_PROPERTY = new Property() {
        public Class getType () { return null; }
        public Type getGenericType () { return null; }
        public <T extends Annotation> T getAnnotation (Class<T> clazz) { return null; }
        public Object get (Object object) { return null; }
        public void set (Object object, Object value) { }
    };
}
