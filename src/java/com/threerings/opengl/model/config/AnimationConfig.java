//
// $Id$

package com.threerings.opengl.model.config;

import java.io.File;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * The configuration of an animation.
 */
public class AnimationConfig extends ParameterizedConfig
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
        /** The animation frame rate. */
        @Editable(min=0, step=0.01)
        public float frameRate;

        /**
         * Sets the source file from which to load the animation data.
         */
        @Editable(nullable=true)
        @FileConstraints(
            description="m.animation_files",
            extensions={".mxml" },
            directory="anim_dir")
        public void setSource (File source)
        {
            _source = source;
        }

        /**
         * Returns the source file.
         */
        @Editable
        public File getSource ()
        {
            return _source;
        }

        /** The file from which we read the animation data. */
        protected File _source;
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
