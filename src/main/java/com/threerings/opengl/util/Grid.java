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

package com.threerings.opengl.util;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.util.BatchFactory;

/**
 * Renders an unlit reference grid on the XY plane, centered about the origin.
 */
public class Grid extends SimpleTransformable
{
    /**
     * Creates a new grid with the specified number of lines in each direction and the given
     * spacing between the lines.
     */
    public Grid (GlContext ctx, int lines, float spacing)
    {
        _ctx = ctx;
        _queue = ctx.getCompositor().getQueue(RenderQueue.OPAQUE);

        // create the batch containing the grid lines
        FloatBuffer vbuf = BufferUtils.createFloatBuffer(lines * 2 * 2 * 3);
        float extent = (lines - 1) * spacing * 0.5f;
        for (int ii = 0; ii < lines; ii++) {
            float y = ii * spacing - extent;
            vbuf.put(-extent).put(y).put(0f);
            vbuf.put(+extent).put(y).put(0f);
        }
        for (int ii = 0; ii < lines; ii++) {
            float x = ii * spacing - extent;
            vbuf.put(x).put(-extent).put(0f);
            vbuf.put(x).put(+extent).put(0f);
        }
        vbuf.rewind();
        _batch = BatchFactory.createLineBatch(_ctx.getRenderer(), vbuf);
        RenderState.copy(createStates(), _batch.getStates());
    }

    /**
     * Returns a reference to the grid color.
     */
    public Color4f getColor ()
    {
        ColorState cstate = (ColorState)_batch.getStates()[RenderState.COLOR_STATE];
        return cstate.getColor();
    }

    @Override
    protected RenderState[] createStates ()
    {
        RenderState[] states = super.createStates();
        states[RenderState.COLOR_STATE] = new ColorState();
        return states;
    }
}
