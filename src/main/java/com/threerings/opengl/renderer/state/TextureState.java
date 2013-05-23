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

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.TextureUnit;

/**
 * Contains the texture state.
 */
public class TextureState extends RenderState
{
    /** A state that disables texturing. */
    public static final TextureState DISABLED = new TextureState(null);

    /**
     * Creates a new texture state.
     */
    public TextureState (TextureUnit[] units)
    {
        _units = units;
    }

    /**
     * Returns a reference to the array of texture units.
     */
    public TextureUnit[] getUnits ()
    {
        return _units;
    }

    /**
     * Returns the unit at the specified index, if any.
     */
    public TextureUnit getUnit (int idx)
    {
        return (_units == null || _units.length <= idx) ? null : _units[idx];
    }

    @Override
    public int getType ()
    {
        return TEXTURE_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setTextureState(_units);
    }

    /** The states of the texture units. */
    protected TextureUnit[] _units;
}
