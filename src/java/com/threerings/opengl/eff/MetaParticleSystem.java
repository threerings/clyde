//
// $Id$

package com.threerings.opengl.eff;

import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.math.Transform3D;

import com.threerings.opengl.effect.AlphaMode;
import com.threerings.opengl.effect.Particle;
import com.threerings.opengl.effect.config.BaseParticleSystemConfig;
import com.threerings.opengl.effect.config.MetaParticleSystemConfig;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.compositor.RenderScheme;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.util.GlContext;

/**
 * The meta particle system model implementation.
 */
public class MetaParticleSystem extends BaseParticleSystem
{
    /**
     * A single layer of the system.
     */
    public static class Layer extends BaseParticleSystem.Layer
    {
        /**
         * Creates a new layer.
         */
        public Layer (GlContext ctx, Scope parentScope, BaseParticleSystemConfig.Layer config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        @Override // documentation inherited
        public void setConfig (BaseParticleSystemConfig.Layer config)
        {
            super.setConfig(config);

            // create the models
            Model[] omodels = _models;
            _models = new Model[config.particleCount];
            MetaParticleSystemConfig.Layer mconfig = (MetaParticleSystemConfig.Layer)config;
            _geometryRadius = 0f;
            for (int ii = 0; ii < _models.length; ii++) {
                Model model = (omodels == null || ii >= omodels.length) ? null : omodels[ii];
                if (model == null) {
                    model = new Model(_ctx);
                    model.setParentScope(this);
                    model.setColorState(new ColorState());
                }
                model.setRenderScheme(
                    config.alphaMode == AlphaMode.OPAQUE ? null : RenderScheme.TRANSLUCENT);
                model.setConfig(mconfig.model);
                if (_geometryRadius == 0f) {
                    model.setLocalTransform(new Transform3D());
                    Box bounds = model.getBounds();
                    if (!bounds.isEmpty()) {
                        _geometryRadius = bounds.getDiagonalLength() * 0.5f;
                    }
                }
                _models[ii] = model;
            }
            if (omodels != null) {
                for (int ii = _models.length; ii < omodels.length; ii++) {
                    omodels[ii].dispose();
                }
            }
        }

        @Override // documentation inherited
        public boolean tick (float elapsed)
        {
            if (super.tick(elapsed)) {
                return true;
            }
            // update and tick the models
            for (int ii = 0; ii < _living.value; ii++) {
                Particle particle = _particles[ii];
                Model model = _models[ii];
                model.getLocalTransform().set(
                    particle.getPosition(), particle.getOrientation(), particle.getSize());
                model.updateBounds();
                model.getColorState().getColor().set(particle.getColor());
                _models[ii].tick(elapsed);
            }
            return false;
        }

        @Override // documentation inherited
        public void enqueue ()
        {
            if (!_config.visible || _living.value == 0) {
                return;
            }
            // enqueue the models
            for (int ii = 0; ii < _living.value; ii++) {
                _models[ii].enqueue();
            }
        }

        @Override // documentation inherited
        public void dispose ()
        {
            super.dispose();
            for (Model model : _models) {
                model.dispose();
            }
        }

        /** The models corresponding to each particle. */
        protected Model[] _models;
    }

    /**
     * Creates a new meta particle system implementation.
     */
    public MetaParticleSystem (GlContext ctx, Scope parentScope, MetaParticleSystemConfig config)
    {
        super(ctx, parentScope);
        setConfig(config);
    }

    @Override // documentation inherited
    protected BaseParticleSystem.Layer createLayer (BaseParticleSystemConfig.Layer config)
    {
        return new Layer(_ctx, this, config);
    }
}
