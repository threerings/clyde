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

import java.io.IOException;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;
import com.threerings.math.Transform3D;
import com.threerings.probs.ColorFunctionVariable;
import com.threerings.probs.FloatFunctionVariable;
import com.threerings.probs.FloatVariable;
import com.threerings.probs.QuaternionVariable;
import com.threerings.probs.VectorVariable;
import com.threerings.util.DeepObject;
import com.threerings.util.Shallow;

import com.threerings.opengl.effect.AlphaMode;
import com.threerings.opengl.effect.ColorFunction;
import com.threerings.opengl.effect.FloatFunction;
import com.threerings.opengl.model.config.InfluenceFlagConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.model.config.ModelConfig.TransientPolicy;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

/**
 * Base class for {@link ParticleSystemConfig} and {@link MetaParticleSystemConfig}.
 */
public abstract class BaseParticleSystemConfig extends ModelConfig.Implementation
    implements Preloadable.LoadableConfig
{
    /**
     * A single layer of the system.
     */
    public static abstract class Layer extends DeepObject
        implements Exportable, Preloadable.LoadableConfig
    {
        /** The name of the layer. */
        @Editable(column=true)
        public String name = "";

        /** Whether or not the layer is visible. */
        @Editable(column=true)
        public boolean visible = true;

        /** The particle count. */
        @Editable(category="appearance", weight=1, min=0)
        public int particleCount = 100;

        /** Determines how colors are modified according to their alpha values. */
        @Editable(category="appearance", weight=2)
        public AlphaMode alphaMode = AlphaMode.TRANSLUCENT;

        /** Controls the particles' change of color over their lifespans. */
        @Editable(category="appearance", weight=3, mode="alpha")
        public ColorFunctionVariable color =
            new ColorFunctionVariable.Fixed(new ColorFunction.Constant());

        /** Controls the particles' change of size over their lifespans. */
        @Editable(category="appearance", weight=3, min=0.0, step=0.01)
        public FloatFunctionVariable size =
            new FloatFunctionVariable.Fixed(new FloatFunction.Constant(0.1f));

        /** The emitter transform. */
        @Editable(category="origin", mode="rigid", step=0.01)
        public Transform3D transform = new Transform3D();

        /** Controls the particles' initial positions. */
        @Editable(category="origin")
        public PlacerConfig placer = new PlacerConfig.Point();

        /** Controls the particles' initial orientations. */
        @Editable(category="origin")
        public QuaternionVariable orientation = new QuaternionVariable.Constant();

        /** Whether or not to rotate the particles' initial orientations with the emitter. */
        @Editable(category="origin")
        public transient boolean rotateOrientationsWithEmitter;

        /** Whether or not to move particles with the emitter. */
        @Editable(category="origin")
        public boolean moveParticlesWithEmitter;

        /** Controls the particles' initial directions. */
        @Editable(category="emission")
        public ShooterConfig shooter = new ShooterConfig.Cone();

        /** Controls the particles' initial speeds. */
        @Editable(category="emission", min=0.0, step=0.01)
        public FloatVariable speed = new FloatVariable.Uniform(1f, 1.5f);

        /** Controls the particles' initial angular velocities. */
        @Editable(category="emission", scale=Math.PI/180.0)
        public VectorVariable angularVelocity = new VectorVariable.Constant();

        /** Whether or not to rotate the velocities with the emitter. */
        @Editable(category="emission")
        public boolean rotateVelocitiesWithEmitter = true;

        /** Controls the number of particles emitted at each frame. */
        @Editable(category="flow")
        public CounterConfig counter = new CounterConfig.Unlimited();

        /** Whether or not to respawn dead particles. */
        @Editable(category="flow")
        public boolean respawnDeadParticles = true;

        /** Controls the particles' lifespans. */
        @Editable(category="world", min=0.0, step=0.01)
        public FloatVariable lifespan = new FloatVariable.Uniform(1f, 1.5f);

        /** The time at which to start the layer. */
        @Editable(category="world", min=0.0, step=0.01)
        public float startTime;

        /** The layer's speed multiplier. */
        @Editable(category="world", min=0.0, step=0.01)
        public float timeScale = 1f;

        /** The layer's influences. */
        @Editable(category="influences")
        public InfluenceConfig[] influences = new InfluenceConfig[0];

        /** Used to identify the layer after customization. */
        @Shallow
        public transient Layer identity = this;

        /**
         * Checks whether we should rotate the orientations and angular velocities (because
         * the alignment is fixed rather than billboard, etc.)
         */
        public abstract boolean shouldRotateOrientations ();

        public void preload (GlContext ctx)
        {
            // Do nothing
        }

        /**
         * Custom write method.
         */
        public void writeFields (Exporter out)
            throws IOException
        {
            // always write the "rotate orientations" flag so that things won't break if (when) we
            // change the default
            out.defaultWriteFields();
            out.write("rotateOrientationsWithEmitter", rotateOrientationsWithEmitter);
        }

        /**
         * Custom read method.
         */
        public void readFields (Importer in)
            throws IOException
        {
            // existing particles expect their orientations to be rotated iff their particles
            // move with the emitter
            in.defaultReadFields();
            rotateOrientationsWithEmitter = in.read(
                "rotateOrientationsWithEmitter", moveParticlesWithEmitter);
        }
    }

    /** The model's tick policy. */
    @Editable(hgroup="t")
    public TickPolicy tickPolicy = TickPolicy.DEFAULT;

    /** A fixed amount by which to expand the bounds. */
    @Editable(min=0.0, step=0.01, hgroup="t")
    public float boundsExpansion;

    /** The amount of time to spend "warming up" the system on reset. */
    @Editable(min=0.0, step=0.01, hgroup="w")
    public float warmupTime;

    /** The maximum tick duration during warmup. */
    @Editable(min=0.0, step=0.01, hgroup="w")
    public float warmupGranularity = 0.1f;

    /** The model's transient policy. */
    @Editable
    public TransientPolicy transientPolicy = TransientPolicy.FRUSTUM;

    /** The influences allowed to affect this model. */
    @Editable
    public InfluenceFlagConfig influences = new InfluenceFlagConfig(true, false, false, false);

    @Override
    public void preload (GlContext ctx)
    {
        for (Layer layer : getLayers()) {
            layer.preload(ctx);
        }
    }

    /**
     * Checks whether any of the system's layers respawn dead particles.
     */
    public boolean anyLayersRespawn ()
    {
        for (Layer layer : getLayers()) {
            if (layer.respawnDeadParticles) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a reference to the system's array of layers.
     */
    public abstract Layer[] getLayers ();
}
