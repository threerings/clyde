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

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the draw color state.
 */
public class ColorState extends RenderState
{
    /** An opaque white color state. */
    public static final ColorState WHITE = new ColorState(Color4f.WHITE);

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static ColorState getInstance (Color4f color)
    {
        return getInstance(new ColorState(color));
    }

    /**
     * If there is a shared equivalent to the specified state, this method will return the shared
     * state; otherwise, it will simply return the parameter.
     */
    public static ColorState getInstance (ColorState state)
    {
        return state.equals(WHITE) ? WHITE : state;
    }

    /**
     * Creates a new color state with the values in the supplied color object.
     */
    public ColorState (Color4f color)
    {
        _color.set(color);
    }

    /**
     * Creates a new color state.
     */
    public ColorState ()
    {
    }

    /**
     * Returns a reference to the draw color object.
     */
    public Color4f getColor ()
    {
        return _color;
    }

    @Override
    public int getType ()
    {
        return COLOR_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setColorState(_color);
    }

    @Override
    public boolean equals (Object other)
    {
        return other instanceof ColorState && _color.equals(((ColorState)other)._color);
    }

    @Override
    public int hashCode ()
    {
        return _color != null ? _color.hashCode() : 0;
    }

    /** The draw color. */
    protected Color4f _color = new Color4f();
}
