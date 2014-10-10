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

package com.threerings.opengl.scene.config;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.util.Tuple;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.ExpressionBinding;
import com.threerings.expr.ExpressionDefinition;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;

import com.threerings.opengl.material.config.ProjectionConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.config.FogStateConfig;
import com.threerings.opengl.renderer.config.LightConfig;
import com.threerings.opengl.scene.AmbientLightInfluence;
import com.threerings.opengl.scene.DefinerInfluence;
import com.threerings.opengl.scene.FogInfluence;
import com.threerings.opengl.scene.LightInfluence;
import com.threerings.opengl.scene.ProjectorInfluence;
import com.threerings.opengl.scene.SceneInfluence;
import com.threerings.opengl.util.GlContext;

/**
 * The configuration of an influence.
 */
@EditorTypes({
    SceneInfluenceConfig.AmbientLight.class, SceneInfluenceConfig.Fog.class,
    SceneInfluenceConfig.Light.class, SceneInfluenceConfig.Projector.class,
    SceneInfluenceConfig.Definer.class })
public abstract class SceneInfluenceConfig extends DeepObject
    implements Exportable
{
    /**
     * Represents the influence of ambient light.
     */
    public static class AmbientLight extends SceneInfluenceConfig
    {
        /** The ambient light color. */
        @Editable
        public Color4f color = new Color4f(0.2f, 0.2f, 0.2f, 1f);

        @Override
        protected SceneInfluence createInfluence (
            GlContext ctx, Scope scope, ArrayList<Updater> updaters)
        {
            return ScopeUtil.resolve(scope, "lightingEnabled", true) ?
                new AmbientLightInfluence(color) : createNoopInfluence();
        }
    }

    /**
     * Represents the influence of fog.
     */
    public static class Fog extends SceneInfluenceConfig
    {
        /** The fog state. */
        @Editable
        public FogStateConfig state = new FogStateConfig.Linear();

        @Override
        protected SceneInfluence createInfluence (
            GlContext ctx, Scope scope, ArrayList<Updater> updaters)
        {
            return ScopeUtil.resolve(scope, "fogEnabled", true) ?
                new FogInfluence(state.getState()) : createNoopInfluence();
        }
    }

    /**
     * Represents the influence of a light.
     */
    public static class Light extends SceneInfluenceConfig
    {
        /** The light config. */
        @Editable
        public LightConfig light = new LightConfig.Directional();

        /** The shadow config, if any. */
        @Editable(nullable=true)
        public ShadowConfig shadow;

        @Override
        protected SceneInfluence createInfluence (
            GlContext ctx, Scope scope, ArrayList<Updater> updaters)
        {
            if (!ScopeUtil.resolve(scope, "lightingEnabled", true)) {
                return createNoopInfluence();
            }
            com.threerings.opengl.renderer.Light viewLight =
                light.createLight(ctx, scope, false, updaters);
            return (shadow == null) ? new LightInfluence(viewLight) :
                shadow.createInfluence(ctx, scope, light, viewLight, updaters);
        }
    }

    /**
     * Represents a projection influence.
     */
    public static class Projector extends SceneInfluenceConfig
    {
        /** The projection config. */
        @Editable
        public ProjectionConfig projection = new ProjectionConfig.Perspective();

        @Override
        protected SceneInfluence createInfluence (
            GlContext ctx, Scope scope, ArrayList<Updater> updaters)
        {
            return new ProjectorInfluence(projection.createProjection(ctx, scope, updaters));
        }
    }

    /**
     * Represents a variable definition influence.
     */
    public static class Definer extends SceneInfluenceConfig
    {
        /** The definition configs. */
        @Editable
        public ExpressionDefinition[] definitions = new ExpressionDefinition[0];

        @Override
        public void invalidate ()
        {
            super.invalidate();
            for (ExpressionDefinition definition : definitions) {
                definition.invalidate();
            }
        }

        @Override
        protected SceneInfluence createInfluence (
            GlContext ctx, Scope scope, ArrayList<Updater> updaters)
        {
            List<Tuple<String, Object>> list = Lists.newArrayList();
            for (ExpressionDefinition definition : definitions) {
                list.add(Tuple.newTuple(definition.name, definition.getValue(scope, updaters)));
            }
            @SuppressWarnings("unchecked") Tuple<String, Object>[] array =
                (Tuple<String, Object>[])new Tuple<?, ?>[list.size()];
            return new DefinerInfluence(list.toArray(array));
        }
    }

    /** The static expression bindings for this influence. */
    @Editable(weight=1)
    public ExpressionBinding[] staticBindings = ExpressionBinding.EMPTY_ARRAY;

    /** The dynamic expression bindings for this influence. */
    @Editable(weight=1)
    public ExpressionBinding[] dynamicBindings = ExpressionBinding.EMPTY_ARRAY;

    /**
     * Creates the scene influence corresponding to this config.
     *
     * @param updaters a list to populate with required updaters.
     */
    public SceneInfluence createSceneInfluence (
        GlContext ctx, Scope scope, ArrayList<Updater> updaters)
    {
        // create the basic influence
        SceneInfluence influence = createInfluence(ctx, scope, updaters);

        // update the static bindings and add the dynamic updaters to the list
        for (ExpressionBinding binding : staticBindings) {
            binding.createUpdater(ctx.getConfigManager(), scope, influence).update();
        }
        for (ExpressionBinding binding : dynamicBindings) {
            updaters.add(binding.createUpdater(ctx.getConfigManager(), scope, influence));
        }

        return influence;
    }

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        for (ExpressionBinding binding : staticBindings) {
            binding.invalidate();
        }
        for (ExpressionBinding binding : dynamicBindings) {
            binding.invalidate();
        }
    }

    /**
     * Creates the actual influence object.
     */
    protected abstract SceneInfluence createInfluence (
        GlContext ctx, Scope scope, ArrayList<Updater> updaters);

    /**
     * Creates an influence that does nothing.
     */
    protected static SceneInfluence createNoopInfluence ()
    {
        return new SceneInfluence() { };
    }
}
