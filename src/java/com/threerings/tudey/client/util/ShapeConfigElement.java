//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.DisplayList;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.LineState;
import com.threerings.opengl.renderer.state.PointState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.scene.SimpleSceneElement;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.shape.config.ShapeConfig;

/**
 * Draws a shape from its config.
 */
public class ShapeConfigElement extends SimpleSceneElement
{
    /**
     * Creates a new shape config element.
     */
    public ShapeConfigElement (GlContext ctx)
    {
        super(ctx);
    }

    /**
     * Sets the configuration of the shape to draw.
     *
     * @param outline if true, draw the outline of the shape; if false, draw the solid form.
     */
    public void setConfig (ShapeConfig config, boolean outline)
    {
        _localBounds = config.getBounds();
        _list = config.getList(_ctx, outline);
        updateBounds();
    }

    /**
     * Returns a reference to the outline color.
     */
    public Color4f getColor ()
    {
        ColorState cstate = (ColorState)_batch.getStates()[RenderState.COLOR_STATE];
        return cstate.getColor();
    }

    @Override // documentation inherited
    protected RenderState[] createStates ()
    {
        RenderState[] states = super.createStates();
        states[RenderState.COLOR_STATE] = new ColorState();
        states[RenderState.LINE_STATE] = LineState.getInstance(3f);
        states[RenderState.POINT_STATE] = PointState.getInstance(3f);
        return states;
    }

    @Override // documentation inherited
    protected Box getLocalBounds ()
    {
        return _localBounds;
    }

    @Override // documentation inherited
    protected void draw ()
    {
        // make sure we're in modelview matrix mode before calling the list
        _ctx.getRenderer().setMatrixMode(GL11.GL_MODELVIEW);
        _list.call();
    }

    /** The local bounds of the shape. */
    protected Box _localBounds;

    /** The display list containing the shape draw commands. */
    protected DisplayList _list;
}
