//
// $Id$

package com.threerings.tudey.data;

import java.util.Set;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.export.Exportable;
import com.threerings.util.DeepUtil;

/**
 * Superclass for objects in the scene.
 */
public abstract class SceneElement extends SimpleStreamableObject
    implements Exportable, Cloneable
{
    /**
     * Finds the resources required for this element that should be preloaded
     * and pinned in the cache.
     */
    public void getResources (Set<SceneResource> results)
    {
        // nothing by default
    }

    @Override // documentation inherited
    public Object clone ()
    {
        return DeepUtil.copy(this);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return DeepUtil.equals(this, other);
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return DeepUtil.hashCode(this);
    }
}
