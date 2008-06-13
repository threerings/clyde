//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.Transform;
import com.threerings.math.Vector3f;
import com.threerings.math.Vector4f;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Program.FloatUniform;
import com.threerings.opengl.renderer.Program.IntegerUniform;
import com.threerings.opengl.renderer.Program.Vector3fUniform;
import com.threerings.opengl.renderer.Program.Vector4fUniform;
import com.threerings.opengl.renderer.Program.Matrix4fUniform;
import com.threerings.opengl.renderer.Program.Uniform;
import com.threerings.opengl.renderer.state.ShaderState;

/**
 * Configurable shader state.
 */
public abstract class ShaderStateConfig extends DeepObject
    implements Exportable
{
    /**
     * Disables the shader.
     */
    public static class Disabled extends ShaderStateConfig
    {
        @Override // documentation inherited
        public ShaderState getState ()
        {
            return ShaderState.DISABLED;
        }
    }

    /**
     * Enables the shader.
     */
    public static class Enabled extends ShaderStateConfig
    {
        /** The uniforms to pass to the shader. */
        @Editable
        public UniformConfig[] uniforms = new UniformConfig[0];

        @Override // documentation inherited
        public ShaderState getState ()
        {
            return null;
        }
    }

    /**
     * Represents the value of a shader uniform variable.
     */
    public static abstract class UniformConfig extends DeepObject
        implements Exportable
    {
        /** The name of the uniform. */
        @Editable(hgroup="p")
        public String name = "";

        /**
         * Returns the subclasses available for selection in the editor.
         */
        public static Class[] getEditorTypes ()
        {
            return new Class[] {
                IntegerUniformConfig.class, FloatUniformConfig.class,
                ColorUniformConfig.class, TransformUniformConfig.class };
        }

        /**
         * Creates a uniform object from this configuration.
         *
         * @param location the location of the uniform.
         */
        public abstract Uniform createUniform (int location);
    }

    /**
     * An integer-valued uniform.
     */
    public static class IntegerUniformConfig extends UniformConfig
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
     * A float-valued uniform.
     */
    public static class FloatUniformConfig extends UniformConfig
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
     * A color-valued uniform.
     */
    public static class ColorUniformConfig extends UniformConfig
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
     * A transform-valued uniform.
     */
    public static class TransformUniformConfig extends UniformConfig
    {
        /** The value of the uniform. */
        @Editable(hgroup="p")
        public Transform transform = new Transform();

        @Override // documentation inherited
        public Uniform createUniform (int location)
        {
            transform.update(Transform.GENERAL);
            return new Matrix4fUniform(location, transform.getMatrix());
        }
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { Disabled.class, Enabled.class };
    }

    /**
     * Returns the corresponding shader state.
     */
    public abstract ShaderState getState ();
}
