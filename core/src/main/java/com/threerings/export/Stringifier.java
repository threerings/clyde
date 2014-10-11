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

import java.io.File;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import java.util.HashMap;
import java.util.StringTokenizer;

import org.lwjgl.BufferUtils;

import com.samskivert.util.StringUtil;

/**
 * Converts objects to and from strings.
 */
public abstract class Stringifier<T>
{
    /**
     * Returns the stringifier, if any, for the specified class.
     */
    public static Stringifier<?> getStringifier (final Class<?> clazz)
    {
        // look for a specific one
        Stringifier<?> stringifier = _stringifiers.get(clazz);
        if (stringifier == null) {
            // create custom stringifiers for enums and encodable types
            if (clazz.isEnum()) {
                _stringifiers.put(clazz, stringifier = new Stringifier<Enum<?>>() {
                    public String toString (Enum<?> value) {
                        return value.name();
                    }
                    public Enum<?> fromString (String string) {
                        @SuppressWarnings("unchecked")
                        Class<Exporter.DummyEnum> eclazz = (Class<Exporter.DummyEnum>)clazz;
                        return Enum.valueOf(eclazz, string);
                    }
                });
            } else if (Encodable.class.isAssignableFrom(clazz)) {
                _stringifiers.put(clazz, stringifier = new Stringifier<Encodable>() {
                    public String toString (Encodable value) {
                        return value.encodeToString();
                    }
                    public Encodable fromString (String string) throws Exception {
                        Encodable value = (Encodable)clazz.newInstance();
                        value.decodeFromString(string);
                        return value;
                    }
                });
            }
        }
        return stringifier;
    }

    /**
     * Converts an object to a string.
     */
    public abstract String toString (T value);

    /**
     * Converts a string to an object.  Some failures to convert will simply return
     * <code>null</code> rather than throwing an exception.
     */
    public abstract T fromString (String string)
        throws Exception;

    /** Registered stringifiers. */
    protected static HashMap<Class<?>, Stringifier<?>> _stringifiers =
        new HashMap<Class<?>, Stringifier<?>>();
    static {
        // register basic stringifiers for wrapper types, primitive arrays
        _stringifiers.put(Boolean.class, new Stringifier<Boolean>() {
            public String toString (Boolean value) {
                return value.toString();
            }
            public Boolean fromString (String string) {
                return Boolean.valueOf(string);
            }
        });
        _stringifiers.put(Byte.class, new Stringifier<Byte>() {
            public String toString (Byte value) {
                return value.toString();
            }
            public Byte fromString (String string) {
                return Byte.valueOf(string);
            }
        });
        _stringifiers.put(Character.class, new Stringifier<Character>() {
            public String toString (Character value) {
                return value.toString();
            }
            public Character fromString (String string) {
                return Character.valueOf(string.charAt(0));
            }
        });
        _stringifiers.put(Class.class, new Stringifier<Class<?>>() {
            public String toString (Class<?> value) {
                return value.getName();
            }
            public Class<?> fromString (String string) throws Exception {
                return Class.forName(string);
            }
        });
        _stringifiers.put(Double.class, new Stringifier<Double>() {
            public String toString (Double value) {
                return value.toString();
            }
            public Double fromString (String string) {
                return Double.valueOf(string);
            }
        });
        _stringifiers.put(Float.class, new Stringifier<Float>() {
            public String toString (Float value) {
                return value.toString();
            }
            public Float fromString (String string) {
                return Float.valueOf(string);
            }
        });
        _stringifiers.put(Integer.class, new Stringifier<Integer>() {
            public String toString (Integer value) {
                return value.toString();
            }
            public Integer fromString (String string) {
                return Integer.valueOf(string);
            }
        });
        _stringifiers.put(Long.class, new Stringifier<Long>() {
            public String toString (Long value) {
                return value.toString();
            }
            public Long fromString (String string) {
                return Long.valueOf(string);
            }
        });
        _stringifiers.put(Short.class, new Stringifier<Short>() {
            public String toString (Short value) {
                return value.toString();
            }
            public Short fromString (String string) {
                return Short.valueOf(string);
            }
        });
        _stringifiers.put(String.class, new Stringifier<String>() {
            public String toString (String value) {
                return value;
            }
            public String fromString (String string) {
                return string;
            }
        });
        _stringifiers.put(boolean[].class, new Stringifier<boolean[]>() {
            public String toString (boolean[] value) {
                return StringUtil.toString(value, "", "");
            }
            public boolean[] fromString (String string) {
                return StringUtil.parseBooleanArray(string);
            }
        });
        _stringifiers.put(byte[].class, new Stringifier<byte[]>() {
            public String toString (byte[] value) {
                return StringUtil.toString(value, "", "");
            }
            public byte[] fromString (String string) {
                return StringUtil.parseByteArray(string);
            }
        });
        _stringifiers.put(char[].class, new Stringifier<char[]>() {
            public String toString (char[] value) {
                return String.valueOf(value);
            }
            public char[] fromString (String string) {
                return string.toCharArray();
            }
        });
        _stringifiers.put(double[].class, new Stringifier<double[]>() {
            public String toString (double[] value) {
                return StringUtil.toString(value, "", "");
            }
            public double[] fromString (String string) {
                return StringUtil.parseDoubleArray(string);
            }
        });
        _stringifiers.put(float[].class, new Stringifier<float[]>() {
            public String toString (float[] value) {
                return StringUtil.toString(value, "", "");
            }
            public float[] fromString (String string) {
                return StringUtil.parseFloatArray(string);
            }
        });
        _stringifiers.put(int[].class, new Stringifier<int[]>() {
            public String toString (int[] value) {
                return StringUtil.toString(value, "", "");
            }
            public int[] fromString (String string) {
                return StringUtil.parseIntArray(string);
            }
        });
        _stringifiers.put(long[].class, new Stringifier<long[]>() {
            public String toString (long[] value) {
                return StringUtil.toString(value, "", "");
            }
            public long[] fromString (String string) {
                return StringUtil.parseLongArray(string);
            }
        });
        _stringifiers.put(short[].class, new Stringifier<short[]>() {
            public String toString (short[] value) {
                return StringUtil.toString(value, "", "");
            }
            public short[] fromString (String string) {
                return StringUtil.parseShortArray(string);
            }
        });
        _stringifiers.put(String[].class, new Stringifier<String[]>() {
            public String toString (String[] value) {
                return StringUtil.joinEscaped(value);
            }
            public String[] fromString (String string) {
                return StringUtil.parseStringArray(string);
            }
        });

        // io types
        _stringifiers.put(File.class, new Stringifier<File>() {
            public String toString (File value) {
                return value.toString();
            }
            public File fromString (String string) {
                return new File(string);
            }
        });

        // buffer types
        _stringifiers.put(ByteBuffer.class, new Stringifier<ByteBuffer>() {
            public String toString (ByteBuffer value) {
                StringBuilder sb = new StringBuilder();
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    if (ii > 0) {
                        sb.append(", ");
                    }
                    sb.append(value.get(ii));
                }
                return sb.toString();
            }
            public ByteBuffer fromString (String string) {
                StringTokenizer tok = new StringTokenizer(string, ",");
                ByteBuffer buf = BufferUtils.createByteBuffer(tok.countTokens());
                for (int ii = 0; tok.hasMoreTokens(); ii++) {
                    buf.put(ii, Byte.parseByte(tok.nextToken().trim()));
                }
                return buf;
            }
        });
        _stringifiers.put(CharBuffer.class, new Stringifier<CharBuffer>() {
            public String toString (CharBuffer value) {
                return value.toString();
            }
            public CharBuffer fromString (String string) {
                CharBuffer buf = BufferUtils.createCharBuffer(string.length());
                buf.put(string).rewind();
                return buf;
            }
        });
        _stringifiers.put(DoubleBuffer.class, new Stringifier<DoubleBuffer>() {
            public String toString (DoubleBuffer value) {
                StringBuilder sb = new StringBuilder();
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    if (ii > 0) {
                        sb.append(", ");
                    }
                    sb.append(value.get(ii));
                }
                return sb.toString();
            }
            public DoubleBuffer fromString (String string) {
                StringTokenizer tok = new StringTokenizer(string, ",");
                DoubleBuffer buf = BufferUtils.createDoubleBuffer(tok.countTokens());
                for (int ii = 0; tok.hasMoreTokens(); ii++) {
                    buf.put(ii, Double.parseDouble(tok.nextToken().trim()));
                }
                return buf;
            }
        });
        _stringifiers.put(FloatBuffer.class, new Stringifier<FloatBuffer>() {
            public String toString (FloatBuffer value) {
                StringBuilder sb = new StringBuilder();
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    if (ii > 0) {
                        sb.append(", ");
                    }
                    sb.append(value.get(ii));
                }
                return sb.toString();
            }
            public FloatBuffer fromString (String string) {
                StringTokenizer tok = new StringTokenizer(string, ",");
                FloatBuffer buf = BufferUtils.createFloatBuffer(tok.countTokens());
                for (int ii = 0; tok.hasMoreTokens(); ii++) {
                    buf.put(ii, Float.parseFloat(tok.nextToken().trim()));
                }
                return buf;
            }
        });
        _stringifiers.put(IntBuffer.class, new Stringifier<IntBuffer>() {
            public String toString (IntBuffer value) {
                StringBuilder sb = new StringBuilder();
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    if (ii > 0) {
                        sb.append(", ");
                    }
                    sb.append(value.get(ii));
                }
                return sb.toString();
            }
            public IntBuffer fromString (String string) {
                StringTokenizer tok = new StringTokenizer(string, ",");
                IntBuffer buf = BufferUtils.createIntBuffer(tok.countTokens());
                for (int ii = 0; tok.hasMoreTokens(); ii++) {
                    buf.put(ii, Integer.parseInt(tok.nextToken().trim()));
                }
                return buf;
            }
        });
        _stringifiers.put(LongBuffer.class, new Stringifier<LongBuffer>() {
            public String toString (LongBuffer value) {
                StringBuilder sb = new StringBuilder();
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    if (ii > 0) {
                        sb.append(", ");
                    }
                    sb.append(value.get(ii));
                }
                return sb.toString();
            }
            public LongBuffer fromString (String string) {
                StringTokenizer tok = new StringTokenizer(string, ",");
                LongBuffer buf = BufferUtils.createLongBuffer(tok.countTokens());
                for (int ii = 0; tok.hasMoreTokens(); ii++) {
                    buf.put(ii, Long.parseLong(tok.nextToken().trim()));
                }
                return buf;
            }
        });
        _stringifiers.put(ShortBuffer.class, new Stringifier<ShortBuffer>() {
            public String toString (ShortBuffer value) {
                StringBuilder sb = new StringBuilder();
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    if (ii > 0) {
                        sb.append(", ");
                    }
                    sb.append(value.get(ii));
                }
                return sb.toString();
            }
            public ShortBuffer fromString (String string) {
                StringTokenizer tok = new StringTokenizer(string, ",");
                ShortBuffer buf = BufferUtils.createShortBuffer(tok.countTokens());
                for (int ii = 0; tok.hasMoreTokens(); ii++) {
                    buf.put(ii, Short.parseShort(tok.nextToken().trim()));
                }
                return buf;
            }
        });
    }
}
