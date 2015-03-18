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

package com.threerings.export;

import java.io.Closeable;
import java.io.IOException;

import java.lang.reflect.Field;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import com.threerings.math.Matrix3f;
import com.threerings.math.Matrix4f;
import com.threerings.math.Quaternion;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;
import com.threerings.opengl.renderer.Color4f;

/**
 * Used to read {@link Exportable} objects.  Other common object types are supported as well
 * (@see {@link Exporter}).
 *
 * @see Exportable
 */
public abstract class Importer
    implements Closeable
{
    /**
     * Imports an object from the underlying stream.
     */
    public abstract Object readObject ()
        throws IOException;

    /**
     * Writes the default fields of the object.
     */
    public void defaultReadFields ()
        throws IOException
    {
        if (_marshaller == null) {
            throw new IllegalStateException("Not invoking a custom readFields method.");
        }
        _marshaller.readFields(_object, this, false);
    }

    /**
     * Reads a boolean value associated with the current object.
     */
    public abstract boolean read (String name, boolean defvalue)
        throws IOException;

    /**
     * Reads a byte value associated with the current object.
     */
    public abstract byte read (String name, byte defvalue)
        throws IOException;

    /**
     * Reads a character value associated with the current object.
     */
    public abstract char read (String name, char defvalue)
        throws IOException;

    /**
     * Reads a double value associated with the current object.
     */
    public abstract double read (String name, double defvalue)
        throws IOException;

    /**
     * Reads a float value associated with the current object.
     */
    public abstract float read (String name, float defvalue)
        throws IOException;

    /**
     * Reads an integer value associated with the current object.
     */
    public abstract int read (String name, int defvalue)
        throws IOException;

    /**
     * Reads a long value associated with the current object.
     */
    public abstract long read (String name, long defvalue)
        throws IOException;

    /**
     * Reads a short value associated with the current object.
     */
    public abstract short read (String name, short defvalue)
        throws IOException;

    /**
     * Reads a boolean array value associated with the current object.
     */
    public boolean[] read (String name, boolean[] defvalue)
        throws IOException
    {
        return read(name, defvalue, boolean[].class);
    }

    /**
     * Reads a byte array value associated with the current object.
     */
    public byte[] read (String name, byte[] defvalue)
        throws IOException
    {
        return read(name, defvalue, byte[].class);
    }

    /**
     * Reads a character array value associated with the current object.
     */
    public char[] read (String name, char[] defvalue)
        throws IOException
    {
        return read(name, defvalue, char[].class);
    }

    /**
     * Reads a double array value associated with the current object.
     */
    public double[] read (String name, double[] defvalue)
        throws IOException
    {
        return read(name, defvalue, double[].class);
    }

    /**
     * Reads a float array value associated with the current object.
     */
    public float[] read (String name, float[] defvalue)
        throws IOException
    {
        return read(name, defvalue, float[].class);
    }

    /**
     * Reads an integer array value associated with the current object.
     */
    public int[] read (String name, int[] defvalue)
        throws IOException
    {
        return read(name, defvalue, int[].class);
    }

    /**
     * Reads a long array value associated with the current object.
     */
    public long[] read (String name, long[] defvalue)
        throws IOException
    {
        return read(name, defvalue, long[].class);
    }

    /**
     * Reads a short array value associated with the current object.
     */
    public short[] read (String name, short[] defvalue)
        throws IOException
    {
        return read(name, defvalue, short[].class);
    }

    /**
     * Reads a string value associated with the current object.
     */
    public String read (String name, String defvalue)
        throws IOException
    {
        return read(name, defvalue, String.class);
    }

    /**
     * Reads a byte buffer value associated with the current object.
     */
    public ByteBuffer read (String name, ByteBuffer defvalue)
        throws IOException
    {
        return read(name, defvalue, ByteBuffer.class);
    }

    /**
     * Reads a character buffer value associated with the current object.
     */
    public CharBuffer read (String name, CharBuffer defvalue)
        throws IOException
    {
        return read(name, defvalue, CharBuffer.class);
    }

    /**
     * Reads a double buffer value associated with the current object.
     */
    public DoubleBuffer read (String name, DoubleBuffer defvalue)
        throws IOException
    {
        return read(name, defvalue, DoubleBuffer.class);
    }

    /**
     * Reads a float buffer value associated with the current object.
     */
    public FloatBuffer read (String name, FloatBuffer defvalue)
        throws IOException
    {
        return read(name, defvalue, FloatBuffer.class);
    }

    /**
     * Reads an integer buffer value associated with the current object.
     */
    public IntBuffer read (String name, IntBuffer defvalue)
        throws IOException
    {
        return read(name, defvalue, IntBuffer.class);
    }

    /**
     * Reads a long buffer value associated with the current object.
     */
    public LongBuffer read (String name, LongBuffer defvalue)
        throws IOException
    {
        return read(name, defvalue, LongBuffer.class);
    }

    /**
     * Reads a short buffer value associated with the current object.
     */
    public ShortBuffer read (String name, ShortBuffer defvalue)
        throws IOException
    {
        return read(name, defvalue, ShortBuffer.class);
    }

    /**
     * Reads a color value associated with the current object.
     */
    public Color4f read (String name, Color4f defvalue)
        throws IOException
    {
        return read(name, defvalue, Color4f.class);
    }

    /**
     * Reads a matrix value associated with the current object.
     */
    public Matrix3f read (String name, Matrix3f defvalue)
        throws IOException
    {
        return read(name, defvalue, Matrix3f.class);
    }

    /**
     * Reads a matrix value associated with the current object.
     */
    public Matrix4f read (String name, Matrix4f defvalue)
        throws IOException
    {
        return read(name, defvalue, Matrix4f.class);
    }

    /**
     * Reads a quaternion value associated with the current object.
     */
    public Quaternion read (String name, Quaternion defvalue)
        throws IOException
    {
        return read(name, defvalue, Quaternion.class);
    }

    /**
     * Reads a vector value associated with the current object.
     */
    public Vector2f read (String name, Vector2f defvalue)
        throws IOException
    {
        return read(name, defvalue, Vector2f.class);
    }

    /**
     * Reads a vector value associated with the current object.
     */
    public Vector3f read (String name, Vector3f defvalue)
        throws IOException
    {
        return read(name, defvalue, Vector3f.class);
    }

    /**
     * Reads an exportable value associated with the current object.
     */
    public Exportable read (String name, Exportable defvalue)
        throws IOException
    {
        return read(name, defvalue, Exportable.class);
    }

    /**
     * Reads an object value associated with the current object.
     */
    public abstract <T> T read (String name, T defvalue, Class<T> clazz)
        throws IOException;

    /**
     * Closes the underlying stream.
     */
    public abstract void close ()
        throws IOException;

    /**
     * Internal method for reading from a FieldMarshaller.
     */
    protected Object read (String name, Object defvalue, Field field)
        throws IOException
    {
        @SuppressWarnings("unchecked")
        Class<Object> clazz = (Class<Object>)field.getType();
        return read(name, defvalue, clazz);
    }

    /**
     * Reads an object's fields.
     */
    protected void readFields (Exportable object)
        throws IOException
    {
        Object oobject = _object;
        ObjectMarshaller omarshaller = _marshaller;
        try {
            _object = object;
            _marshaller = ObjectMarshaller.getObjectMarshaller(object.getClass());
            _marshaller.readFields(_object, this, true);
        } finally {
            _object = oobject;
            _marshaller = omarshaller;
        }
    }

    /** The object whose fields are being read. */
    protected Object _object;

    /** The marshaller for the current object. */
    protected ObjectMarshaller _marshaller;
}
