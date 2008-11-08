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
        _values = new Object[fields.length];
        Object[] oarray = new Object[1], narray = new Object[1];
        for (int ii = 0; ii < fields.length; ii++) {
            Field field = fields[ii];
            try {
                Object ovalue = oarray[0] = field.get(original);
                Object nvalue = narray[0] = field.get(revised);
                if (Arrays.deepEquals(oarray, narray)) {
                    continue; // leave as null to indicate no change
                } else if (nvalue == null) {
                    nvalue = NULL;
                } else if (Delta.checkDeltable(ovalue, nvalue)) {
                    nvalue = Delta.createDelta(ovalue, nvalue);
                }
                _values[ii] = nvalue;

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + field +
                    " for delta computation", e);
            }
        }
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ReflectiveDelta ()
    {
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
        for (int ii = 0; ii < fields.length; ii++) {
            Field field = fields[ii];
            Object value = _values[ii];
            try {
                if (value == null) {
                    value = field.get(original);
                } else if (value == NULL) {
                    value = null;
                } else if (value instanceof Delta) {
                    value = ((Delta)value).apply(field.get(original));
                }
                field.set(revised, value);

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + field +
                    " for delta application", e);
            }
        }
        return revised;
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
        byte[] mask = new byte[getMaskLength()];
        for (int ii = 0; ii < _values.length; ii++) {
            if (_values[ii] != null) {
                int idx = ii / 8, bit = 1 << (ii % 8);
                mask[idx] |= bit;
            }
        }
        out.write(mask, 0, mask.length);

        // write the changed fields
        Field[] fields = getFields(_clazz);
        for (int ii = 0; ii < _values.length; ii++) {
            Object value = _values[ii];
            if (value == null) {
                continue;
            }
            Class type = fields[ii].getType();
            if (type.isPrimitive()) {
                out.writeBareObject(value);
            } else {
                out.writeObject(value == NULL ? null : value);
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
        _values = new Object[fields.length];
        byte[] mask = new byte[getMaskLength()];
        in.read(mask, 0, mask.length);

        // read the changed fields
        for (int ii = 0; ii < _values.length; ii++) {
            int idx = ii / 8, bit = 1 << (ii % 8);
            if ((mask[idx] & bit) == 0) {
                continue;
            }
            Class type = fields[ii].getType();
            if (type.isPrimitive()) {
                Streamer streamer = _wrapperStreamers.get(type);
                _values[ii] = streamer.createObject(in);
            } else {
                Object value = in.readObject();
                _values[ii] = (value == null) ? NULL : value;
            }
        }
    }

    /**
     * Returns the length of the bitmask as derived from the length of the values array.
     */
    protected int getMaskLength ()
    {
        return (_values.length / 8) + (_values.length % 8 == 0 ? 0 : 1);
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

    /** The values for each of the object's fields.  Each entry may be <code>null</code> to
     * indicate that the entry hasn't changed, a {@link Delta} to apply to the original
     * field value, the special {@link #NULL} value to indicate that the field has become
     * <code>null</code>, or a new value for the field. */
    protected Object[] _values;

    /** Cached lists of non-transient fields for deltable classes. */
    protected static HashMap<Class, Field[]> _fields = new HashMap<Class, Field[]>();

    /** Streamer for raw class references. */
    protected static Streamer _classStreamer;

    /** Maps primitive types to {@link Streamer} instances for corresponding wrappers. */
    protected static HashMap<Class, Streamer> _wrapperStreamers = Maps.newHashMap();
    static {
        try {
            _classStreamer = Streamer.getStreamer(Class.class);
            _wrapperStreamers.put(Boolean.TYPE, Streamer.getStreamer(Boolean.class));
            _wrapperStreamers.put(Byte.TYPE, Streamer.getStreamer(Byte.class));
            _wrapperStreamers.put(Character.TYPE, Streamer.getStreamer(Character.class));
            _wrapperStreamers.put(Double.TYPE, Streamer.getStreamer(Double.class));
            _wrapperStreamers.put(Float.TYPE, Streamer.getStreamer(Float.class));
            _wrapperStreamers.put(Integer.TYPE, Streamer.getStreamer(Integer.class));
            _wrapperStreamers.put(Long.TYPE, Streamer.getStreamer(Long.class));
            _wrapperStreamers.put(Short.TYPE, Streamer.getStreamer(Short.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize ReflectiveDelta class", e);
        }
    }

    /** A special value indicating that the field has been changed to <code>null</code>. */
    protected static final Object NULL = new Object() {};
}
