//
// $Id$

package com.threerings.export;

import java.io.File;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import java.util.Arrays;
import java.util.HashMap;

import org.lwjgl.BufferUtils;

import com.threerings.math.Matrix3f;
import com.threerings.math.Matrix4f;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;
import com.threerings.math.Quaternion;

import com.threerings.opengl.renderer.Color4f;

import static java.util.logging.Level.*;
import static com.threerings.export.Log.*;

/**
 * Used to write {@link Exportable} objects.  Other common object types are supported as well:
 * <code>Boolean, Byte, Character, Short, Integer, Long, Float, Double, String, boolean[], byte[],
 * char[], short[], int[], long[], float[], double[], Object[], Collection, Map, Enum, ByteBuffer,
 * CharBuffer, DoubleBuffer, FloatBuffer, IntBuffer, LongBuffer, ShortBuffer, ...</code>.
 *
 * @see Exportable
 */
public abstract class Exporter
{
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
     * buffer instances and enums.
     */
    protected static Class getClass (Object value)
    {
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
        } else if (value instanceof File) {
            return File.class;
        } else if (value instanceof Enum) {
            return ((Enum)value).getDeclaringClass();
        } else {
            return value.getClass();
        }
    }

    /** The object whose fields are being written. */
    protected Object _object;

    /** The marshaller for the current object. */
    protected ObjectMarshaller _marshaller;

    /** Used for object comparisons using {@link Arrays#deepEquals}. */
    protected Object[] _a1 = new Object[1], _a2 = new Object[1];
}
