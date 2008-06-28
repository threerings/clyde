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
import com.threerings.expr.Transform3DExpression;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;
import com.threerings.util.Shallow;

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
    @EditorTypes({ Imported.class, Procedural.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
    }

    /**
     * Superclass of the original implementations.
     */
    public static class Original extends Implementation
    {
        /** The priority level of this animation. */
        @Editable(hgroup="p")
        public int priority;

        /** Whether or not to override other animations at the same priority level. */
        @Editable(hgroup="p")
        public boolean override = true;

        /** The interval over which to transition into the first frame. */
        @Editable(min=0, step=0.01, hgroup="t")
        public float transition;

        /** The blend weight of the animation. */
        @Editable(min=0, max=1, step=0.01, hgroup="t")
        public float weight = 1f;

        /** The amount of time to spend blending in the animation. */
        @Editable(min=0, step=0.01, hgroup="b")
        public float blendIn;

        /** The amount of time to spend blending out the animation. */
        @Editable(min=0, step=0.01, hgroup="b")
        public float blendOut;
    }

    /**
     * A frame-based animation imported from an export file.
     */
    public static class Imported extends Original
    {
        /** The speed of the animation. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float speed = 1f;

        /** The global animation scale. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float scale = 0.01f;

        /** Whether or not the animation loops. */
        @Editable(hgroup="l")
        public boolean loop;

        /** Whether or not to skip the last frame when looping (because it's the same as the
         * first). */
        @Editable(hgroup="l")
        public boolean skipLastFrame = true;

        /**
         * Sets the source file from which to load the animation data.
         */
        @Editable(weight=-1, nullable=true)
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
            if (_source == null) {
                _targets = new String[0];
                _transforms = new Transform3D[0][];
                return;
            }
            if (_parser == null) {
                _parser = new AnimationParser();
            }
            AnimationDef def;
            try {
                def = _parser.parseAnimation(_source.toString());
            } catch (Exception e) {
                log.warning("Error parsing animation [source=" + _source + "].", e);
                return;
            }
            _rate = def.frameRate;
            _targets = def.getTargets();
            _transforms = def.getTransforms(_targets, scale);
        }

        /** The file from which we read the animation data. */
        protected File _source;

        /** The base animation frame rate. */
        protected float _rate;

        /** The targets of the animation. */
        @Shallow
        protected String[] _targets = new String[0];

        /** The transforms for each target, each frame. */
        @Shallow
        protected Transform3D[][] _transforms = new Transform3D[0][];
    }

    /**
     * A procedural animation.
     */
    public static class Procedural extends Original
    {
        /** The list of target transforms. */
        @Editable
        public TargetTransform[] transforms = new TargetTransform[0];
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

    /**
     * Controls the transform of one or more nodes.
     */
    public static class TargetTransform extends DeepObject
        implements Exportable
    {
        /** The nodes to affect. */
        @Editable
        public String[] targets = new String[0];

        /** The expression that determines the transform. */
        @Editable
        public Transform3DExpression expression = new Transform3DExpression.Constant();
    }

    /** The actual animation implementation. */
    @Editable
    public Implementation implementation = new Imported();

    /** Parses animation exports. */
    protected static AnimationParser _parser;
}
