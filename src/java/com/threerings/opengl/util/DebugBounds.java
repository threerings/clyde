//
// $Id$

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

    @Override // documentation inherited
    protected RenderState[] createStates ()
    {
        RenderState[] states = super.createStates();
        states[RenderState.COLOR_STATE] = null;
        return states;
    }
}
