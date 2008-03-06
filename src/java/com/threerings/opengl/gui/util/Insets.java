//
// $Id$

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

    // documentation inherited
    public boolean equals (Object other)
    {
        Insets oi = (Insets)other;
        return (left == oi.left) && (top == oi.top) &&
            (right == oi.right) && (bottom == oi.bottom);
    }

    public String toString ()
    {
        return "l:" + left + " t:" + top + " r:" + right + " b:" + bottom;
    }
}
