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

import java.util.IdentityHashMap;

import com.google.common.collect.Maps;

import com.threerings.expr.Bound;
import com.threerings.expr.MutableInteger;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.math.Box;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.compositor.Enqueueable;
import com.threerings.opengl.effect.config.BaseParticleSystemConfig;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig.TransientPolicy;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;

/**
 * Base class for {@link ParticleSystem} and {@link MetaParticleSystem}.
 */
public abstract class BaseParticleSystem extends Model.Implementation
    implements Enqueueable
{
    /**
     * A single layer of the system.
     */
    public static abstract class Layer extends SimpleScope
    {
        /**
         * Creates a new layer.
         */
        public Layer (GlContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
        }

        /**
         * Sets the layer's config.
         */
        public void setConfig (BaseParticleSystemConfig.Layer config)
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
            _shooter = config.shooter.createShooter(this);

            // create the influences
            _influences = new Influence[config.influences.length];
            for (int ii = 0; ii < _influences.length; ii++) {
                _influences[ii] = config.influences[ii].createInfluence(this);
            }
        }

        /**
         * Returns a reference to the layer's config.
         */
        public BaseParticleSystemConfig.Layer getConfig ()
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
         * Notes that the layer was added to the scene.
         */
        public void wasAdded ()
        {
            // nothing by default
        }

        /**
         * Notes that the layer will be removed from the scene.
         */
        public void willBeRemoved ()
        {
            // nothing by default
        }

        /**
         * Resets the state of the layer.
         */
        public void reset ()
        {
            // clear the elapsed time and completion flag
            _total = 0f;
            _completed = false;

            // reset the counter and counts
            _counter.reset();
            _living.value = 0;
            _preliving = _particles.length;
        }

        /**
         * Updates the current particle state based on the elapsed time in seconds.
         *
         * @return true if this layer has completed, false if it is still active.
         */
        public boolean tick (float elapsed)
        {
            if (!_config.visible || _completed) {
                return true;
            } else if ((_total += elapsed) <= _config.startTime) {
                return false;
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
            float scale = 1f;
            if (!_config.moveParticlesWithEmitter) {
                scale = _worldTransform.approximateUniformScale();
            }
            for (int ii = 0; ii < _living.value; ii++) {
                Particle particle = _particles[ii];
                if (particle.tick(elapsed)) {
                    // apply the influences
                    for (Influence influence : _influences) {
                        influence.apply(particle);
                    }
                    // modulate by the inherited color, if any
                    if (_colorState != null) {
                        particle.getColor().multLocal(_colorState.getColor());
                    }
                    // multiply by the inherited scale, if any
                    if (!_config.moveParticlesWithEmitter) {
                        particle.setSize(particle.getSize() * scale);
                    }
                    // add to bounds
                    _bounds.addLocal(particle.getPosition());
                    msize = Math.max(msize, particle.getSize());

                } else {
                    killParticle(ii);

                    // move this particle to the end of the list
                    if (ii != --_living.value) {
                        swapParticles(ii, _living.value);
                        ii--; // update the swapped particle on the next iteration
                    }
                    // then to the end of the preliving list
                    if (_preliving != 0) {
                        swapParticles(_living.value, _living.value + _preliving);
                    }
                }
            }

            // check for completion
            if (_living.value == 0 && _preliving == 0 && !_config.respawnDeadParticles) {
                return (_completed = true);
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
                if (_config.shouldRotateOrientations()) {
                    boolean emitter = _config.rotateOrientationsWithEmitter;
                    rotationToLayer(particle.getOrientation(), emitter);
                    vectorToLayer(particle.getAngularVelocity(), emitter);
                }
                initParticle(ii);
                _living.value++;
                _preliving = Math.max(_preliving - 1, 0);
                _bounds.addLocal(particle.getPosition());
                msize = Math.max(msize, particle.getSize());
            }

            // expand the bounds (TODO: account for tails)
            if (_bounds.isEmpty()) {
                return false;
            }
            float amount = _geometryRadius * msize;
            _bounds.expandLocal(amount, amount, amount);

            // add bounds to parent
            addBounds();

            return false;
        }

        /**
         * Transforms a point in-place from world space or emitter space into the space of
         * the layer (either world space or emitter space, depending on the value of
         * {@link com.threerings.opengl.effect.config.BaseParticleSystemConfig.Layer#moveParticlesWithEmitter}).
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
         * {@link com.threerings.opengl.effect.config.BaseParticleSystemConfig.Layer#moveParticlesWithEmitter}).
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
         * Transforms a rotation in-place from world space or emitter space into the space of
         * the layer (either world space or emitter space, depending on the value of
         * {@link com.threerings.opengl.effect.config.BaseParticleSystemConfig.Layer#moveParticlesWithEmitter}).
         *
         * @param emitter if true, transform from emitter space (else from world space).
         * @return a reference to the transformed rotation, for chaining.
         */
        public Quaternion rotationToLayer (Quaternion rot, boolean emitter)
        {
            return _config.moveParticlesWithEmitter ?
                (emitter ? rot : _worldTransformInv.extractRotation(_wrot).mult(rot, rot)) :
                (emitter ? _worldTransform.extractRotation(_wrot).mult(rot, rot) : rot);
        }

        /**
         * Draws the bounds of the layer.
         */
        public void drawBounds ()
        {
            DebugBounds.draw(_bounds, Color4f.GRAY);
        }

        /**
         * Composites the layer for rendering.
         */
        public abstract void composite ();

        @Override
        public String getScopeName ()
        {
            return "layer";
        }

        /**
         * Swaps the two particles at the specified indices.
         */
        protected void swapParticles (int idx0, int idx1)
        {
            Particle tmp = _particles[idx0];
            _particles[idx0] = _particles[idx1];
            _particles[idx1] = tmp;
        }

        /**
         * Initializes the particle at the specified index.
         */
        protected void initParticle (int idx)
        {
            _particles[idx].init(_config.lifespan.getValue(), _config.alphaMode,
                _config.color, _config.size, null, null, null);
        }

        /**
         * Kills any state revolving around the particle.
         */
        protected void killParticle (int idx)
        {
            // DO NOTHING!
        }

        /**
         * Adds this layer's bounds to those of its parent.
         */
        protected void addBounds ()
        {
            // add layer bounds to the system bounds
            if (_config.moveParticlesWithEmitter) {
                _bounds.transformLocal(_worldTransform);
            }
            _parentBounds.addLocal(_bounds);
        }

        /** The application context. */
        protected GlContext _ctx;

        /** The layer configuration. */
        @Scoped
        protected BaseParticleSystemConfig.Layer _config;

        /** The parent view transform. */
        @Bound("viewTransform")
        protected Transform3D _parentViewTransform;

        /** The parent world transform. */
        @Bound("worldTransform")
        protected Transform3D _parentWorldTransform;

        /** The parent bounds. */
        @Bound("nbounds")
        protected Box _parentBounds;

        /** The inherited color state. */
        @Bound
        protected ColorState _colorState;

        /** The layer's transform in world space. */
        @Scoped
        protected Transform3D _worldTransform = new Transform3D();

        /** The inverse of the world space transform. */
        protected Transform3D _worldTransformInv = new Transform3D();

        /** The bounds of the layer. */
        @Scoped
        protected Box _bounds = new Box();

        /** The radius of the geometry (used to expand the bounds). */
        protected float _geometryRadius;

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

        /** The total time elapsed since reset. */
        protected float _total;

        /** Whether or not the layer has completed. */
        protected boolean _completed;

        /** Holds the world/world inverse rotation. */
        protected Quaternion _wrot = new Quaternion();
    }

    /**
     * Creates a new particle system implementation.
     */
    public BaseParticleSystem (GlContext ctx, Scope parentScope)
    {
        super(parentScope);
        _ctx = ctx;
    }

    /**
     * Sets the configuration of this system.
     */
    public void setConfig (GlContext ctx, BaseParticleSystemConfig config)
    {
        _ctx = ctx;
        _config = config;
        updateFromConfig();
    }

    // documentation inherited from interface Enqueueable
    public void enqueue ()
    {
        // update the view transform
        _parentViewTransform.compose(_localTransform, _viewTransform);
    }

    @Override
    public boolean hasCompleted ()
    {
        return _completed;
    }

    @Override
    public void reset ()
    {
        for (Layer layer : _layers) {
            layer.reset();
        }
        _warmed = _completed = false;
    }

    @Override
    public int getInfluenceFlags ()
    {
        return _influenceFlags;
    }

    @Override
    public Box getBounds ()
    {
        return _bounds;
    }

    @Override
    public void updateBounds ()
    {
        tick(0f);
    }

    @Override
    public void drawBounds ()
    {
        DebugBounds.draw(_bounds, Color4f.WHITE);
        for (Layer layer : _layers) {
            layer.drawBounds();
        }
    }

    @Override
    public void dumpInfo (String prefix)
    {
        System.out.println(prefix + "ParticleSystem: " + _worldTransform + " " + _bounds);
    }

    @Override
    public TransientPolicy getTransientPolicy ()
    {
        return _transientPolicy;
    }

    @Override
    public TickPolicy getTickPolicy ()
    {
        return _tickPolicy;
    }

    @Override
    public void wasAdded ()
    {
        super.wasAdded();
        for (Layer layer : _layers) {
            layer.wasAdded();
        }
    }

    @Override
    public void willBeRemoved ()
    {
        super.willBeRemoved();
        for (Layer layer : _layers) {
            layer.willBeRemoved();
        }
    }

    @Override
    public void tick (float elapsed)
    {
        // if we're completed, there's nothing more to do
        if (_completed || _layers.length == 0) {
            return;
        }

        // update the world transform
        if (_parentWorldTransform == null) {
            _worldTransform.set(_localTransform);
        } else {
            _parentWorldTransform.compose(_localTransform, _worldTransform);
        }

        // the first non-zero elapsed interval triggers the warmup
        if (!_warmed && elapsed > 0f) {
            float remaining = _config.warmupTime;
            while (remaining > 0f) {
                float welapsed = Math.min(remaining, _config.warmupGranularity);
                for (Layer layer : _layers) {
                    layer.tick(welapsed);
                }
                remaining -= welapsed;
            }
            _warmed = true;
        }

        // reset the bounds
        resetBounds();

        // tick the layers (they will expand the bounds)
        _completed = true;
        for (Layer layer : _layers) {
            _completed &= layer.tick(elapsed);
        }

        // update the bounds if necessary
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange(this);
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange(this);
        }

        // notify containing model if completed
        if (_completed) {
            ((Model)_parentScope).completed(this);
        }
    }

    @Override
    public void composite ()
    {
        // add an enqueueable to initialize the shared state
        _ctx.getCompositor().addEnqueueable(this);

        // composite the layers
        for (Layer layer : _layers) {
            layer.composite();
        }
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        // save the old layers (if any)
        IdentityHashMap<BaseParticleSystemConfig.Layer, Layer> olayers = Maps.newIdentityHashMap();
        if (_layers != null) {
            for (Layer layer : _layers) {
                olayers.put(layer.getConfig().identity, layer);
            }
        }

        // update the influence flags
        _influenceFlags = _config.influences.getFlags();

        // update the tick policy if necessary
        TickPolicy npolicy = _config.tickPolicy;
        if (npolicy == TickPolicy.DEFAULT) {
            npolicy = _config.anyLayersRespawn() ? TickPolicy.WHEN_VISIBLE : TickPolicy.ALWAYS;
        }
        if (_tickPolicy != npolicy) {
            ((Model)_parentScope).tickPolicyWillChange(this);
            _tickPolicy = npolicy;
            ((Model)_parentScope).tickPolicyDidChange(this);
        }

        TransientPolicy tpolicy = _config.transientPolicy;
        if (tpolicy == TransientPolicy.DEFAULT) {
            tpolicy = TransientPolicy.FRUSTUM;
        }
        _transientPolicy = tpolicy;

        // (re)create the layers
        BaseParticleSystemConfig.Layer[] configs = _config.getLayers();
        _layers = new Layer[configs.length];
        Scene scene = getScene();
        for (int ii = 0; ii < _layers.length; ii++) {
            BaseParticleSystemConfig.Layer config = configs[ii];
            Layer layer = olayers.remove(config.identity);
            if (layer != null) {
                layer.setConfig(config);
            } else {
                layer = createLayer(config);
            }
            _layers[ii] = layer;
            if (scene != null) {
                layer.wasAdded();
            }
        }
        for (Layer layer : olayers.values()) {
            if (scene != null) {
                layer.willBeRemoved();
            }
            layer.dispose(); // dispose of the unrecycled old layers
        }

        // update the bounds
        updateBounds();
    }

    protected Scene getScene ()
    {
        return ((Model)_parentScope).getScene(this);
    }

    /**
     * Creates a new layer for the supplied config.
     */
    protected abstract Layer createLayer (BaseParticleSystemConfig.Layer config);

    /**
     * Resets the bounds before the tick.
     */
    protected void resetBounds ()
    {
        float expand = _config.boundsExpansion;
        Box.ZERO.expand(expand, expand, expand, _nbounds).transformLocal(_worldTransform);
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The system configuration. */
    protected BaseParticleSystemConfig _config;

    /** The layers of the system. */
    protected Layer[] _layers;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** The view transform. */
    @Scoped
    protected Transform3D _viewTransform = new Transform3D();

    /** Flags indicating which influences can affect the system. */
    protected int _influenceFlags;

    /** The bounds of the system. */
    @Scoped
    protected Box _bounds = new Box();

    /** Holds the bounds of the model when updating. */
    @Scoped
    protected Box _nbounds = new Box();

    /** World space bounds of each group. */
    protected Box[] _groupBounds = new Box[0];

    /** The model's tick policy. */
    protected TickPolicy _tickPolicy;

    /** The model's transient policy. */
    protected TransientPolicy _transientPolicy;

    /** If true, the particle system has warmed up. */
    protected boolean _warmed;

    /** If true, the particle system has completed. */
    protected boolean _completed;

    /** Working vector. */
    protected static Vector3f _vector = new Vector3f();
}
