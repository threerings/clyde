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

package com.threerings.opengl.renderer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL11;

import com.samskivert.util.HashIntMap;

import com.threerings.math.Matrix4f;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;
import com.threerings.math.Vector4f;

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
  public static class IntegerUniform extends Uniform
  {
    /** The integer value. */
    public int value;

    /**
     * Creates a new integer uniform with the specified location.
     */
    public IntegerUniform (int location)
    {
      super(location);
    }

    /**
     * Creates a new integer uniform with the specified location and value.
     */
    public IntegerUniform (int location, int value)
    {
      super(location);
      this.value = value;
    }

    @Override
    public void apply ()
    {
      GL20.glUniform1i(_location, value);
    }

    @Override
    public Uniform clone (Uniform uniform)
    {
      IntegerUniform clone = (uniform instanceof IntegerUniform) ?
        ((IntegerUniform)uniform) : new IntegerUniform(_location);
      clone.value = value;
      return clone;
    }

    @Override
    public boolean equals (Object other)
    {
      return other instanceof IntegerUniform &&
        ((IntegerUniform)other).value == value;
    }

    @Override
    public int hashCode ()
    {
      return value;
    }
  }

  /**
   * A uniform containing a float.
   */
  public static class FloatUniform extends Uniform
  {
    /** The float value. */
    public float value;

    /**
     * Creates a new float uniform with the specified location.
     */
    public FloatUniform (int location)
    {
      super(location);
    }

    /**
     * Creates a new float uniform with the specified location and value.
     */
    public FloatUniform (int location, float value)
    {
      super(location);
      this.value = value;
    }

    @Override
    public void apply ()
    {
      GL20.glUniform1f(_location, value);
    }

    @Override
    public Uniform clone (Uniform uniform)
    {
      FloatUniform clone = (uniform instanceof FloatUniform) ?
        ((FloatUniform)uniform) : new FloatUniform(_location);
      clone.value = value;
      return clone;
    }

    @Override
    public boolean equals (Object other)
    {
      return other instanceof FloatUniform &&
        ((FloatUniform)other).value == value;
    }

    @Override
    public int hashCode ()
    {
      return Float.floatToIntBits(value);
    }
  }

  /**
   * A uniform containing a two-element vector.
   */
  public static class Vector2fUniform extends Uniform
  {
    /** The vector value. */
    public Vector2f value = new Vector2f();

    /**
     * Creates a new vector uniform with the specified location.
     */
    public Vector2fUniform (int location)
    {
      super(location);
    }

    /**
     * Creates a new vector uniform with the specified location and value.
     */
    public Vector2fUniform (int location, Vector2f value)
    {
      super(location);
      this.value.set(value);
    }

    @Override
    public void apply ()
    {
      GL20.glUniform2f(_location, value.x, value.y);
    }

    @Override
    public Uniform clone (Uniform uniform)
    {
      Vector2fUniform clone = (uniform instanceof Vector2fUniform) ?
        ((Vector2fUniform)uniform) : new Vector2fUniform(_location);
      clone.value.set(value);
      return clone;
    }

    @Override
    public boolean equals (Object other)
    {
      return other instanceof Vector2fUniform &&
        ((Vector2fUniform)other).value.equals(value);
    }

    @Override
    public int hashCode ()
    {
      return value != null ? value.hashCode() : 0;
    }
  }

  /**
   * A uniform containing a three-element vector.
   */
  public static class Vector3fUniform extends Uniform
  {
    /** The vector value. */
    public Vector3f value = new Vector3f();

    /**
     * Creates a new vector uniform with the specified location.
     */
    public Vector3fUniform (int location)
    {
      super(location);
    }

    /**
     * Creates a new vector uniform with the specified location and value.
     */
    public Vector3fUniform (int location, Vector3f value)
    {
      super(location);
      this.value.set(value);
    }

    @Override
    public void apply ()
    {
      GL20.glUniform3f(_location, value.x, value.y, value.z);
    }

    @Override
    public Uniform clone (Uniform uniform)
    {
      Vector3fUniform clone = (uniform instanceof Vector3fUniform) ?
        ((Vector3fUniform)uniform) : new Vector3fUniform(_location);
      clone.value.set(value);
      return clone;
    }

    @Override
    public boolean equals (Object other)
    {
      return other instanceof Vector3fUniform &&
        ((Vector3fUniform)other).value.equals(value);
    }

    @Override
    public int hashCode ()
    {
      return value != null ? value.hashCode() : 0;
    }
  }

  /**
   * A uniform containing a four-element vector.
   */
  public static class Vector4fUniform extends Uniform
  {
    /** The vector value. */
    public Vector4f value = new Vector4f();

    /**
     * Creates a new vector uniform with the specified location.
     */
    public Vector4fUniform (int location)
    {
      super(location);
    }

    /**
     * Creates a new vector uniform with the specified location and value.
     */
    public Vector4fUniform (int location, Vector4f value)
    {
      super(location);
      this.value.set(value);
    }

    @Override
    public void apply ()
    {
      GL20.glUniform4f(_location, value.x, value.y, value.z, value.w);
    }

    @Override
    public Uniform clone (Uniform uniform)
    {
      Vector4fUniform clone = (uniform instanceof Vector4fUniform) ?
        ((Vector4fUniform)uniform) : new Vector4fUniform(_location);
      clone.value.set(value);
      return clone;
    }

    @Override
    public boolean equals (Object other)
    {
      return other instanceof Vector4fUniform &&
        ((Vector4fUniform)other).value.equals(value);
    }

    @Override
    public int hashCode ()
    {
      return value != null ? value.hashCode() : 0;
    }
  }

  /**
   * A uniform containing a 4x4 matrix.
   */
  public static class Matrix4fUniform extends Uniform
  {
    /** The matrix value. */
    public Matrix4f value = new Matrix4f();

    /**
     * Creates a new matrix uniform with the specified location.
     */
    public Matrix4fUniform (int location)
    {
      super(location);
    }

    /**
     * Creates a new matrix uniform with the specified location and value.
     */
    public Matrix4fUniform (int location, Matrix4f value)
    {
      super(location);
      this.value.set(value);
    }

    @Override
    public void apply ()
    {
      value.get(_vbuf).rewind();
      GL20.glUniformMatrix4fv(_location, false, _vbuf);
    }

    @Override
    public Uniform clone (Uniform uniform)
    {
      Matrix4fUniform clone = (uniform instanceof Matrix4fUniform) ?
        ((Matrix4fUniform)uniform) : new Matrix4fUniform(_location);
      clone.value.set(value);
      return clone;
    }

    @Override
    public boolean equals (Object other)
    {
      return other instanceof Matrix4fUniform &&
        ((Matrix4fUniform)other).value.equals(value);
    }

    @Override
    public int hashCode ()
    {
      return value != null ? value.hashCode() : 0;
    }
  }

  /**
   * Creates a new shader program.
   */
  public Program (Renderer renderer)
  {
    super(renderer);
    _id = GL20.glCreateProgram();
    _renderer.shaderObjectCreated();
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
        GL20.glDetachShader(_id, _vertex.getId());
      }
      if ((_vertex = vertex) != null) {
        GL20.glAttachShader(_id, _vertex.getId());
      }
    }
    if (_fragment != fragment) {
      if (_fragment != null) {
        GL20.glDetachShader(_id, _fragment.getId());
      }
      if ((_fragment = fragment) != null) {
        GL20.glAttachShader(_id, _fragment.getId());
      }
    }
    GL20.glLinkProgram(_id);
    return GL20.glGetProgrami(_id, GL20.GL_LINK_STATUS) == GL11.GL_TRUE;
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
        name, location = GL20.glGetUniformLocation(_id, name));
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
    GL20.glBindAttribLocation(_id, index, name);
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
        location = GL20.glGetAttribLocation(_id, name));
    }
    return location;
  }

  @Override
  public String getInfoLog ()
  {
    int length = GL20.glGetProgrami(_id, GL20.GL_INFO_LOG_LENGTH);
    if (length <= 0) {
      return "";
    }
    return GL20.glGetProgramInfoLog(_id, length);
  }

  @Override
  public void delete ()
  {
    GL20.glDeleteProgram(_id);
    _id = 0;
    _renderer.shaderObjectDeleted();
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
