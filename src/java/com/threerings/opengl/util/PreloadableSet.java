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
     * Creates a new preloadable set.
     */
    public PreloadableSet ()
    {
    }

    /**
     * Preloads a batch of the default size.
     */
    public void preloadBatch (GlContext ctx)
    {
        preloadBatch(ctx, 10);
    }

    /**
     * Preloads a batch of resources in the set.
     *
     * @param count the (maximum) number of resources to preload.
     */
    public void preloadBatch (GlContext ctx, int count)
    {
        if (_remaining == null) {
            _remaining = iterator();
        }
        while (_remaining.hasNext() && count-- > 0) {
            _remaining.next().preload(ctx);
            _preloaded++;
        }
    }

    /**
     * Returns the number of resources preloaded so far.
     */
    public int getPreloaded ()
    {
        return _preloaded;
    }

    /** The iterator over the resources remaining to be preloaded. */
    protected Iterator<Preloadable> _remaining;

    /** The number of resources preloaded so far. */
    protected int _preloaded;
}
