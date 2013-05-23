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

package com.threerings.opengl.geometry;

import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.state.ArrayState;

/**
 * A simple geometry implementation that returns the same array state and draw command for all
 * passes.
 */
public class SimpleGeometry extends Geometry
{
    /**
     * For convenience, creates a geometry instance that calls the {@link #draw} method (which
     * must be overridden) to draw the geometry in immediate mode.
     */
    public SimpleGeometry ()
    {
        this(false, 0);
    }

    /**
     * For convenience, creates a geometry instance that calls the {@link #draw} method (which
     * must be overridden) to draw the geometry in immediate mode.
     *
     * @param modifiesColorState whether or not the draw method modifies the color state.
     * @param primitiveCount the primitive count to report to the renderer.
     */
    public SimpleGeometry (final boolean modifiesColorState, final int primitiveCount)
    {
        _drawCommand = new DrawCommand() {
            public boolean call () {
                draw();
                return modifiesColorState;
            }
            public int getPrimitiveCount () {
                return primitiveCount;
            }
        };
    }

    /**
     * Creates a geometry instance with the specified draw command and no array state.
     */
    public SimpleGeometry (DrawCommand drawCommand)
    {
        _drawCommand = drawCommand;
    }

    /**
     * Creates a geometry instance with the specified array state and draw command.
     */
    public SimpleGeometry (ArrayState arrayState, DrawCommand drawCommand)
    {
        _arrayState = arrayState;
        _drawCommand = drawCommand;
    }

    @Override
    public ArrayState getArrayState (int pass)
    {
        return _arrayState;
    }

    @Override
    public DrawCommand getDrawCommand (int pass)
    {
        return _drawCommand;
    }

    /**
     * Draws the geometry in immediate mode.
     */
    protected void draw ()
    {
        throw new RuntimeException("Override draw method to draw geometry.");
    }

    /** The array state for all passes. */
    protected ArrayState _arrayState;

    /** The draw command for all passes. */
    protected DrawCommand _drawCommand;
}
