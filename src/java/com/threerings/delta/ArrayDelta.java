//
// $Id$

package com.threerings.delta;

import java.io.IOException;

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.Arrays;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamer;

/**
 * A delta object representing the different between two arrays.  This doesn't do anything fancy
 * like try to find inserted and deleted strings; it simply represents a change of size and any
 * changed elements.
 */
public class ArrayDelta extends Delta
{
    /**
     * Creates a new array delta that transforms the original object into the revised object
     * (both of which must be instances of the same class).
     */
    public ArrayDelta (Object original, Object revised)
    {
        // compare the elements
        _clazz = original.getClass();
        _length = Array.getLength(revised);
        _mask = new BareArrayMask(_length);
        ArrayList<Object> values = new ArrayList<Object>();
        int olen = Array.getLength(original);
        Object defvalue = Array.get(Array.newInstance(_clazz.getComponentType(), 1), 0);
        Object[] oarray = new Object[1], narray = new Object[1];
        for (int ii = 0; ii < _length; ii++) {
            Object ovalue = oarray[0] = (ii < olen) ? Array.get(original, ii) : defvalue;
            Object nvalue = narray[0] = Array.get(revised, ii);
            if (Arrays.deepEquals(oarray, narray)) {
                continue; // no change
            }
            if (Delta.checkDeltable(ovalue, nvalue)) {
                nvalue = Delta.createDelta(ovalue, nvalue);
            }
            _mask.set(ii);
            values.add(nvalue);
        }
        _values = values.toArray(new Object[values.size()]);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ArrayDelta ()
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

        // write the array length
        out.writeInt(_length);

        // write the bitmask indicating which fields are changed
        _mask.writeTo(out);

        // write the changed elements
        boolean primitive = _clazz.getComponentType().isPrimitive();
        for (int ii = 0, idx = 0; ii < _length; ii++) {
            if (!_mask.isSet(ii)) {
                continue;
            }
            Object value = _values[idx++];
            if (primitive) {
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

        // read the length
        _length = in.readInt();

        // read the bitmask
        _mask = new BareArrayMask(_length);
        _mask.readFrom(in);

        // read the changed elements
        Class ctype = _clazz.getComponentType();
        Streamer streamer = ctype.isPrimitive() ? _wrapperStreamers.get(ctype) : null;
        ArrayList<Object> values = new ArrayList<Object>();
        for (int ii = 0; ii < _length; ii++) {
            if (!_mask.isSet(ii)) {
                continue;
            }
            if (streamer != null) {
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

        // create the new instance
        Object revised = Array.newInstance(_clazz.getComponentType(), _length);

        // set the entries
        int olen = Array.getLength(original);
        for (int ii = 0, idx = 0; ii < _length; ii++) {
            Object value;
            if (_mask.isSet(ii)) {
                value = _values[idx++];
                if (value instanceof Delta) {
                    value = ((Delta)value).apply(Array.get(original, ii));
                }
            } else {
                if (ii < olen) {
                    value = Array.get(original, ii);
                } else {
                    continue; // leave the entry at the default
                }
            }
            Array.set(revised, ii, value);
        }
        return revised;
    }

    @Override // documentation inherited
    public String toString ()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[class=" + _clazz.getName());
        for (int ii = 0, idx = 0; ii < _length; ii++) {
            if (_mask.isSet(ii)) {
                buf.append(", " + ii + ":" + _values[idx++]);
            }
        }
        return buf.append("]").toString();
    }

    /** The object class. */
    protected Class _clazz;

    /** The length of the array. */
    protected int _length;

    /** The mask indicating which fields have changed. */
    protected BareArrayMask _mask;

    /** The array values. */
    protected Object[] _values;
}
