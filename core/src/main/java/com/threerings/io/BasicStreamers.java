//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.io;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.EOFException;
import java.io.IOException;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

/**
 * Code to read and write basic object types (like arrays of primitives, {@link Integer} instances,
 * {@link Double} instances, etc.).
 */
public class BasicStreamers
{
    public static final Map<Class<?>, Streamer> BSTREAMERS =
        ImmutableMap.<Class<?>, Streamer>builder()
        .put(Boolean.class, new BooleanStreamer())
        .put(Byte.class, new ByteStreamer())
        .put(Short.class, new ShortStreamer())
        .put(Character.class, new CharacterStreamer())
        .put(Integer.class, new IntegerStreamer())
        .put(Long.class, new LongStreamer())
        .put(Float.class, new FloatStreamer())
        .put(Double.class, new DoubleStreamer())
        .put(Class.class, new ClassStreamer())
        .put(String.class, Boolean.getBoolean("com.threerings.io.unmodifiedUTFStreaming")
            ? new UnmodifiedUTFStringStreamer()
            : new StringStreamer())
        .put(boolean[].class, new BooleanArrayStreamer())
        .put(byte[].class, new ByteArrayStreamer())
        .put(short[].class, new ShortArrayStreamer())
        .put(char[].class, new CharArrayStreamer())
        .put(int[].class, new IntArrayStreamer())
        .put(long[].class, new LongArrayStreamer())
        .put(float[].class, new FloatArrayStreamer())
        .put(double[].class, new DoubleArrayStreamer())
        .put(Object[].class, new ObjectArrayStreamer())
        .put(List.class, ListStreamer.INSTANCE)
        .put(Collection.class, ListStreamer.INSTANCE)
        .put(Set.class, new SetStreamer())
        .put(Map.class, new MapStreamer())
        .put(Multiset.class, new MultisetStreamer())
        .put(Iterable.class, new IterableStreamer())
        .build();

    /** Abstract base class for basic streamers. */
    public abstract static class BasicStreamer extends Streamer
    {
        @Override
        public void readObject (Object object, ObjectInputStream in, boolean useReader)
            throws IOException, ClassNotFoundException
        {
            // nothing to do here
        }
    }

    /** Streams {@link Boolean} instances. */
    public static class BooleanStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return Boolean.valueOf(in.readBoolean());
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeBoolean(((Boolean)object).booleanValue());
        }
    }

    /** Streams {@link Byte} instances. */
    public static class ByteStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return Byte.valueOf(in.readByte());
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeByte(((Byte)object).byteValue());
        }
    }

    /** Streams {@link Short} instances. */
    public static class ShortStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return Short.valueOf(in.readShort());
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeShort(((Short)object).shortValue());
        }
    }

    /** Streams {@link Character} instances. */
    public static class CharacterStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return Character.valueOf(in.readChar());
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeChar(((Character)object).charValue());
        }
    }

    /** Streams {@link Integer} instances. */
    public static class IntegerStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return Integer.valueOf(in.readInt());
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeInt(((Integer)object).intValue());
        }
    }

    /** Streams {@link Long} instances. */
    public static class LongStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return Long.valueOf(in.readLong());
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeLong(((Long)object).longValue());
        }
    }

    /** Streams {@link Float} instances. */
    public static class FloatStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return Float.valueOf(in.readFloat());
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeFloat(((Float)object).floatValue());
        }
    }

    /** Streams {@link Double} instances. */
    public static class DoubleStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return Double.valueOf(in.readDouble());
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeDouble(((Double)object).doubleValue());
        }
    }

    /** Streams {@link Class} instances (but only those that represent streamable classes). */
    public static class ClassStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            return in.readClassMapping().sclass;
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeClassMapping((Class<?>)object);
        }
    }

    /** Streams {@link String} instances, using modifiedUTF. */
    public static class StringStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return in.readUTF();
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeUTF((String)object);
        }
    }

    /** Streams {@link String} instances, without using modifiedUTF. */
    public static class UnmodifiedUTFStringStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return in.readUnmodifiedUTF();
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeUnmodifiedUTF((String)object);
        }
    }

    /** Streams arrays of booleans. */
    public static class BooleanArrayStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return readBooleanArray(in);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            writeBooleanArray(out, (boolean[])object);
        }
    }

    /** Streams arrays of bytes. */
    public static class ByteArrayStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return readByteArray(in);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            writeByteArray(out, (byte[])object);
        }
    }

    /** Streams arrays of shorts. */
    public static class ShortArrayStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return readShortArray(in);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            writeShortArray(out, (short[])object);
        }
    }

    /** Streams arrays of chars. */
    public static class CharArrayStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return readCharArray(in);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            writeCharArray(out, (char[])object);
        }
    }

    /** Streams arrays of ints. */
    public static class IntArrayStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return readIntArray(in);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            writeIntArray(out, (int[])object);
        }
    }

    /** Streams arrays of longs. */
    public static class LongArrayStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return readLongArray(in);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            writeLongArray(out, (long[])object);
        }
    }

    /** Streams arrays of floats. */
    public static class FloatArrayStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return readFloatArray(in);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            writeFloatArray(out, (float[])object);
        }
    }

    /** Streams arrays of doubles. */
    public static class DoubleArrayStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return readDoubleArray(in);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            writeDoubleArray(out, (double[])object);
        }
    }

    /** Streams arrays of Object instances. */
    public static class ObjectArrayStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            return readObjectArray(in);
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            writeObjectArray(out, (Object[])object);
        }
    }

    /** A building-block class for streaming Collections. */
    protected abstract static class CollectionStreamer extends BasicStreamer
    {
        /** The ordering for Collection/Iterable classes, most to least specific. */
        public static final List<Class<?>> SPECIFICITY_ORDER = ImmutableList.of(
            /** Pretty specific. */
            List.class, Map.class, Set.class, Multiset.class,
            /** General. */
            Collection.class,
            /** Last resort. */
            Iterable.class);

        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            int size = in.readInt();
            Collection<Object> coll = createCollection(size);
            for (int ii = 0; ii < size; ii++) {
                coll.add(in.readObject());
            }
            return coll;
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            Collection<?> coll = (Collection<?>)object;
            out.writeInt(coll.size());
            for (Object o : coll) {
                out.writeObject(o);
            }
        }

        /**
         * Called to create the collection being read.
         *
         * @param size the exact size of the collection.
         */
        protected abstract Collection<Object> createCollection (int size);
    }

    /** Streams {@link Set} instances. */
    public static class SetStreamer extends CollectionStreamer
    {
        @Override
        protected Collection<Object> createCollection (int size)
        {
            return Sets.newHashSetWithExpectedSize(size);
        }
    }

    /** Streams {@link List} instances. */
    public static class ListStreamer extends CollectionStreamer
    {
        /** A singleton instance. */
        public static final ListStreamer INSTANCE = new ListStreamer();

        @Override
        protected Collection<Object> createCollection (int size)
        {
            return Lists.newArrayListWithCapacity(size);
        }
    }

    /** Copy a non-Collection {@link Iterable} into a List. */
    public static class IterableStreamer extends ListStreamer
    {
        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            // re-wrap the Iterable in a List and let superclass ListStreamer do the rest
            super.writeObject(Lists.newArrayList((Iterable<?>)object), out, useWriter);
        }
    }

    /** Streams {@link Map} instances. */
    public static class MapStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            int size = in.readInt();
            Map<Object, Object> map = createMap(size);
            for (int ii = 0; ii < size; ii++) {
                map.put(in.readObject(), in.readObject());
            }
            return map;
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            Map<?, ?> map = (Map<?, ?>)object;
            out.writeInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                out.writeObject(entry.getKey());
                out.writeObject(entry.getValue());
            }
        }

        /**
         * Overrideable if a subclass is desired to stream a different kind of map.
         */
        protected Map<Object, Object> createMap (int size)
        {
            return Maps.newHashMapWithExpectedSize(size);
        }
    }

    /** Streams {@link Multiset} instances. */
    public static class MultisetStreamer extends BasicStreamer
    {
        @Override
        public Object createObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            int size = in.readInt();
            Multiset<Object> set = createMultiset(size);
            for (int ii = 0; ii < size; ii++) {
                set.add(in.readObject(), in.readInt());
            }
            return set;
        }

        @Override
        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            // There seems to be a weird bug with the compiler: if I cast to Multiset<?>
            // then it has a problem assigning the Set<Multiset.Entry<?>>.
            @SuppressWarnings("unchecked")
            Multiset<Object> set = (Multiset<Object>)object;
            Set<Multiset.Entry<Object>> entrySet = set.entrySet();
            out.writeInt(entrySet.size());
            for (Multiset.Entry<Object> entry : entrySet) {
                out.writeObject(entry.getElement());
                out.writeInt(entry.getCount());
            }
        }

        /**
         * Overrideable if a subclass is desired to stream a different kind of multiset.
         */
        protected Multiset<Object> createMultiset (int size)
        {
            return HashMultiset.create(size);
        }
    }

    public static boolean[] readBooleanArray (ObjectInputStream ins)
        throws IOException
    {
        boolean[] value = new boolean[ins.readInt()];
        int ecount = value.length;
        for (int ii = 0; ii < ecount; ii++) {
            value[ii] = ins.readBoolean();
        }
        return value;
    }

    public static byte[] readByteArray (ObjectInputStream ins)
        throws IOException
    {
        byte[] value = new byte[ins.readInt()];
        int remain = value.length, offset = 0, read;
        while (remain > 0) {
            if ((read = ins.read(value, offset, remain)) > 0) {
                remain -= read;
                offset += read;
            } else {
                throw new EOFException();
            }
        }
        return value;
    }

    public static short[] readShortArray (ObjectInputStream ins)
        throws IOException
    {
        short[] value = new short[ins.readInt()];
        int ecount = value.length;
        for (int ii = 0; ii < ecount; ii++) {
            value[ii] = ins.readShort();
        }
        return value;
    }

    public static char[] readCharArray (ObjectInputStream ins)
        throws IOException
    {
        char[] value = new char[ins.readInt()];
        int ecount = value.length;
        for (int ii = 0; ii < ecount; ii++) {
            value[ii] = ins.readChar();
        }
        return value;
    }

    public static int[] readIntArray (ObjectInputStream ins)
        throws IOException
    {
        int[] value = new int[ins.readInt()];
        int ecount = value.length;
        for (int ii = 0; ii < ecount; ii++) {
            value[ii] = ins.readInt();
        }
        return value;
    }

    public static long[] readLongArray (ObjectInputStream ins)
        throws IOException
    {
        long[] value = new long[ins.readInt()];
        int ecount = value.length;
        for (int ii = 0; ii < ecount; ii++) {
            value[ii] = ins.readLong();
        }
        return value;
    }

    public static float[] readFloatArray (ObjectInputStream ins)
        throws IOException
    {
        float[] value = new float[ins.readInt()];
        int ecount = value.length;
        for (int ii = 0; ii < ecount; ii++) {
            value[ii] = ins.readFloat();
        }
        return value;
    }

    public static double[] readDoubleArray (ObjectInputStream ins)
        throws IOException
    {
        double[] value = new double[ins.readInt()];
        int ecount = value.length;
        for (int ii = 0; ii < ecount; ii++) {
            value[ii] = ins.readDouble();
        }
        return value;
    }

    public static Object[] readObjectArray (ObjectInputStream ins)
        throws IOException, ClassNotFoundException
    {
        Object[] value = new Object[ins.readInt()];
        int ecount = value.length;
        for (int ii = 0; ii < ecount; ii++) {
            value[ii] = ins.readObject();
        }
        return value;
    }

    public static void writeBooleanArray (ObjectOutputStream out, boolean[] value)
        throws IOException
    {
        int ecount = value.length;
        out.writeInt(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            out.writeBoolean(value[ii]);
        }
    }

    public static void writeByteArray (ObjectOutputStream out, byte[] value)
        throws IOException
    {
        int ecount = value.length;
        out.writeInt(ecount);
        out.write(value);
    }

    public static void writeCharArray (ObjectOutputStream out, char[] value)
        throws IOException
    {
        int ecount = value.length;
        out.writeInt(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            out.writeChar(value[ii]);
        }
    }

    public static void writeShortArray (ObjectOutputStream out, short[] value)
        throws IOException
    {
        int ecount = value.length;
        out.writeInt(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            out.writeShort(value[ii]);
        }
    }

    public static void writeIntArray (ObjectOutputStream out, int[] value)
        throws IOException
    {
        int ecount = value.length;
        out.writeInt(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            out.writeInt(value[ii]);
        }
    }

    public static void writeLongArray (ObjectOutputStream out, long[] value)
        throws IOException
    {
        int ecount = value.length;
        out.writeInt(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            out.writeLong(value[ii]);
        }
    }

    public static void writeFloatArray (ObjectOutputStream out, float[] value)
        throws IOException
    {
        int ecount = value.length;
        out.writeInt(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            out.writeFloat(value[ii]);
        }
    }

    public static void writeDoubleArray (ObjectOutputStream out, double[] value)
        throws IOException
    {
        int ecount = value.length;
        out.writeInt(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            out.writeDouble(value[ii]);
        }
    }

    public static void writeObjectArray (ObjectOutputStream out, Object[] value)
        throws IOException
    {
        int ecount = value.length;
        out.writeInt(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            out.writeObject(value[ii]);
        }
    }
}
