//
// $Id$

package com.threerings.opengl.util;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import com.threerings.math.Transform;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.AlphaState;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.renderer.state.TextureState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.renderer.util.BatchFactory;

/**
 * Renders an unlit reference grid on the XY plane, centered about the origin.
 */
public class Grid
    implements Renderable
{
    /**
     * Creates a new grid with the specified number of lines in each direction and the given
     * spacing between the lines.
     */
    public Grid (GlContext ctx, int lines, float spacing)
    {
        _ctx = ctx;

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
        _batch = BatchFactory.createLineBatch(ctx.getRenderer(), vbuf);

        // set the required render states
        RenderState[] states = _batch.getStates();
        states[RenderState.ALPHA_STATE] = AlphaState.OPAQUE;
        states[RenderState.COLOR_STATE] = new ColorState();
        states[RenderState.DEPTH_STATE] = DepthState.TEST_WRITE;
        states[RenderState.FOG_STATE] = FogState.DISABLED;
        states[RenderState.LIGHT_STATE] = LightState.DISABLED;
        states[RenderState.SHADER_STATE] = ShaderState.DISABLED;
        states[RenderState.TEXTURE_STATE] = TextureState.DISABLED;
        states[RenderState.TRANSFORM_STATE] = new TransformState();
    }

    /**
     * Returns a reference to the grid color.
     */
    public Color4f getColor ()
    {
        ColorState cstate = (ColorState)_batch.getStates()[RenderState.COLOR_STATE];
        return cstate.getColor();
    }

    /**
     * Returns a reference to the grid's world transform.
     */
    public Transform getTransform ()
    {
        return _transform;
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // update the transform state
        TransformState tstate = (TransformState)_batch.getStates()[RenderState.TRANSFORM_STATE];
        Transform modelview = tstate.getModelview();
        Renderer renderer = _ctx.getRenderer();
        renderer.getCamera().getViewTransform().compose(_transform, modelview);
        tstate.setDirty(true);

        // queue up the batch
        renderer.enqueueOpaque(_batch);
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The batch that we submit to the renderer. */
    protected SimpleBatch _batch;

    /** Our world transform. */
    protected Transform _transform = new Transform(Transform.UNIFORM);
}
