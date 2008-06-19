//
// $Id$

package com.threerings.opengl.model.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.ResourceLoaded;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * The configuration of an animation.
 */
public class AnimationConfig extends ParameterizedConfig
    implements ResourceLoaded
{
    /**
     * Contains the actual implementation of the animation.
     */
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Returns the subclasses available for selection in the editor.
         */
        public static Class[] getEditorTypes ()
        {
            return new Class[] { Keyframe.class, Derived.class };
        }
    }

    /**
     * An original keyframe animation.
     */
    public static class Keyframe extends Implementation
    {
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The animation reference. */
        @Editable
        public ConfigReference<AnimationConfig> animation;
    }

    /** The actual animation implementation. */
    @Editable
    public Implementation implementation = new Keyframe();
}
