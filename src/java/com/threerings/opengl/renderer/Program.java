//
// $Id$

package com.threerings.opengl.renderer;

import java.io.UnsupportedEncodingException;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

import com.samskivert.util.HashIntMap;

import com.threerings.math.Matrix4f;

import static com.threerings.opengl.Log.*;

/**
 * Contains a shader program.
 */
public class Program extends ShaderObject
{
    /**
     * Contains the location and value of a uniform variable.
     */
    public static abstract class Uniform
    {
        /** Set when the uniform value has changed and must be reapplied. */
        public boolean dirty;

        /**
         * Creates a new uniform variable with the specified location.
         */
        public Uniform (int location)
        {
            _location = location;
        }

        /**
         * Returns the location of this uniform.
         */
        public int getLocation ()
        {
            return _location;
        }

        /**
         * Applies the value of this uniform.
         */
        public abstract void apply ();

        /**
         * Clones this uniform, reusing the supplied object if possible.
         */
        public abstract Uniform clone (Uniform uniform);

        /** The location of this uniform. */
        protected int _location;
    }

    /**
     * A uniform containing an integer.
     */
    public static class UniformInteger extends Uniform
    {
        /** The integer value. */
        public int value;

        /**
         * Creates a new integer uniform with the specified location.
         */
        public UniformInteger (int location)
        {
            super(location);
        }

        @Override // documentation inherited
        public void apply ()
        {
            ARBShaderObjects.glUniform1iARB(_location, value);
        }

        @Override // documentation inherited
        public Uniform clone (Uniform uniform)
        {
            UniformInteger clone = (uniform instanceof UniformInteger) ?
                ((UniformInteger)uniform) : new UniformInteger(_location);
            clone.value = value;
            return clone;
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return other instanceof UniformInteger &&
                ((UniformInteger)other).value == value;
        }
    }

    /**
     * A uniform containing a 4x4 matrix.
     */
    public static class UniformMatrix4f extends Uniform
    {
        /** The matrix value. */
        public Matrix4f value = new Matrix4f();

        /**
         * Creates a new matrix uniform with the specified location.
         */
        public UniformMatrix4f (int location)
        {
            super(location);
        }

        @Override // documentation inherited
        public void apply ()
        {
            value.get(_vbuf).rewind();
            ARBShaderObjects.glUniformMatrix4ARB(_location, false, _vbuf);
        }

        @Override // documentation inherited
        public Uniform clone (Uniform uniform)
        {
            UniformMatrix4f clone = (uniform instanceof UniformMatrix4f) ?
                ((UniformMatrix4f)uniform) : new UniformMatrix4f(_location);
            clone.value.set(value);
            return clone;
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return other instanceof UniformMatrix4f &&
                ((UniformMatrix4f)other).value.equals(value);
        }
    }

    /**
     * Creates a new shader program.
     */
    public Program (Renderer renderer)
    {
        super(renderer);
        _id = ARBShaderObjects.glCreateProgramObjectARB();
    }

    /**
     * Relinks the program with its current vertex and fragment shaders.
     *
     * @return true if the program linked successfully, false if there was an error.
     */
    public boolean relink ()
    {
        return setShaders(_vertex, _fragment);
    }

    /**
     * Sets the shaders for this program and links it.  If linkage fails (or even if it
     * succeeds), {@link #getInfoLog} can be used to return more information.
     *
     * @return true if the program linked successfully, false if there was an error.
     */
    public boolean setShaders (Shader vertex, Shader fragment)
    {
        if (_vertex != vertex) {
            if (_vertex != null) {
                ARBShaderObjects.glDetachObjectARB(_id, _vertex.getId());
            }
            if ((_vertex = vertex) != null) {
                ARBShaderObjects.glAttachObjectARB(_id, _vertex.getId());
            }
        }
        if (_fragment != fragment) {
            if (_fragment != null) {
                ARBShaderObjects.glDetachObjectARB(_id, _fragment.getId());
            }
            if ((_fragment = fragment) != null) {
                ARBShaderObjects.glAttachObjectARB(_id, _fragment.getId());
            }
        }
        ARBShaderObjects.glLinkProgramARB(_id);
        IntBuffer ibuf = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(
            _id, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB, ibuf);
        return (ibuf.get(0) == GL11.GL_TRUE);
    }

    /**
     * Returns a reference to the vertex shader.
     */
    public Shader getVertexShader ()
    {
        return _vertex;
    }

    /**
     * Returns a reference to the fragment shader.
     */
    public Shader getFragmentShader ()
    {
        return _fragment;
    }

    /**
     * Returns the location of the identified uniform variable.
     */
    public int getUniformLocation (String name)
    {
        Integer location = _uniformLocations.get(name);
        if (location == null) {
            _uniformLocations.put(
                name, location = ARBShaderObjects.glGetUniformLocationARB(_id, toBuffer(name)));
        }
        return location;
    }

    /**
     * Sets the values of the shader's uniform variables.
     */
    public void setUniforms (Uniform[] uniforms)
    {
        for (Uniform uniform : uniforms) {
            int location = uniform.getLocation();
            Uniform uval = _uniforms.get(location);
            if (uval == uniform && !uniform.dirty) {
                continue;
            }
            uniform.dirty = false;
            if (!uniform.equals(uval)) {
                uniform.apply();
                Uniform nval = uniform.clone(uval);
                if (nval != uval) {
                    _uniforms.put(location, nval);
                }
            }
        }
    }

    /**
     * Binds an attribute to the specified location.
     */
    public void setAttribLocation (String name, int index)
    {
        ARBVertexShader.glBindAttribLocationARB(_id, index, toBuffer(name));
        _attribLocations.put(name, index);
    }

    /**
     * Returns the location of the identified attribute.
     */
    public int getAttribLocation (String name)
    {
        Integer location = _attribLocations.get(name);
        if (location == null) {
            _attribLocations.put(name,
                location = ARBVertexShader.glGetAttribLocationARB(_id, toBuffer(name)));
        }
        return location;
    }

    /**
     * Creates an invalid program (used by the renderer to force reapplication).
     */
    protected Program ()
    {
        super(null);
    }

    /**
     * Creates a new byte buffer containing the specified string in null-terminated ASCII
     * format.
     */
    protected static ByteBuffer toBuffer (String string)
    {
        ByteBuffer buf = ASCII_CHARSET.encode(string);
        ByteBuffer buf0 = BufferUtils.createByteBuffer(buf.remaining() + 1);
        buf0.put(buf).put((byte)0).rewind();
        return buf0;
    }

    /** The vertex shader. */
    protected Shader _vertex;

    /** The fragment shader. */
    protected Shader _fragment;

    /** Maps uniform names to their locations. */
    protected HashMap<String, Integer> _uniformLocations = new HashMap<String, Integer>();

    /** Maps attribute names to their locations. */
    protected HashMap<String, Integer> _attribLocations = new HashMap<String, Integer>();

    /** Maps uniform locations to their current values. */
    protected HashIntMap<Uniform> _uniforms = new HashIntMap<Uniform>();

    /** Used to set values. */
    protected static FloatBuffer _vbuf = BufferUtils.createFloatBuffer(16);
}
