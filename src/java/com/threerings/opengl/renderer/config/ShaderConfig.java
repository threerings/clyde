//
// $Id$

package com.threerings.opengl.renderer.config;

import java.text.DecimalFormat;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;

import com.threerings.math.Matrix4f;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.math.Vector4f;

import com.threerings.opengl.geom.config.PassDescriptor;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Program.FloatUniform;
import com.threerings.opengl.renderer.Program.IntegerUniform;
import com.threerings.opengl.renderer.Program.Vector3fUniform;
import com.threerings.opengl.renderer.Program.Vector4fUniform;
import com.threerings.opengl.renderer.Program.Matrix4fUniform;
import com.threerings.opengl.renderer.Program.Uniform;
import com.threerings.opengl.renderer.Shader;
import com.threerings.opengl.util.GlContext;

/**
 * Shader metadata.
 */
public class ShaderConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the shader.
     */
    @EditorTypes({ Vertex.class, Fragment.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Populates the relevant portion of the supplied descriptor.
         */
        public abstract void populateDescriptor (GlContext ctx, PassDescriptor desc);

        /**
         * Returns the shader corresponding to this configuration.
         */
        public abstract Shader getShader (GlContext ctx);

        /**
         * Returns the array of uniforms for this configuration.
         */
        public abstract UniformConfig[] getUniforms (GlContext ctx);
    }

    /**
     * The superclass of the original implementations.
     */
    public static abstract class Original extends Implementation
    {
        /** The uniforms to pass to the shader. */
        @Editable
        public UniformConfig[] uniforms = new UniformConfig[0];

        @Override // documentation inherited
        public UniformConfig[] getUniforms (GlContext ctx)
        {
            return uniforms;
        }
    }

    /**
     * A vertex shader.
     */
    public static class Vertex extends Original
    {
        /** Hints to pass to the geometry handler. */
        @Editable(width=15)
        public String[] hints = new String[0];

        /** The names of the attributes required by this shader. */
        @Editable(width=15)
        public String[] attributes = new String[0];

        /** The coordinate space in which the shader operates. */
        @Editable(hgroup="t")
        public CoordSpace coordSpace = CoordSpace.OBJECT;

        /** Whether or not the shader uses the color state. */
        @Editable(hgroup="t")
        public boolean colors = true;

        /** Whether or not the shader uses the normal state. */
        @Editable(hgroup="t")
        public boolean normals = true;

        /**
         * The initial contents of the shader.
         */
        @EditorTypes({ SourceFile.class })
        public static abstract class Contents extends DeepObject
            implements Exportable
        {
            /**
             * Returns the shader.
             */
            public abstract Shader getShader (GlContext ctx);
        }

        /**
         * Creates a shader from the specified file.
         */
        public static class SourceFile extends Contents
        {
            /** The resource from which to load the shader. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.vertex_shader_files",
                extensions={".vert"},
                directory="shader_dir")
            public String file;

            /** The preprocessor definitions to use. */
            @Editable
            public Definition[] definitions = new Definition[0];

            @Override // documentation inherited
            public Shader getShader (GlContext ctx)
            {
                if (file == null) {
                    return null;
                }
                ArrayList<String> defs = new ArrayList<String>();
                for (Definition definition : definitions) {
                    String def = definition.getString();
                    if (def != null) {
                        defs.add(def);
                    }
                }
                return ctx.getShaderCache().getShader(file, defs.toArray(new String[defs.size()]));
            }
        }

        /** The initial contents of the shader. */
        @Editable
        public Contents contents = new SourceFile();

        @Override // documentation inherited
        public void populateDescriptor (GlContext ctx, PassDescriptor desc)
        {
            desc.coordSpace = coordSpace;
            desc.hints = hints;
            desc.firstVertexAttribIndex = FIRST_VERTEX_ATTRIB_INDEX;
            desc.vertexAttribs = attributes;
            desc.colors |= colors;
            desc.normals |= normals;
        }

        @Override // documentation inherited
        public Shader getShader (GlContext ctx)
        {
            return contents.getShader(ctx);
        }
    }

    /**
     * A fragment shader.
     */
    public static class Fragment extends Original
    {
        /**
         * The initial contents of the shader.
         */
        @EditorTypes({ SourceFile.class })
        public static abstract class Contents extends DeepObject
            implements Exportable
        {
            /**
             * Returns the shader.
             */
            public abstract Shader getShader (GlContext ctx);
        }

        /**
         * Creates a shader from the specified file.
         */
        public static class SourceFile extends Contents
        {
            /** The resource from which to load the shader. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.fragment_shader_files",
                extensions={".frag"},
                directory="shader_dir")
            public String file;

            /** The preprocessor definitions to use. */
            @Editable
            public Definition[] definitions = new Definition[0];

            @Override // documentation inherited
            public Shader getShader (GlContext ctx)
            {
                if (file == null) {
                    return null;
                }
                ArrayList<String> defs = new ArrayList<String>();
                for (Definition definition : definitions) {
                    String def = definition.getString();
                    if (def != null) {
                        defs.add(def);
                    }
                }
                return ctx.getShaderCache().getShader(file, defs.toArray(new String[defs.size()]));
            }
        }

        /** The initial contents of the shader. */
        @Editable
        public Contents contents = new SourceFile();

        @Override // documentation inherited
        public void populateDescriptor (GlContext ctx, PassDescriptor desc)
        {
            // no-op
        }

        @Override // documentation inherited
        public Shader getShader (GlContext ctx)
        {
            return contents.getShader(ctx);
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The shader reference. */
        @Editable(nullable=true)
        public ConfigReference<ShaderConfig> shader;

        @Override // documentation inherited
        public void populateDescriptor (GlContext ctx, PassDescriptor desc)
        {
            ShaderConfig config = getConfig(ctx);
            if (config != null) {
                config.populateDescriptor(ctx, desc);
            }
        }

        @Override // documentation inherited
        public Shader getShader (GlContext ctx)
        {
            ShaderConfig config = getConfig(ctx);
            return (config == null) ? null : config.getShader(ctx);
        }

        @Override // documentation inherited
        public UniformConfig[] getUniforms (GlContext ctx)
        {
            ShaderConfig config = getConfig(ctx);
            return (config == null) ? null : config.getUniforms(ctx);
        }

        /**
         * Returns the referenced config.
         */
        protected ShaderConfig getConfig (GlContext ctx)
        {
            return (shader == null) ?
                null : ctx.getConfigManager().getConfig(ShaderConfig.class, shader);
        }
    }

    /**
     * Represents the configuration of one or more shader uniforms.
     */
    @EditorTypes({
        BooleanUniformConfig.class, ColorUniformConfig.class,
        FloatUniformConfig.class, IntegerUniformConfig.class,
        TransformUniformConfig.class, MatrixArrayRefUniformConfig.class })
    public static abstract class UniformConfig extends DeepObject
        implements Exportable
    {
        /** The name of the uniform. */
        @Editable(hgroup="p")
        public String name = "";

        /**
         * Creates the uniform objects for this config and adds them to the provided list.
         */
        public abstract void createUniforms (
            Scope scope, Program program, ArrayList<Uniform> uniforms,
            ArrayList<Updater> updaters);
    }

    /**
     * Base class for simple uniforms.
     */
    public static abstract class SimpleUniformConfig extends UniformConfig
    {
        @Override // documentation inherited
        public void createUniforms (
            Scope scope, Program program, ArrayList<Uniform> uniforms,
            ArrayList<Updater> updaters)
        {
            int location = program.getUniformLocation(name);
            if (location != -1) {
                uniforms.add(createUniform(location));
            }
        }

        /**
         * Creates a uniform object from this configuration.
         *
         * @param location the location of the uniform.
         */
        protected abstract Uniform createUniform (int location);
    }

    /**
     * A boolean-valued uniform.
     */
    public static class BooleanUniformConfig extends SimpleUniformConfig
    {
        /** The value of the uniform. */
        @Editable(hgroup="p")
        public boolean value;

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            return new IntegerUniform(location, value ? 1 : 0);
        }
    }

    /**
     * A color-valued uniform.
     */
    public static class ColorUniformConfig extends SimpleUniformConfig
    {
        /** The value of the uniform. */
        @Editable(mode="alpha", hgroup="p")
        public Color4f value = new Color4f();

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            return new Vector4fUniform(location, new Vector4f(value.r, value.g, value.b, value.a));
        }
    }

    /**
     * A float-valued uniform.
     */
    public static class FloatUniformConfig extends SimpleUniformConfig
    {
        /** The value of the uniform. */
        @Editable(step=0.01, hgroup="p")
        public float value;

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            return new FloatUniform(location, value);
        }
    }

    /**
     * An integer-valued uniform.
     */
    public static class IntegerUniformConfig extends SimpleUniformConfig
    {
        /** The value of the uniform. */
        @Editable(hgroup="p")
        public int value;

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            return new IntegerUniform(location, value);
        }
    }

    /**
     * A transform-valued uniform.
     */
    public static class TransformUniformConfig extends SimpleUniformConfig
    {
        /** The value of the uniform. */
        @Editable(hgroup="p")
        public Transform3D value = new Transform3D();

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            value.update(Transform3D.GENERAL);
            return new Matrix4fUniform(location, value.getMatrix());
        }
    }

    /**
     * Base class for configs representing uniform arrays whose values come from references
     * to scoped variables.
     */
    public static abstract class ArrayRefUniformConfig<T> extends UniformConfig
    {
        @Override // documentation inherited
        public void createUniforms (
            Scope scope, Program program, ArrayList<Uniform> uniforms,
            ArrayList<Updater> updaters)
        {
            // look up the array values
            T[] values = ScopeUtil.resolve(scope, name, null, getArrayClass());
            if (values == null) {
                return;
            }
            ArrayList<Uniform> list = new ArrayList<Uniform>();
            for (int ii = 0; ii < values.length; ii++) {
                int location = program.getUniformLocation(name + "[" + ii + "]");
                if (location != -1) {
                    list.add(createUniform(location, values[ii]));
                }
            }
            if (list.isEmpty()) {
                return;
            }
            uniforms.addAll(list);
            final Uniform[] array = list.toArray(new Uniform[list.size()]);
            updaters.add(new Updater() {
                public void update () {
                    // dirty the uniforms
                    for (Uniform uniform : array) {
                        uniform.dirty = true;
                    }
                }
            });
        }

        /**
         * Returns the array class that we're expecting.
         */
        protected abstract Class<? extends T[]> getArrayClass ();

        /**
         * Creates a uniform object from this configuration.
         *
         * @param location the location of the uniform.
         * @param value a reference to the value of the uniform.
         */
        protected abstract Uniform createUniform (int location, T value);
    }

    /**
     * References an array of matrices.
     */
    public static class MatrixArrayRefUniformConfig extends ArrayRefUniformConfig<Matrix4f>
    {
        @Override // documentation inherited
        protected Class<? extends Matrix4f[]> getArrayClass ()
        {
            return Matrix4f.EMPTY_ARRAY.getClass();
        }

        @Override // documentation inherited
        protected Uniform createUniform (int location, Matrix4f value)
        {
            // set the value by reference
            Matrix4fUniform uniform = new Matrix4fUniform(location);
            uniform.value = value;
            return uniform;
        }
    }

    /**
     * Represents a preprocessor definition.
     */
    @EditorTypes({
        BooleanDefinition.class, ColorDefinition.class, FloatDefinition.class,
        IntegerDefinition.class, StringDefinition.class, TransformDefinition.class })
    public static abstract class Definition extends DeepObject
        implements Exportable
    {
        /** The name of the definition. */
        @Editable(hgroup="p")
        public String name = "";

        /**
         * Returns the string for this definition.
         *
         * @return the definition string, or <code>null</code> to omit the definition.
         */
        public abstract String getString ();
    }

    /**
     * A boolean definition.
     */
    public static class BooleanDefinition extends Definition
    {
        /** The value of the definition. */
        @Editable(hgroup="p")
        public boolean value;

        @Override // documentation inherited
        public String getString ()
        {
            return value ? name : null;
        }
    }

    /**
     * A color-valued definition.
     */
    public static class ColorDefinition extends Definition
    {
        /** The value of the definition. */
        @Editable(mode="alpha", hgroup="p")
        public Color4f value = new Color4f();

        @Override // documentation inherited
        public String getString ()
        {
            return name + " vec4(" +
                GLSL_FLOAT.format(value.r) + ", " +
                GLSL_FLOAT.format(value.g) + ", " +
                GLSL_FLOAT.format(value.b) + ", " +
                GLSL_FLOAT.format(value.a) + ")";
        }
    }

    /**
     * A float-valued definition.
     */
    public static class FloatDefinition extends Definition
    {
        /** The value of the definition. */
        @Editable(step=0.01, hgroup="p")
        public float value;

        @Override // documentation inherited
        public String getString ()
        {
            return name + " " + GLSL_FLOAT.format(value);
        }
    }

    /**
     * An integer-valued definition.
     */
    public static class IntegerDefinition extends Definition
    {
        /** The value of the definition. */
        @Editable(hgroup="p")
        public int value;

        @Override // documentation inherited
        public String getString ()
        {
            return name + " " + value;
        }
    }

    /**
     * A string-valued definition.
     */
    public static class StringDefinition extends Definition
    {
        /** The value of the definition. */
        @Editable(hgroup="p")
        public String value = "";

        @Override // documentation inherited
        public String getString ()
        {
            return name + " " + value;
        }
    }

    /**
     * A transform-valued definition.
     */
    public static class TransformDefinition extends Definition
    {
        /** The value of the definition. */
        @Editable(hgroup="p")
        public Transform3D value = new Transform3D();

        @Override // documentation inherited
        public String getString ()
        {
            value.update(Transform3D.GENERAL);
            Matrix4f matrix = value.getMatrix();
            return name + " mat4(" +
                GLSL_FLOAT.format(matrix.m00) + ", " +
                GLSL_FLOAT.format(matrix.m01) + ", " +
                GLSL_FLOAT.format(matrix.m02) + ", " +
                GLSL_FLOAT.format(matrix.m03) + ", " +

                GLSL_FLOAT.format(matrix.m10) + ", " +
                GLSL_FLOAT.format(matrix.m11) + ", " +
                GLSL_FLOAT.format(matrix.m12) + ", " +
                GLSL_FLOAT.format(matrix.m13) + ", " +

                GLSL_FLOAT.format(matrix.m20) + ", " +
                GLSL_FLOAT.format(matrix.m21) + ", " +
                GLSL_FLOAT.format(matrix.m22) + ", " +
                GLSL_FLOAT.format(matrix.m23) + ", " +

                GLSL_FLOAT.format(matrix.m30) + ", " +
                GLSL_FLOAT.format(matrix.m31) + ", " +
                GLSL_FLOAT.format(matrix.m32) + ", " +
                GLSL_FLOAT.format(matrix.m33) + ")";
        }
    }

    /** The actual shader implementation. */
    @Editable
    public Implementation implementation = new Vertex();

    /**
     * Populates the relevant portion of the supplied descriptor.
     */
    public void populateDescriptor (GlContext ctx, PassDescriptor desc)
    {
        implementation.populateDescriptor(ctx, desc);
    }

    /**
     * Returns the shader corresponding to this configuration.
     */
    public Shader getShader (GlContext ctx)
    {
        return implementation.getShader(ctx);
    }

    /**
     * Returns the array of uniforms for this configuration.
     */
    public UniformConfig[] getUniforms (GlContext ctx)
    {
        return implementation.getUniforms(ctx);
    }

    /** Formats floats so that they will be recognized as float constants in GLSL. */
    protected static final DecimalFormat GLSL_FLOAT = new DecimalFormat("0.0");

    /** The index of the first vertex attribute (earlier ones are reserved). */
    protected static final int FIRST_VERTEX_ATTRIB_INDEX = 9;
}
