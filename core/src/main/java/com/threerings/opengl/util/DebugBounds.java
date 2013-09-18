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

import org.lwjgl.opengl.GL11;

import com.threerings.math.Box;
import com.threerings.math.Vector3f;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.RenderState;

/**
 * Renders bounding boxes for debugging purposes.
 */
public abstract class DebugBounds extends SimpleTransformable
{
    /**
     * Draws a single bounding box in the specified color.
     */
    public static void draw (Box bounds, Color4f color)
    {
        Vector3f min = bounds.getMinimumExtent();
        Vector3f max = bounds.getMaximumExtent();
        float lx = min.x, ly = min.y, lz = min.z;
        float ux = max.x, uy = max.y, uz = max.z;
        GL11.glColor4f(color.r, color.g, color.b, color.a);
        GL11.glBegin(GL11.GL_LINES);
        // bottom
        GL11.glVertex3f(lx, ly, lz);
        GL11.glVertex3f(lx, uy, lz);
        GL11.glVertex3f(lx, uy, lz);
        GL11.glVertex3f(ux, uy, lz);
        GL11.glVertex3f(ux, uy, lz);
        GL11.glVertex3f(ux, ly, lz);
        GL11.glVertex3f(ux, ly, lz);
        GL11.glVertex3f(lx, ly, lz);
        // sides
        GL11.glVertex3f(lx, ly, lz);
        GL11.glVertex3f(lx, ly, uz);
        GL11.glVertex3f(lx, uy, lz);
        GL11.glVertex3f(lx, uy, uz);
        GL11.glVertex3f(ux, uy, lz);
        GL11.glVertex3f(ux, uy, uz);
        GL11.glVertex3f(ux, ly, lz);
        GL11.glVertex3f(ux, ly, uz);
        // top
        GL11.glVertex3f(lx, ly, uz);
        GL11.glVertex3f(lx, uy, uz);
        GL11.glVertex3f(lx, uy, uz);
        GL11.glVertex3f(ux, uy, uz);
        GL11.glVertex3f(ux, uy, uz);
        GL11.glVertex3f(ux, ly, uz);
        GL11.glVertex3f(ux, ly, uz);
        GL11.glVertex3f(lx, ly, uz);
        GL11.glEnd();
    }

    /**
     * Creates a new set of debug bounds.
     */
    public DebugBounds (GlContext ctx)
    {
        super(ctx, RenderQueue.OPAQUE, 0, true, 0);
    }

    @Override
    protected RenderState[] createStates ()
    {
        RenderState[] states = super.createStates();
        states[RenderState.COLOR_STATE] = null;
        return states;
    }
}
