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

import java.lang.reflect.Array;

import java.util.List;
import java.util.Arrays;

import com.google.common.collect.Lists;
import com.google.common.primitives.Primitives;

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
        List<Object> values = Lists.newArrayList();
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
        _values = values.toArray();
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
        _clazz = (Class<?>)_classStreamer.createObject(in);

        // read the length
        _length = in.readInt();

        // read the bitmask
        _mask = new BareArrayMask(_length);
        _mask.readFrom(in);

        // read the changed elements
        Class<?> ctype = _clazz.getComponentType();
        Streamer streamer = ctype.isPrimitive()
            ? Streamer.getStreamer(Primitives.wrap(ctype))
            : null;
        List<Object> values = Lists.newArrayList();
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

    @Override
    public Delta merge (Delta other)
    {
        ArrayDelta aother;
        if (!(other instanceof ArrayDelta && (aother = (ArrayDelta)other)._clazz == _clazz)) {
            throw new IllegalArgumentException("Cannot merge delta " + other);
        }
        ArrayDelta merged = new ArrayDelta();
        merged._clazz = _clazz;
        int mlength = aother._length;
        merged._length = mlength;
        merged._mask = new BareArrayMask(mlength);
        List<Object> values = Lists.newArrayList();
        for (int ii = 0, oidx = 0, nidx = 0; ii < mlength; ii++) {
            Object value;
            if (ii < _length && _mask.isSet(ii)) {
                Object ovalue = _values[oidx++];
                if (aother._mask.isSet(ii)) {
                    Object nvalue = aother._values[nidx++];
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
                if (aother._mask.isSet(ii)) {
                    value = aother._values[nidx++];
                } else {
                    continue;
                }
            }
            merged._mask.set(ii);
            values.add(value);
        }
        merged._values = values.toArray();
        return merged;
    }

    @Override
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
    protected Class<?> _clazz;

    /** The length of the array. */
    protected int _length;

    /** The mask indicating which fields have changed. */
    protected BareArrayMask _mask;

    /** The array values. */
    protected Object[] _values;
}
