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

/**
 * The configuration of an animation.
 */
public class AnimationConfig extends ParameterizedConfig
    implements ResourceLoaded
{
    /**
     * Contains the actual implementation of the animation.
     */
    @EditorTypes({ Keyframe.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
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
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> animation;
    }

    /** The actual animation implementation. */
    @Editable
    public Implementation implementation = new Keyframe();
}
