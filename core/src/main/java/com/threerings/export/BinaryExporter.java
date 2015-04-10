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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;

import java.util.zip.DeflaterOutputStream;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import com.threerings.util.ReflectionUtil;

import static com.threerings.export.Log.log;

/**
 * Exports to a compact binary format.
 */
public class BinaryExporter extends Exporter
{
    /** Identifies the file type. */
    public static final int MAGIC_NUMBER = 0xFACEAF0E;

    /** The format version. */
    public static final short VERSION = 0x1002;

    /** The compressed format flag. */
    public static final short COMPRESSED_FORMAT_FLAG = 0x1000;

    /** Indicates that a stored class is final. */
    public static final byte FINAL_CLASS_FLAG = (byte)(1 << 0);

    /** Indicates that a stored class is a non-static inner class. */
    public static final byte INNER_CLASS_FLAG = (byte)(1 << 1);

    /** Indicates that a stored class is a (non-{@link Exportable}) collection. */
    public static final byte COLLECTION_CLASS_FLAG = (byte)(1 << 2);

    /** Indicates that the stored class is a (non-{@link Exportable}) map. */
    public static final byte MAP_CLASS_FLAG = (byte)(1 << 3);

    /** Indicates that a stored class is a (non-{@link Exportable})
     * Multiset (in combination with the collection flag) or
     * Multimap (in combination with the map flag). */
    public static final byte MULTI_FLAG = (byte)(1 << 4);

    /** We seed the class map with these class references.
     * NOTE: Do not remove any entries or change their order. */
    public static final Class<?>[] BOOTSTRAP_CLASSES = {
        Boolean.TYPE, Byte.TYPE, Character.TYPE, Double.TYPE,
        Float.TYPE, Integer.TYPE, Long.TYPE, Short.TYPE };

    /**
     * Creates an exporter to write to the specified stream with compression.
     */
    public BinaryExporter (OutputStream out)
    {
        this(out, true);
    }

    /**
     * Creates an exporter to write to the specified stream.
     *
     * @param compress if true, compress the output.
     */
    public BinaryExporter (OutputStream out, boolean compress)
    {
        _out = new DataOutputStream(_base = out);
        _compress = compress;

        // populate the class map with the bootstrap classes
        for (Class<?> clazz : BOOTSTRAP_CLASSES) {
            _classIds.put(clazz, ++_lastClassId);
        }
    }

    @Override
    public BinaryExporter setReplacer (Replacer replacer)
    {
        super.setReplacer(replacer);
        return this;
    }

    @Override
    public void writeObject (Object object)
        throws IOException
    {
        if (_objectIds == null) {
            // write the preamble
            _out.writeInt(MAGIC_NUMBER);
            _out.writeShort(VERSION);
            _out.writeShort(_compress ? COMPRESSED_FORMAT_FLAG : 0x0);

            // everything thereafter will be compressed if so requested
            if (_compress) {
                _out = new DataOutputStream(_defout = new DeflaterOutputStream(_base));
            }

            // initialize mapping
            _objectIds = new IdentityHashMap<Object, Integer>();
            _objectIds.put(null, 0);
        }
        write(object, Object.class);
    }

    @Override
    public void write (String name, boolean value)
        throws IOException
    {
        _fields.put(name, new FieldValue(value, Boolean.TYPE));
    }

    @Override
    public void write (String name, byte value)
        throws IOException
    {
        _fields.put(name, new FieldValue(value, Byte.TYPE));
    }

    @Override
    public void write (String name, char value)
        throws IOException
    {
        _fields.put(name, new FieldValue(value, Character.TYPE));
    }

    @Override
    public void write (String name, double value)
        throws IOException
    {
        _fields.put(name, new FieldValue(value, Double.TYPE));
    }

    @Override
    public void write (String name, float value)
        throws IOException
    {
        _fields.put(name, new FieldValue(value, Float.TYPE));
    }

    @Override
    public void write (String name, int value)
        throws IOException
    {
        _fields.put(name, new FieldValue(value, Integer.TYPE));
    }

    @Override
    public void write (String name, long value)
        throws IOException
    {
        _fields.put(name, new FieldValue(value, Long.TYPE));
    }

    @Override
    public void write (String name, short value)
        throws IOException
    {
        _fields.put(name, new FieldValue(value, Short.TYPE));
    }

    @Override
    public <T> void write (String name, T value, Class<T> clazz)
        throws IOException
    {
        Object tVal = value;
        Class<?> tClazz = clazz;
        if (_replacer != null) {
            Replacement repl = _replacer.getReplacement(value, clazz);
            if (repl != null) {
                tVal = repl.value;
                tClazz = repl.clazz;
            }
        }
        _fields.put(name, new FieldValue(tVal, tClazz));
    }

    @Override
    public void close ()
        throws IOException
    {
        // close the underlying stream (automatically finishes the deflation)
        _out.close();
    }

    @Override
    public void finish ()
        throws IOException
    {
        // finish up the deflation, provided we ever started
        if (_defout != null) {
            _defout.finish();
        }
    }

    /**
     * Writes out an object of the specified class.
     */
    protected void write (Object value, Class<?> clazz)
        throws IOException
    {
        // possibly sub the value on the way out
        if (_replacer != null) {
            Replacement repl = _replacer.getReplacement(value, clazz);
            if (repl != null) {
                value = repl.value;
                clazz = repl.clazz;
            }
        }

        writeNoReplace(value, clazz);
    }

    /**
     * Writes out an object of the specified class, after the replacement has been done.
     */
    protected void writeNoReplace (Object value, Class<?> clazz)
        throws IOException
    {
        // write primitive types out directly
        if (clazz.isPrimitive()) {
            writeValue(value, clazz);
            return;
        }
        // intern all strings before looking them up. Strings are immutable. We can share them.
        if (value instanceof String) {
            value = ((String)value).intern();
        }
        // see if we've written it before
        Integer objectId = _objectIds.get(value);
        if (objectId != null) {
            Streams.writeVarInt(_out, objectId);
            return;
        }
        // if not, assign and write a new id
        Streams.writeVarInt(_out, ++_lastObjectId);
        _objectIds.put(value, _lastObjectId);

        // and write the value
        writeValue(value, clazz);
    }

    /**
     * Writes the value of an object of the specified class.
     */
    protected void writeValue (Object value, Class<?> clazz)
        throws IOException
    {
        // write the class unless we can determine that implicitly
        Class<?> cclazz = getClass(value);
        if (!Modifier.isFinal(clazz.getModifiers())) {
            writeClass(cclazz);
        }
        // see if we can stream the value directly
        @SuppressWarnings("unchecked") Streamer<Object> streamer =
            (Streamer<Object>)Streamer.getStreamer(cclazz);
        if (streamer != null) {
            streamer.write(value, _out);
            return;
        }
        // write the array dimension, if applicable
        if (cclazz.isArray()) {
            Streams.writeVarInt(_out, Array.getLength(value));
        }
        // and the outer class reference
        if (!(value instanceof Collection) && !(value instanceof Map)) {
            Object outer = ReflectionUtil.getOuter(value);
            if (outer != null) {
                write(outer, Object.class);
            }
        }
        if (value instanceof Exportable) {
            writeFields((Exportable)value);
        } else if (value instanceof Object[]) {
            @SuppressWarnings("unchecked") Class<Object> ctype =
                (Class<Object>)cclazz.getComponentType();
            writeEntries((Object[])value, ctype);
        } else if (value instanceof Collection) {
            if (value instanceof EnumSet) {
                writeEntries((EnumSet<?>)value);
            } else if (value instanceof Multiset) {
                writeEntries((Multiset)value);
            } else {
                writeEntries((Collection)value);
            }
        } else if (value instanceof Map) {
            writeEntries((Map)value);
        } else if (value instanceof Multimap) {
            throw new IOException("TODO: Multimap support");
        } else {
            throw new IOException("Value is not exportable [class=" + cclazz + "].");
        }
    }

    /**
     * Writes out a class reference.  While it's possibly simply to write the class reference out
     * as a normal object, we keep a separate id space for object/field classes in order to keep
     * the ids small.
     */
    protected void writeClass (Class<?> clazz)
        throws IOException
    {
        // see if we've written it before
        Integer classId = _classIds.get(clazz);
        if (classId != null) {
            Streams.writeVarInt(_out, classId);
            return;
        }
        // if not, assign and write a new id
        Streams.writeVarInt(_out, ++_lastClassId);
        _classIds.put(clazz, _lastClassId);

        // write the name
        _out.writeUTF(clazz.getName());

        // write the flags (for arrays, the flags of the inmost component type)
        _out.writeByte(getFlags(getInmostComponentType(clazz)));
    }

    @Override
    protected void writeFields (Exportable object)
        throws IOException
    {
        // populate the field map
        Map<String, FieldValue> fields = new HashMap<String, FieldValue>();
        _fields = fields;
        super.writeFields(object);
        _fields = null;

        // write out the values
        Class<?> clazz = object.getClass();
        ClassData cdata = _classData.get(clazz);
        if (cdata == null) {
            _classData.put(clazz, cdata = new ClassData());
        }
        cdata.writeFields(fields);
    }

    /**
     * Writes out the entries of an array.
     */
    protected <T> void writeEntries (T[] array, Class<T> ctype)
        throws IOException
    {
        for (T entry : array) {
            write(entry, ctype);
        }
    }

    /**
     * Writes out the enum class of an EnumSet before writing the entries.  This will fail if the
     * enum class has no defined enums.
     */
    protected void writeEntries (EnumSet<?> set)
        throws IOException
    {
        EnumSet<?> typer = set.isEmpty() ? EnumSet.complementOf(set) : set;
        Class<?> ctype = typer.iterator().next().getDeclaringClass();
        writeClass(ctype);
        writeEntries((Collection<?>)set);
    }

    /**
     * Writes out the entries of a collection.
     */
    protected void writeEntries (Collection<?> collection)
        throws IOException
    {
        Streams.writeVarInt(_out, collection.size());
        for (Object entry : collection) {
            write(entry, Object.class);
        }
    }

    /**
     * Writes out the entries of a multiset.
     */
    protected void writeEntries (Multiset<?> multiset)
        throws IOException
    {
        @SuppressWarnings("unchecked")
        Multiset<Object> mset = (Multiset<Object>)multiset;
        Set<Multiset.Entry<Object>> entrySet = mset.entrySet();
        Streams.writeVarInt(_out, entrySet.size());
        for (Multiset.Entry<Object> entry : entrySet) {
            write(entry.getElement(), Object.class);
            Streams.writeVarInt(_out, entry.getCount());
        }
    }

    /**
     * Writes out the entries of a map.
     */
    protected void writeEntries (Map<?, ?> map)
        throws IOException
    {
        Streams.writeVarInt(_out, map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            write(entry.getKey(), Object.class);
            write(entry.getValue(), Object.class);
        }
    }

    /**
     * Returns the inmost component type of the specified class.
     */
    protected static Class<?> getInmostComponentType (Class<?> clazz)
    {
        while (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        return clazz;
    }

    /**
     * Returns the class flags for the specified class.
     */
    protected static byte getFlags (Class<?> clazz)
    {
        byte flags = 0;
        int mods = clazz.getModifiers();
        if (Modifier.isFinal(mods)) {
            flags |= FINAL_CLASS_FLAG;
        }
        if (ReflectionUtil.isInner(clazz)) {
            flags |= INNER_CLASS_FLAG;
        }
        if (!Exportable.class.isAssignableFrom(clazz)) {
            if (Collection.class.isAssignableFrom(clazz)) {
                flags |= COLLECTION_CLASS_FLAG;
                if (Multiset.class.isAssignableFrom(clazz)) {
                    flags |= MULTI_FLAG;
                }
            } else if (Map.class.isAssignableFrom(clazz)) {
                flags |= MAP_CLASS_FLAG;
            }
        }
        return flags;
    }

    /**
     * Contains information on an exportable class.
     */
    protected class ClassData
    {
        /**
         * Writes out the field values in the supplied map.
         */
        public void writeFields (Map<String, FieldValue> fields)
            throws IOException
        {
            Streams.writeVarInt(_out, fields.size());
            for (Map.Entry<String, FieldValue> entry : fields.entrySet()) {
                FieldValue value = entry.getValue();
                writeField(entry.getKey(), value.value, value.clazz);
            }
        }

        /**
         * Writes out a single field value.
         */
        protected void writeField (String name, Object value, Class<?> clazz)
            throws IOException
        {
            FieldData field = new FieldData(name, clazz);
            Integer fieldId = _fieldIds.get(field);
            if (fieldId == null) {
                int newFieldId = _nextFieldId++;
                Streams.writeVarInt(_out, newFieldId);
                _fieldIds.put(field, newFieldId);
                writeNoReplace(name, String.class);
                writeClass(clazz);
            } else {
                Streams.writeVarInt(_out, fieldId.intValue());
            }
            writeNoReplace(value, clazz);
        }

        /** Maps field name/class pairs to field ids. */
        protected Map<FieldData, Integer> _fieldIds = Maps.newHashMap();

        /** The next field id to be used. */
        protected int _nextFieldId;
    }

    /**
     * Contains the value and class for a sub-field of an exportable object.
     */
    protected static class FieldValue
    {
        /** The value. */
        public final Object value;

        /** The upper bound class type. */
        public final Class<?> clazz;

        /**
         * Construct a FieldValue.
         */
        public FieldValue (Object value, Class<?> clazz)
        {
            this.value = value;
            this.clazz = clazz;
        }
    }

    /**
     * Encapsulates the type of a field.
     */
    protected static class FieldData
    {
        /** The name. */
        public final String name;

        /** The type. */
        public final Class<?> clazz;

        /**
         * Constructor.
         */
        public FieldData (String name, Class<?> clazz)
        {
            this.name = name;
            this.clazz = clazz;
        }

        @Override
        public boolean equals (Object o)
        {
            if (!(o instanceof FieldData)) {
                return false;
            }
            FieldData that = (FieldData)o;
            return this.name.equals(that.name) && this.clazz.equals(that.clazz);
        }

        @Override
        public int hashCode ()
        {
            return name.hashCode() ^ clazz.hashCode();
        }
    }

    /** The underlying output stream. */
    protected OutputStream _base;

    /** The stream that we use for writing data. */
    protected DataOutputStream _out;

    /** Whether or not to compress the output. */
    protected boolean _compress;

    /** The deflater stream between the data output and the underlying output. */
    protected DeflaterOutputStream _defout;

    /** Maps objects written to their integer ids.  A null value indicates that the stream has not
     * yet been initialized. */
    protected IdentityHashMap<Object, Integer> _objectIds;

    /** The last object id assigned. */
    protected int _lastObjectId;

    /** Maps classes written to their integer ids. */
    protected Map<Class<?>, Integer> _classIds = new HashMap<Class<?>, Integer>();

    /** The last class id assigned. */
    protected int _lastClassId;

    /** Field values associated with the current object. */
    protected Map<String, FieldValue> _fields;

    /** Class<?> data. */
    protected Map<Class<?>, ClassData> _classData = new HashMap<Class<?>, ClassData>();
}
