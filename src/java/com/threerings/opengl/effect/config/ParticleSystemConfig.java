//
// $Id$

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
import com.threerings.math.Transform3D;
import com.threerings.probs.ColorFunctionVariable;
import com.threerings.probs.FloatFunctionVariable;
import com.threerings.probs.FloatVariable;
import com.threerings.probs.QuaternionVariable;
import com.threerings.probs.VectorVariable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.eff.ParticleGeometry;
import com.threerings.opengl.eff.ParticleSystem;
import com.threerings.opengl.effect.AlphaMode;
import com.threerings.opengl.effect.ColorFunction;
import com.threerings.opengl.effect.FloatFunction;
import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.geometry.config.DeformerConfig;
import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.geometry.config.GeometryConfig.IndexedStored;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.util.GlContext;

/**
 * The configuration of a particle system.
 */
public class ParticleSystemConfig extends ModelConfig.Implementation
{
    /** The different alignment modes. */
    public enum Alignment { FIXED, BILLBOARD, VELOCITY };

    /**
     * A single layer of the system.
     */
    public static class Layer extends DeepObject
        implements Exportable
    {
        /** The name of the layer. */
        @Editable(column=true)
        public String name = "";

        /** Whether or not the layer is visible. */
        @Editable(column=true)
        public boolean visible = true;

        /** The particle geometry. */
        @Editable(category="appearance")
        public ParticleGeometryConfig geometry = new Points();

        /** The particle count. */
        @Editable(category="appearance", min=0)
        public int particleCount = 100;

        /** Whether or not to align particles' orientations with their velocities. */
        @Editable(category="appearance")
        public Alignment alignment = Alignment.BILLBOARD;

        /** The material to use for the particle system. */
        @Editable(category="appearance", mode="compact", nullable=true)
        public ConfigReference<MaterialConfig> material;

        /** The number of texture divisions in the S direction. */
        @Editable(category="appearance", min=1)
        public int textureDivisionsS = 1;

        /** The number of texture divisions in the T direction. */
        @Editable(category="appearance", min=1)
        public int textureDivisionsT = 1;

        /** Determines how colors are modified according to their alpha values. */
        @Editable(category="appearance")
        public AlphaMode alphaMode = AlphaMode.TRANSLUCENT;

        /** Whether or not to sort the particles by depth before rendering. */
        @Editable(category="appearance")
        public boolean depthSort;

        /** The render priority (higher priority layers are rendered above lower priority ones). */
        @Editable(category="appearance", nullable=true)
        public GroupPriority priorityMode;

        /** Controls the particles' change of color over their lifespans. */
        @Editable(category="appearance", mode="alpha")
        public ColorFunctionVariable color =
            new ColorFunctionVariable.Fixed(new ColorFunction.Constant());

        /** Controls the particles' change of size over their lifespans. */
        @Editable(category="appearance", min=0.0, step=0.01)
        public FloatFunctionVariable size =
            new FloatFunctionVariable.Fixed(new FloatFunction.Constant(0.1f));

        /** Controls the particles' change of length over their lifespans. */
        @Editable(category="appearance", min=0.0, step=0.01)
        public FloatFunctionVariable length =
            new FloatFunctionVariable.Fixed(new FloatFunction.Constant(0.1f));

        /** Controls the particles' change of texture frame over their lifespans. */
        @Editable(category="appearance", min=0.0)
        public FloatFunctionVariable frame =
            new FloatFunctionVariable.Fixed(new FloatFunction.Constant(0f));

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
    }

    /**
     * Determines how particles are rendered.
     */
    @EditorTypes({ Points.class, Lines.class, Quads.class, Meshes.class })
    public static abstract class ParticleGeometryConfig extends GeometryConfig
    {
        /**
         * Adds the geometry's update references to the provided set.
         */
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

        @Override // documentation inherited
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
        @Override // documentation inherited
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
        @Editable(min=0)
        public int segments;

        public Lines (Quads quads)
        {
            this.segments = quads.segments;
        }

        public Lines ()
        {
        }

        @Override // documentation inherited
        public int getSegments ()
        {
            return segments;
        }

        @Override // documentation inherited
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
        @Editable(min=0)
        public int segments;

        public Quads (Lines lines)
        {
            this.segments = lines.segments;
        }

        public Quads ()
        {
        }

        @Override // documentation inherited
        public int getSegments ()
        {
            return segments;
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(ModelConfig.class, model);
        }

        @Override // documentation inherited
        public Geometry createGeometry (
            GlContext ctx, Scope scope, DeformerConfig deformer, PassDescriptor[] passes)
        {
            ModelConfig config = ctx.getConfigManager().getConfig(ModelConfig.class, model);
            GeometryConfig geom = (config == null) ? null : config.getParticleGeometry(ctx);
            return ParticleGeometry.Meshes.create(ctx, scope, passes, geom);
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

    @Override // documentation inherited
    public void getUpdateReferences (ConfigReferenceSet refs)
    {
        for (Layer layer : layers) {
            layer.geometry.getUpdateReferences(refs);
        }
    }

    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (impl instanceof ParticleSystem) {
            ((ParticleSystem)impl).setConfig(this);
        } else {
            if (impl != null) {
                impl.dispose();
            }
            impl = new ParticleSystem(ctx, scope, this);
        }
        return impl;
    }

    @Override // documentation inherited
    public void invalidate ()
    {
        for (Layer layer : layers) {
            layer.invalidate();
        }
    }
}
