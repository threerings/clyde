//
// $Id$

package com.threerings.opengl.renderer;

import org.lwjgl.opengl.GL11;

/**
 * An OpenGL display list.
 */
public class DisplayList
{
    /**
     * Creates a new display list for the specified renderer.
     */
    public DisplayList (Renderer renderer)
    {
        _renderer = renderer;
        _id = GL11.glGenLists(1);
    }

    /**
     * Returns this list's OpenGL identifier.
     */
    public final int getId ()
    {
        return _id;
    }

    /**
     * Starts recording OpenGL calls for this list.
     */
    public void begin ()
    {
        GL11.glNewList(_id, GL11.GL_COMPILE);
    }

    /**
     * Stops recording OpenGL calls for this list.
     */
    public void end ()
    {
        GL11.glEndList();
    }

    /**
     * Calls this display list.
     */
    public void call ()
    {
        GL11.glCallList(_id);
    }

    /**
     * Deletes this list, rendering it unusable.
     */
    public void delete ()
    {
        GL11.glDeleteLists(_id, 1);
        _id = 0;
    }

    @Override // documentation inherited
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _renderer.displayListFinalized(_id);
        }
    }

    /** The renderer that loaded this list. */
    protected Renderer _renderer;

    /** The OpenGL identifer for the list. */
    protected int _id;
}
