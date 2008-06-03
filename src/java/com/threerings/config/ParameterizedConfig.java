//
// $Id$

package com.threerings.config;

import com.threerings.editor.Editable;
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

        /** The reference paths of the properties that this parameter adjusts. */
        @Editable
        public ParameterAdjuster adjuster = new PathAdjuster();
    }

    /**
     * Handles the actual implementation of the parameter.
     */
    public static abstract class ParameterAdjuster extends DeepObject
        implements Exportable
    {
        /**
         * Creates a property for the supplied configuration that may be used to adjust the
         * parameter value.
         */
        public abstract Property createProperty (ManagedConfig config);

        /**
         * Returns the subtypes available for selection in the editor.
         */
        public static Class[] getEditorTypes ()
        {
            return new Class[] { PathAdjuster.class };
        }
    }

    /**
     * Contains a set of reference paths used to locate properties within the object.
     */
    public static class PathAdjuster extends ParameterAdjuster
    {
        /** The reference paths of the properties that this parameter adjusts. */
        @Editable
        public String[] paths = new String[0];

        @Override // documentation inherited
        public Property createProperty (ManagedConfig config)
        {
            return null;
        }
    }

    /** The parameters of the configuration. */
    @Editable(weight=1, nullable=false)
    public Parameter[] parameters = new Parameter[0];
}
