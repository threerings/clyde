//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamer;

import static com.threerings.ClydeLog.*;

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
        ArrayList<Object> values = new ArrayList<Object>();
        Object[] oarray = new Object[1], narray = new Object[1];
        for (int ii = 0, midx = 0; ii < fields.length; ii++) {
            Field field = fields[ii];
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            int idx = midx++;
            try {
                Object ovalue = oarray[0] = field.get(original);
                Object nvalue = narray[0] = field.get(revised);
                if (Arrays.deepEquals(oarray, narray)) {
                    continue; // no change
                }
                if (Delta.checkDeltable(ovalue, nvalue)) {
                    nvalue = Delta.createDelta(ovalue, nvalue);
                }
                _mask.set(idx);
                values.add(nvalue);

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + field +
                    " for delta computation", e);
            }
        }
        _values = values.toArray(new Object[values.size()]);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ReflectiveDelta ()
    {
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
        Field[] fields = getClassMapping(_clazz).getFields();
        for (int ii = 0, midx = 0, vidx = 0; ii < fields.length; ii++) {
            Field field = fields[ii];
            if (Modifier.isFinal(field.getModifiers()) || !_mask.isSet(midx++)) {
                continue;
            }
            Class type = field.getType();
            Object value = _values[vidx++];
            if (type.isPrimitive()) {
                out.writeBareObject(value);
            } else {
                out.writeObject(value);
            }
        }
    }

    /**
     * Custom read method.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        // read the class reference
        _clazz = (Class)_classStreamer.createObject(in);

        // read the bitmask
        ClassMapping cmap = getClassMapping(_clazz);
        _mask = new BareArrayMask(cmap.getMaskLength());
        _mask.readFrom(in);

        // read the changed fields
        Field[] fields = cmap.getFields();
        ArrayList<Object> values = new ArrayList<Object>();
        for (int ii = 0, midx = 0; ii < fields.length; ii++) {
            Field field = fields[ii];
            if (Modifier.isFinal(field.getModifiers()) || !_mask.isSet(midx++)) {
                continue;
            }
            Class type = field.getType();
            if (type.isPrimitive()) {
                Streamer streamer = _wrapperStreamers.get(type);
                values.add(streamer.createObject(in));
            } else {
                values.add(in.readObject());
            }
        }
        _values = values.toArray(new Object[values.size()]);
    }

    @Override // documentation inherited
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
        Field[] fields = getClassMapping(_clazz).getFields();
        for (int ii = 0, midx = 0, vidx = 0; ii < fields.length; ii++) {
            Field field = fields[ii];
            try {
                Object value;
                if (!Modifier.isFinal(field.getModifiers()) && _mask.isSet(midx++)) {
                    value = _values[vidx++];
                    if (value instanceof Delta) {
                        value = ((Delta)value).apply(field.get(original));
                    }
                } else {
                    value = field.get(original);
                }
                field.set(revised, value);

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + field +
                    " for delta application", e);
            }
        }
        return revised;
    }

    @Override // documentation inherited
    public String toString ()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[class=" + _clazz.getName());
        Field[] fields = getClassMapping(_clazz).getFields();
        for (int ii = 0, midx = 0, vidx = 0; ii < fields.length; ii++) {
            Field field = fields[ii];
            if (!Modifier.isFinal(field.getModifiers()) && _mask.isSet(midx++)) {
                buf.append(", " + field.getName() + "=" + _values[vidx++]);
            }
        }
        return buf.append("]").toString();
    }

    /**
     * Returns the class mapping for the specified class.
     */
    protected static ClassMapping getClassMapping (Class clazz)
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
    protected static void collectFields (Class clazz, ArrayList<Field> fields)
    {
        // add those of the superclass, if any
        Class sclazz = clazz.getSuperclass();
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
        public ClassMapping (Class clazz)
        {
            ArrayList<Field> fields = new ArrayList<Field>();
            collectFields(clazz, fields);
            _fields = fields.toArray(new Field[fields.size()]);

            // count the non-final fields
            for (Field field : _fields) {
                if (!Modifier.isFinal(field.getModifiers())) {
                    _maskLength++;
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
         * Returns the number of elements in the field mask (the number of non-transient, non-final
         * fields).
         */
        public int getMaskLength ()
        {
            return _maskLength;
        }

        /** The array of non-transient fields. */
        protected Field[] _fields;

        /** The number of elements in the field mask. */
        protected int _maskLength;
    }

    /** The object class. */
    protected Class _clazz;

    /** The mask indicating which fields have changed. */
    protected BareArrayMask _mask;

    /** The values for each of the object's changed fields (either a new value or a {@link Delta}
     * object). */
    protected Object[] _values;

    /** Cached mappings for deltable classes. */
    protected static HashMap<Class, ClassMapping> _classes = new HashMap<Class, ClassMapping>();
}
