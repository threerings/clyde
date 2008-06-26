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

import com.threerings.math.Transform3D;

import com.threerings.opengl.model.tools.AnimationDef;
import com.threerings.opengl.model.tools.xml.AnimationParser;

import static com.threerings.opengl.Log.*;

/**
 * The configuration of an animation.
 */
public class AnimationConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the animation.
     */
    @EditorTypes({ Frames.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
    }

    /**
     * An original frame-based animation.
     */
    public static class Frames extends Implementation
    {
        /** The animation frame rate. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float rate;

        /** The global animation scale. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float scale = 0.01f;

        /** Whether or not the animation loops. */
        @Editable(hgroup="l")
        public boolean loop;

        /** Whether or not to skip the last frame when looping. */
        @Editable(hgroup="l")
        public boolean skipLastFrame = true;

        /**
         * Sets the source file from which to load the animation data.
         */
        @Editable(nullable=true)
        @FileConstraints(
            description="m.exported_anims",
            extensions={ ".mxml" },
            directory="exported_anim_dir")
        public void setSource (File source)
        {
            _source = source;
            updateFromSource();
        }

        /**
         * Returns the source file.
         */
        @Editable
        public File getSource ()
        {
            return _source;
        }

        /**
         * (Re)reads the source data.
         */
        public void updateFromSource ()
        {
            if (_animParser == null) {
                _animParser = new AnimationParser();
            }
            AnimationDef def;
            try {
                def = _animParser.parseAnimation(_source.toString());
            } catch (Exception e) {
                log.warning("Error parsing animation [source=" + _source + "].", e);
                return;
            }
            _targets = def.getTargets();
        }

        /** The file from which we read the animation data. */
        protected File _source;

        /** The targets of the animation. */
        protected String[] _targets = new String[0];

        /** The transforms for each target, each frame. */
        protected Transform3D[][] _transforms = new Transform3D[0][];
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
    public Implementation implementation = new Frames();

    /** Parses animation exports. */
    protected static AnimationParser _animParser;
}
