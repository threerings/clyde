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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import java.util.HashMap;

import com.google.common.base.Charsets;

import org.lwjgl.BufferUtils;

import static com.threerings.export.Log.log;

/**
 * Writes objects to and reads objects from a binary stream.
 */
public abstract class Streamer<T>
{
    /**
     * Write the specified String to the stream as <em>regular</em>, <em>unmodified</em> UTF-8.
     * Previously, exporting would write modified UTF-8, because that's what's built-in to Java.
     * But that's a pain in the ass when communicating with other languages.
     *
     * Code expecting to read "regular" will not properly decode "modified", but the other
     * way around works. So, "regular" is the universal writer and "modified" is the universal
     * reader. From now on, that's what exporting will do here on the Java side, but I'll
     * soon drop support for reading "modified" in other languages.
     */
    public static void writeUTF (DataOutputStream out, String s)
        throws IOException
    {
        byte[] bytes = s.getBytes(Charsets.UTF_8);
        if (bytes.length > Short.MAX_VALUE) {
            throw new IOException(
                    "String too long to use with writeUTF [byteLength=" + bytes.length + "]");
        }
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    /**
     * Returns the streamer, if any, for the specified class.
     */
    public static Streamer<?> getStreamer (final Class<?> clazz)
    {
        // look for a specific one
        Streamer<?> streamer = _streamers.get(clazz);
        if (streamer == null) {
            // create custom streamers for enums and encodable types
            if (clazz.isEnum()) {
                _streamers.put(clazz, streamer = new Streamer<Enum<?>>() {
                    public void write (Enum<?> value, DataOutputStream out)
                        throws IOException {
                        writeUTF(out, value.name());
                    }
                    public Enum<?> read (DataInputStream in) throws IOException {
                        @SuppressWarnings("unchecked")
                        Class<Exporter.DummyEnum> eclass = (Class<Exporter.DummyEnum>)clazz;
                        return Enum.valueOf(eclass, in.readUTF());
                    }
                });
            } else if (Encodable.class.isAssignableFrom(clazz)) {
                _streamers.put(clazz, streamer = new Streamer<Encodable>() {
                    public void write (Encodable value, DataOutputStream out) throws IOException {
                        value.encodeToStream(out);
                    }
                    public Encodable read (DataInputStream in) throws IOException {
                        Encodable value;
                        try {
                            value = (Encodable)clazz.newInstance();
                        } catch (Exception e) {
                            log.warning("Failed to create instance.", e);
                            return null;
                        }
                        value.decodeFromStream(in);
                        return value;
                    }
                });
            }
        }
        return streamer;
    }

    /**
     * Writes an object to the stream.
     */
    public abstract void write (T value, DataOutputStream out)
        throws IOException;

    /**
     * Reads an object from the stream.
     */
    public abstract T read (DataInputStream in)
        throws IOException, ClassNotFoundException;

    /** Registered streamers. */
    protected static HashMap<Class<?>, Streamer<?>> _streamers =
            new HashMap<Class<?>, Streamer<?>>();
    static {
        // register basic streamers for wrapper types, primitive arrays
        Streamer<?> streamer = new Streamer<Boolean>() {
            public void write (Boolean value, DataOutputStream out) throws IOException {
                out.writeBoolean(value.booleanValue());
            }
            public Boolean read (DataInputStream in) throws IOException {
                return Boolean.valueOf(in.readBoolean());
            }
        };
        _streamers.put(Boolean.class, streamer);
        _streamers.put(Boolean.TYPE, streamer);

        streamer = new Streamer<Byte>() {
            public void write (Byte value, DataOutputStream out) throws IOException {
                out.writeByte(value.byteValue());
            }
            public Byte read (DataInputStream in) throws IOException {
                return Byte.valueOf(in.readByte());
            }
        };
        _streamers.put(Byte.class, streamer);
        _streamers.put(Byte.TYPE, streamer);

        streamer = new Streamer<Character>() {
            public void write (Character value, DataOutputStream out) throws IOException {
                out.writeChar(value.charValue());
            }
            public Character read (DataInputStream in) throws IOException {
                return Character.valueOf(in.readChar());
            }
        };
        _streamers.put(Character.class, streamer);
        _streamers.put(Character.TYPE, streamer);

        _streamers.put(Class.class, new Streamer<Class<?>>() {
            public void write (Class<?> value, DataOutputStream out) throws IOException {
                writeUTF(out, value.getName());
            }
            public Class<?> read (DataInputStream in) throws IOException, ClassNotFoundException {
                return Class.forName(in.readUTF());
            }
        });

        streamer = new Streamer<Double>() {
            public void write (Double value, DataOutputStream out) throws IOException {
                out.writeDouble(value.doubleValue());
            }
            public Double read (DataInputStream in) throws IOException {
                return Double.valueOf(in.readDouble());
            }
        };
        _streamers.put(Double.class, streamer);
        _streamers.put(Double.TYPE, streamer);

        streamer = new Streamer<Float>() {
            public void write (Float value, DataOutputStream out) throws IOException {
                out.writeFloat(value.floatValue());
            }
            public Float read (DataInputStream in) throws IOException {
                return Float.valueOf(in.readFloat());
            }
        };
        _streamers.put(Float.class, streamer);
        _streamers.put(Float.TYPE, streamer);

        streamer = new Streamer<Integer>() {
            public void write (Integer value, DataOutputStream out) throws IOException {
                out.writeInt(value.intValue());
            }
            public Integer read (DataInputStream in) throws IOException {
                return Integer.valueOf(in.readInt());
            }
        };
        _streamers.put(Integer.class, streamer);
        _streamers.put(Integer.TYPE, streamer);

        streamer = new Streamer<Long>() {
            public void write (Long value, DataOutputStream out) throws IOException {
                out.writeLong(value.longValue());
            }
            public Long read (DataInputStream in) throws IOException {
                return Long.valueOf(in.readLong());
            }
        };
        _streamers.put(Long.class, streamer);
        _streamers.put(Long.TYPE, streamer);

        streamer = new Streamer<Short>() {
            public void write (Short value, DataOutputStream out) throws IOException {
                out.writeShort(value.shortValue());
            }
            public Short read (DataInputStream in) throws IOException {
                return Short.valueOf(in.readShort());
            }
        };
        _streamers.put(Short.class, streamer);
        _streamers.put(Short.TYPE, streamer);

        _streamers.put(String.class, new Streamer<String>() {
            public void write (String value, DataOutputStream out) throws IOException {
                writeUTF(out, value);
            }
            public String read (DataInputStream in) throws IOException {
                return in.readUTF();
            }
        });
        _streamers.put(boolean[].class, new Streamer<boolean[]>() {
            public void write (boolean[] value, DataOutputStream out) throws IOException {
                out.writeInt(value.length);
                for (boolean val : value) {
                    out.writeBoolean(val);
                }
            }
            public boolean[] read (DataInputStream in) throws IOException {
                boolean[] value = new boolean[in.readInt()];
                for (int ii = 0; ii < value.length; ii++) {
                    value[ii] = in.readBoolean();
                }
                return value;
            }
        });
        _streamers.put(byte[].class, new Streamer<byte[]>() {
            public void write (byte[] value, DataOutputStream out) throws IOException {
                out.writeInt(value.length);
                for (byte val : value) {
                    out.writeByte(val);
                }
            }
            public byte[] read (DataInputStream in) throws IOException {
                byte[] value = new byte[in.readInt()];
                for (int ii = 0; ii < value.length; ii++) {
                    value[ii] = in.readByte();
                }
                return value;
            }
        });
        _streamers.put(char[].class, new Streamer<char[]>() {
            public void write (char[] value, DataOutputStream out) throws IOException {
                out.writeInt(value.length);
                for (char val : value) {
                    out.writeChar(val);
                }
            }
            public char[] read (DataInputStream in) throws IOException {
                char[] value = new char[in.readInt()];
                for (int ii = 0; ii < value.length; ii++) {
                    value[ii] = in.readChar();
                }
                return value;
            }
        });
        _streamers.put(double[].class, new Streamer<double[]>() {
            public void write (double[] value, DataOutputStream out) throws IOException {
                out.writeInt(value.length);
                for (double val : value) {
                    out.writeDouble(val);
                }
            }
            public double[] read (DataInputStream in) throws IOException {
                double[] value = new double[in.readInt()];
                for (int ii = 0; ii < value.length; ii++) {
                    value[ii] = in.readDouble();
                }
                return value;
            }
        });
        _streamers.put(float[].class, new Streamer<float[]>() {
            public void write (float[] value, DataOutputStream out) throws IOException {
                out.writeInt(value.length);
                for (float val : value) {
                    out.writeFloat(val);
                }
            }
            public float[] read (DataInputStream in) throws IOException {
                float[] value = new float[in.readInt()];
                for (int ii = 0; ii < value.length; ii++) {
                    value[ii] = in.readFloat();
                }
                return value;
            }
        });
        _streamers.put(int[].class, new Streamer<int[]>() {
            public void write (int[] value, DataOutputStream out) throws IOException {
                out.writeInt(value.length);
                for (int val : value) {
                    out.writeInt(val);
                }
            }
            public int[] read (DataInputStream in) throws IOException {
                int[] value = new int[in.readInt()];
                for (int ii = 0; ii < value.length; ii++) {
                    value[ii] = in.readInt();
                }
                return value;
            }
        });
        _streamers.put(long[].class, new Streamer<long[]>() {
            public void write (long[] value, DataOutputStream out) throws IOException {
                out.writeInt(value.length);
                for (long val : value) {
                    out.writeLong(val);
                }
            }
            public long[] read (DataInputStream in) throws IOException {
                long[] value = new long[in.readInt()];
                for (int ii = 0; ii < value.length; ii++) {
                    value[ii] = in.readLong();
                }
                return value;
            }
        });
        _streamers.put(short[].class, new Streamer<short[]>() {
            public void write (short[] value, DataOutputStream out) throws IOException {
                out.writeInt(value.length);
                for (short val : value) {
                    out.writeShort(val);
                }
            }
            public short[] read (DataInputStream in) throws IOException {
                short[] value = new short[in.readInt()];
                for (int ii = 0; ii < value.length; ii++) {
                    value[ii] = in.readShort();
                }
                return value;
            }
        });

        // io types
        _streamers.put(File.class, new Streamer<File>() {
            public void write (File value, DataOutputStream out) throws IOException {
                writeUTF(out, value.toString());
            }
            public File read (DataInputStream in) throws IOException {
                return new File(in.readUTF());
            }
        });

        // buffer types
        _streamers.put(ByteBuffer.class, new Streamer<ByteBuffer>() {
            public void write (ByteBuffer value, DataOutputStream out) throws IOException {
                out.writeInt(value.limit());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    out.writeByte(value.get(ii));
                }
            }
            public ByteBuffer read (DataInputStream in) throws IOException {
                ByteBuffer value = BufferUtils.createByteBuffer(in.readInt());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    value.put(ii, in.readByte());
                }
                return value;
            }
        });
        _streamers.put(CharBuffer.class, new Streamer<CharBuffer>() {
            public void write (CharBuffer value, DataOutputStream out) throws IOException {
                out.writeInt(value.limit());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    out.writeChar(value.get(ii));
                }
            }
            public CharBuffer read (DataInputStream in) throws IOException {
                CharBuffer value = BufferUtils.createCharBuffer(in.readInt());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    value.put(ii, in.readChar());
                }
                return value;
            }
        });
        _streamers.put(DoubleBuffer.class, new Streamer<DoubleBuffer>() {
            public void write (DoubleBuffer value, DataOutputStream out) throws IOException {
                out.writeInt(value.limit());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    out.writeDouble(value.get(ii));
                }
            }
            public DoubleBuffer read (DataInputStream in) throws IOException {
                DoubleBuffer value = BufferUtils.createDoubleBuffer(in.readInt());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    value.put(ii, in.readDouble());
                }
                return value;
            }
        });
        _streamers.put(FloatBuffer.class, new Streamer<FloatBuffer>() {
            public void write (FloatBuffer value, DataOutputStream out) throws IOException {
                out.writeInt(value.limit());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    out.writeFloat(value.get(ii));
                }
            }
            public FloatBuffer read (DataInputStream in) throws IOException {
                FloatBuffer value = BufferUtils.createFloatBuffer(in.readInt());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    value.put(ii, in.readFloat());
                }
                return value;
            }
        });
        _streamers.put(IntBuffer.class, new Streamer<IntBuffer>() {
            public void write (IntBuffer value, DataOutputStream out) throws IOException {
                out.writeInt(value.limit());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    out.writeInt(value.get(ii));
                }
            }
            public IntBuffer read (DataInputStream in) throws IOException {
                IntBuffer value = BufferUtils.createIntBuffer(in.readInt());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    value.put(ii, in.readInt());
                }
                return value;
            }
        });
        _streamers.put(LongBuffer.class, new Streamer<LongBuffer>() {
            public void write (LongBuffer value, DataOutputStream out) throws IOException {
                out.writeInt(value.limit());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    out.writeLong(value.get(ii));
                }
            }
            public LongBuffer read (DataInputStream in) throws IOException {
                LongBuffer value = BufferUtils.createLongBuffer(in.readInt());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    value.put(ii, in.readLong());
                }
                return value;
            }
        });
        _streamers.put(ShortBuffer.class, new Streamer<ShortBuffer>() {
            public void write (ShortBuffer value, DataOutputStream out) throws IOException {
                out.writeInt(value.limit());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    out.writeShort(value.get(ii));
                }
            }
            public ShortBuffer read (DataInputStream in) throws IOException {
                ShortBuffer value = BufferUtils.createShortBuffer(in.readInt());
                for (int ii = 0, nn = value.limit(); ii < nn; ii++) {
                    value.put(ii, in.readShort());
                }
                return value;
            }
        });
    }
}
