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

package com.threerings.opengl.effect.config;

import java.lang.ref.SoftReference;

import java.nio.ShortBuffer;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.probs.FloatFunctionVariable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.effect.FloatFunction;
import com.threerings.opengl.effect.ParticleGeometry;
import com.threerings.opengl.effect.ParticleSystem;
import com.threerings.opengl.geometry.Geometry;
import com.threerings.opengl.geometry.config.DeformerConfig;
import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

/**
 * The configuration of a particle system.
 */
public class ParticleSystemConfig extends BaseParticleSystemConfig
{
    /** The different alignment modes. */
    public enum Alignment { FIXED, BILLBOARD, VELOCITY };

    /**
     * A single layer of the system.
     */
    public static class Layer extends BaseParticleSystemConfig.Layer
    {
        /** The particle geometry. */
        @Editable(category="appearance", weight=-0.5)
        public ParticleGeometryConfig geometry = new Points();

        /** Whether or not to align particles' orientations with their velocities. */
        @Editable(category="appearance", weight=-0.5)
        public Alignment alignment = Alignment.BILLBOARD;

        /** The material to use for the particle system. */
        @Editable(category="appearance", weight=1.5, nullable=true)
        public ConfigReference<MaterialConfig> material =
            new ConfigReference<MaterialConfig>(DEFAULT_MATERIAL);

        /** The number of texture divisions in the S direction. */
        @Editable(category="appearance", weight=1.5, min=1)
        public int textureDivisionsS = 1;

        /** The number of texture divisions in the T direction. */
        @Editable(category="appearance", weight=1.5, min=1)
        public int textureDivisionsT = 1;

        /** Whether or not to sort the particles by depth before rendering. */
        @Editable(category="appearance", weight=2.5)
        public boolean depthSort;

        /** The render priority (higher priority layers are rendered above lower priority ones). */
        @Editable(category="appearance", weight=2.5, nullable=true)
        public GroupPriority priorityMode;

        /** Controls the particles' change of length over their lifespans. */
        @Editable(category="appearance", weight=3.5, min=0.0, step=0.01)
        public FloatFunctionVariable length =
            new FloatFunctionVariable.Fixed(new FloatFunction.Constant(0.1f));

        /** Controls the particles' change of texture frame over their lifespans. */
        @Editable(category="appearance", weight=3.5, min=0.0)
        public FloatFunctionVariable frame =
            new FloatFunctionVariable.Fixed(new FloatFunction.Constant(0f));

        /** The shared data array. */
        @DeepOmit
        public transient SoftReference<float[]> data;

        /** The shared indices. */
        @DeepOmit
        public transient SoftReference<ShortBuffer> indices;

        /** The shared element array buffer. */
        @DeepOmit
        public transient SoftReference<BufferObject> elementArrayBuffer;

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            data = null;
            indices = null;
            elementArrayBuffer = null;
        }

        @Override
        public boolean shouldRotateOrientations ()
        {
            return alignment == Alignment.FIXED;
        }

        @Override
        public void preload (GlContext ctx)
        {
            super.preload(ctx);
            new Preloadable.Config(MaterialConfig.class, material);
        }
    }

    /**
     * Determines how particles are rendered.
     */
    @EditorTypes({ Points.class, Lines.class, Quads.class, Meshes.class })
    public static abstract class ParticleGeometryConfig extends GeometryConfig
    {
        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Returns the number of segments in each particle.
         */
        public int getSegments ()
        {
            return 0;
        }

        /**
         * Returns whether or not to move the particle trails with the particles.
         */
        public boolean getMoveTrailsWithParticles ()
        {
            return true;
        }

        /**
         * Returns the radius of the geometry (used to expand the bounds).
         */
        public float getRadius (GlContext ctx)
        {
            return 1f;
        }

        @Override
        public Box getBounds ()
        {
            return Box.EMPTY; // not used
        }
    }

    /**
     * Renders particles as points.
     */
    public static class Points extends ParticleGeometryConfig
    {
        @Override
        public float getRadius (GlContext ctx)
        {
            return 0f;
        }

        @Override
        public Geometry createGeometry (
            GlContext ctx, Scope scope, DeformerConfig deformer, PassDescriptor[] passes)
        {
            return new ParticleGeometry.Points(ctx, scope, passes);
        }
    }

    /**
     * Renders particles as lines or line strips.
     */
    public static class Lines extends ParticleGeometryConfig
    {
        /** The number of segments in each particle. */
        @Editable(min=0, hgroup="s")
        public int segments;

        /** Whether or not to move the trails with the particles. */
        @Editable(hgroup="s")
        public boolean moveTrailsWithParticles = true;

        public Lines (Quads quads)
        {
            this.segments = quads.segments;
        }

        public Lines ()
        {
        }

        @Override
        public int getSegments ()
        {
            return segments;
        }

        @Override
        public boolean getMoveTrailsWithParticles ()
        {
            return moveTrailsWithParticles;
        }

        @Override
        public Geometry createGeometry (
            GlContext ctx, Scope scope, DeformerConfig deformer, PassDescriptor[] passes)
        {
            return (segments > 0) ?
                new ParticleGeometry.LineTrails(ctx, scope, passes, segments) :
                new ParticleGeometry.Lines(ctx, scope, passes);
        }
    }

    /**
     * Renders particles as quads or quad strips.
     */
    public static class Quads extends ParticleGeometryConfig
    {
        /** The number of segments in each particle. */
        @Editable(min=0, hgroup="s")
        public int segments;

        /** Whether or not to move the trails with the particles. */
        @Editable(hgroup="s")
        public boolean moveTrailsWithParticles = true;

        public Quads (Lines lines)
        {
            this.segments = lines.segments;
        }

        public Quads ()
        {
        }

        @Override
        public int getSegments ()
        {
            return segments;
        }

        @Override
        public boolean getMoveTrailsWithParticles ()
        {
            return moveTrailsWithParticles;
        }

        @Override
        public Geometry createGeometry (
            GlContext ctx, Scope scope, DeformerConfig deformer, PassDescriptor[] passes)
        {
            return (segments > 0) ?
                new ParticleGeometry.QuadTrails(ctx, scope, passes, segments) :
                new ParticleGeometry.Quads(ctx, scope, passes);
        }
    }

    /**
     * Renders particles as mesh instances.
     */
    public static class Meshes extends ParticleGeometryConfig
    {
        /** The model containing the mesh. */
        @Editable(mode="compact", nullable=true)
        public ConfigReference<ModelConfig> model;

        @Override
        public float getRadius (GlContext ctx)
        {
            GeometryConfig geom = getParticleGeometry(ctx);
            return (geom == null) ? 0f : geom.getBounds().getDiagonalLength() * 0.5f;
        }

        @Override
        public Geometry createGeometry (
            GlContext ctx, Scope scope, DeformerConfig deformer, PassDescriptor[] passes)
        {
            return ParticleGeometry.Meshes.create(ctx, scope, passes, getParticleGeometry(ctx));
        }

        /**
         * Returns the particle geometry for the model.
         */
        protected GeometryConfig getParticleGeometry (GlContext ctx)
        {
            ModelConfig config = ctx.getConfigManager().getConfig(ModelConfig.class, model);
            return (config == null) ? null : config.getParticleGeometry(ctx);
        }
    }

    /**
     * Controls the order in which layers are rendered within the system.
     */
    public static class GroupPriority extends DeepObject
        implements Exportable
    {
        /** The priority group to which the layer belongs. */
        @Editable(min=0)
        public int group;

        /** The priority within the group. */
        @Editable
        public int priority;
    }

    /** The layers comprising the system. */
    @Editable(editor="table")
    public Layer[] layers = new Layer[0];

    @Override
    public BaseParticleSystemConfig.Layer[] getLayers ()
    {
        return layers;
    }

    @Override
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (impl instanceof ParticleSystem) {
            ((ParticleSystem)impl).setConfig(ctx, this);
        } else {
            impl = new ParticleSystem(ctx, scope, this);
        }
        return impl;
    }

    @Override
    public void invalidate ()
    {
        for (Layer layer : layers) {
            layer.invalidate();
        }
    }

    /** The default layer material. */
    protected static final String DEFAULT_MATERIAL = "Model/Translucent";
}
