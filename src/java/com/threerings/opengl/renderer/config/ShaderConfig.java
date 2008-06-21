//
// $Id$

package com.threerings.opengl.renderer.config;

import java.text.DecimalFormat;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.Matrix4f;
import com.threerings.math.Transform;
import com.threerings.math.Vector3f;
import com.threerings.math.Vector4f;

import com.threerings.opengl.renderer.Color4f;
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
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Returns the subclasses available for selection in the editor.
         */
        public static Class[] getEditorTypes ()
        {
            return new Class[] { Vertex.class, Fragment.class, Derived.class };
        }

        /**
         * Returns the shader corresponding to this configuration.
         */
        public abstract Shader getShader (GlContext ctx);

        /**
         * Returns the array of uniforms for this configuration.
         */
        public abstract Variable[] getUniforms (GlContext ctx);
    }

    /**
     * The superclass of the original implementations.
     */
    public static abstract class Original extends Implementation
    {
        /** The uniform variables to pass to the shader. */
        @Editable(types={
            ColorVariable.class, FloatVariable.class,
            IntegerVariable.class, TransformVariable.class }, nullable=false)
        public Variable[] uniforms = new Variable[0];

        @Override // documentation inherited
        public Variable[] getUniforms (GlContext ctx)
        {
            return uniforms;
        }
    }

    /**
     * A vertex shader.
     */
    public static class Vertex extends Original
    {
        /**
         * The initial contents of the shader.
         */
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
            @Editable(editor="resource")
            @FileConstraints(
                description="m.vertex_shader_files",
                extensions={".vert" },
                directory="shader_dir")
            public String file;

            /** The preprocessor definitions to use. */
            @Editable
            public Variable[] definitions = new Variable[0];

            @Override // documentation inherited
            public Shader getShader (GlContext ctx)
            {
                if (file == null) {
                    return null;
                }
                ArrayList<String> defs = new ArrayList<String>();
                for (Variable var : definitions) {
                    String def = var.createDefinition();
                    if (def != null) {
                        defs.add(def);
                    }
                }
                return ctx.getShaderCache().getShader(file, defs.toArray(new String[defs.size()]));
            }
        }

        /** The initial contents of the shader. */
        @Editable(types={ SourceFile.class }, nullable=false)
        public Contents contents = new SourceFile();

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
            @Editable(editor="resource")
            @FileConstraints(
                description="m.fragment_shader_files",
                extensions={".frag" },
                directory="shader_dir")
            public String file;

            /** The preprocessor definitions to use. */
            @Editable
            public Variable[] definitions = new Variable[0];

            @Override // documentation inherited
            public Shader getShader (GlContext ctx)
            {
                if (file == null) {
                    return null;
                }
                ArrayList<String> defs = new ArrayList<String>();
                for (Variable var : definitions) {
                    String def = var.createDefinition();
                    if (def != null) {
                        defs.add(def);
                    }
                }
                return ctx.getShaderCache().getShader(file, defs.toArray(new String[defs.size()]));
            }
        }

        /** The initial contents of the shader. */
        @Editable(types={ SourceFile.class }, nullable=false)
        public Contents contents = new SourceFile();

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
        @Editable
        public ConfigReference<ShaderConfig> shader;

        @Override // documentation inherited
        public Shader getShader (GlContext ctx)
        {
            ShaderConfig config = getConfig(ctx);
            return (config == null) ? null : config.getShader(ctx);
        }

        @Override // documentation inherited
        public Variable[] getUniforms (GlContext ctx)
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
     * Represents the value of a shader variable (used for both preprocessor definitions and
     * uniform variables).
     */
    public static abstract class Variable extends DeepObject
        implements Exportable
    {
        /**
         * Returns the subclasses available for selection in the editor.
         */
        public static Class[] getEditorTypes ()
        {
            return new Class[] {
                BooleanVariable.class, ColorVariable.class, FloatVariable.class,
                IntegerVariable.class, StringVariable.class, TransformVariable.class };
        }

        /** The name of the variable. */
        @Editable(hgroup="p")
        public String name = "";

        /**
         * Creates a uniform object from this configuration.
         *
         * @param location the location of the uniform.
         */
        public abstract Uniform createUniform (int location);

        /**
         * Creates a preprocessor definition from this configuration.
         *
         * @return the definition string, or <code>null</code> to omit the definition.
         */
        public abstract String createDefinition ();
    }

    /**
     * A boolean-valued variable.
     */
    public static class BooleanVariable extends Variable
    {
        /** The value of the variable. */
        @Editable(hgroup="p")
        public boolean value;

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            return new IntegerUniform(location, value ? 1 : 0);
        }

        @Override // documentation inherited
        public String createDefinition ()
        {
            return value ? name : null;
        }
    }

    /**
     * A color-valued variable.
     */
    public static class ColorVariable extends Variable
    {
        /** The value of the variable. */
        @Editable(mode="alpha", hgroup="p")
        public Color4f value = new Color4f();

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            return new Vector4fUniform(location, new Vector4f(value.r, value.g, value.b, value.a));
        }

        @Override // documentation inherited
        public String createDefinition ()
        {
            return name + " vec4(" +
                GLSL_FLOAT.format(value.r) + ", " +
                GLSL_FLOAT.format(value.g) + ", " +
                GLSL_FLOAT.format(value.b) + ", " +
                GLSL_FLOAT.format(value.a) + ")";
        }
    }

    /**
     * A float-valued variable.
     */
    public static class FloatVariable extends Variable
    {
        /** The value of the variable. */
        @Editable(step=0.01, hgroup="p")
        public float value;

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            return new FloatUniform(location, value);
        }

        @Override // documentation inherited
        public String createDefinition ()
        {
            return name + " " + GLSL_FLOAT.format(value);
        }
    }

    /**
     * An integer-valued variable.
     */
    public static class IntegerVariable extends Variable
    {
        /** The value of the variable. */
        @Editable(hgroup="p")
        public int value;

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            return new IntegerUniform(location, value);
        }

        @Override // documentation inherited
        public String createDefinition ()
        {
            return name + " " + value;
        }
    }

    /**
     * An string-valued variable.
     */
    public static class StringVariable extends Variable
    {
        /** The value of the variable. */
        @Editable(hgroup="p")
        public String value = "";

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            throw new RuntimeException(); // not allowed
        }

        @Override // documentation inherited
        public String createDefinition ()
        {
            return name + " " + value;
        }
    }

    /**
     * A transform-valued variable.
     */
    public static class TransformVariable extends Variable
    {
        /** The value of the variable. */
        @Editable(hgroup="p")
        public Transform value = new Transform();

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            value.update(Transform.GENERAL);
            return new Matrix4fUniform(location, value.getMatrix());
        }

        @Override // documentation inherited
        public String createDefinition ()
        {
            value.update(Transform.GENERAL);
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
     * Returns the shader corresponding to this configuration.
     */
    public Shader getShader (GlContext ctx)
    {
        return implementation.getShader(ctx);
    }

    /**
     * Returns the array of uniforms for this configuration.
     */
    public Variable[] getUniforms (GlContext ctx)
    {
        return implementation.getUniforms(ctx);
    }

    /** Formats floats so that they will be recognized as float constants in GLSL. */
    protected static final DecimalFormat GLSL_FLOAT = new DecimalFormat("0.0");
}
