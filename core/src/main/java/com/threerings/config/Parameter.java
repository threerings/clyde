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

import java.io.PrintStream;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import proguard.annotation.Keep;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ListUtil;

import com.threerings.editor.ArgumentPathProperty;
import com.threerings.editor.DynamicallyEditable;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.InvalidPathsException;
import com.threerings.editor.PathProperty;
import com.threerings.editor.Property;
import com.threerings.editor.TranslatedPathProperty;
import com.threerings.editor.util.Validator;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;
import com.threerings.util.Inner;
import com.threerings.util.ReflectionUtil;

import static com.threerings.ClydeLog.log;

/**
 * A single configuration parameter.
 */
@EditorTypes({ Parameter.Direct.class, Parameter.Choice.class, Parameter.Translated.class })
public abstract class Parameter extends DeepObject
    implements Exportable
{
    /** An empty (and thus immutable and sharable) Parameter array. */
    public static final Parameter[] EMPTY_ARRAY = new Parameter[0];

    /**
     * A parameter that directly controls a number of fields identified by paths.
     */
    public static class Direct extends Parameter
    {
        /** An empty (and thus immutable and sharable) Direct array. */
        public static final Direct[] EMPTY_ARRAY = new Direct[0];

        /** The reference paths of the properties that this parameter adjusts.  The first valid
         * path determines the type and default value. */
        @Editable(width=40, editor="paths")
        public String[] paths = ArrayUtil.EMPTY_STRING;

        @Override
        public boolean validatePaths (Validator validator, ParameterizedConfig reference)
        {
            boolean result = true;
            for (String path : paths) {
                if (!reference.isInvalidParameterPath(path)) {
                    try {
                        new PathProperty(reference.getConfigManager(), name, reference, path);
                        continue; // it must have worked

                    } catch (InvalidPathsException e) {
                        // fall through to below
                    }
                }
                // we only get here on failure:
                validator.output("invalid path: " + path);
                result = false;
            }
            return result;
        }

        @Override
        protected Property createProperty (ParameterizedConfig reference)
        {
            try {
                return new PathProperty(reference.getConfigManager(), name, reference, paths);
            } catch (InvalidPathsException e) {
                return null;
            }
        }

        @Override
        protected Property createArgumentProperty (ParameterizedConfig reference)
        {
            if (paths.length == 0) {
                return null;
            }
            try {
                return new ArgumentPathProperty(
                    reference.getConfigManager(), name, reference, paths[0]);
            } catch (InvalidPathsException e) {
                return null;
            } catch (Exception e) {
                log.warning("Failed to create argument property.", "name", name, e);
                return null;
            }
        }
    }

    /**
     * A parameter that translates its values.
     */
    public static class Translated extends Direct
    {
        /** The translation bundle. */
        @Editable
        public String bundle = "";

        @Override
        protected Property createProperty (ParameterizedConfig reference)
        {
            try {
                return new TranslatedPathProperty(
                        reference.getConfigManager(), name, bundle, reference, paths);
            } catch (InvalidPathsException e) {
                return null;
            }
        }
    }

    /**
     * A marker parameter to indicate that we're from a derived config.
     * This is not a normally-selectable Parameter type, but rather one that a DerivedConfig
     * when it is forced to "show its work" and list the parameters that went into it.
     */
    public static class DerivedConfigParameter extends Direct
    {
        @Editable // see setter
        public String getImportantInfo ()
        {
            return "This isn't a real parameter. This config was created from a derivation.";
        }

        @Editable(constant=true)
        public void setImportantInfo (String s)
        {
            // nada
        }

        @Override
        public String toString ()
        {
            return name + "(derived parameter)";
        }
    }

    /**
     * A parameter that allows choosing between several options.
     */
    public static class Choice extends Parameter
        implements Inner
    {
        /** An empty (and thus immutable and sharable) Option array. */
        public static final Option[] EMPTY_OPTION_ARRAY = new Option[0];

        /**
         * An option available for selection.
         */
        public class Option extends DeepObject
            implements DynamicallyEditable, Exportable
        {
            /** The name of this option. */
            @Editable
            public String name = "";

            /**
             * Applies this option to the specified instance.
             */
            public void apply (ParameterizedConfig instance)
            {
                _outer.applyArguments(instance, _arguments, directs);
            }

            /**
             * Validates the outer object references in this option.
             */
            public void validateOuters (String where, Choice outer)
            {
                if (Choice.this != outer) {
                    ReflectionUtil.setOuter(this, outer);
                    log.warning("Fixed invalid outer reference.",
                        "where", where, "parameter", outer.name, "option", name);
                }
                VALIDATE_ARGS:
                for (Iterator<String> it = _arguments.keySet().iterator(); it.hasNext(); ) {
                    String arg = it.next();
                    for (Direct d : directs) {
                        if (arg.equals(d.name)) {
                            continue VALIDATE_ARGS;
                        }
                    }
                    log.warning("Removing invalid option argument.",
                        "where", where, "parameter", outer.name, "option", name, "arg", arg);
                    it.remove();
                }
            }

            /**
             * Update the arguments: copying something to the new name if it makes sense.
             */
            protected void updateArguments ()
            {
                Set<String> directNames = Sets.newHashSet();
                for (Direct d : directs) {
                    directNames.add(d.name);
                }
                List<String> added = Lists.newArrayList(
                    Sets.difference(directNames, _arguments.keySet()));
                List<String> removed = Lists.newArrayList(
                    Sets.difference(_arguments.keySet(), directNames));
                // If a direct was added at the same time as a direct was removed, it's likely
                // that someone just edited the name: copy the value over to the new name
                if (added.size() == 1 && removed.size() == 1) {
                    _arguments.put(added.get(0), _arguments.remove(removed.get(0)));

                } else {
                    // otherwise, remove all stale values
                    _arguments.keySet().removeAll(removed);
                }
            }

            // documentation inherited from interface DynamicallyEditable
            public Property[] getDynamicProperties ()
            {
                return getOptionProperties();
            }

            /** The arguments for this option. */
            protected ArgumentMap _arguments = new ArgumentMap();
        }

        /** The direct controls for each option. */
        @Editable
        public Direct[] directs = Direct.EMPTY_ARRAY;

        /** The options available for selection. */
        @Editable(depends={ "directs" })
        public Option[] options = EMPTY_OPTION_ARRAY;

        /** The selected option. */
        @Editable(editor="choice", depends={ "options" })
        public String choice;

        public Choice (ParameterizedConfig outer)
        {
            _outer = outer;
        }

        /**
         * Returns the names of the options available for selection.
         */
        @Keep
        public String[] getChoiceOptions ()
        {
            String[] names = new String[options.length];
            for (int ii = 0; ii < options.length; ii++) {
                names[ii] = options[ii].name;
            }
            return names;
        }

        // documentation inherited from interface Inner
        public void setOuter (Object outer)
        {
            _outer = (ParameterizedConfig)outer;
        }

        // documentation inherited from interface Inner
        public Object getOuter ()
        {
            return _outer;
        }

        @Override
        public void invalidateProperties ()
        {
            super.invalidateProperties();
            for (Direct direct : directs) {
                direct.invalidateProperties();
            }
            for (Option option : options) {
                option.updateArguments();
            }
            _optionProperties = null;
        }

        @Override
        public boolean validatePaths (Validator validator, ParameterizedConfig reference)
        {
            boolean result = true;
            for (Direct direct : directs) {
                result &= direct.validatePaths(validator, reference);
            }
            return result;
        }

        @Override
        public void validateOuters (String where, ParameterizedConfig outer)
        {
            if (_outer != outer) {
                _outer = outer;
                log.warning("Fixed invalid outer reference.", "where", where, "parameter", name);
            }
            for (Option option : options) {
                option.validateOuters(where, this);
            }
        }

        @Override
        protected Property createProperty (ParameterizedConfig reference)
        {
            int idx = ListUtil.indexOfRef(reference.parameters, this);
            if (idx == -1) {
                return null;
            }
            try {
                return new PathProperty(
                    reference.getConfigManager(), name, reference,
                    "parameters[" + idx + "].choice") {
                    public void set (Object object, Object value) {
                        super.set(object, value);
                        Option option = getOption((String)value);
                        if (option != null) {
                            option.apply((ParameterizedConfig)object);
                        }
                    }
                };
            } catch (InvalidPathsException e) {
                return null;
            }
        }

        @Override
        protected Property createArgumentProperty (ParameterizedConfig reference)
        {
            int idx = ListUtil.indexOfRef(reference.parameters, this);
            if (idx == -1) {
                return null;
            }
            try {
                return new ArgumentPathProperty(
                    reference.getConfigManager(), name, reference,
                    "parameters[" + idx + "].choice");
            } catch (InvalidPathsException e) {
                return null;
            } catch (Exception e) {
                log.warning("Failed to create argument property.", "name", name, e);
                return null;
            }
        }

        /**
         * Retrieves an option by name.
         */
        protected Option getOption (String name)
        {
            for (Option option : options) {
                if (option.name.equals(name)) {
                    return option;
                }
            }
            return null;
        }

        /**
         * Returns the array of option argument properties.
         */
        protected Property[] getOptionProperties ()
        {
            if (_optionProperties == null) {
                ArrayList<Property> props = new ArrayList<Property>(directs.length);
                for (Direct direct : directs) {
                    final Property aprop = direct.getArgumentProperty(_outer);
                    if (aprop == null) {
                        continue;
                    }
                    props.add(new Property() { {
                            _name = aprop.getName();
                        }
                        public Member getMember () {
                            return aprop.getMember();
                        }
                        public Object getMemberObject (Object object) {
                            return aprop.getMemberObject(object);
                        }
                        public Class<?> getType () {
                            return aprop.getType();
                        }
                        public Type getGenericType () {
                            return aprop.getGenericType();
                        }
                        public Object get (Object object) {
                            return aprop.get(((Option)object)._arguments);
                        }
                        public void set (Object object, Object value) {
                            aprop.set(((Option)object)._arguments, value);
                        }
                    });
                }
                _optionProperties = props.toArray(new Property[props.size()]);
            }
            return _optionProperties;
        }

        /** The outer config reference. */
        @DeepOmit
        protected transient ParameterizedConfig _outer;

        /** The cached option properties. */
        @DeepOmit
        protected transient Property[] _optionProperties;
    }

    /** The name of the parameter. */
    @Editable
    public String name = "";

    /**
     * Retrieves the property used to apply this parameter.
     *
     * @param reference the configuration to use as a reference to resolve paths.
     * @return the property, or <code>null</code> if the parameter is invalid.
     */
    public Property getProperty (ParameterizedConfig reference)
    {
        if (_property == INVALID_PROPERTY) {
            _property = createProperty(reference);
        }
        return _property;
    }

    /**
     * Retrieves the property used to set and retrieve the argument corresponding to this
     * property.  The property's {@link Property#get} and {@link Property#set} methods will
     * expect a {@link java.util.Map} instance with keys representing the parameter names
     * and values representing the argument values.  Retrieving the property value will
     * return the value in the map (or, if absent, the default value obtained from the
     * reference object) and setting the value will set the value in the map (unless it is
     * equal to the default, in which case it will be removed).
     *
     * @param reference the configuration to use as a reference to resolve paths and obtain
     * default values.
     * @return the property, or <code>null</code> if the parameter is invalid.
     */
    public Property getArgumentProperty (ParameterizedConfig reference)
    {
        if (_argumentProperty == INVALID_PROPERTY) {
            _argumentProperty = createArgumentProperty(reference);
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

    /**
     * Validates the parameter's paths.
     *
     * @return true if the paths are valid.
     */
    public abstract boolean validatePaths (Validator validator, ParameterizedConfig reference);

    /**
     * Validates the parameter's outer object references.
     */
    public void validateOuters (String where, ParameterizedConfig outer)
    {
        // nothing by default
    }

    /**
     * Creates the property used to apply this parameter.
     */
    protected abstract Property createProperty (ParameterizedConfig reference);

    /**
     * Creates the property used to set and retrieve the argument corresponding to this
     * property.
     */
    protected abstract Property createArgumentProperty (ParameterizedConfig reference);

    /** The property corresponding to this parameter. */
    @DeepOmit
    protected transient Property _property = INVALID_PROPERTY;

    /** The argument property corresponding to this parameter. */
    @DeepOmit
    protected transient Property _argumentProperty = INVALID_PROPERTY;

    /** Indicates that a property field is invalid and should be (re)created. */
    protected static final Property INVALID_PROPERTY = new Property() {
        public Member getMember () { return null; }
        public Class<?> getType () { return null; }
        public Type getGenericType () { return null; }
        public Object get (Object object) { return null; }
        public void set (Object object, Object value) { }
    };
}
