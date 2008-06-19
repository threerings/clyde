//
// $Id$

package com.threerings.opengl.model.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.ResourceLoaded;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.material.config.MaterialConfig;

/**
 * The configuration of a model.
 */
public class ModelConfig extends ParameterizedConfig
    implements ResourceLoaded
{
    /**
     * Contains the actual implementation of the model.
     */
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Returns the subclasses available for selection in the editor.
         */
        public static Class[] getEditorTypes ()
        {
            return new Class[] { Static.class, Articulated.class, Derived.class };
        }
    }

    /**
     * Superclass of the original implementations.
     */
    public static abstract class Original extends Implementation
    {
    }

    /**
     * An original static implementation.
     */
    public static class Static extends Original
    {
    }

    /**
     * An original articulated implementation.
     */
    public static class Articulated extends Original
    {
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The model reference. */
        @Editable
        public ConfigReference<ModelConfig> model;
    }

    /** The actual model implementation. */
    @Editable
    public Implementation implementation = new Static();
}
