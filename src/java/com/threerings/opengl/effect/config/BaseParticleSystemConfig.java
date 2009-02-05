//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
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
import com.threerings.opengl.effect.MetaParticleSystem;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.InfluenceFlagConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.GlContext;

/**
 * Base class for {@link ParticleSystemConfig} and {@link MetaParticleSystemConfig}.
 */
public abstract class BaseParticleSystemConfig extends ModelConfig.Implementation
{
    /**
     * A single layer of the system.
     */
    public static abstract class Layer extends DeepObject
        implements Exportable
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
    }

    /** The influences allowed to affect this model. */
    @Editable
    public InfluenceFlagConfig influences = new InfluenceFlagConfig(true, false, false);

    /**
     * Returns a reference to the system's array of layers.
     */
    public abstract Layer[] getLayers ();
}
