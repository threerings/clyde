//
// $Id$

package com.threerings.opengl.eff;

import java.util.Comparator;

import com.samskivert.util.QuickSort;

import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Box;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.effect.Particle;
import com.threerings.opengl.effect.config.BaseParticleSystemConfig;
import com.threerings.opengl.effect.config.ParticleSystemConfig;
import com.threerings.opengl.effect.config.ParticleSystemConfig.GroupPriority;
import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;

/**
 * The particle system model implementation.
 */
public class ParticleSystem extends BaseParticleSystem
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

            // transform state depends on whether we use local or world coordinates
            _transformState = config.moveParticlesWithEmitter ? new TransformState() :
                ScopeUtil.resolve(
                    _parentScope, "viewTransformState",
                    TransformState.IDENTITY, TransformState.class);

            // recreate the surface
            if (_surface != null) {
                _surface.dispose();
            }
            ParticleSystemConfig.Layer psconfig = (ParticleSystemConfig.Layer)config;
            _surface = new Surface(
                _ctx, this, psconfig.geometry,
                _ctx.getConfigManager().getConfig(MaterialConfig.class, psconfig.material));

            // get the geometry radius
            _geometryRadius = psconfig.geometry.getRadius(_ctx);
        }

        @Override // documentation inherited
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
            ParticleSystemConfig.Layer psconfig = (ParticleSystemConfig.Layer)_config;
            if (psconfig.depthSort) {
                Transform3D xform = _transformState.getModelview();
                for (int ii = 0, nn = _living.value; ii < nn; ii++) {
                    Particle particle = _particles[ii];
                    particle.depth = xform.transformPointZ(particle.getPosition());
                }
                QuickSort.sort(_particles, 0, _living.value - 1, DEPTH_COMP);
            }

            // update the center if necessary
            GroupPriority priorityMode = psconfig.priorityMode;
            if (priorityMode != null) {
                Box bounds = ((ParticleSystem)_parentScope).getGroupBounds(priorityMode.group);
                bounds.getCenter(_center);
                Transform3D xform = _ctx.getCompositor().getCamera().getWorldTransform();
                xform.getRotation().transformUnitZ(_vector).multLocal(
                    priorityMode.priority * 0.0001f);
                pointToLayer(_center.addLocal(_vector), false);
            }

            // enqueue the surface
            _surface.enqueue();
        }

        @Override // documentation inherited
        public void dispose ()
        {
            super.dispose();
            if (_surface != null) {
                _surface.dispose();
            }
        }

        @Override // documentation inherited
        protected void initParticle (int idx)
        {
            ParticleSystemConfig.Layer psconfig = (ParticleSystemConfig.Layer)_config;
            _particles[idx].init(
                _config.lifespan.getValue(), _config.alphaMode, _config.color, _config.size,
                (psconfig.geometry.getSegments() > 0) ? psconfig.length : null,
                (psconfig.textureDivisionsS > 1 || psconfig.textureDivisionsT > 1) ?
                    psconfig.frame : null);
        }

        @Override // documentation inherited
        protected void addBounds ()
        {
            // get the center of the bounds before transforming them
            ParticleSystemConfig.Layer psconfig = (ParticleSystemConfig.Layer)_config;
            GroupPriority priorityMode = psconfig.priorityMode;
            if (priorityMode == null) {
                _bounds.getCenter(_center);
            }
            super.addBounds();

            // add layer bounds to group bounds, if applicable
            if (priorityMode != null) {
                ((ParticleSystem)_parentScope).getGroupBounds(
                    priorityMode.group).addLocal(_bounds);
            }
        }

        /** The shared transform state. */
        @Scoped
        protected TransformState _transformState = new TransformState();

        /** The layer center. */
        @Scoped
        protected Vector3f _center = new Vector3f();

        /** The layer surface. */
        protected Surface _surface;
    }

    /**
     * Creates a new particle system implementation.
     */
    public ParticleSystem (GlContext ctx, Scope parentScope, ParticleSystemConfig config)
    {
        super(ctx, parentScope);
        setConfig(config);
    }

    @Override // documentation inherited
    protected BaseParticleSystem.Layer createLayer (BaseParticleSystemConfig.Layer config)
    {
        return new Layer(_ctx, this, config);
    }

    @Override // documentation inherited
    protected void resetBounds ()
    {
        super.resetBounds();
        for (Box bounds : _groupBounds) {
            bounds.setToEmpty();
        }
    }

    /**
     * Returns the bounds of the group at the specified index.
     */
    protected Box getGroupBounds (int idx)
    {
        if (_groupBounds.length <= idx) {
            Box[] obounds = _groupBounds;
            _groupBounds = new Box[idx + 1];
            System.arraycopy(obounds, 0, _groupBounds, 0, obounds.length);
            for (int ii = obounds.length; ii < _groupBounds.length; ii++) {
                _groupBounds[ii] = new Box();
            }
        }
        return _groupBounds[idx];
    }

    /** World space bounds of each group. */
    protected Box[] _groupBounds = new Box[0];

    /** Sorts particles by decreasing depth. */
    protected static final Comparator<Particle> DEPTH_COMP = new Comparator<Particle>() {
        public int compare (Particle p1, Particle p2) {
            return Float.compare(p1.depth, p2.depth);
        }
    };
}
