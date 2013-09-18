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

import com.threerings.math.Vector2f;

/**
 * Represents the four cardinal and four ordinal directions and their offsets in the Tudey
 * coordinate system.
 */
public enum Direction
{
    NORTH(+0, +1),
    NORTHWEST(-1, +1),
    WEST(-1, +0),
    SOUTHWEST(-1, -1),
    SOUTH(+0, -1),
    SOUTHEAST(+1, -1),
    EAST(+1, +0),
    NORTHEAST(+1, +1);

    /** The cardinal directions. */
    public static final Direction[] CARDINAL_VALUES = { NORTH, WEST, SOUTH, EAST };

    /**
     * Returns the x offset corresponding to the direction.
     */
    public int getX ()
    {
        return _x;
    }

    /**
     * Returns the y offset corresponding to the direction.
     */
    public int getY ()
    {
        return _y;
    }

    /**
     * Returne the vector.
     */
    public Vector2f getVector2f ()
    {
        return _vec;
    }

    Direction (int x, int y)
    {
        _x = x;
        _y = y;
        _vec = new Vector2f((float)x, (float)y);
    }

    /** The x and y offsets corresponding to the direction. */
    protected final int _x, _y;

    /** The vector representation. */
    protected final Vector2f _vec;
}
