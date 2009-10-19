//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Executor;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

import com.threerings.openal.Sounder;
import com.threerings.openal.config.SounderConfig;
import com.threerings.opengl.model.Articulated;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.util.GlContext;

/**
 * Configurations for actions taken by models.
 */
@EditorTypes({
    ActionConfig.CallFunction.class, ActionConfig.SpawnTransient.class,
    ActionConfig.PlaySound.class })
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

        @Override // documentation inherited
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            final Function fn = ScopeUtil.resolve(scope, name, Function.NULL);
            return new Executor() {
                public void execute () {
                    fn.call();
                }
            };
        }
    }

    /**
     * Creates a transient model (such as a particle system) and adds it to the scene at the
     * location of one of the model's nodes.
     */
    public static class SpawnTransient extends ActionConfig
    {
        /** The model to spawn. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The node at whose transform the model should be added. */
        @Editable
        public String node = "";

        /** The transform relative to the node. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();

        @Override // documentation inherited
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            // get an instance from the transient pool and immediately return it, but retain a
            // hard reference to prevent it from being garbage collected.  hopefully this will
            // prevent the system's having to (re)load the model from disk at a crucial moment
            final Model instance = (Model)ScopeUtil.call(scope, "getFromTransientPool", model);
            ScopeUtil.call(scope, "returnToTransientPool", instance);

            final Function spawnTransient = ScopeUtil.resolve(
                scope, "spawnTransient", Function.NULL);
            Articulated.Node node = (Articulated.Node)ScopeUtil.call(scope, "getNode", this.node);
            final Transform3D parent = (node == null) ?
                ScopeUtil.resolve(scope, "worldTransform", new Transform3D()) :
                node.getWorldTransform();
            return new Executor() {
                public void execute () {
                    spawnTransient.call(model, parent.compose(transform, _world));
                }
                protected Transform3D _world = new Transform3D();
                protected Model _instance = instance;
            };
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

        @Override // documentation inherited
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
    }

    /**
     * Creates an executor for this action.
     */
    public abstract Executor createExecutor (GlContext ctx, Scope scope);
}
