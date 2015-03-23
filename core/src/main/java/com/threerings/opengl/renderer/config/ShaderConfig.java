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

package com.threerings.opengl.renderer.config;

import java.text.DecimalFormat;

import java.util.List;
import java.util.HashSet;

import com.google.common.collect.Lists;

import com.samskivert.util.ArrayUtil;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.expr.BooleanExpression;
import com.threerings.expr.Color4fExpression;
import com.threerings.expr.FloatExpression;
import com.threerings.expr.IntegerExpression;
import com.threerings.expr.ObjectExpression;
import com.threerings.expr.Scope;
import com.threerings.expr.Transform3DExpression;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;

import com.threerings.math.Matrix4f;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector4f;

import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Program.FloatUniform;
import com.threerings.opengl.renderer.Program.IntegerUniform;
import com.threerings.opengl.renderer.Program.Vector2fUniform;
import com.threerings.opengl.renderer.Program.Vector4fUniform;
import com.threerings.opengl.renderer.Program.Matrix4fUniform;
import com.threerings.opengl.renderer.Program.Uniform;
import com.threerings.opengl.renderer.Shader;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.util.SnippetUtil;
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
        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Adds the implementation's update resources to the provided set.
         */
        public void getUpdateResources (HashSet<String> paths)
        {
            // nothing by default
        }

        /**
         * Populates the relevant portion of the supplied descriptor.
         */
        public abstract void populateDescriptor (GlContext ctx, PassDescriptor desc);

        /**
         * Returns the shader corresponding to this configuration.
         */
        public abstract Shader getShader (
            GlContext ctx, Scope scope, RenderState[] states, boolean vertexProgramTwoSide);

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

        @Override
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
        public String[] hints = ArrayUtil.EMPTY_STRING;

        /** The names of the attributes required by this shader. */
        @Editable(width=15)
        public String[] attributes = ArrayUtil.EMPTY_STRING;

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
             * Adds the contents' update resources to the provided set.
             */
            public void getUpdateResources (HashSet<String> paths)
            {
                // nothing by default
            }

            /**
             * Returns the shader.
             */
            public abstract Shader getShader (
                GlContext ctx, Scope scope, RenderState[] states, boolean vertexProgramTwoSide);
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

            @Override
            public void getUpdateResources (HashSet<String> paths)
            {
                if (file != null) {
                    paths.add(file);
                }
            }

            @Override
            public Shader getShader (
                GlContext ctx, Scope scope, RenderState[] states, boolean vertexProgramTwoSide)
            {
                if (file == null) {
                    return null;
                }
                List<String> defs = Lists.newArrayList();
                for (Definition definition : definitions) {
                    definition.getDefinitions(scope, states, vertexProgramTwoSide, defs);
                }
                return ctx.getShaderCache().getShader(file, defs.toArray(new String[defs.size()]));
            }
        }

        /** The initial contents of the shader. */
        @Editable
        public Contents contents = new SourceFile();

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            contents.getUpdateResources(paths);
        }

        @Override
        public void populateDescriptor (GlContext ctx, PassDescriptor desc)
        {
            desc.coordSpace = coordSpace;
            desc.hints = hints;
            desc.firstVertexAttribIndex = FIRST_VERTEX_ATTRIB_INDEX;
            desc.vertexAttribs = attributes;
            desc.colors |= colors;
            desc.normals |= normals;
        }

        @Override
        public Shader getShader (
            GlContext ctx, Scope scope, RenderState[] states, boolean vertexProgramTwoSide)
        {
            return contents.getShader(ctx, scope, states, vertexProgramTwoSide);
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
             * Adds the contents' update resources to the provided set.
             */
            public void getUpdateResources (HashSet<String> paths)
            {
                // nothing by default
            }

            /**
             * Returns the shader.
             */
            public abstract Shader getShader (
                GlContext ctx, Scope scope, RenderState[] states, boolean vertexProgramTwoSide);
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

            @Override
            public void getUpdateResources (HashSet<String> paths)
            {
                if (file != null) {
                    paths.add(file);
                }
            }

            @Override
            public Shader getShader (
                GlContext ctx, Scope scope, RenderState[] states, boolean vertexProgramTwoSide)
            {
                if (file == null) {
                    return null;
                }
                List<String> defs = Lists.newArrayList();
                for (Definition definition : definitions) {
                    definition.getDefinitions(scope, states, vertexProgramTwoSide, defs);
                }
                return ctx.getShaderCache().getShader(file, defs.toArray(new String[defs.size()]));
            }
        }

        /** The initial contents of the shader. */
        @Editable
        public Contents contents = new SourceFile();

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            contents.getUpdateResources(paths);
        }

        @Override
        public void populateDescriptor (GlContext ctx, PassDescriptor desc)
        {
            // no-op
        }

        @Override
        public Shader getShader (
            GlContext ctx, Scope scope, RenderState[] states, boolean vertexProgramTwoSide)
        {
            return contents.getShader(ctx, scope, states, vertexProgramTwoSide);
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

        @Override
        public void populateDescriptor (GlContext ctx, PassDescriptor desc)
        {
            ShaderConfig config = getConfig(ctx);
            if (config != null) {
                config.populateDescriptor(ctx, desc);
            }
        }

        @Override
        public Shader getShader (
            GlContext ctx, Scope scope, RenderState[] states, boolean vertexProgramTwoSide)
        {
            ShaderConfig config = getConfig(ctx);
            return (config == null) ?
                null : config.getShader(ctx, scope, states, vertexProgramTwoSide);
        }

        @Override
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
        BooleanUniformConfig.class, ColorUniformConfig.class, FloatUniformConfig.class,
        IntegerUniformConfig.class, PolarUniformConfig.class, TransformUniformConfig.class,
        BooleanExprUniformConfig.class, ColorExprUniformConfig.class, FloatExprUniformConfig.class,
        IntegerExprUniformConfig.class, TransformExprUniformConfig.class,
        MatrixArrayRefUniformConfig.class })
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
            Scope scope, Program program, List<Uniform> uniforms, List<Updater> updaters);
    }

    /**
     * Base class for simple uniforms.
     */
    public static abstract class SimpleUniformConfig extends UniformConfig
    {
        @Override
        public void createUniforms (
            Scope scope, Program program, List<Uniform> uniforms, List<Updater> updaters)
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

        @Override
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

        @Override
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

        @Override
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

        @Override
        public Uniform createUniform (int location)
        {
            return new IntegerUniform(location, value);
        }
    }

    /**
     * A polar coordinate-valued uniform.
     */
    public static class PolarUniformConfig extends SimpleUniformConfig
    {
        /** The value of the uniform. */
        @Editable(step=0.01, mode="polar", hgroup="p")
        public Vector2f value = new Vector2f();

        @Override
        public Uniform createUniform (int location)
        {
            return new Vector2fUniform(location, value);
        }
    }

    /**
     * A transform-valued uniform.
     */
    public static class TransformUniformConfig extends SimpleUniformConfig
    {
        /** The value of the uniform. */
        @Editable(step=0.01, hgroup="p")
        public Transform3D value = new Transform3D();

        @Override
        public Uniform createUniform (int location)
        {
            value.update(Transform3D.GENERAL);
            return new Matrix4fUniform(location, value.getMatrix());
        }
    }

    /**
     * Base class for expression-derived uniforms.
     */
    public static abstract class ExpressionUniformConfig extends UniformConfig
    {
        @Override
        public void createUniforms (
            Scope scope, Program program, List<Uniform> uniforms, List<Updater> updaters)
        {
            int location = program.getUniformLocation(name);
            if (location != -1) {
                uniforms.add(createUniform(location, scope, updaters));
            }
        }

        /**
         * Creates a uniform object from this configuration.
         *
         * @param location the location of the uniform.
         */
        protected abstract Uniform createUniform (
            int location, Scope scope, List<Updater> updaters);
    }

    /**
     * A boolean-valued uniform whose value is derived from an expression.
     */
    public static class BooleanExprUniformConfig extends ExpressionUniformConfig
    {
        /** The expression for the uniform. */
        @Editable
        public BooleanExpression expression = new BooleanExpression.Constant();

        @Override
        protected Uniform createUniform (int location, Scope scope, List<Updater> updaters)
        {
            final IntegerUniform uniform = new IntegerUniform(location);
            final BooleanExpression.Evaluator eval = expression.createEvaluator(scope);
            updaters.add(new Updater() {
                public void update () {
                    uniform.value = eval.evaluate() ? 1 : 0;
                    uniform.dirty = true;
                }
            });
            return uniform;
        }
    }

    /**
     * A color-valued uniform whose value is derived from an expression.
     */
    public static class ColorExprUniformConfig extends ExpressionUniformConfig
    {
        /** The expression for the uniform. */
        @Editable
        public Color4fExpression expression = new Color4fExpression.Constant();

        @Override
        protected Uniform createUniform (int location, Scope scope, List<Updater> updaters)
        {
            final Vector4fUniform uniform = new Vector4fUniform(location);
            final ObjectExpression.Evaluator<Color4f> eval = expression.createEvaluator(scope);
            updaters.add(new Updater() {
                public void update () {
                    Color4f value = eval.evaluate();
                    uniform.value.set(value.r, value.g, value.b, value.a);
                    uniform.dirty = true;
                }
            });
            return uniform;
        }
    }

    /**
     * A float-valued uniform whose value is derived from an expression.
     */
    public static class FloatExprUniformConfig extends ExpressionUniformConfig
    {
        /** The expression for the uniform. */
        @Editable
        public FloatExpression expression = new FloatExpression.Constant();

        @Override
        protected Uniform createUniform (int location, Scope scope, List<Updater> updaters)
        {
            final FloatUniform uniform = new FloatUniform(location);
            final FloatExpression.Evaluator eval = expression.createEvaluator(scope);
            updaters.add(new Updater() {
                public void update () {
                    uniform.value = eval.evaluate();
                    uniform.dirty = true;
                }
            });
            return uniform;
        }
    }

    /**
     * An integer-valued uniform whose value is derived from an expression.
     */
    public static class IntegerExprUniformConfig extends ExpressionUniformConfig
    {
        /** The expression for the uniform. */
        @Editable
        public IntegerExpression expression = new IntegerExpression.Constant();

        @Override
        protected Uniform createUniform (int location, Scope scope, List<Updater> updaters)
        {
            final IntegerUniform uniform = new IntegerUniform(location);
            final IntegerExpression.Evaluator eval = expression.createEvaluator(scope);
            updaters.add(new Updater() {
                public void update () {
                    uniform.value = eval.evaluate();
                    uniform.dirty = true;
                }
            });
            return uniform;
        }
    }

    /**
     * A transform-valued uniform whose value is derived from an expression.
     */
    public static class TransformExprUniformConfig extends ExpressionUniformConfig
    {
        /** The expression for the uniform. */
        @Editable
        public Transform3DExpression expression = new Transform3DExpression.Constant();

        @Override
        protected Uniform createUniform (int location, Scope scope, List<Updater> updaters)
        {
            final Matrix4fUniform uniform = new Matrix4fUniform(location);
            final ObjectExpression.Evaluator<Transform3D> eval = expression.createEvaluator(scope);
            updaters.add(new Updater() {
                public void update () {
                    Transform3D value = eval.evaluate();
                    value.update(Transform3D.GENERAL);
                    uniform.value.set(value.getMatrix());
                    uniform.dirty = true;
                }
            });
            return uniform;
        }
    }

    /**
     * Base class for configs representing uniform arrays whose values come from references
     * to scoped variables.
     */
    public static abstract class ArrayRefUniformConfig<T> extends UniformConfig
    {
        @Override
        public void createUniforms (
            Scope scope, Program program, List<Uniform> uniforms, List<Updater> updaters)
        {
            // look up the array values
            T[] values = ScopeUtil.resolve(scope, name, null, getArrayClass());
            if (values == null) {
                return;
            }
            List<Uniform> list = Lists.newArrayList();
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
        @Override
        protected Class<? extends Matrix4f[]> getArrayClass ()
        {
            return Matrix4f.EMPTY_ARRAY.getClass();
        }

        @Override
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
        IntegerDefinition.class, StringDefinition.class, TransformDefinition.class,
        FogParamSnippet.class, FogBlendSnippet.class, TexCoordSnippet.class,
        VertexLightingSnippet.class, FragmentLightingSnippet.class })
    public static abstract class Definition extends DeepObject
        implements Exportable
    {
        /** The name of the definition. */
        @Editable(hgroup="p")
        public String name = "";

        /**
         * Retrieves the definitions for this config and adds them to the provided list.
         */
        public abstract void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs);
    }

    /**
     * A boolean definition.
     */
    public static class BooleanDefinition extends Definition
    {
        /** The value of the definition. */
        @Editable(hgroup="p")
        public boolean value;

        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            if (value) {
                defs.add(name);
            }
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

        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            defs.add(name + " vec4(" +
                GLSL_FLOAT.format(value.r) + ", " +
                GLSL_FLOAT.format(value.g) + ", " +
                GLSL_FLOAT.format(value.b) + ", " +
                GLSL_FLOAT.format(value.a) + ")");
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

        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            defs.add(name + " " + GLSL_FLOAT.format(value));
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

        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            defs.add(name + " " + value);
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

        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            defs.add(name + " " + value);
        }
    }

    /**
     * A transform-valued definition.
     */
    public static class TransformDefinition extends Definition
    {
        /** The value of the definition. */
        @Editable(step=0.01, hgroup="p")
        public Transform3D value = new Transform3D();

        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            value.update(Transform3D.GENERAL);
            Matrix4f matrix = value.getMatrix();
            defs.add(name + " mat4(" +
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
                GLSL_FLOAT.format(matrix.m33) + ")");
        }
    }

    /**
     * Defines a snippet that sets the fog parameter value to simulate the behavior of the
     * fixed-function pipeline.
     */
    public static class FogParamSnippet extends Definition
    {
        /** The variable containing the location of the vertex in eye space. */
        @Editable
        public String eyeVertex = "eyeVertex";

        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            SnippetUtil.getFogParam(name, eyeVertex, states, defs);
        }
    }

    /**
     * Defines a snippet that blends in the fog according to the fog parameter to simulate the
     * behavior of the fixed-function pipeline.
     */
    public static class FogBlendSnippet extends Definition
    {
        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            SnippetUtil.getFogBlend(name, states, defs);
        }
    }

    /**
     * Defines a snippet that sets the tex coords to simulate the behavior of the fixed-function
     * pipeline.
     */
    public static class TexCoordSnippet extends Definition
    {
        /** The variable containing the location of the vertex in eye space. */
        @Editable
        public String eyeVertex = "eyeVertex";

        /** The variable containing the normal in eye space. */
        @Editable
        public String eyeNormal = "eyeNormal";

        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            SnippetUtil.getTexCoord(name, eyeVertex, eyeNormal, states, defs);
        }
    }

    /**
     * Defines a snippet that sets the front and/or back colors to simulate the behavior of the
     * fixed-function pipeline.
     */
    public static class VertexLightingSnippet extends Definition
    {
        /** The variable containing the location of the vertex in eye space. */
        @Editable
        public String eyeVertex = "eyeVertex";

        /** The variable containing the normal in eye space. */
        @Editable
        public String eyeNormal = "eyeNormal";

        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            SnippetUtil.getVertexLighting(
                name, eyeVertex, eyeNormal, states, vertexProgramTwoSide, defs);
        }
    }

    /**
     * Defines a snippet that computes the fragment color based on the lighting parameters.
     */
    public static class FragmentLightingSnippet extends Definition
    {
        /** The variable containing the location of the vertex in eye space. */
        @Editable
        public String eyeVertex = "eyeVertex";

        /** The variable containing the normal in eye space. */
        @Editable
        public String eyeNormal = "eyeNormal";

        @Override
        public void getDefinitions (
            Scope scope, RenderState[] states, boolean vertexProgramTwoSide, List<String> defs)
        {
            SnippetUtil.getFragmentLighting(name, eyeVertex, eyeNormal, states, defs);
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
    public Shader getShader (
        GlContext ctx, Scope scope, RenderState[] states, boolean vertexProgramTwoSide)
    {
        return implementation.getShader(ctx, scope, states, vertexProgramTwoSide);
    }

    /**
     * Returns the array of uniforms for this configuration.
     */
    public UniformConfig[] getUniforms (GlContext ctx)
    {
        return implementation.getUniforms(ctx);
    }

    @Override
    protected void getUpdateResources (HashSet<String> paths)
    {
        implementation.getUpdateResources(paths);
    }

    /** Formats floats so that they will be recognized as float constants in GLSL. */
    protected static final DecimalFormat GLSL_FLOAT = new DecimalFormat("0.0");

    /** The index of the first vertex attribute (earlier ones are reserved). */
    protected static final int FIRST_VERTEX_ATTRIB_INDEX = 9;
}
