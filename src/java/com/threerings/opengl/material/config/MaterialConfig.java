//
// $Id$

package com.threerings.opengl.material.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Describes a material.
 */
public class MaterialConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the material.
     */
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Returns the subclasses available for selection in the editor.
         */
        public static Class[] getEditorTypes ()
        {
            return new Class[] { Original.class, Derived.class };
        }
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The techniques available to render the material. */
        @Editable(nullable=false)
        public Technique[] techniques = new Technique[0];
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The material reference. */
        @Editable
        public ConfigReference<MaterialConfig> material;
    }

    /**
     * A technique available to render the material.
     */
    public static class Technique extends DeepObject
        implements Exportable
    {
        /** The passes used to render the material. */
        @Editable(nullable=false)
        public PassConfig[] passes = new PassConfig[0];
    }

    /** The actual material implementation. */
    @Editable
    public Implementation implementation = new Original();
}
