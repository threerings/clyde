//
// $Id$

package com.threerings.opengl.renderer;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A batch that renders a list of sub-batches in order.
 */
public class CompoundBatch extends Batch
{
    /**
     * Creates an empty compound batch.
     */
    public CompoundBatch ()
    {
    }

    /**
     * Creates a compound batch to render the specified batches.
     */
    public CompoundBatch (Batch... batches)
    {
        Collections.addAll(_batches, batches);
    }

    /**
     * Returns a reference to the list of batches to render in order.
     */
    public ArrayList<Batch> getBatches ()
    {
        return _batches;
    }

    @Override // documentation inherited
    public boolean draw (Renderer renderer)
    {
        renderer.render(_batches);
        return false;
    }

    @Override // documentation inherited
    public int getPrimitiveCount ()
    {
        return 0; // the call to render(_batches) above counts the sub-batch primitives
    }

    /** The sub-batches of this batch. */
    protected ArrayList<Batch> _batches = new ArrayList<Batch>();
}
