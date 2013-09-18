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

package com.threerings.delta;

import java.io.IOException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.io.ArrayMask;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.expr.MutableInteger;

/**
 * A delta object that uses reflection to compare and modify the objects' fields.  Note that
 * unchanged object fields will be preserved by reference.
 */
public class ReflectiveDelta extends Delta
{
    /**
     * Creates a new reflective delta that transforms the original object into the revised object
     * (both of which must be instances of the same class).
     */
    public ReflectiveDelta (Object original, Object revised)
    {
        // compare the fields
        ClassMapping cmap = getClassMapping(_clazz = original.getClass());
        _mask = new BareArrayMask(cmap.getMaskLength());
        Field[] fields = cmap.getFields();
        FieldHandler[] handlers = cmap.getHandlers();
        List<Object> values = Lists.newArrayList();
        MutableInteger midx = new MutableInteger();
        for (int ii = 0; ii < fields.length; ii++) {
            try {
                handlers[ii].populate(fields[ii], original, revised, _mask, midx, values);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + fields[ii] +
                    " for delta computation", e);
            }
        }
        _values = values.toArray();
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ReflectiveDelta ()
    {
    }

    /**
     * Checks whether the delta is empty.
     */
    public boolean isEmpty ()
    {
        return (_values.length == 0);
    }

    /**
     * Custom write method.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        // write the class reference
        _classStreamer.writeObject(_clazz, out, true);

        // write the bitmask indicating which fields are changed
        _mask.writeTo(out);

        // write the changed fields
        MutableInteger midx = new MutableInteger(), vidx = new MutableInteger();
        for (FieldHandler handler : getClassMapping(_clazz).getHandlers()) {
            handler.write(_mask, midx, _values, vidx, out);
        }
    }

    /**
     * Custom read method.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        // read the class reference
        _clazz = (Class<?>)_classStreamer.createObject(in);

        // read the bitmask
        ClassMapping cmap = getClassMapping(_clazz);
        _mask = new BareArrayMask(cmap.getMaskLength());
        _mask.readFrom(in);

        // read the changed fields
        List<Object> values = Lists.newArrayList();
        MutableInteger midx = new MutableInteger();
        for (FieldHandler handler : cmap.getHandlers()) {
            handler.read(_mask, midx, values, in);
        }
        _values = values.toArray();
    }

    @Override
    public Object apply (Object original)
    {
        // make sure it's the right class
        if (original.getClass() != _clazz) {
            throw new IllegalArgumentException("Delta class mismatch: original is " +
                original.getClass() + ", expected " + _clazz);
        }

        // create a new instance
        Object revised;
        try {
            revised = _clazz.newInstance();
        } catch (Exception e) { // InstantiationException, IllegalAccessException
            throw new RuntimeException("Failed to instantiate " + _clazz +
                " for delta application", e);
        }

        // set the fields
        ClassMapping cmap = getClassMapping(_clazz);
        Field[] fields = cmap.getFields();
        FieldHandler[] handlers = cmap.getHandlers();
        MutableInteger midx = new MutableInteger(), vidx = new MutableInteger();
        for (int ii = 0; ii < fields.length; ii++) {
            try {
                handlers[ii].apply(fields[ii], original, revised, _mask, midx, _values, vidx);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + fields[ii] +
                    " for delta application", e);
            }
        }
        return revised;
    }

    @Override
    public Delta merge (Delta other)
    {
        if (!(other instanceof ReflectiveDelta)) {
            throw new IllegalArgumentException("Cannot merge delta " + other);
        }
        ReflectiveDelta merged = new ReflectiveDelta();
        populateMerged((ReflectiveDelta)other, merged);
        return merged;
    }

    @Override
    public String toString ()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[class=").append(_clazz.getName());
        ClassMapping cmap = getClassMapping(_clazz);
        Field[] fields = cmap.getFields();
        FieldHandler[] handlers = cmap.getHandlers();
        MutableInteger midx = new MutableInteger(), vidx = new MutableInteger();
        for (int ii = 0; ii < fields.length; ii++) {
            handlers[ii].toString(fields[ii], _mask, midx, _values, vidx, buf);
        }
        return buf.append("]").toString();
    }

    /**
     * Populates the merged delta.
     */
    protected void populateMerged (ReflectiveDelta other, ReflectiveDelta merged)
    {
        if (_clazz != other._clazz) {
            throw new IllegalArgumentException("Merge class mismatch: other is " +
                other._clazz + ", expected " + _clazz);
        }
        merged._clazz = _clazz;
        int mlength = getClassMapping(_clazz).getMaskLength();
        merged._mask = new BareArrayMask(mlength);
        List<Object> values = Lists.newArrayList();
        for (int ii = 0, oidx = 0, nidx = 0; ii < mlength; ii++) {
            Object value;
            if (_mask.isSet(ii)) {
                Object ovalue = _values[oidx++];
                if (other._mask.isSet(ii)) {
                    Object nvalue = other._values[nidx++];
                    if (nvalue instanceof Delta) {
                        Delta ndelta = (Delta)nvalue;
                        value = (ovalue instanceof Delta) ?
                            ((Delta)ovalue).merge(ndelta) : ndelta.apply(ovalue);
                    } else {
                        value = nvalue;
                    }
                } else {
                    value = ovalue;
                }
            } else {
                if (other._mask.isSet(ii)) {
                    value = other._values[nidx++];
                } else {
                    continue;
                }
            }
            merged._mask.set(ii);
            values.add(value);
        }
        merged._values = values.toArray();
    }

    /**
     * Returns the class mapping for the specified class.
     */
    protected static ClassMapping getClassMapping (Class<?> clazz)
    {
        ClassMapping cmap = _classes.get(clazz);
        if (cmap == null) {
            _classes.put(clazz, cmap = new ClassMapping(clazz));
        }
        return cmap;
    }

    /**
     * Collects all appropriate fields of the specified class (and its superclasses) and places
     * them in the provided results object.
     */
    protected static void collectFields (Class<?> clazz, List<Field> fields)
    {
        // add those of the superclass, if any
        Class<?> sclazz = clazz.getSuperclass();
        if (sclazz != Object.class) {
            collectFields(sclazz, fields);
        }

        // add any non-static, non-synthetic, non-transient fields
        for (Field field : clazz.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (!(Modifier.isStatic(mods) || Modifier.isTransient(mods) || field.isSynthetic())) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
    }

    /**
     * Contains cached information about a class.
     */
    protected static class ClassMapping
    {
        /**
         * Creates a new mapping for the specified class.
         */
        public ClassMapping (Class<?> clazz)
        {
            List<Field> fields = Lists.newArrayList();
            collectFields(clazz, fields);
            _fields = fields.toArray(new Field[fields.size()]);
            _handlers = new FieldHandler[_fields.length];

            // get the handlers and count the non-final fields
            for (int ii = 0; ii < _fields.length; ii++) {
                Field field = _fields[ii];
                Class<?> type = field.getType();
                if (Modifier.isFinal(field.getModifiers()) ||
                        field.isAnnotationPresent(DeltaFinal.class)) {
                    _handlers[ii] = type.isPrimitive() ?
                        FINAL_PRIMITIVE_FIELD_HANDLERS.get(type) : FINAL_OBJECT_FIELD_HANDLER;
                } else {
                    _maskLength++;
                    _handlers[ii] = type.isPrimitive() ?
                        PRIMITIVE_FIELD_HANDLERS.get(type) : OBJECT_FIELD_HANDLER;
                }
            }
        }

        /**
         * Returns a reference to the array of non-transient fields.
         */
        public Field[] getFields ()
        {
            return _fields;
        }

        /**
         * Returns a reference to the array of field handlers.
         */
        public FieldHandler[] getHandlers ()
        {
            return _handlers;
        }

        /**
         * Returns the number of elements in the field mask (the number of non-transient, non-final
         * fields).
         */
        public int getMaskLength ()
        {
            return _maskLength;
        }

        /** The array of non-transient fields. */
        protected Field[] _fields;

        /** Handlers for each field. */
        protected FieldHandler[] _handlers;

        /** The number of elements in the field mask. */
        protected int _maskLength;
    }

    /**
     * Handles a particular field.
     */
    protected static abstract class FieldHandler
    {
        /**
         * Compares the field in the original and revised objects and, if they differ, populates
         * the supplied mask and values list with the delta values.
         *
         * @param midx an in/out parameter representing the index in the mask.
         */
        public abstract void populate (
            Field field, Object original, Object revised,
            ArrayMask mask, MutableInteger midx, List<Object> values)
                throws IllegalAccessException;

        /**
         * Writes the delta value for the field (if any) to the stream.
         *
         * @param midx an in/out parameter representing the index in the mask.
         * @param vidx an in/out parameter representing the index in the value array.
         */
        public abstract void write (
            ArrayMask mask, MutableInteger midx, Object[] values,
            MutableInteger vidx, ObjectOutputStream out)
                throws IOException;

        /**
         * Reads the delta value for the field (if any) from the stream.
         *
         * @param midx an in/out parameter representing the index in the mask.
         */
        public abstract void read (
            ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
                throws IOException, ClassNotFoundException;

        /**
         * Applies the delta value (if any) to the provided objects.
         *
         * @param midx an in/out parameter representing the index in the mask.
         * @param vidx an in/out parameter representing the index in the value array.
         */
        public abstract void apply (
            Field field, Object original, Object revised, ArrayMask mask,
            MutableInteger midx, Object[] values, MutableInteger vidx)
                throws IllegalAccessException;

        /**
         * Writes the delta value (if any) to the specified string.
         *
         * @param midx an in/out parameter representing the index in the mask.
         * @param vidx an in/out parameter representing the index in the value array.
         */
        public void toString (
            Field field, ArrayMask mask, MutableInteger midx,
            Object[] values, MutableInteger vidx, StringBuilder buf)
        {
            if (mask.isSet(midx.value++)) {
                buf.append(", " + field.getName() + "=" + values[vidx.value++]);
            }
        }
    }

    /**
     * Base class for final field handlers.
     */
    protected static abstract class FinalFieldHandler extends FieldHandler
    {
        @Override
        public void populate (
            Field field, Object original, Object revised,
            ArrayMask mask, MutableInteger midx, List<Object> values)
        {
            // no-op
        }

        @Override
        public void write (
            ArrayMask mask, MutableInteger midx, Object[] values,
            MutableInteger vidx, ObjectOutputStream out)
        {
            // no-op
        }

        @Override
        public void read (
            ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
        {
            // no-op
        }

        @Override
        public void toString (
            Field field, ArrayMask mask, MutableInteger midx,
            Object[] values, MutableInteger vidx, StringBuilder buf)
        {
            // no-op
        }
    }

    /** The object class. */
    protected Class<?> _clazz;

    /** The mask indicating which fields have changed. */
    protected BareArrayMask _mask;

    /** The values for each of the object's changed fields (either a new value or a {@link Delta}
     * object). */
    protected Object[] _values;

    /** Cached mappings for deltable classes. */
    protected static Map<Class<?>, ClassMapping> _classes = Maps.newHashMap();

    /** Field handlers for primitive fields mapped by class. */
    protected static final Map<Class<?>, FieldHandler> PRIMITIVE_FIELD_HANDLERS =
        ImmutableMap.<Class<?>, FieldHandler>builder()
            .put(Boolean.TYPE, new FieldHandler() {
                @Override public void populate (
                    Field field, Object original, Object revised,
                    ArrayMask mask, MutableInteger midx, List<Object> values)
                        throws IllegalAccessException {
                    int idx = midx.value++;
                    boolean nvalue = field.getBoolean(revised);
                    if (field.getBoolean(original) != nvalue) {
                        mask.set(idx);
                        values.add(nvalue);
                    }
                }
                @Override public void write (
                   ArrayMask mask, MutableInteger midx, Object[] values,
                   MutableInteger vidx, ObjectOutputStream out)
                       throws IOException {
                   if (mask.isSet(midx.value++)) {
                       out.writeBoolean((Boolean)values[vidx.value++]);
                   }
                }
                @Override public void read (
                    ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
                    if (mask.isSet(midx.value++)) {
                        values.add(in.readBoolean());
                    }
                }
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    boolean value;
                    if (mask.isSet(midx.value++)) {
                        value = (Boolean)values[vidx.value++];
                    } else {
                        value = field.getBoolean(original);
                    }
                    field.setBoolean(revised, value);
                }
            })
            .put(Byte.TYPE, new FieldHandler() {
                @Override public void populate (
                    Field field, Object original, Object revised,
                    ArrayMask mask, MutableInteger midx, List<Object> values)
                        throws IllegalAccessException {
                    int idx = midx.value++;
                    byte nvalue = field.getByte(revised);
                    if (field.getByte(original) != nvalue) {
                        mask.set(idx);
                        values.add(nvalue);
                    }
                }
                @Override public void write (
                   ArrayMask mask, MutableInteger midx, Object[] values,
                   MutableInteger vidx, ObjectOutputStream out)
                       throws IOException {
                   if (mask.isSet(midx.value++)) {
                       out.writeByte((Byte)values[vidx.value++]);
                   }
                }
                @Override public void read (
                    ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
                    if (mask.isSet(midx.value++)) {
                        values.add(in.readByte());
                    }
                }
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    byte value;
                    if (mask.isSet(midx.value++)) {
                        value = (Byte)values[vidx.value++];
                    } else {
                        value = field.getByte(original);
                    }
                    field.setByte(revised, value);
                }
            })
            .put(Character.TYPE, new FieldHandler() {
                @Override public void populate (
                    Field field, Object original, Object revised,
                    ArrayMask mask, MutableInteger midx, List<Object> values)
                        throws IllegalAccessException {
                    int idx = midx.value++;
                    char nvalue = field.getChar(revised);
                    if (field.getChar(original) != nvalue) {
                        mask.set(idx);
                        values.add(nvalue);
                    }
                }
                @Override public void write (
                   ArrayMask mask, MutableInteger midx, Object[] values,
                   MutableInteger vidx, ObjectOutputStream out)
                       throws IOException {
                   if (mask.isSet(midx.value++)) {
                       out.writeChar((Character)values[vidx.value++]);
                   }
                }
                @Override public void read (
                    ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
                    if (mask.isSet(midx.value++)) {
                        values.add(in.readChar());
                    }
                }
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    char value;
                    if (mask.isSet(midx.value++)) {
                        value = (Character)values[vidx.value++];
                    } else {
                        value = field.getChar(original);
                    }
                    field.setChar(revised, value);
                }
            })
            .put(Double.TYPE, new FieldHandler() {
                @Override public void populate (
                    Field field, Object original, Object revised,
                    ArrayMask mask, MutableInteger midx, List<Object> values)
                        throws IllegalAccessException {
                    int idx = midx.value++;
                    double nvalue = field.getDouble(revised);
                    if (field.getDouble(original) != nvalue) {
                        mask.set(idx);
                        values.add(nvalue);
                    }
                }
                @Override public void write (
                   ArrayMask mask, MutableInteger midx, Object[] values,
                   MutableInteger vidx, ObjectOutputStream out)
                       throws IOException {
                   if (mask.isSet(midx.value++)) {
                       out.writeDouble((Double)values[vidx.value++]);
                   }
                }
                @Override public void read (
                    ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
                    if (mask.isSet(midx.value++)) {
                        values.add(in.readDouble());
                    }
                }
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    double value;
                    if (mask.isSet(midx.value++)) {
                        value = (Double)values[vidx.value++];
                    } else {
                        value = field.getDouble(original);
                    }
                    field.setDouble(revised, value);
                }
            })
            .put(Float.TYPE, new FieldHandler() {
                @Override public void populate (
                    Field field, Object original, Object revised,
                    ArrayMask mask, MutableInteger midx, List<Object> values)
                        throws IllegalAccessException {
                    int idx = midx.value++;
                    float nvalue = field.getFloat(revised);
                    if (field.getFloat(original) != nvalue) {
                        mask.set(idx);
                        values.add(nvalue);
                    }
                }
                @Override public void write (
                   ArrayMask mask, MutableInteger midx, Object[] values,
                   MutableInteger vidx, ObjectOutputStream out)
                       throws IOException {
                   if (mask.isSet(midx.value++)) {
                       out.writeFloat((Float)values[vidx.value++]);
                   }
                }
                @Override public void read (
                    ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
                    if (mask.isSet(midx.value++)) {
                        values.add(in.readFloat());
                    }
                }
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    float value;
                    if (mask.isSet(midx.value++)) {
                        value = (Float)values[vidx.value++];
                    } else {
                        value = field.getFloat(original);
                    }
                    field.setFloat(revised, value);
                }
            })
            .put(Integer.TYPE, new FieldHandler() {
                @Override public void populate (
                    Field field, Object original, Object revised,
                    ArrayMask mask, MutableInteger midx, List<Object> values)
                        throws IllegalAccessException {
                    int idx = midx.value++;
                    int nvalue = field.getInt(revised);
                    if (field.getInt(original) != nvalue) {
                        mask.set(idx);
                        values.add(nvalue);
                    }
                }
                @Override public void write (
                   ArrayMask mask, MutableInteger midx, Object[] values,
                   MutableInteger vidx, ObjectOutputStream out)
                       throws IOException {
                   if (mask.isSet(midx.value++)) {
                       out.writeInt((Integer)values[vidx.value++]);
                   }
                }
                @Override public void read (
                    ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
                    if (mask.isSet(midx.value++)) {
                        values.add(in.readInt());
                    }
                }
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    int value;
                    if (mask.isSet(midx.value++)) {
                        value = (Integer)values[vidx.value++];
                    } else {
                        value = field.getInt(original);
                    }
                    field.setInt(revised, value);
                }
            })
            .put(Long.TYPE, new FieldHandler() {
                @Override public void populate (
                    Field field, Object original, Object revised,
                    ArrayMask mask, MutableInteger midx, List<Object> values)
                        throws IllegalAccessException {
                    int idx = midx.value++;
                    long nvalue = field.getLong(revised);
                    if (field.getLong(original) != nvalue) {
                        mask.set(idx);
                        values.add(nvalue);
                    }
                }
                @Override public void write (
                   ArrayMask mask, MutableInteger midx, Object[] values,
                   MutableInteger vidx, ObjectOutputStream out)
                       throws IOException {
                   if (mask.isSet(midx.value++)) {
                       out.writeLong((Long)values[vidx.value++]);
                   }
                }
                @Override public void read (
                    ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
                    if (mask.isSet(midx.value++)) {
                        values.add(in.readLong());
                    }
                }
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    long value;
                    if (mask.isSet(midx.value++)) {
                        value = (Long)values[vidx.value++];
                    } else {
                        value = field.getLong(original);
                    }
                    field.setLong(revised, value);
                }
            })
            .put(Short.TYPE, new FieldHandler() {
                @Override public void populate (
                    Field field, Object original, Object revised,
                    ArrayMask mask, MutableInteger midx, List<Object> values)
                        throws IllegalAccessException {
                    int idx = midx.value++;
                    short nvalue = field.getShort(revised);
                    if (field.getShort(original) != nvalue) {
                        mask.set(idx);
                        values.add(nvalue);
                    }
                }
                @Override public void write (
                   ArrayMask mask, MutableInteger midx, Object[] values,
                   MutableInteger vidx, ObjectOutputStream out)
                       throws IOException {
                   if (mask.isSet(midx.value++)) {
                       out.writeShort((Short)values[vidx.value++]);
                   }
                }
                @Override public void read (
                    ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
                    if (mask.isSet(midx.value++)) {
                        values.add(in.readShort());
                    }
                }
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    short value;
                    if (mask.isSet(midx.value++)) {
                        value = (Short)values[vidx.value++];
                    } else {
                        value = field.getShort(original);
                    }
                    field.setShort(revised, value);
                }
            })
            .build();

    /** Field handlers for final primitive fields mapped by class. */
    protected static final Map<Class<?>, FieldHandler> FINAL_PRIMITIVE_FIELD_HANDLERS =
        ImmutableMap.<Class<?>, FieldHandler>builder()
            .put(Boolean.TYPE, new FinalFieldHandler() {
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    field.setBoolean(revised, field.getBoolean(original));
                }
            })
            .put(Byte.TYPE, new FinalFieldHandler() {
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    field.setByte(revised, field.getByte(original));
                }
            })
            .put(Character.TYPE, new FinalFieldHandler() {
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    field.setChar(revised, field.getChar(original));
                }
            })
            .put(Double.TYPE, new FinalFieldHandler() {
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    field.setDouble(revised, field.getDouble(original));
                }
            })
            .put(Float.TYPE, new FinalFieldHandler() {
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    field.setFloat(revised, field.getFloat(original));
                }
            })
            .put(Integer.TYPE, new FinalFieldHandler() {
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    field.setInt(revised, field.getInt(original));
                }
            })
            .put(Long.TYPE, new FinalFieldHandler() {
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    field.setLong(revised, field.getLong(original));
                }
            })
            .put(Short.TYPE, new FinalFieldHandler() {
                @Override public void apply (
                    Field field, Object original, Object revised, ArrayMask mask,
                    MutableInteger midx, Object[] values, MutableInteger vidx)
                        throws IllegalAccessException {
                    field.setShort(revised, field.getShort(original));
                }
            })
            .build();

    /** Handler for object fields. */
    protected static final FieldHandler OBJECT_FIELD_HANDLER = new FieldHandler() {
        @Override public void populate (
            Field field, Object original, Object revised,
            ArrayMask mask, MutableInteger midx, List<Object> values)
                throws IllegalAccessException {
            int idx = midx.value++;
            Object ovalue = _oarray[0] = field.get(original);
            Object nvalue = _narray[0] = field.get(revised);
            if (!Arrays.deepEquals(_oarray, _narray)) {
                if (Delta.checkDeltable(ovalue, nvalue)) {
                    nvalue = Delta.createDelta(ovalue, nvalue);
                }
                mask.set(idx);
                values.add(nvalue);
            }
        }
        @Override public void write (
            ArrayMask mask, MutableInteger midx, Object[] values,
            MutableInteger vidx, ObjectOutputStream out)
                throws IOException {
            if (mask.isSet(midx.value++)) {
                out.writeObject(values[vidx.value++]);
            }
        }
        @Override public void read (
            ArrayMask mask, MutableInteger midx, List<Object> values, ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            if (mask.isSet(midx.value++)) {
                values.add(in.readObject());
            }
        }
        @Override public void apply (
            Field field, Object original, Object revised, ArrayMask mask,
            MutableInteger midx, Object[] values, MutableInteger vidx)
                throws IllegalAccessException {
            Object value;
            if (mask.isSet(midx.value++)) {
                value = values[vidx.value++];
                if (value instanceof Delta) {
                    value = ((Delta)value).apply(field.get(original));
                }
            } else {
                value = field.get(original);
            }
            field.set(revised, value);
        }
        protected Object[] _oarray = new Object[1], _narray = new Object[1];
    };

    /** Handler for final object fields. */
    protected static final FieldHandler FINAL_OBJECT_FIELD_HANDLER = new FinalFieldHandler() {
        @Override public void apply (
            Field field, Object original, Object revised, ArrayMask mask,
            MutableInteger midx, Object[] values, MutableInteger vidx)
                throws IllegalAccessException {
            field.set(revised, field.get(original));
        }
    };
}
