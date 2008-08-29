//
// $Id$

package com.threerings.opengl.eff;

import java.util.Comparator;
import java.util.IdentityHashMap;

import com.google.common.collect.Maps;

import com.samskivert.util.QuickSort;

import com.threerings.expr.Bound;
import com.threerings.expr.MutableInteger;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Box;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.camera.Camera;
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
                _living.value = 0;
                _preliving = _particles.length;
            } else {
                _living.value = Math.min(_living.value, _particles.length);
                _preliving = Math.min(_living.value + _preliving, _particles.length) -
                    _living.value;
            }

            // create the counter, placer, and shooter
            _counter = config.counter.createCounter();
            _placer = config.placer.createPlacer(this);
            _shooter = config.shooter.createShooter();

            // create the influences
            _influences = new Influence[config.influences.length];
            for (int ii = 0; ii < _influences.length; ii++) {
                _influences[ii] = config.influences[ii].createInfluence(this);
            }

            // transform state depends on whether we use local or world coordinates
            _transformState = config.moveParticlesWithEmitter ? new TransformState() :
                ScopeUtil.resolve(
                    _parentScope, "viewTransformState",
                    TransformState.IDENTITY, TransformState.class);

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
         * Returns a reference to the camera.
         */
        public Camera getCamera ()
        {
            return _ctx.getCompositor().getCamera();
        }

        /**
         * Determines whether the layer has completed.
         */
        public boolean hasCompleted ()
        {
            return _living.value == 0 && _preliving == 0 && !_config.respawnDeadParticles;
        }

        /**
         * Resets the state of the layer.
         */
        public void reset ()
        {
            // clear the elapsed time
            _total = 0f;

            // reset the counts
            _living.value = 0;
            _preliving = _particles.length;
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

            // update the world transform and its inverse
            _parentWorldTransform.compose(_config.transform, _worldTransform).invert(
                _worldTransformInv);

            // tick the influences
            for (Influence influence : _influences) {
                influence.tick(elapsed);
            }

            // update the living particles and the bounds
            _bounds.setToEmpty();
            float msize = 0f;
            for (int ii = 0; ii < _living.value; ii++) {
                Particle particle = _particles[ii];
                if (particle.tick(elapsed)) {
                    // apply the influences
                    for (Influence influence : _influences) {
                        influence.apply(particle);
                    }
                    // add to bounds
                    _bounds.addLocal(particle.getPosition());
                    msize = Math.max(msize, particle.getSize());

                } else {
                    // move this particle to the end of the list
                    if (ii != --_living.value) {
                        _particles[ii] = _particles[_living.value];
                        _particles[_living.value] = particle;
                        ii--; // update the swapped particle on the next iteration
                    }
                    // then to the end of the preliving list
                    if (_preliving != 0) {
                        int idx = _living.value + _preliving;
                        _particles[_living.value] = _particles[idx];
                        _particles[idx] = particle;
                    }
                }
            }

            // find out how many particles the counter thinks we should emit
            int count = _counter.count(elapsed, _config.respawnDeadParticles ?
                (_particles.length - _living.value) : _preliving);

            // spawn those particles
            for (int ii = _living.value, nn = _living.value + count; ii < nn; ii++) {
                Particle particle = _particles[ii];
                _placer.place(particle);
                _config.orientation.getValue(particle.getOrientation());
                vectorToLayer(
                    _shooter.shoot(particle).multLocal(_config.speed.getValue()),
                    _config.rotateVelocitiesWithEmitter);
                _config.angularVelocity.getValue(particle.getAngularVelocity());
                particle.init(
                    _config.lifespan.getValue(),
                    _config.alphaMode, _config.color, _config.size,
                    (_config.geometry.getSegments() > 0) ? _config.length : null,
                    (_config.textureDivisionsS > 1 || _config.textureDivisionsT > 1) ?
                        _config.frame : null);
                _living.value++;
                _preliving = Math.max(_preliving - 1, 0);
                _bounds.addLocal(particle.getPosition());
                msize = Math.max(msize, particle.getSize());
            }
        }

        /**
         * Transforms a point in-place from world space or emitter space into the space of
         * the layer (either world space or emitter space, depending on the value of
         * {@link ParticleSystemConfig.Layer#moveParticlesWithEmitter}).
         *
         * @param emitter if true, transform from emitter space (else from world space).
         * @return a reference to the transformed point, for chaining.
         */
        public Vector3f pointToLayer (Vector3f point, boolean emitter)
        {
            return _config.moveParticlesWithEmitter ?
                (emitter ? point : _worldTransformInv.transformPointLocal(point)) :
                (emitter ? _worldTransform.transformPointLocal(point) : point);
        }

        /**
         * Transforms a vector in-place from world space or emitter space into the space of
         * the layer (either world space or emitter space, depending on the value of
         * {@link ParticleSystemConfig.Layer#moveParticlesWithEmitter}).
         *
         * @param emitter if true, transform from emitter space (else from world space).
         * @return a reference to the transformed vector, for chaining.
         */
        public Vector3f vectorToLayer (Vector3f vector, boolean emitter)
        {
            return _config.moveParticlesWithEmitter ?
                (emitter ? vector : _worldTransformInv.transformVectorLocal(vector)) :
                (emitter ? _worldTransform.transformVectorLocal(vector) : vector);
        }

        /**
         * Enqueues the layer for rendering.
         */
        public void enqueue ()
        {
            if (!_config.visible || _living.value == 0) {
                return;
            }
            // update the transform state if necessary
            if (_config.moveParticlesWithEmitter) {
                _parentViewTransform.compose(_config.transform, _transformState.getModelview());
                _transformState.setDirty(true);
            }

            // sort by depth if so required (TODO: radix or incremental sort?)
            if (_config.depthSort) {
                Transform3D xform = _transformState.getModelview();
                for (int ii = 0, nn = _living.value; ii < nn; ii++) {
                    Particle particle = _particles[ii];
                    particle.depth = xform.transformPointZ(particle.getPosition());
                }
                QuickSort.sort(_particles, 0, _living.value - 1, DEPTH_COMP);
            }

            // enqueue the surface
            _surface.enqueue();
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "layer";
        }

        @Override // documentation inherited
        public void dispose ()
        {
            super.dispose();
            if (_surface != null) {
                _surface.dispose();
            }
        }

        /** The application context. */
        protected GlContext _ctx;

        /** The layer configuration. */
        @Scoped
        protected ParticleSystemConfig.Layer _config;

        /** The parent view transform. */
        @Bound("viewTransform")
        protected Transform3D _parentViewTransform;

        /** The parent world transform. */
        @Bound("worldTransform")
        protected Transform3D _parentWorldTransform;

        /** The layer's transform in world space. */
        protected Transform3D _worldTransform = new Transform3D();

        /** The inverse of the world space transform. */
        protected Transform3D _worldTransformInv = new Transform3D();

        /** The bounds of the layer. */
        protected Box _bounds = new Box();

        /** The particles in the layer (first the living particles, then the pre-living particles,
         * then the dead particles). */
        @Scoped
        protected Particle[] _particles;

        /** The particle counter. */
        protected Counter _counter;

        /** The particle placer. */
        protected Placer _placer;

        /** The particle shooter. */
        protected Shooter _shooter;

        /** The particle influences. */
        protected Influence[] _influences;

        /** The number of particles currently alive. */
        @Scoped
        protected MutableInteger _living = new MutableInteger();

        /** The number of particles currently "pre-alive." */
        protected int _preliving;

        /** The shared transform state. */
        @Scoped
        protected TransformState _transformState = new TransformState();

        /** The layer surface. */
        protected Surface _surface;

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
        // update the view transform
        _parentViewTransform.compose(_localTransform, _viewTransform);

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
        // update the world transform
        _parentWorldTransform.compose(_localTransform, _worldTransform);

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
        for (int ii = 0; ii < _layers.length; ii++) {
            ParticleSystemConfig.Layer config = _config.layers[ii];
            Layer layer = olayers.remove(config);
            if (layer != null) {
                layer.setConfig(config);
            } else {
                layer = new Layer(_ctx, this, config);
            }
            _layers[ii] = layer;
        }
        for (Layer layer : olayers.values()) {
            layer.dispose(); // dispose of the unrecycled old layers
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

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The view transform. */
    @Scoped
    protected Transform3D _viewTransform = new Transform3D();

    /** The world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** Sorts particles by decreasing depth. */
    protected static final Comparator<Particle> DEPTH_COMP = new Comparator<Particle>() {
        public int compare (Particle p1, Particle p2) {
            return Float.compare(p1.depth, p2.depth);
        }
    };
}
