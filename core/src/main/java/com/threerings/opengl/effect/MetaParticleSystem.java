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

package com.threerings.opengl.effect;

import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.compositor.Enqueueable;
import com.threerings.opengl.effect.config.BaseParticleSystemConfig;
import com.threerings.opengl.effect.config.MetaParticleSystemConfig;
import com.threerings.opengl.effect.config.MetaParticleSystemConfig.Alignment;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.compositor.RenderScheme;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.log;

/**
 * The meta particle system model implementation.
 */
public class MetaParticleSystem extends BaseParticleSystem
{
    /**
     * A single layer of the system.
     */
    public static class Layer extends BaseParticleSystem.Layer
        implements Enqueueable
    {
        /**
         * Creates a new layer.
         */
        public Layer (GlContext ctx, Scope parentScope, BaseParticleSystemConfig.Layer config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        // documentation inherited from interface Enqueueable
        public void enqueue ()
        {
            // update the view transform
            if (_config.moveParticlesWithEmitter) {
                _parentViewTransform.compose(_config.transform, _viewTransform);
            } else {
                _viewTransform.set(_ctx.getCompositor().getCamera().getViewTransform());
            }

            // get the inverse of the view rotation and the view vector
            Alignment alignment = ((MetaParticleSystemConfig.Layer)_config).alignment;
            if (alignment != Alignment.FIXED) {
                _viewTransform.getRotation().invert(_vrot);
                if (alignment == Alignment.VELOCITY) {
                    _vrot.transformUnitZ(_view);
                }
            }

            // update the model transforms
            for (int ii = 0; ii < _living.value; ii++) {
                Model model = _models[ii];
                if (alignment != Alignment.FIXED) {
                    Particle particle = _particles[ii];
                    if (alignment == Alignment.VELOCITY) {
                        Vector3f velocity = particle.getVelocity();
                        _view.cross(velocity, _t);
                        float length = _t.length();
                        if (length > FloatMath.EPSILON) {
                            _t.multLocal(1f / length);
                            velocity.normalize(_s);
                            _s.cross(_t, _r);
                            _vrot.fromAxes(_s, _t, _r);
                        } else {
                            _vrot.set(Quaternion.IDENTITY);
                        }
                    }
                    _vrot.mult(particle.getOrientation(), model.getLocalTransform().getRotation());
                }
            }
        }

        @Override
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
                model.setVisible(ii < _living.value);
                if (_geometryRadius == 0f) {
                    model.setLocalTransform(new Transform3D());
                    Box bounds = model.getBounds();
                    if (!bounds.isEmpty()) {
                        float length = bounds.getDiagonalLength();
                        if (Float.isNaN(length)) {
                            log.warning("Diagonal length is not-a-number.",
                                "model", mconfig.model, "bounds", bounds);
                        } else {
                            _geometryRadius = length * 0.5f;
                        }
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

        @Override
        public void wasAdded ()
        {
            super.wasAdded();
            for (Model model : _models) {
                model.wasAdded(((MetaParticleSystem)_parentScope).getScene());
            }
        }

        @Override
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
                model.getColorState().getColor().set(particle.getColor());
                model.tick(elapsed);
                _parentBounds.addLocal(model.getBounds());
            }
            return false;
        }

        @Override
        public void composite ()
        {
            if (!_config.visible || _living.value == 0) {
                return;
            }
            // add an enqueueable to initialize the shared state
            _ctx.getCompositor().addEnqueueable(this);

            // composite the models
            for (int ii = 0; ii < _living.value; ii++) {
                _models[ii].composite();
            }
        }

        @Override
        public void dispose ()
        {
            super.dispose();
            for (Model model : _models) {
                model.dispose();
            }
        }

        @Override
        protected void swapParticles (int idx0, int idx1)
        {
            super.swapParticles(idx0, idx1);

            // swap the models
            Model tmp = _models[idx0];
            _models[idx0] = _models[idx1];
            _models[idx1] = tmp;
        }

        @Override
        protected void initParticle (int idx)
        {
            super.initParticle(idx);

            // reset the model
            _models[idx].reset();
            _models[idx].setVisible(true);
        }

        @Override
        protected void killParticle (int idx)
        {
            super.killParticle(idx);

            _models[idx].setVisible(false);
        }

        /** The models corresponding to each particle. */
        protected Model[] _models;

        /** The layer's view transform. */
        @Scoped
        protected Transform3D _viewTransform = new Transform3D(Transform3D.UNIFORM);

        /** Holds the view rotation. */
        protected Quaternion _vrot = new Quaternion();

        /** Holds the view vector. */
        protected Vector3f _view = new Vector3f();

        /** Holds the axis vectors. */
        protected Vector3f _s = new Vector3f(), _t = new Vector3f(), _r = new Vector3f();
    }

    /**
     * Creates a new meta particle system implementation.
     */
    public MetaParticleSystem (GlContext ctx, Scope parentScope, MetaParticleSystemConfig config)
    {
        super(ctx, parentScope);
        setConfig(ctx, config);
    }

    @Override
    protected BaseParticleSystem.Layer createLayer (BaseParticleSystemConfig.Layer config)
    {
        return new Layer(_ctx, this, config);
    }
}
