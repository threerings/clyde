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
 * Represents insets from the edges of a component.
 */
public class Insets
{
    /** A convenient set of blank insets. */
    public static final Insets ZERO_INSETS = new Insets(0, 0, 0, 0);

    /** The inset from the left edge. */
    public int left;

    /** The inset from the top edge. */
    public int top;

    /** The inset from the right edge. */
    public int right;

    /** The inset from the bottom edge. */
    public int bottom;

    public Insets (int left, int top, int right, int bottom)
    {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public Insets (Insets other)
    {
        left = other.left;
        top = other.top;
        right = other.right;
        bottom = other.bottom;
    }

    public Insets ()
    {
    }

    /**
     * Returns the sum of the horizontal insets.
     */
    public int getHorizontal ()
    {
        return left + right;
    }

    /**
     * Returns the sum of the vertical insets.
     */
    public int getVertical ()
    {
        return top + bottom;
    }

    /**
     * Returns insets which are the sum of these insets with the specified
     * insets. <em>Note:</em> if either insets are all zeros, the other set
     * will be returned directly rather than creating a new insets instance.
     */
    public Insets add (Insets insets)
    {
        if (ZERO_INSETS.equals(this)) {
            return insets;
        } else if (ZERO_INSETS.equals(insets)) {
            return this;
        } else {
            return new Insets(left + insets.left, top + insets.top,
                              right + insets.right, bottom + insets.bottom);
        }
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Insets)) {
            return false;
        }
        Insets oi = (Insets)other;
        return (left == oi.left) && (top == oi.top) &&
            (right == oi.right) && (bottom == oi.bottom);
    }

    @Override
    public int hashCode ()
    {
        return left ^ top ^ right ^ bottom;
    }

    public String toString ()
    {
        return "l:" + left + " t:" + top + " r:" + right + " b:" + bottom;
    }
}
