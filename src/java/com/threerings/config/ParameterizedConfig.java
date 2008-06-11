//
// $Id$

package com.threerings.config;

import com.threerings.editor.Editable;
import com.threerings.editor.InvalidPathsException;
import com.threerings.editor.ArgumentPathProperty;
import com.threerings.editor.Property;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

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
        public Property createArgumentProperty (ManagedConfig reference)
        {
            try {
                return new ArgumentPathProperty(name, reference, paths);
            } catch (InvalidPathsException e) {
                return null;
            }
        }
    }

    /** The parameters of the configuration. */
    @Editable(weight=1, nullable=false)
    public Parameter[] parameters = new Parameter[0];
}
