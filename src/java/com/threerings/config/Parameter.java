//
// $Id$

package com.threerings.config;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

import java.util.ArrayList;

import com.samskivert.util.ListUtil;

import com.threerings.editor.ArgumentPathProperty;
import com.threerings.editor.DynamicallyEditable;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.InvalidPathsException;
import com.threerings.editor.PathProperty;
import com.threerings.editor.Property;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;
import com.threerings.util.Inner;

/**
 * A single configuration parameter.
 */
@EditorTypes({ Parameter.Direct.class, Parameter.Choice.class })
public abstract class Parameter extends DeepObject
    implements Exportable
{
    /**
     * A parameter that directly controls a number of fields identified by paths.
     */
    public static class Direct extends Parameter
    {
        /** The reference paths of the properties that this parameter adjusts.  The first valid
         * path determines the type and default value. */
        @Editable(width=40)
        public String[] paths = new String[0];

        @Override // documentation inherited
        protected Property createProperty (ParameterizedConfig reference)
        {
            try {
                return new PathProperty(reference.getConfigManager(), name, reference, paths);
            } catch (InvalidPathsException e) {
                return null;
            }
        }

        @Override // documentation inherited
        protected Property createArgumentProperty (ParameterizedConfig reference)
        {
            try {
                return new ArgumentPathProperty(
                    reference.getConfigManager(), name, reference, paths);
            } catch (InvalidPathsException e) {
                return null;
            }
        }
    }

    /**
     * A parameter that allows choosing between several options.
     */
    public static class Choice extends Parameter
        implements Inner
    {
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
        public Direct[] directs = new Direct[0];

        /** The options available for selection. */
        @Editable(depends={ "directs" })
        public Option[] options = new Option[0];

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
        public String[] getChoiceOptions ()
        {
            String[] names = new String[options.length];
            for (int ii = 0; ii < options.length; ii++) {
                names[ii] = options[ii].name;
            }
            return names;
        }

        // documentation inherited from interface Inner
        public Object getOuter ()
        {
            return _outer;
        }

        @Override // documentation inherited
        public void invalidateProperties ()
        {
            super.invalidateProperties();
            for (Direct direct : directs) {
                direct.invalidateProperties();
            }
            _optionProperties = null;
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
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
                ArrayList<Property> props = new ArrayList<Property>();
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
                        public Class getType () {
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
        public Class getType () { return null; }
        public Type getGenericType () { return null; }
        public Object get (Object object) { return null; }
        public void set (Object object, Object value) { }
    };
}
