//
// $Id$

package com.threerings.opengl.gui.util;

/**
 * Represents the size of a component.
 */
public class Dimension
{
    /** The width of the entity in question. */
    public int width;

    /** The height of the entity in question. */
    public int height;

    public Dimension (int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    public Dimension (Dimension other)
    {
        width = other.width;
        height = other.height;
    }

    public Dimension ()
    {
    }

    public String toString ()
    {
        return width + "x" + height;
    }
}
