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

import com.threerings.math.Box;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.LineState;
import com.threerings.opengl.renderer.state.PointState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.scene.SimpleSceneElement;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.shape.Shape;

/**
 * Draws a shape in the scene.
 */
public class ShapeSceneElement extends SimpleSceneElement
{
    /**
     * Creates a new shape scene element.
     */
    public ShapeSceneElement (GlContext ctx, boolean outline)
    {
        super(ctx);
        _outline = outline;
    }

    /**
     * Sets the shape to draw.
     */
    public void setShape (Shape shape)
    {
        _shape = shape;
        updateBounds();
    }

    /**
     * Returns a reference to the shape being drawn.
     */
    public Shape getShape ()
    {
        return _shape;
    }

    /**
     * Returns a reference to the color.
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
        states[RenderState.LINE_STATE] = LineState.getInstance(3f);
        states[RenderState.POINT_STATE] = PointState.getInstance(3f);
        return states;
    }

    @Override
    protected void computeBounds (Box result)
    {
        if (_shape == null) {
            result.setToEmpty();
            return;
        }
        Rect bounds = _shape.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        result.getMinimumExtent().set(min.x, min.y, 0f);
        result.getMaximumExtent().set(max.x, max.y, 0f);
        result.transformLocal(_transform);
    }

    @Override
    protected void draw ()
    {
        if (_shape != null) {
            _shape.draw(_outline);
        }
    }

    /** The shape to draw, or <code>null</code> for none. */
    protected Shape _shape;

    /** Whether or not to draw the shape in outline mode. */
    protected boolean _outline;
}
