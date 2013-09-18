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

package com.threerings.opengl.gui.util;

/**
 * Represents the location of a component.
 */
public class Point
    implements Cloneable
{
    /** The x position of the entity in question. */
    public int x;

    /** The y position of the entity in question. */
    public int y;

    public Point (int x, int y)
    {
        set(x, y);
    }

    public Point (Point other)
    {
        set(other);
    }

    public Point ()
    {
    }

    /**
     * Sets the value of this point to that of the specified other.
     */
    public Point set (Point point)
    {
        return set(point.x, point.y);
    }

    /**
     * Sets the value of this point.
     *
     * @return a reference to this point, for chaining.
     */
    public Point set (int x, int y)
    {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public Point clone ()
    {
        try {
            return (Point) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int hashCode ()
    {
        return x*31 + y;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Point)) {
            return false;
        }
        Point opoint = (Point)other;
        return x == opoint.x && y == opoint.y;
    }

    @Override
    public String toString ()
    {
        return (x >= 0 ? "+" : "") + x + (y >= 0 ? "+" : "") + y;
    }
}
