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

package com.threerings.config;

import java.io.IOException;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Copyable;
import com.threerings.util.DeepUtil;

/**
 * Stores arguments, extending {@link TreeMap} to implement {@link #hashCode} and {@link #equals}
 * using {@link Arrays#deepHashCode} and {@link Arrays#deepEquals} to provide expected behavior
 * when using arrays as values.  Also implements {@link #clone} to deep-copy values.
 */
public class ArgumentMap extends TreeMap<String, Object>
    implements Copyable, Streamable
{
    /**
     * Creates an argument map with the supplied arguments.
     */
    public ArgumentMap (String firstKey, Object firstValue, Object... otherArgs)
    {
        put(firstKey, firstValue);
        for (int ii = 0; ii < otherArgs.length; ii += 2) {
            put((String)otherArgs[ii], otherArgs[ii + 1]);
        }
    }

    /**
     * Creates an empty map.
     */
    public ArgumentMap ()
    {
    }

    /**
     * Custom write method.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.writeInt(size());
        for (Map.Entry<String, Object> entry : entrySet()) {
            out.writeIntern(entry.getKey());
            out.writeObject(entry.getValue());
        }
    }

    /**
     * Custom read method.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        for (int ii = 0, nn = in.readInt(); ii < nn; ii++) {
            put(in.readIntern(), in.readObject());
        }
    }

    // documentation inherited from interface Copyable
    public Object copy (Object dest)
    {
        ArgumentMap cmap;
        if (dest instanceof ArgumentMap) {
            cmap = (ArgumentMap)dest;
            cmap.clear();
        } else {
            cmap = new ArgumentMap();
        }
        for (Map.Entry<String, Object> entry : entrySet()) {
            cmap.put(entry.getKey(), DeepUtil.copy(entry.getValue()));
        }
        return cmap;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        return copy(null);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ArgumentMap)) {
            return false;
        }
        ArgumentMap omap = (ArgumentMap)other;
        int size = size(), osize = omap.size();
        if (size != osize) {
            return false;
        }
        if (size == 0) {
            return true;
        }
        for (Map.Entry<String, Object> entry : entrySet()) {
            String key = entry.getKey();
            if (!omap.containsKey(key)) {
                return false;
            }
            _a1[0] = entry.getValue();
            _a2[0] = omap.get(key);
            if (!Arrays.deepEquals(_a1, _a2)) {
                return false;
            }
        }
        return true;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        int hash = 0;
        for (Map.Entry<String, Object> entry : entrySet()) {
            _a1[0] = entry.getValue();
            hash += entry.getKey().hashCode() ^ Arrays.deepHashCode(_a1);
        }
        return hash;
    }

    /** Used for {@link Arrays#deepHashCode} and {@link Arrays#deepEquals}. */
    protected transient Object[] _a1 = new Object[1], _a2 = new Object[1];
}
