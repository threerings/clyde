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
    /**
     * Compares two packed state keys.
     */
    public static int compareKeys (int[] k1, int[] k2)
    {
        int l1 = (k1 == null) ? 0 : k1.length;
        int l2 = (k2 == null) ? 0 : k2.length;
        int v1, v2, comp;
        for (int ii = 0, nn = Math.max(l1, l2); ii < nn; ii++) {
            v1 = (ii < l1) ? k1[ii] : 0;
            v2 = (ii < l2) ? k2[ii] : 0;
            if ((comp = v1 - v2) != 0) {
                return comp;
            }
        }
        return 0;
    }

    /** The batch's eye space z coordinate (negative if in front of the viewer). */
    public float depth;

    /** A comparable representation of the batch's state. */
    public int[] key;

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
