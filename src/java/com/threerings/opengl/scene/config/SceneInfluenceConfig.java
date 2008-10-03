//
// $Id$

package com.threerings.opengl.scene.config;

import java.util.ArrayList;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.ExpressionBinding;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.config.FogStateConfig;
import com.threerings.opengl.renderer.config.LightConfig;
import com.threerings.opengl.scene.AmbientLightInfluence;
import com.threerings.opengl.scene.FogInfluence;
import com.threerings.opengl.scene.LightInfluence;
import com.threerings.opengl.scene.SceneInfluence;
import com.threerings.opengl.util.GlContext;

/**
 * The configuration of an influence.
 */
@EditorTypes({
    SceneInfluenceConfig.AmbientLight.class, SceneInfluenceConfig.Fog.class,
    SceneInfluenceConfig.Light.class })
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

        @Override // documentation inherited
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

        @Override // documentation inherited
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

        @Override // documentation inherited
        protected SceneInfluence createInfluence (
            GlContext ctx, Scope scope, ArrayList<Updater> updaters)
        {
            return ScopeUtil.resolve(scope, "lightingEnabled", true) ?
                new LightInfluence(light.createLight(ctx, scope, updaters)) :
                    createNoopInfluence();
        }
    }

    /** The static expression bindings for this influence. */
    @Editable(weight=1)
    public ExpressionBinding[] staticBindings = new ExpressionBinding[0];

    /** The dynamic expression bindings for this influence. */
    @Editable(weight=1)
    public ExpressionBinding[] dynamicBindings = new ExpressionBinding[0];

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
