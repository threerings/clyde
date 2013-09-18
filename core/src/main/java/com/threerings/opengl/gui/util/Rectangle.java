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
 * Represents the bounds of a component.
 */
public final class Rectangle
{
    /** The x position of the entity in question. */
    public int x;

    /** The y position of the entity in question. */
    public int y;

    /** The width of the entity in question. */
    public int width;

    /** The height of the entity in question. */
    public int height;

    /**
     * Creates a rectangle with the specified location and dimensions.
     */
    public Rectangle (int x, int y, int width, int height)
    {
        set(x, y, width, height);
    }

    /**
     * Copy constructor.
     */
    public Rectangle (Rectangle other)
    {
        set(other);
    }

    /**
     * Creates an empty rectangle.
     */
    public Rectangle ()
    {
    }

    /**
     * Returns the area of the rectangle.
     */
    public int getArea ()
    {
        return width * height;
    }

    /**
     * Computes the intersection in-place of this rectangle and the specified other.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rectangle intersectLocal (Rectangle other)
    {
        return intersect(other, this);
    }

    /**
     * Computes the intersection of this rectangle and the specified other.
     *
     * @return a new rectangle containing the result.
     */
    public Rectangle intersect (Rectangle other)
    {
        return intersect(other, new Rectangle());
    }

    /**
     * Computes the intersection of this rectangle and the specified other, placing the
     * result in the object provided.
     *
     * @return a reference to the result object, for chaining.
     */
    public Rectangle intersect (Rectangle other, Rectangle result)
    {
        int x1 = Math.max(x, other.x), y1 = Math.max(y, other.y),
            x2 = Math.min(x + width, other.x + other.width),
            y2 = Math.min(y + height, other.y + other.height);
        return result.set(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
    }

    /**
     * Sets the state of this rectangle to that of the specified other.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rectangle set (Rectangle other)
    {
        return set(other.x, other.y, other.width, other.height);
    }

    /**
     * Sets the fields of this rectangle.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rectangle set (int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Adds the specified rectangle to this rectangle, causing this rectangle
     * to become the union of itself and the specified rectangle.
     */
    public void add (int x, int y, int width, int height)
    {
        int fx = Math.max(this.x+this.width, x+width);
        int fy = Math.max(this.y+this.height, y+height);
        this.x = Math.min(x, this.x);
        this.y = Math.min(y, this.y);
        this.width = fx-this.x;
        this.height = fy-this.y;
    }

    /**
     * Increases the size of this rectangle by the specified amounts in the horizontal and vertical
     * dimensions.
     */
    public void grow (int h, int v)
    {
        x -= h;
        y -= v;
        width += h*2;
        height += v*2;
    }

    // documentation inherited
    public boolean equals (Object other)
    {
        if (other instanceof Rectangle) {
            Rectangle orect = (Rectangle)other;
            return x == orect.x && y == orect.y &&
                width == orect.width && height == orect.height;
        }
        return false;
    }

    // documentation inherited
    public int hashCode ()
    {
        return x ^ y ^ width ^ height;
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return width + "x" + height + (x >= 0 ? "+" : "") + x +
            (y >= 0 ? "+" : "") + y;
    }
}
