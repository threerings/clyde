//
// $Id$

package com.threerings.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.threerings.editor.ArgumentPathProperty;
import com.threerings.editor.Editable;
import com.threerings.editor.InvalidPathsException;
import com.threerings.editor.PathProperty;
import com.threerings.editor.Property;
import com.threerings.editor.util.EditorContext;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

/**
 * A single configuration parameter.
 */
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

        public Direct (Parameter other)
        {
            name = other.name;
        }

        public Direct ()
        {
        }

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
     * A parameter that allows the user to choose between a number of options.
     */
    public static class Choice extends Parameter
    {
        /** The selectable options. */
        @Editable(nullable=false)
        public Option[] options = new Option[0];

        public Choice (Parameter other)
        {
            name = other.name;
        }

        public Choice ()
        {
        }

        @Override // documentation inherited
        protected Property createProperty (ParameterizedConfig reference)
        {
            return null;
        }

        @Override // documentation inherited
        protected Property createArgumentProperty (ParameterizedConfig reference)
        {
            return null;
        }
    }

    /**
     * A single selectable option.
     */
    public static class Option extends DeepObject
        implements Exportable
    {
        /** The name of the option. */
        @Editable
        public String name = "";
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { Direct.class, Choice.class };
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
        public Class getType () { return null; }
        public Type getGenericType () { return null; }
        public <T extends Annotation> T getAnnotation (Class<T> clazz) { return null; }
        public Object get (Object object) { return null; }
        public void set (Object object, Object value) { }
    };
}
