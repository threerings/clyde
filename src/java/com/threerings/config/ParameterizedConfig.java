//
// $Id$

package com.threerings.config;

import com.threerings.editor.Editable;
import com.threerings.editor.InvalidPathsException;
import com.threerings.editor.PathProperty;
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
         * Creates the property used to manipulate this parameter using the supplied configuration
         * to resolve the paths.
         *
         * @return the property, or <code>null</code> if none of the paths are valid.
         */
        public Property createProperty (ManagedConfig config)
        {
            try {
                return new PathProperty(name, config, paths);
            } catch (InvalidPathsException e) {
                return null;
            }
        }
    }

    /** The parameters of the configuration. */
    @Editable(weight=1, nullable=false)
    public Parameter[] parameters = new Parameter[0];
}
