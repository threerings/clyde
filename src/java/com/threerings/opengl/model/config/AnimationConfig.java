//
// $Id$

package com.threerings.opengl.model.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.editor.util.EditorContext;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Transform3DExpression;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;
import com.threerings.util.Shallow;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.tools.AnimationDef;
import com.threerings.opengl.model.tools.xml.AnimationParser;
import com.threerings.opengl.util.GlContext;

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
        /**
         * Updates this implementation from its external source, if any.
         *
         * @param force if true, reload the source data even if it has already been loaded.
         */
        public void updateFromSource (EditorContext ctx, boolean force)
        {
            // nothing by default
        }

        /**
         * Adds the implementation's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Creates or updates an animation implementation for this configuration.
         *
         * @param scope the animation's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract Animation.Implementation getAnimationImplementation (
            GlContext ctx, Scope scope, Animation.Implementation impl);
    }

    /**
     * Superclass of the original implementations.
     */
    public static abstract class Original extends Implementation
    {
        /** The priority level of this animation. */
        @Editable(hgroup="p")
        public int priority;

        /** Whether or not to override other animations at the same priority level. */
        @Editable(hgroup="p")
        public boolean override = true;

        /** The blend weight of the animation. */
        @Editable(min=0, max=1, step=0.01, hgroup="w")
        public float weight = 1f;

        /** The amount of time to spend blending in the animation. */
        @Editable(min=0, step=0.01, hgroup="w")
        public float blendIn;

        /** The amount of time to spend blending out the animation. */
        @Editable(min=0, step=0.01, hgroup="w")
        public float blendOut;
    }

    /**
     * A frame-based animation imported from an export file.
     */
    public static class Imported extends Original
    {
        /** The interval over which to transition into the first frame. */
        @Editable(min=0, step=0.01, hgroup="t")
        public float transition;

        /** The speed of the animation. */
        @Editable(min=0, step=0.01, hgroup="t")
        public float speed = 1f;

        /** The global animation scale. */
        @Editable(min=0, step=0.01, hgroup="t")
        public float scale = 0.01f;

        /** Whether or not the animation loops. */
        @Editable(hgroup="l")
        public boolean loop;

        /** Whether or not to skip the last frame when looping (because it's the same as the
         * first). */
        @Editable(hgroup="l")
        public boolean skipLastFrame = true;

        /** Actions to perform at specific times within the animation. */
        @Editable
        public FrameAction[] actions = new FrameAction[0];

        /** The base animation frame rate. */
        public float rate;

        /** The targets of the animation. */
        @Shallow
        public String[] targets;

        /** The transforms for each target, each frame. */
        @Shallow
        public Transform3D[][] transforms;

        /**
         * Sets the source file from which to load the animation data.
         */
        @Editable(editor="resource", weight=-1, nullable=true)
        @FileConstraints(
            description="m.exported_anims",
            extensions={".mxml"},
            directory="exported_anim_dir")
        public void setSource (String source)
        {
            _source = source;
            _reload = true;
        }

        /**
         * Returns the source resource.
         */
        @Editable
        public String getSource ()
        {
            return _source;
        }

        /**
         * Returns the duration of the animation (assuming it doesn't loop).
         */
        public float getDuration ()
        {
            return transition + (transforms.length - 1) / getScaledRate();
        }

        /**
         * Returns the frame rate as scaled by the speed.
         */
        public float getScaledRate ()
        {
            return speed * rate;
        }

        @Override // documentation inherited
        public void updateFromSource (EditorContext ctx, boolean force)
        {
            if (!(_reload || force)) {
                return;
            }
            _reload = false;
            if (_source == null) {
                updateFromSource(null);
                return;
            }
            if (_parser == null) {
                _parser = new AnimationParser();
            }
            AnimationDef def;
            try {
                updateFromSource(_parser.parseAnimation(
                    ctx.getResourceManager().getResource(_source)));
            } catch (Exception e) {
                log.warning("Error parsing animation [source=" + _source + "].", e);
                return;
            }
        }

        @Override // documentation inherited
        public Animation.Implementation getAnimationImplementation (
            GlContext ctx, Scope scope, Animation.Implementation impl)
        {
            if (targets == null) {
                return null;
            }
            if (impl instanceof Animation.Imported) {
                ((Animation.Imported)impl).setConfig(this);
            } else {
                impl = new Animation.Imported(ctx, scope, this);
            }
            return impl;
        }

        /**
         * Updates from a parsed animation definition.
         */
        protected void updateFromSource (AnimationDef def)
        {
            if (def == null) {
                targets = null;
                transforms = null;
            } else {
                def.update(this);
            }
        }

        /** The resource from which we read the animation data. */
        protected String _source;

        /** Indicates that {@link #updateFromSource} should reload the data. */
        @DeepOmit
        protected transient boolean _reload;
    }

    /**
     * A procedural animation.
     */
    public static class Procedural extends Original
    {
        /** The list of target transforms. */
        @Editable
        public TargetTransform[] transforms = new TargetTransform[0];

        @Override // documentation inherited
        public Animation.Implementation getAnimationImplementation (
            GlContext ctx, Scope scope, Animation.Implementation impl)
        {
            if (impl instanceof Animation.Procedural) {
                ((Animation.Procedural)impl).setConfig(this);
            } else {
                impl = new Animation.Procedural(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The animation reference. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> animation;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(AnimationConfig.class, animation);
        }

        @Override // documentation inherited
        public Animation.Implementation getAnimationImplementation (
            GlContext ctx, Scope scope, Animation.Implementation impl)
        {
            AnimationConfig config = ctx.getConfigManager().getConfig(
                AnimationConfig.class, animation);
            return (config == null) ? null : config.getAnimationImplementation(ctx, scope, impl);
        }
    }

    /**
     * An action to perform at a specific frame in the animation.
     */
    public static class FrameAction extends DeepObject
        implements Exportable
    {
        /** The frame at which to perform the action. */
        @Editable(min=0, step=0.01)
        public float frame;

        /** The action to perform. */
        @Editable
        public ActionConfig action = new ActionConfig.CallFunction();
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

    /**
     * Creates or updates an animation implementation for this configuration.
     *
     * @param scope the animation's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public Animation.Implementation getAnimationImplementation (
        GlContext ctx, Scope scope, Animation.Implementation impl)
    {
        return implementation.getAnimationImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    public void updateFromSource (EditorContext ctx, boolean force)
    {
        implementation.updateFromSource(ctx, force);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }

    /** Parses animation exports. */
    protected static AnimationParser _parser;
}
