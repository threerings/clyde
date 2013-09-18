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

package com.threerings.tudey.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;

import com.threerings.export.Encodable;

/**
 * Represents a pair of 2D integer coordinates.
 */
public class Coord
    implements Encodable, Streamable, Cloneable
{
    /** A value used to signify an empty coordinate. */
    public static final int EMPTY = encode(Short.MIN_VALUE, Short.MIN_VALUE);

    /** The coordinates. */
    public int x, y;

    /**
     * Encodes the supplied coordinates (presumed to be in [-32768, +32767]) into a single
     * integer.
     */
    public static int encode (int x, int y)
    {
        return (x << 16) | (y & 0xFFFF);
    }

    /**
     * Extracts the x component from the specified encoded coordinate pair.
     */
    public static int decodeX (int pair)
    {
        return pair >> 16;
    }

    /**
     * Extracts the y component from the specified encoded coordinate pair.
     */
    public static int decodeY (int pair)
    {
        return (pair << 16) >> 16;
    }

    /**
     * Creates a coord from two components.
     */
    public Coord (int x, int y)
    {
        set(x, y);
    }

    /**
     * Creates a coord from the supplied encoded coordinate pair.
     */
    public Coord (int pair)
    {
        set(pair);
    }

    /**
     * Creates a coord from an array of values.
     */
    public Coord (int[] values)
    {
        set(values);
    }

    /**
     * Copy constructor.
     */
    public Coord (Coord other)
    {
        set(other);
    }

    /**
     * Creates a zero coord.
     */
    public Coord ()
    {
    }

    /**
     * Copies the elements of another coord.
     *
     * @return a reference to this coord, for chaining.
     */
    public Coord set (Coord other)
    {
        return set(other.x, other.y);
    }

    /**
     * Copies the elements of an array.
     *
     * @return a reference to this coord, for chaining.
     */
    public Coord set (int[] values)
    {
        return set(values[0], values[1]);
    }

    /**
     * Sets the fields of the coord to those contained in the supplied encoded coordinate pair.
     *
     * @return a reference to this coord, for chaining.
     */
    public Coord set (int pair)
    {
        return set(decodeX(pair), decodeY(pair));
    }

    /**
     * Sets the fields of the coord.
     *
     * @return a reference to this coord, for chaining.
     */
    public Coord set (int x, int y)
    {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Returns an encoded version of this coord.
     */
    public int encode ()
    {
        return encode(x, y);
    }

    // documentation inherited from interface Encodable
    public String encodeToString ()
    {
        return x + ", " + y;
    }

    // documentation inherited from interface Encodable
    public void decodeFromString (String string)
        throws Exception
    {
        set(StringUtil.parseIntArray(string));
    }

    // documentation inherited from interface Encodable
    public void encodeToStream (DataOutputStream out)
        throws IOException
    {
        out.writeInt(x);
        out.writeInt(y);
    }

    // documentation inherited from interface Encodable
    public void decodeFromStream (DataInputStream in)
        throws IOException
    {
        set(in.readInt(), in.readInt());
    }

    @Override
    public String toString ()
    {
        return "[" + x + ", " + y + "]";
    }

    @Override
    public Coord clone ()
    {
        try {
            return (Coord) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int hashCode ()
    {
        return x + 31*y;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Coord)) {
            return false;
        }
        Coord ocoord = (Coord)other;
        return x == ocoord.x && y == ocoord.y;
    }
}
