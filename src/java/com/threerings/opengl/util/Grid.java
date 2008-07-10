//
// $Id$

package com.threerings.opengl.util;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.SimpleBatch;
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
        _queue = ctx.getCompositor().getQueue();

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

    @Override // documentation inherited
    protected RenderState[] createStates ()
    {
        RenderState[] states = super.createStates();
        states[RenderState.COLOR_STATE] = new ColorState();
        return states;
    }
}
