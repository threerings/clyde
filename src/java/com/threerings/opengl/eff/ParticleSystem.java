//
// $Id$

package com.threerings.opengl.eff;

import java.util.IdentityHashMap;

import com.google.common.collect.Maps;

import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;

import com.threerings.opengl.effect.Particle;
import com.threerings.opengl.effect.config.ParticleSystemConfig;
import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;

/**
 * The particle system model implementation.
 */
public class ParticleSystem extends Model.Implementation
{
    /**
     * A single layer of the system.
     */
    public static class Layer extends SimpleScope
    {
        /**
         * Creates a new layer.
         */
        public Layer (GlContext ctx, Scope parentScope, ParticleSystemConfig.Layer config)
        {
            super(parentScope);
            _ctx = ctx;
            setConfig(config);
        }

        /**
         * Sets the configuration of this layer.
         */
        public void setConfig (ParticleSystemConfig.Layer config)
        {
            _config = config;

            // recreate the particles and adjust the counts
            Particle[] oparts = _particles;
            _particles = new Particle[config.particleCount];
            for (int ii = 0; ii < _particles.length; ii++) {
                _particles[ii] = (oparts == null || oparts.length <= ii) ?
                    new Particle() : oparts[ii];
            }
            if (oparts == null) {
                _living = 0;
                _preliving = _particles.length;
            } else {
                _living = Math.min(_living, _particles.length);
                _preliving = Math.min(_living + _preliving, _particles.length) - _living;
            }

            // recreate the surface
            if (_surface != null) {
                _surface.dispose();
            }
            _surface = new Surface(
                _ctx, this, config.geometry,
                _ctx.getConfigManager().getConfig(MaterialConfig.class, config.material));
        }

        /**
         * Returns a reference to the layer config.
         */
        public ParticleSystemConfig.Layer getConfig ()
        {
            return _config;
        }

        /**
         * Determines whether the layer has completed.
         */
        public boolean hasCompleted ()
        {
            return _living == 0 && _preliving == 0 && !_config.respawnDeadParticles;
        }

        /**
         * Resets the state of the layer.
         */
        public void reset ()
        {
            // clear the elapsed time
            _total = 0f;

            // reset the counts
            _living = 0;
            _preliving = _config.particleCount;
        }

        /**
         * Updates the current particle state based on the elapsed time in seconds.
         */
        public void tick (float elapsed)
        {
            if ((_total += elapsed) < _config.startTime) {
                return;
            }
            elapsed *= _config.timeScale;


        }

        /**
         * Enqueues the layer for rendering.
         */
        public void enqueue ()
        {
            if (!_config.visible || _living == 0) {
                return;
            }
            // enqueue the surface
            _surface.enqueue();
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "layer";
        }

        /** The application context. */
        protected GlContext _ctx;

        /** The layer configuration. */
        protected ParticleSystemConfig.Layer _config;

        /** The number of particles currently alive. */
        protected int _living;

        /** The number of particles currently "pre-alive." */
        protected int _preliving;

        /** The layer surface. */
        protected Surface _surface;

        /** The particles in the layer (first the living particles, then the pre-living particles,
         * then the dead particles). */
        @Scoped
        protected Particle[] _particles;

        /** The shared transform state. */
        @Scoped
        protected TransformState _transformState = new TransformState();

        /** The total time elapsed since reset. */
        protected float _total;
    }

    /**
     * Creates a new particle system implementation.
     */
    public ParticleSystem (GlContext ctx, Scope parentScope, ParticleSystemConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (ParticleSystemConfig config)
    {
        _config = config;
        updateFromConfig();
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // enqueue the layers
        for (Layer layer : _layers) {
            layer.enqueue();
        }
    }

    @Override // documentation inherited
    public boolean hasCompleted ()
    {
        for (Layer layer : _layers) {
            if (!layer.hasCompleted()) {
                return false;
            }
        }
        return true;
    }

    @Override // documentation inherited
    public void reset ()
    {
        for (Layer layer : _layers) {
            layer.reset();
        }
    }

    @Override // documentation inherited
    public boolean requiresTick ()
    {
        return true;
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        // tick the layers
        for (Layer layer : _layers) {
            layer.tick(elapsed);
        }
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        // save the old layers (if any)
        IdentityHashMap<ParticleSystemConfig.Layer, Layer> olayers = Maps.newIdentityHashMap();
        if (_layers != null) {
            for (Layer layer : _layers) {
                olayers.put(layer.getConfig(), layer);
            }
        }

        // (re)create the layers
        _layers = new Layer[_config.layers.length];
        int idx = 0;
        for (ParticleSystemConfig.Layer config : _config.layers) {
            Layer layer = olayers.get(config);
            if (layer != null) {
                layer.setConfig(config);
            } else {
                layer = new Layer(_ctx, this, config);
            }
            _layers[idx++] = layer;
        }
    }

    /**
     * Notes that the system has completed.
     */
    protected void completed ()
    {
        // notify containing model
        ((Model)_parentScope).completed();
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The system configuration. */
    protected ParticleSystemConfig _config;

    /** The layers of the system. */
    protected Layer[] _layers;
}
