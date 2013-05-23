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

package com.threerings.tudey.client.util;

import org.lwjgl.opengl.GL11;

import com.threerings.math.Box;

import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.LineState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.scene.SimpleSceneElement;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * Draws a grid-aligned rectangle.
 */
public class RectangleElement extends SimpleSceneElement
{
    /**
     * Creates a new rectangle element.
     */
    public RectangleElement (GlContext ctx, boolean outline)
    {
        super(ctx);
        _outline = outline;
    }

    /**
     * Returns a reference to the box color.
     */
    public Color4f getColor ()
    {
        ColorState cstate = (ColorState)_batch.getStates()[RenderState.COLOR_STATE];
        return cstate.getColor();
    }

    /**
     * Returns a reference to the outlined region.
     */
    public Rectangle getRegion ()
    {
        return _region;
    }

    /**
     * Sets the elevation at which to draw the box.
     */
    public void setElevation (int elevation)
    {
        _elevation = elevation;
    }

    /**
     * Returns the elevation at which the box is being drawn.
     */
    public int getElevation ()
    {
        return _elevation;
    }

    @Override
    protected RenderState[] createStates ()
    {
        RenderState[] states = super.createStates();
        states[RenderState.COLOR_STATE] = new ColorState();
        states[RenderState.LINE_STATE] = LineState.getInstance(3f);
        return states;
    }

    @Override
    protected void computeBounds (Box result)
    {
        float z = TudeySceneMetrics.getTileZ(_elevation);
        result.getMinimumExtent().set(_region.x, _region.y, z);
        result.getMaximumExtent().set(_region.x + _region.width, _region.y + _region.height, z);
        result.transformLocal(_transform);
    }

    @Override
    protected void draw ()
    {
        float lx = _region.x, ux = _region.x + _region.width;
        float ly = _region.y, uy = _region.y + _region.height;
        float z = TudeySceneMetrics.getTileZ(_elevation);

        GL11.glBegin(_outline ? GL11.GL_LINE_LOOP : GL11.GL_POLYGON);
        GL11.glVertex3f(lx, ly, z);
        GL11.glVertex3f(lx, uy, z);
        GL11.glVertex3f(ux, uy, z);
        GL11.glVertex3f(ux, ly, z);
        GL11.glEnd();
    }

    /** The outlined region. */
    protected Rectangle _region = new Rectangle();

    /** The elevation at which to draw the region. */
    protected int _elevation;

    /** Whether or not to draw the rectangle in outline mode. */
    protected boolean _outline;
}
