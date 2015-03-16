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

/**
 * Contains the line state.
 */
public class LineState extends RenderState
{
    /** The default state. */
    public static final LineState DEFAULT = new LineState(1f);

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static LineState getInstance (float lineWidth)
    {
        return getInstance(new LineState(lineWidth));
    }

    /**
     * If there is a shared equivalent to the specified state, this method will return the shared
     * state; otherwise, it will simply return the parameter.
     */
    public static LineState getInstance (LineState state)
    {
        if (state.equals(DEFAULT)) {
            return DEFAULT;
        } else {
            return state;
        }
    }

    /**
     * Creates a new line state.
     */
    public LineState (float lineWidth)
    {
        _lineWidth = lineWidth;
    }

    /**
     * Returns the line width.
     */
    public float getLineWidth ()
    {
        return _lineWidth;
    }

    @Override
    public int getType ()
    {
        return LINE_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setLineState(_lineWidth);
    }

    @Override
    public boolean equals (Object other)
    {
        return other instanceof LineState && _lineWidth == ((LineState)other)._lineWidth;
    }

    @Override
    public int hashCode ()
    {
        return Float.floatToIntBits(_lineWidth);
    }

    /** The line width. */
    protected float _lineWidth;
}
