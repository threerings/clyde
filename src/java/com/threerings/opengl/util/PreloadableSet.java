//
// $Id$

package com.threerings.opengl.util;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Contains a set of preloadables.
 */
public class PreloadableSet extends HashSet<Preloadable>
{
    /**
     * Preloads a batch of the default size.
     *
     * @return the percentage of the total resources loaded, from zero to one.
     */
    public float preloadBatch (GlContext ctx)
    {
        return preloadBatch(ctx, 10);
    }

    /**
     * Preloads a batch of resources in the set.
     *
     * @param count the (maximum) number of resources to preload.
     * @return the percentage of the total resources loaded, from zero to one.
     */
    public float preloadBatch (GlContext ctx, int count)
    {
        if (_remaining == null) {
            _remaining = iterator();
        }
        while (_remaining.hasNext() && count-- > 0) {
            _remaining.next().preload(ctx);
            _preloaded++;
        }
        float size = (float)size();
        return (size == 0f) ? 1f : (_preloaded / size);
    }

    /** The iterator over the resources remaining to be preloaded. */
    protected Iterator<Preloadable> _remaining;

    /** The number of resources preloaded so far. */
    protected int _preloaded;
}
