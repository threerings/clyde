//
// $Id$

package com.threerings.opengl.model.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.ResourceLoaded;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
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
    @EditorTypes({ Static.class, Articulated.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
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
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;
    }

    /** The actual model implementation. */
    @Editable
    public Implementation implementation = new Static();
}
