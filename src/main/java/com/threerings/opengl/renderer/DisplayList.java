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
        _renderer.displayListCreated();
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
        _renderer.displayListDeleted();
    }

    @Override
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
