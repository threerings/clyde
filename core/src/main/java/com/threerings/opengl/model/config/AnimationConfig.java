//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.model.config;

import com.samskivert.util.ArrayUtil;

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
import com.threerings.probs.FloatVariable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;
import com.threerings.util.Shallow;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.tools.AnimationDef;
import com.threerings.opengl.model.tools.xml.AnimationParser;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.log;

/**
 * The configuration of an animation.
 */
public class AnimationConfig extends ParameterizedConfig
    implements Preloadable.LoadableConfig
{
    /**
     * Contains the actual implementation of the animation.
     */
    @EditorTypes({ Imported.class, Procedural.class, Sequential.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable, Preloadable.LoadableConfig
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

        @Deprecated
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

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
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

        /** A (possibly random) offset to apply when the animation starts. */
        @Editable(min=0, step=0.01, weight=2)
        public FloatVariable offset = new FloatVariable.Constant(0f);

        /** Actions to perform at specific times within the animation. */
        @Editable(weight=2)
        public FrameAction[] actions = new FrameAction[0];

        /** A set of targets that will be modified. */
        @Editable
        public TargetModifier[] modifiers = new TargetModifier[0];

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
         * Included in order to make duration appear as an editable property.
         */
        @Editable(step=0.01, hgroup="l", weight=1,
            depends={"transition", "source", "speed", "duration"})
        public void setDuration (float duration)
        {
            // no-op
        }

        /**
         * Returns the duration of the animation (assuming it doesn't loop).
         */
        @Editable
        public float getDuration ()
        {
            return transition +
                (transforms == null ? 0f : (transforms.length - 1) / getScaledRate());
        }

        /**
         * Returns the frame rate as scaled by the speed.
         */
        public float getScaledRate ()
        {
            return speed * rate;
        }

        @Override
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
            try {
                updateFromSource(_parser.parseAnimation(
                    ctx.getResourceManager().getResource(_source)));
            } catch (Exception e) {
                log.warning("Error parsing animation [source=" + _source + "].", e);
                return;
            }
        }

        @Override
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

        @Override
        public void preload (GlContext ctx)
        {
            for (FrameAction action : actions) {
                action.action.preload(ctx);
            }
        }

        /**
         * Returns the modified transform array.  If there are no modifiers then this simply returns
         * the 2D transforms array.
         */
        public Transform3D[][] getModifiedTransforms (Transform3D[] defaults)
        {
            if (modifiers.length == 0) {
                return transforms;
            }
            Transform3D[][] result = new Transform3D[transforms.length][];
            for (int ii = 0; ii < result.length; ii++) {
                result[ii] = transforms[ii].clone();
            }
            for (TargetModifier modifier : modifiers) {
                int idx = ArrayUtil.indexOf(targets, modifier.target);
                if (idx != -1) {
                    for (int ii = 0; ii < result.length; ii++) {
                        result[ii][idx] = modifier.modifyTransform(
                                result[ii][idx].promote(Transform3D.UNIFORM), defaults[idx]);
                    }
                }
            }
            return result;
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
        /** The duration of the animation, or zero for unlimited. */
        @Editable(min=0, step=0.01)
        public float duration;

        /** A (possibly random) offset to apply when the animation starts. */
        @Editable(min=0, step=0.01)
        public FloatVariable offset = new FloatVariable.Constant(0f);

        /** The list of target transforms. */
        @Editable
        public TargetTransform[] transforms = new TargetTransform[0];

        @Override
        public void preload (GlContext ctx)
        {
            // Do Nothing
        }

        @Override
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

        @Override
        public void invalidate ()
        {
            for (TargetTransform transform : transforms) {
                transform.expression.invalidate();
            }
        }
    }

    /**
     * Runs a series of animations in sequence.
     */
    public static class Sequential extends Original
    {
        /** Whether or not the animation loops. */
        @Editable
        public boolean loop;

        /** The component animations. */
        @Editable
        public ComponentAnimation[] animations = new ComponentAnimation[0];

        @Override
        public Animation.Implementation getAnimationImplementation (
            GlContext ctx, Scope scope, Animation.Implementation impl)
        {
            if (animations.length == 0) {
                impl = null;
            } else if (impl instanceof Animation.Sequential) {
                ((Animation.Sequential)impl).setConfig(this);
            } else {
                impl = new Animation.Sequential(ctx, scope, this);
            }
            return impl;
        }

        @Override
        public void preload (GlContext ctx)
        {
            for (ComponentAnimation animation : animations) {
                new Preloadable.Animation(animation.animation).preload(ctx);
            }
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

        @Override
        public Animation.Implementation getAnimationImplementation (
            GlContext ctx, Scope scope, Animation.Implementation impl)
        {
            AnimationConfig config = ctx.getConfigManager().getConfig(
                AnimationConfig.class, animation);
            return (config == null) ? null : config.getAnimationImplementation(ctx, scope, impl);
        }

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Animation(animation).preload(ctx);
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
        public String[] targets = ArrayUtil.EMPTY_STRING;

        /** The expression that determines the transform. */
        @Editable
        public Transform3DExpression expression = new Transform3DExpression.Constant();
    }

    /**
     * Contains a component animation in a sequence.
     */
    public static class ComponentAnimation extends DeepObject
        implements Exportable
    {
        /** The speed of the animation. */
        @Editable(min=0, step=0.01)
        public float speed = 1f;

        /** The animation reference. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> animation;
    }

    /**
     * Contains a target transformation override for an animation sequence.
     */
    public static class TargetModifier extends DeepObject
        implements Exportable
    {
        /** The target name. */
        @Editable
        public String target = "";

        /** If the target ignores the animation translation. */
        @Editable
        public boolean ignoreTranslation = false;

        /** If the target ignores the animation rotation. */
        @Editable
        public boolean ignoreRotation = false;

        /** If the target ignores the animation scale. */
        @Editable
        public boolean ignoreScale = false;

        /** A base transformation on the target. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D(Transform3D.UNIFORM);

        /**
         * Alters the supplied transformation.
         */
        public Transform3D modifyTransform (Transform3D anim, Transform3D def)
        {
            _transform.set(
                    (ignoreTranslation ? def : anim).getTranslation(),
                    (ignoreRotation ? def : anim).getRotation(),
                    (ignoreScale ? def : anim).getScale());
            Transform3D result = transform.compose(_transform);
            return result;
        }

        protected static Transform3D _transform = new Transform3D();
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

    @Override
    public void preload (GlContext ctx)
    {
        implementation.preload(ctx);
    }

    @Override
    public void updateFromSource (EditorContext ctx, boolean force)
    {
        implementation.updateFromSource(ctx, force);
    }

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    /** Parses animation exports. */
    protected static AnimationParser _parser;
}
