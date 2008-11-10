//
// $Id$

package com.threerings.delta;

import java.io.IOException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.google.common.collect.Maps;

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
        Field[] fields = getFields(_clazz = original.getClass());
        _mask = new BareArrayMask(fields.length);
        ArrayList<Object> values = new ArrayList<Object>();
        Object[] oarray = new Object[1], narray = new Object[1];
        for (int ii = 0; ii < fields.length; ii++) {
            Field field = fields[ii];
            try {
                Object ovalue = oarray[0] = field.get(original);
                Object nvalue = narray[0] = field.get(revised);
                if (Arrays.deepEquals(oarray, narray)) {
                    continue; // no change
                }
                if (Delta.checkDeltable(ovalue, nvalue)) {
                    nvalue = Delta.createDelta(ovalue, nvalue);
                }
                _mask.set(ii);
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
        Field[] fields = getFields(_clazz);
        for (int ii = 0, idx = 0; ii < fields.length; ii++) {
            if (!_mask.isSet(ii)) {
                continue;
            }
            Class type = fields[ii].getType();
            Object value = _values[idx++];
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
        Field[] fields = getFields(_clazz);
        _mask = new BareArrayMask(fields.length);
        _mask.readFrom(in);

        // read the changed fields
        ArrayList<Object> values = new ArrayList<Object>();
        for (int ii = 0; ii < fields.length; ii++) {
            if (!_mask.isSet(ii)) {
                continue;
            }
            Class type = fields[ii].getType();
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
        Field[] fields = getFields(_clazz);
        for (int ii = 0, idx = 0; ii < fields.length; ii++) {
            Field field = fields[ii];
            try {
                Object value;
                if (_mask.isSet(ii)) {
                    value = _values[idx++];
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
        Field[] fields = getFields(_clazz);
        for (int ii = 0, idx = 0; ii < fields.length; ii++) {
            if (_mask.isSet(ii)) {
                buf.append(", " + fields[ii].getName() + "=" + _values[idx++]);
            }
        }
        return buf.append("]").toString();
    }

    /**
     * Returns an array containing the non-transient fields of the specified class.
     */
    protected static Field[] getFields (Class clazz)
    {
        Field[] fields = _fields.get(clazz);
        if (fields == null) {
            ArrayList<Field> list = new ArrayList<Field>();
            collectFields(clazz, list);
            _fields.put(clazz, fields = list.toArray(new Field[list.size()]));
        }
        return fields;
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

    /** The object class. */
    protected Class _clazz;

    /** The mask indicating which fields have changed. */
    protected BareArrayMask _mask;

    /** The values for each of the object's changed fields (either a new value or a {@link Delta}
     * object). */
    protected Object[] _values;

    /** Cached lists of non-transient fields for deltable classes. */
    protected static HashMap<Class, Field[]> _fields = Maps.newHashMap();
}
