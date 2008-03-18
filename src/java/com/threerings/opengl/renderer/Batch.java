//
// $Id$

package com.threerings.opengl.renderer;

import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.renderer.state.TextureState;

/**
 * A geometry batch that can be queued for rendering.
 */
public abstract class Batch
    implements Cloneable
{
    /** The batch's eye space z coordinate (negative if in front of the viewer). */
    public float depth;

    /** A comparable representation of the batch's state. */
    public int[] key;

    /** The batch's ortho queue layer. */
    public int layer;

    /**
     * Draws this batch using the specified renderer.
     *
     * @return true if the batch changed the color state, in which case it should be invalidated.
     */
    public abstract boolean draw (Renderer renderer);

    /**
     * Returns the number of primitives in this batch (optional; returns zero by default).
     */
    public int getPrimitiveCount ()
    {
        return 0;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            Batch cbatch = (Batch)super.clone();
            cbatch.key = (key == null) ? null : key.clone();
            return cbatch;
        } catch (CloneNotSupportedException e) {
            return null; // should never happen
        }
    }
}
