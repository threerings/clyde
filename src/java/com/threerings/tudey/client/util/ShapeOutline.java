//
// $Id$

package com.threerings.tudey.client.util;

import org.lwjgl.opengl.GL11;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.DisplayList;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.LineState;
import com.threerings.opengl.renderer.state.PointState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.scene.SimpleSceneElement;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.config.ShapeConfig;

/**
 * Renders the outline of a shape.
 */
public class ShapeOutline extends SimpleSceneElement
{
    /**
     * Creates a new shape outline.
     */
    public ShapeOutline (GlContext ctx)
    {
        super(ctx);
        _list = new DisplayList(ctx.getRenderer());
    }

    /**
     * Sets the configuration of the shape to draw.
     */
    public void setConfig (ShapeConfig config)
    {
        // record the draw commands in our display list
        _list.begin();
        config.drawOutline();
        _list.end();
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
    protected void draw ()
    {
        // make sure we're in modelview matrix mode before calling the list
        _ctx.getRenderer().setMatrixMode(GL11.GL_MODELVIEW);
        _list.call();
    }

    /** The display list containing the shape draw commands. */
    protected DisplayList _list;
}
