//
// $Id$

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

    @Override // documentation inherited
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
