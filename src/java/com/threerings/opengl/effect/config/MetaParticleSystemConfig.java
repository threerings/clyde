//
// $Id$

package com.threerings.opengl.effect.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.expr.Scope;

import com.threerings.opengl.eff.MetaParticleSystem;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.GlContext;

/**
 * The configuration of a meta particle system (like a particle system, but each particle is a
 * model instance).
 */
public class MetaParticleSystemConfig extends BaseParticleSystemConfig
{
    /**
     * A single layer of the system.
     */
    public static class Layer extends BaseParticleSystemConfig.Layer
    {
        /** The model to use for the particles. */
        @Editable(category="appearance", weight=-0.5, nullable=true)
        public ConfigReference<ModelConfig> model;
    }

    /** The layers comprising the system. */
    @Editable(editor="table")
    public Layer[] layers = new Layer[0];

    @Override // documentation inherited
    public BaseParticleSystemConfig.Layer[] getLayers ()
    {
        return layers;
    }

    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (impl instanceof MetaParticleSystem) {
            ((MetaParticleSystem)impl).setConfig(this);
        } else {
            impl = new MetaParticleSystem(ctx, scope, this);
        }
        return impl;
    }
}
