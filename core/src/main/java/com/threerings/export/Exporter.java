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
import java.io.File;
import java.io.IOException;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;

import com.threerings.config.ArgumentMap;

import com.threerings.math.Matrix3f;
import com.threerings.math.Matrix4f;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;
import com.threerings.math.Quaternion;

import com.threerings.opengl.renderer.Color4f;

import static com.threerings.export.Log.log;

/**
 * Used to write {@link Exportable} objects.  Other common object types are supported as well:
 * <code>Boolean, Byte, Character, Short, Integer, Long, Float, Double, String, boolean[], byte[],
 * char[], short[], int[], long[], float[], double[], Object[], Collection, Map, Enum, ByteBuffer,
 * CharBuffer, DoubleBuffer, FloatBuffer, IntBuffer, LongBuffer, ShortBuffer, ...</code>.
 *
 * @see Exportable
 */
public abstract class Exporter
    implements Closeable
{
    /**
     * Can be used to rewrite objects on as they are exported.
     */
    public interface Replacer
    {
        /**
         * Return the replacement to use, or null.
         *
         * @param value the value to be written
         * @param clazz the expected class type
         */
        public Replacement getReplacement (Object value, Class<?> clazz);
    }

    /**
     * Helper class for Replacer.
     */
    public static class Replacement
    {
        /** The new value to write instead. */
        public final Object value;

        /** The class type to write instead. */
        public final Class<?> clazz;

        /**
         * Construct a replacement. */
        public Replacement (Object value, Class<?> clazz)
        {
            this.value = value;
            this.clazz = clazz;
        }
    }

    /**
     * Set the replacer to use with this Exporter.
     */
    public Exporter setReplacer (Replacer replacer)
    {
        _replacer = replacer;
        return this;
    }

    /**
     * Writes the object to the underlying stream.
     */
    public abstract void writeObject (Object oject)
        throws IOException;

    /**
     * Writes the default fields of the object.
     */
    public void defaultWriteFields ()
        throws IOException
    {
        if (_marshaller == null) {
            throw new IllegalStateException("Not invoking a custom writeFields method.");
        }
        _marshaller.writeFields(_object, this, false);
    }

    /**
     * Associates a boolean value with the current object (if not equal to the default).
     */
    public void write (String name, boolean value, boolean defvalue)
        throws IOException
    {
        if (value != defvalue) {
            write(name, value);
        }
    }

    /**
     * Associates a boolean value with the current object.
     */
    public abstract void write (String name, boolean value)
        throws IOException;

    /**
     * Associates a byte value with the current object (if not equal to the default).
     */
    public void write (String name, byte value, byte defvalue)
        throws IOException
    {
        if (value != defvalue) {
            write(name, value);
        }
    }

    /**
     * Associates a byte value with the current object.
     */
    public abstract void write (String name, byte value)
        throws IOException;

    /**
     * Associates a character value with the current object (if not equal to the default).
     */
    public void write (String name, char value, char defvalue)
        throws IOException
    {
        if (value != defvalue) {
            write(name, value);
        }
    }

    /**
     * Associates a character value with the current object.
     */
    public abstract void write (String name, char value)
        throws IOException;

    /**
     * Associates a double value with the current object (if not equal to the default).
     */
    public void write (String name, double value, double defvalue)
        throws IOException
    {
        if (value != defvalue) {
            write(name, value);
        }
    }

    /**
     * Associates a double value with the current object.
     */
    public abstract void write (String name, double value)
        throws IOException;

    /**
     * Associates a float value with the current object (if not equal to the default).
     */
    public void write (String name, float value, float defvalue)
        throws IOException
    {
        if (value != defvalue) {
            write(name, value);
        }
    }

    /**
     * Associates a float value with the current object.
     */
    public abstract void write (String name, float value)
        throws IOException;

    /**
     * Associates an integer value with the current object (if not equal to the default).
     */
    public void write (String name, int value, int defvalue)
        throws IOException
    {
        if (value != defvalue) {
            write(name, value);
        }
    }

    /**
     * Associates an integer value with the current object.
     */
    public abstract void write (String name, int value)
        throws IOException;

    /**
     * Associates a long value with the current object (if not equal to the default).
     */
    public void write (String name, long value, long defvalue)
        throws IOException
    {
        if (value != defvalue) {
            write(name, value);
        }
    }

    /**
     * Associates a long value with the current object.
     */
    public abstract void write (String name, long value)
        throws IOException;

    /**
     * Associates a short value with the current object (if not equal to the default).
     */
    public void write (String name, short value, short defvalue)
        throws IOException
    {
        if (value != defvalue) {
            write(name, value);
        }
    }

    /**
     * Associates a short value with the current object.
     */
    public abstract void write (String name, short value)
        throws IOException;

    /**
     * Associates a boolean array value with the current object (if not equal to the default).
     */
    public void write (String name, boolean[] value, boolean[] defvalue)
        throws IOException
    {
        write(name, value, defvalue, boolean[].class);
    }

    /**
     * Associates a boolean array value with the current object.
     */
    public void write (String name, boolean[] value)
        throws IOException
    {
        write(name, value, boolean[].class);
    }

    /**
     * Associates a byte array value with the current object (if not equal to the default).
     */
    public void write (String name, byte[] value, byte[] defvalue)
        throws IOException
    {
        write(name, value, defvalue, byte[].class);
    }

    /**
     * Associates a byte array value with the current object.
     */
    public void write (String name, byte[] value)
        throws IOException
    {
        write(name, value, byte[].class);
    }

    /**
     * Associates a character array value with the current object (if not equal to the default).
     */
    public void write (String name, char[] value, char[] defvalue)
        throws IOException
    {
        write(name, value, defvalue, char[].class);
    }

    /**
     * Associates a character array value with the current object.
     */
    public void write (String name, char[] value)
        throws IOException
    {
        write(name, value, char[].class);
    }

    /**
     * Associates a double array value with the current object (if not equal to the default).
     */
    public void write (String name, double[] value, double[] defvalue)
        throws IOException
    {
        write(name, value, defvalue, double[].class);
    }

    /**
     * Associates a double array value with the current object.
     */
    public void write (String name, double[] value)
        throws IOException
    {
        write(name, value, double[].class);
    }

    /**
     * Associates a float array value with the current object (if not equal to the default).
     */
    public void write (String name, float[] value, float[] defvalue)
        throws IOException
    {
        write(name, value, defvalue, float[].class);
    }

    /**
     * Associates a float array value with the current object.
     */
    public void write (String name, float[] value)
        throws IOException
    {
        write(name, value, float[].class);
    }

    /**
     * Associates an integer array value with the current object (if not equal to the default).
     */
    public void write (String name, int[] value, int[] defvalue)
        throws IOException
    {
        write(name, value, defvalue, int[].class);
    }

    /**
     * Associates an integer array value with the current object.
     */
    public void write (String name, int[] value)
        throws IOException
    {
        write(name, value, int[].class);
    }

    /**
     * Associates a long array value with the current object (if not equal to the default).
     */
    public void write (String name, long[] value, long[] defvalue)
        throws IOException
    {
        write(name, value, defvalue, long[].class);
    }

    /**
     * Associates a long array value with the current object.
     */
    public void write (String name, long[] value)
        throws IOException
    {
        write(name, value, long[].class);
    }

    /**
     * Associates a short array value with the current object (if not equal to the default).
     */
    public void write (String name, short[] value, short[] defvalue)
        throws IOException
    {
        write(name, value, defvalue, short[].class);
    }

    /**
     * Associates a short array value with the current object.
     */
    public void write (String name, short[] value)
        throws IOException
    {
        write(name, value, short[].class);
    }

    /**
     * Associates a string value with the current object (if not equal to the default).
     */
    public void write (String name, String value, String defvalue)
        throws IOException
    {
        write(name, value, defvalue, String.class);
    }

    /**
     * Associates a string value with the current object.
     */
    public void write (String name, String value)
        throws IOException
    {
        write(name, value, String.class);
    }

    /**
     * Associates a byte buffer value with the current object (if not equal to the default).
     */
    public void write (String name, ByteBuffer value, ByteBuffer defvalue)
        throws IOException
    {
        write(name, value, defvalue, ByteBuffer.class);
    }

    /**
     * Associates a byte buffer value with the current object.
     */
    public void write (String name, ByteBuffer value)
        throws IOException
    {
        write(name, value, ByteBuffer.class);
    }

    /**
     * Associates a character buffer value with the current object (if not equal to the default).
     */
    public void write (String name, CharBuffer value, CharBuffer defvalue)
        throws IOException
    {
        write(name, value, defvalue, CharBuffer.class);
    }

    /**
     * Associates a character buffer value with the current object.
     */
    public void write (String name, CharBuffer value)
        throws IOException
    {
        write(name, value, CharBuffer.class);
    }

    /**
     * Associates a double buffer value with the current object (if not equal to the default).
     */
    public void write (String name, DoubleBuffer value, DoubleBuffer defvalue)
        throws IOException
    {
        write(name, value, defvalue, DoubleBuffer.class);
    }

    /**
     * Associates a double buffer value with the current object.
     */
    public void write (String name, DoubleBuffer value)
        throws IOException
    {
        write(name, value, DoubleBuffer.class);
    }

    /**
     * Associates a float buffer value with the current object (if not equal to the default).
     */
    public void write (String name, FloatBuffer value, FloatBuffer defvalue)
        throws IOException
    {
        write(name, value, defvalue, FloatBuffer.class);
    }

    /**
     * Associates a float buffer value with the current object.
     */
    public void write (String name, FloatBuffer value)
        throws IOException
    {
        write(name, value, FloatBuffer.class);
    }

    /**
     * Associates an integer buffer value with the current object (if not equal to the default).
     */
    public void write (String name, IntBuffer value, IntBuffer defvalue)
        throws IOException
    {
        write(name, value, defvalue, IntBuffer.class);
    }

    /**
     * Associates an integer buffer value with the current object.
     */
    public void write (String name, IntBuffer value)
        throws IOException
    {
        write(name, value, IntBuffer.class);
    }

    /**
     * Associates a long buffer value with the current object (if not equal to the default).
     */
    public void write (String name, LongBuffer value, LongBuffer defvalue)
        throws IOException
    {
        write(name, value, defvalue, LongBuffer.class);
    }

    /**
     * Associates a long buffer value with the current object.
     */
    public void write (String name, LongBuffer value)
        throws IOException
    {
        write(name, value, LongBuffer.class);
    }

    /**
     * Associates a short buffer value with the current object (if not equal to the default).
     */
    public void write (String name, ShortBuffer value, ShortBuffer defvalue)
        throws IOException
    {
        write(name, value, defvalue, ShortBuffer.class);
    }

    /**
     * Associates a short buffer value with the current object.
     */
    public void write (String name, ShortBuffer value)
        throws IOException
    {
        write(name, value, ShortBuffer.class);
    }

    /**
     * Associates a color value with the current object (if not equal to the default).
     */
    public void write (String name, Color4f value, Color4f defvalue)
        throws IOException
    {
        write(name, value, defvalue, Color4f.class);
    }

    /**
     * Associates a color value with the current object.
     */
    public void write (String name, Color4f value)
        throws IOException
    {
        write(name, value, Color4f.class);
    }

    /**
     * Associates a matrix value with the current object (if not equal to the default).
     */
    public void write (String name, Matrix3f value, Matrix3f defvalue)
        throws IOException
    {
        write(name, value, defvalue, Matrix3f.class);
    }

    /**
     * Associates a matrix value with the current object.
     */
    public void write (String name, Matrix3f value)
        throws IOException
    {
        write(name, value, Matrix3f.class);
    }

    /**
     * Associates a matrix value with the current object (if not equal to the default).
     */
    public void write (String name, Matrix4f value, Matrix4f defvalue)
        throws IOException
    {
        write(name, value, defvalue, Matrix4f.class);
    }

    /**
     * Associates a matrix value with the current object.
     */
    public void write (String name, Matrix4f value)
        throws IOException
    {
        write(name, value, Matrix4f.class);
    }

    /**
     * Associates a quaternion value with the current object (if not equal to the default).
     */
    public void write (String name, Quaternion value, Quaternion defvalue)
        throws IOException
    {
        write(name, value, defvalue, Quaternion.class);
    }

    /**
     * Associates a quaternion value with the current object.
     */
    public void write (String name, Quaternion value)
        throws IOException
    {
        write(name, value, Quaternion.class);
    }

    /**
     * Associates a vector value with the current object (if not equal to the default).
     */
    public void write (String name, Vector2f value, Vector2f defvalue)
        throws IOException
    {
        write(name, value, defvalue, Vector2f.class);
    }

    /**
     * Associates a vector value with the current object.
     */
    public void write (String name, Vector2f value)
        throws IOException
    {
        write(name, value, Vector2f.class);
    }

    /**
     * Associates a vector value with the current object (if not equal to the default).
     */
    public void write (String name, Vector3f value, Vector3f defvalue)
        throws IOException
    {
        write(name, value, defvalue, Vector3f.class);
    }

    /**
     * Associates a vector value with the current object.
     */
    public void write (String name, Vector3f value)
        throws IOException
    {
        write(name, value, Vector3f.class);
    }

    /**
     * Associates an exportable value with the current object (if not equal to the default).
     */
    public void write (String name, Exportable value, Exportable defvalue)
        throws IOException
    {
        write(name, value, defvalue, Exportable.class);
    }

    /**
     * Associates an exportable value with the current object.
     */
    public void write (String name, Exportable value)
        throws IOException
    {
        write(name, value, Exportable.class);
    }

    /**
     * Associates an object value with the current object (if not equal to the default).
     */
    public <T> void write (String name, T value, T defvalue, Class<T> clazz)
        throws IOException
    {
        // use Arrays.deepEquals in order to compare arrays sensibly
        _a1[0] = value;
        _a2[0] = defvalue;
        if (!Arrays.deepEquals(_a1, _a2)) {
            write(name, value, clazz);
        }
    }

    /**
     * Associates an object value with the current object.
     */
    public abstract <T> void write (String name, T value, Class<T> clazz)
        throws IOException;

    /**
     * Writes out any remaining data and closes the underlying stream.
     */
    public abstract void close ()
        throws IOException;

    /**
     * Writes out any remaining data without closing the underlying stream.
     */
    public abstract void finish ()
        throws IOException;

    /**
     * Writes an object's fields.
     */
    protected void writeFields (Exportable object)
        throws IOException
    {
        Object oobject = _object;
        ObjectMarshaller omarshaller = _marshaller;
        try {
            _object = object;
            _marshaller = ObjectMarshaller.getObjectMarshaller(object.getClass());
            _marshaller.writeFields(_object, this, true);
        } finally {
            _object = oobject;
            _marshaller = omarshaller;
        }
    }

    /**
     * Gets the actual class of the specified value, performing some slight modifications for
     * buffer instances and enums, etc.
     */
    protected static Class<?> getClass (Object value)
    {
        if (value instanceof Enum) {
            // check enum first- no getting around this with interfaces
            return ((Enum)value).getDeclaringClass();

        } else if (value instanceof Exportable) {
            return value.getClass(); // you're the boss

        } else if (value instanceof Collection) {
            if (value instanceof List) {
                return (value instanceof ImmutableList)
                        ? ImmutableList.class
                        : ArrayList.class;
            } else if (value instanceof Set) {
                return (value instanceof ImmutableSet)
                        ? ImmutableSet.class
                        : (value instanceof EnumSet) ? EnumSet.class : HashSet.class;
            } else if (value instanceof Multiset) {
                return (value instanceof ImmutableMultiset)
                        ? ImmutableMultiset.class
                        : HashMultiset.class;
            }
            // for now return ArrayList for unknown collections
            return ArrayList.class;

        } else if (value instanceof Map) {
            // we make a concession to ArgumentMap in here
            return (value instanceof ImmutableMap)
                    ? ImmutableMap.class
                    : (value instanceof ArgumentMap) ? ArgumentMap.class : HashMap.class;

        } else if (value instanceof Buffer) {
            if (value instanceof ByteBuffer) {
                return ByteBuffer.class;
            } else if (value instanceof CharBuffer) {
                return CharBuffer.class;
            } else if (value instanceof DoubleBuffer) {
                return DoubleBuffer.class;
            } else if (value instanceof FloatBuffer) {
                return FloatBuffer.class;
            } else if (value instanceof IntBuffer) {
                return IntBuffer.class;
            } else if (value instanceof LongBuffer) {
                return LongBuffer.class;
            } else if (value instanceof ShortBuffer) {
                return ShortBuffer.class;
            }
            // fall out to default

        } else if (value instanceof File) {
            return File.class;
        }

        // default case
        return value.getClass();
    }

    /** The object whose fields are being written. */
    protected Object _object;

    /** The marshaller for the current object. */
    protected ObjectMarshaller _marshaller;

    /** An optional replacer, for rewriting objects on their way out. */
    protected Replacer _replacer;

    /** Used for object comparisons using {@link Arrays#deepEquals}. */
    protected Object[] _a1 = new Object[1], _a2 = new Object[1];

    /** A Dummy enum class used to provide satisfaction to the type system. */
    protected enum DummyEnum {}
}
