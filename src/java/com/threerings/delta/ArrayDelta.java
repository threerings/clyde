//
// $Id$

package com.threerings.delta;

import java.io.IOException;

import java.lang.reflect.Array;

import java.util.Arrays;

import com.threerings.io.ArrayMask;
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
        _values = new Object[Array.getLength(revised)];
        int olen = Array.getLength(original);
        Object defvalue = Array.get(Array.newInstance(_clazz.getComponentType(), 1), 0);
        Object[] oarray = new Object[1], narray = new Object[1];
        for (int ii = 0; ii < _values.length; ii++) {
            Object ovalue = oarray[0] = (ii < olen) ? Array.get(original, ii) : defvalue;
            Object nvalue = narray[0] = Array.get(revised, ii);
            if (Arrays.deepEquals(oarray, narray)) {
                continue; // leave as null to indicate no change
            } else if (nvalue == null) {
                nvalue = NULL;
            } else if (Delta.checkDeltable(ovalue, nvalue)) {
                nvalue = Delta.createDelta(ovalue, nvalue);
            }
            _values[ii] = nvalue;
        }
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
        out.writeInt(_values.length);

        // write the bitmask indicating which fields are changed
        ArrayMask mask = new ArrayMask(_values.length);
        for (int ii = 0; ii < _values.length; ii++) {
            if (_values[ii] != null) {
                mask.set(ii);
            }
        }
        mask.writeTo(out);

        // write the changed elements
        boolean primitive = _clazz.getComponentType().isPrimitive();
        for (int ii = 0; ii < _values.length; ii++) {
            Object value = _values[ii];
            if (value == null) {
                continue;
            }
            if (primitive) {
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

        // read the length and create the array
        _values = new Object[in.readInt()];

        // read the bitmask
        ArrayMask mask = new ArrayMask(_values.length);
        mask.readFrom(in);

        // read the changed elements
        Class ctype = _clazz.getComponentType();
        Streamer streamer = ctype.isPrimitive() ? _wrapperStreamers.get(ctype) : null;
        for (int ii = 0; ii < _values.length; ii++) {
            if (!mask.isSet(ii)) {
                continue;
            }
            if (streamer != null) {
                _values[ii] = streamer.createObject(in);
            } else {
                Object value = in.readObject();
                _values[ii] = (value == null) ? NULL : value;
            }
        }
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
        Object revised = Array.newInstance(_clazz.getComponentType(), _values.length);

        // set the entries
        int olen = Array.getLength(original);
        for (int ii = 0; ii < _values.length; ii++) {
            Object value = _values[ii];
            if (value == null) {
                if (ii < olen) {
                    value = Array.get(original, ii);
                } else {
                    continue; // leave the entry at the default
                }
            } else if (value == NULL) {
                value = null;
            } else if (value instanceof Delta) {
                value = ((Delta)value).apply(Array.get(original, ii));
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
        for (int ii = 0; ii < _values.length; ii++) {
            Object value = _values[ii];
            if (value != null) {
                buf.append(", " + ii + ":" + value);
            }
        }
        return buf.append("]").toString();
    }

    /** The object class. */
    protected Class _clazz;

    /** The array values. */
    protected Object[] _values;
}
