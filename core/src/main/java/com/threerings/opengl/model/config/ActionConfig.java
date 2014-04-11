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

import com.samskivert.util.RandomUtil;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.BooleanExpression;
import com.threerings.expr.Executor;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.openal.Sounder;
import com.threerings.openal.config.SounderConfig;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.model.Articulated;
import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.log;

/**
 * Configurations for actions taken by models.
 */
@EditorTypes({
    ActionConfig.CallFunction.class, ActionConfig.SpawnTransient.class,
    ActionConfig.PlaySound.class, ActionConfig.ShakeCamera.class,
    ActionConfig.Conditional.class, ActionConfig.Compound.class,
    ActionConfig.Random.class })
public abstract class ActionConfig extends DeepObject
    implements Exportable
{
    /**
     * Generic action that calls a scoped function.
     */
    public static class CallFunction extends ActionConfig
    {
        /** The name of the function to call. */
        @Editable
        public String name = "";

        /** A string argument that will be supplied if not empty. */
        @Editable
        public String arg = "";

        @Override
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            final Function fn = ScopeUtil.resolve(scope, name, Function.NULL);
            return new Executor() {
                public void execute () {
                    if (arg.isEmpty()) {
                        fn.call();
                    } else {
                        fn.call(arg);
                    }
                }
            };
        }

        @Override
        public void preload (GlContext ctx)
        {
            // Do nothing
        }
    }

    /**
     * Creates a transient model (such as a particle system) and adds it to the scene at the
     * location of one of the model's nodes.
     */
    public static class SpawnTransient extends ActionConfig
    {
        /** Whether or not to move the transient with its origin. */
        @Editable(hgroup="u")
        public boolean moveWithOrigin;

        /** Whether or not to propogate animation speed modifiers. */
        @Editable(hgroup="u")
        public boolean modifyAnimSpeed;

        /** The model to spawn. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The node at whose transform the model should be added. */
        @Editable
        public String node = "";

        /** The transform relative to the node. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();

        @Override
        public Executor createExecutor (GlContext ctx, final Scope scope)
        {
            // get an instance from the transient pool and immediately return it, but retain a
            // hard reference to prevent it from being garbage collected.  hopefully this will
            // prevent the system's having to (re)load the model from disk at a crucial moment
            final Model instance = (Model)ScopeUtil.call(scope, "getFromTransientPool", model);
            ScopeUtil.call(scope, "returnToTransientPool", instance);

            final Function spawnTransient = ScopeUtil.resolve(
                scope, "spawnTransient", Function.NULL);
            final Articulated.Node node = (Articulated.Node)ScopeUtil.call(
                scope, "getNode", this.node);
            final Transform3D parent = (node == null) ?
                ScopeUtil.resolve(scope, "worldTransform", new Transform3D()) :
                node.getWorldTransform();
            final Function getSpeedModifier = modifyAnimSpeed ?
                ScopeUtil.resolve(scope, "getSpeedModifier", Function.NULL) :
                null;
            return new Executor() {
                public void execute () {
                    final Scene.Transient spawned = (Scene.Transient)spawnTransient.call(
                        model, parent.compose(transform, _world));
                    if ((moveWithOrigin || getSpeedModifier != null) && spawned != null) {
                        // install an updater to update the transform
                        spawned.setUpdater(new Updater() {
                            public void update () {
                                if (moveWithOrigin) {
                                    spawned.setLocalTransform(parent.compose(transform, _world));
                                }
                                if (getSpeedModifier != null) {
                                    float mod = (Float)getSpeedModifier.call();
                                    for (Animation anim : spawned.getPlayingAnimations()) {
                                        anim.setSpeedModifier(mod);
                                    }
                                }
                            }
                        });
                    }
                }
                protected Transform3D _world = new Transform3D();
                protected Model _instance = instance;
            };
        }

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Model(model).preload(ctx);
        }
    }

    /**
     * Plays a sound.
     */
    public static class PlaySound extends ActionConfig
    {
        /** The configuration of the sounder that will play the sound. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;

        /** The node at whose transform the sound should be played. */
        @Editable
        public String node = "";

        /** The transform relative to the node. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();

        @Override
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            Articulated.Node node = (Articulated.Node)ScopeUtil.call(scope, "getNode", this.node);
            final Transform3D parent = (node == null) ?
                ScopeUtil.resolve(scope, "worldTransform", new Transform3D()) :
                node.getWorldTransform();
            final Transform3D world = new Transform3D();
            final Sounder sounder = new Sounder(ctx, scope, world, this.sounder);
            return new Executor() {
                public void execute () {
                    parent.compose(transform, world);
                    sounder.start();
                }
            };
        }

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Config(SounderConfig.class, sounder).preload(ctx);
        }
    }

    /**
     * Shakes the camera briefly using a damped oscillation.
     */
    public static class ShakeCamera extends ActionConfig
    {
        /** The oscillation frequency. */
        @Editable(min=0, step=0.01, hgroup="f")
        public float frequency = 50f;

        @Editable(min=0.0, step=0.01, hgroup="f")
        public float falloff = 30f;

        /** The total duration of the shake. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float duration = 1f;

        /** The inner radius of the effect (within which the shake will be at full amplitude). */
        @Editable(min=0.0, step=0.01, hgroup="r")
        public float innerRadius = 10f;

        /** The outer radius of the effect (outside of which the shake will not be felt). */
        @Editable(min=0.0, step=0.01, hgroup="r")
        public float outerRadius = 100f;

        /** The amplitude of the shake. */
        @Editable(step=0.01)
        public Vector3f amplitude = new Vector3f(Vector3f.UNIT_Z);

        @Override
        public Executor createExecutor (final GlContext ctx, Scope scope)
        {
            final Transform3D transform = ScopeUtil.resolve(
                scope, "worldTransform", new Transform3D());
            return new Executor() {
                public void execute () {
                    transform.extractTranslation(_translation);
                    CameraHandler camhand = ctx.getCameraHandler();
                    float dist = camhand.getViewerTranslation().distance(_translation);
                    if (dist <= outerRadius) {
                        float scale = (dist <= innerRadius) ? 1f :
                            1f - (dist - innerRadius) / (outerRadius - innerRadius);
                        camhand.addOffset(createOffset(scale));
                    }
                }
                protected Vector3f _translation = new Vector3f();
            };
        }

        /**
         * Creates the actual handler offset.
         */
        protected CameraHandler.Offset createOffset (final float scale)
        {
            return new CameraHandler.Offset() {
                public boolean apply (Transform3D transform) {
                    float elapsed = (System.currentTimeMillis() - _start) / 1000f;
                    transform.getTranslation().addScaledLocal(amplitude,
                        scale * FloatMath.exp(-falloff*elapsed)*FloatMath.sin(frequency*elapsed));
                    return elapsed < duration;
                }
                protected long _start = System.currentTimeMillis();
            };
        }

        @Override
        public void preload (GlContext ctx)
        {
            // DO NOTHING
        }
    }

    /**
     * Performs one of a number of sub-actions depending on conditions.
     */
    public static class Conditional extends ActionConfig
    {
        /** The cases. */
        @Editable
        public Case[] cases = new Case[0];

        /** The default action. */
        @Editable
        public ActionConfig defaultAction = new CallFunction();

        @Override
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            final BooleanExpression.Evaluator[] evaluators =
                new BooleanExpression.Evaluator[cases.length];
            final Executor[] executors = new Executor[cases.length];
            for (int ii = 0; ii < cases.length; ii++) {
                Case caze = cases[ii];
                evaluators[ii] = caze.condition.createEvaluator(scope);
                executors[ii] = caze.action.createExecutor(ctx, scope);
            }
            final Executor defaultExecutor = defaultAction.createExecutor(ctx, scope);
            return new Executor() {
                public void execute () {
                    for (int ii = 0; ii < cases.length; ii++) {
                        if (evaluators[ii].evaluate()) {
                            executors[ii].execute();
                            return;
                        }
                    }
                    defaultExecutor.execute();
                }
            };
        }

        @Override
        public void preload (GlContext ctx)
        {
            defaultAction.preload(ctx);
            for (Case c : cases) {
                c.action.preload(ctx);
            }
        }
    }

    /**
     * Combines an action with a condition.
     */
    public static class Case extends DeepObject
        implements Exportable
    {
        /** The condition for the case. */
        @Editable
        public BooleanExpression condition = new BooleanExpression.Constant(true);

        /** The action itself. */
        @Editable
        public ActionConfig action = new CallFunction();
    }

    /**
     * Performs a number of sub-actions.
     */
    public static class Compound extends ActionConfig
    {
        /** The contained actions. */
        @Editable
        public ActionConfig[] actions = new ActionConfig[0];

        @Override
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            final Executor[] executors = new Executor[actions.length];
            for (int ii = 0; ii < actions.length; ii++) {
                executors[ii] = actions[ii].createExecutor(ctx, scope);
            }
            return new Executor() {
                public void execute () {
                    for (Executor executor : executors) {
                        executor.execute();
                    }
                }
            };
        }

        @Override
        public void preload (GlContext ctx)
        {
            for (ActionConfig action : actions) {
                action.preload(ctx);
            }
        }
    }

    /**
     * Performs one of a number of weighted sub-actions.
     */
    public static class Random extends ActionConfig
    {
        /** The contained actions. */
        @Editable
        public WeightedAction[] actions = new WeightedAction[0];

        @Override
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            final float[] weights = new float[actions.length];
            final Executor[] executors = new Executor[actions.length];
            for (int ii = 0; ii < actions.length; ii++) {
                WeightedAction waction = actions[ii];
                weights[ii] = waction.weight;
                executors[ii] = waction.action.createExecutor(ctx, scope);
            }
            return new Executor() {
                public void execute () {
                    executors[RandomUtil.getWeightedIndex(weights)].execute();
                }
            };
        }

        @Override
        public void preload (GlContext ctx)
        {
            for (WeightedAction waction : actions) {
                waction.action.preload(ctx);
            }
        }
    }

    /**
     * Combines an action with a weight.
     */
    public static class WeightedAction extends DeepObject
        implements Exportable
    {
        /** The weight of the action. */
        @Editable(min=0, step=0.01)
        public float weight = 1f;

        /** The action itself. */
        @Editable
        public ActionConfig action = new CallFunction();
    }

    /**
     * Creates an executor for this action.
     */
    public abstract Executor createExecutor (GlContext ctx, Scope scope);

    /**
     *
     */
    public abstract void preload (GlContext ctx);
}
